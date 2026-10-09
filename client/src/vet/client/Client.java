package vet.client;

import vet.common.AnimalRemote;
import vet.common.Dog;
import vet.common.MedicalRecordRemote;
import vet.common.Species;
import vet.common.VetClinicRemote;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
public class Client {

    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && "a3-writer".equalsIgnoreCase(args[0])) {
            runA3Writer(args);
            return;
        }
        if (args.length >= 1 && "a3-reader".equalsIgnoreCase(args[0])) {
            runA3Reader(args);
            return;
        }
        if (args.length >= 1 && "observer".equalsIgnoreCase(args[0])) {
            runObserverProcess(args);
            return;
        }
        if (args.length >= 1 && "populate".equalsIgnoreCase(args[0])) {
            runPopulate(args);
            return;
        }

        runTestSuite(args);
    }

    private static void runA3Writer(String[] args) throws Exception {
        String patient = args.length >= 2 ? args[1] : "Rex";
        String status = args.length >= 3 ? args[2] : "Treated by Process 1";
        String observation = args.length >= 4 ? args[3] : "Observation from Process 1";
        String host = args.length >= 5 ? args[4] : "localhost";
        int port = args.length >= 6 ? Integer.parseInt(args[5]) : 1099;

        VetClinicRemote clinic = (VetClinicRemote) LocateRegistry.getRegistry(host, port).lookup("VetClinic");
        AnimalRemote animal = clinic.findByName(patient);
        if (animal == null) {
            System.err.println("[A3-Writer] Patient '" + patient + "' not found.");
            System.exit(1);
        }
        MedicalRecordRemote record = animal.getMedicalRecord();
        record.setHealthStatus(status);
        record.addObservation(observation);
        System.out.println("[Client-1 (PID " + ProcessHandle.current().pid() + ")] Updated " + patient + " healthStatus='" + status + "', added observation='" + observation + "'");
    }

    private static void runA3Reader(String[] args) throws Exception {
        String patient = args.length >= 2 ? args[1] : "Rex";
        String expectedStatus = args.length >= 3 ? args[2] : "Treated by Process 1";
        String expectedObs = args.length >= 4 ? args[3] : "Observation from Process 1";
        String host = args.length >= 5 ? args[4] : "localhost";
        int port = args.length >= 6 ? Integer.parseInt(args[5]) : 1099;

        VetClinicRemote clinic = (VetClinicRemote) LocateRegistry.getRegistry(host, port).lookup("VetClinic");
        AnimalRemote animal = clinic.findByName(patient);
        if (animal == null) {
            System.err.println("[A3-Reader] Patient '" + patient + "' not found.");
            System.exit(1);
        }
        MedicalRecordRemote record = animal.getMedicalRecord();
        String actualStatus = record.getHealthStatus();
        List<String> actualObs = record.getObservations();

        System.out.println("[Client-2 (PID " + ProcessHandle.current().pid() + ")] Read " + patient + " healthStatus='" + actualStatus + "', total observations=" + actualObs.size());

        boolean statusMatch = expectedStatus.equals(actualStatus);
        boolean obsMatch = actualObs.contains(expectedObs);

        if (statusMatch && obsMatch) {
            System.out.println("[A3] PASS (Multi-process verification: changes made by Client 1 observed by Client 2 in distinct JVM)");
            System.exit(0);
        } else {
            System.err.println("[A3] FAIL (Expected status='" + expectedStatus + "' got='" + actualStatus + "', obsFound=" + obsMatch + ")");
            System.exit(1);
        }
    }

    private static void runObserverProcess(String[] args) throws Exception {
        String name = args.length >= 2 ? args[1] : "ObserverProcess";
        int durationSec = args.length >= 3 ? Integer.parseInt(args[2]) : 15;
        String host = args.length >= 4 ? args[3] : "localhost";
        int port = args.length >= 5 ? Integer.parseInt(args[4]) : 1099;

        VetClinicRemote clinic = (VetClinicRemote) LocateRegistry.getRegistry(host, port).lookup("VetClinic");
        String logFile = "/tmp/vet_obs_" + name + ".log";

        VetObserverImpl observer = new VetObserverImpl(name) {
            @Override
            public void onAlert(String message) throws java.rmi.RemoteException {
                super.onAlert(message);
                try (FileWriter fw = new FileWriter(logFile, true);
                     PrintWriter pw = new PrintWriter(fw)) {
                    pw.println(message);
                } catch (Exception ignored) {
                }
            }
        };

        clinic.subscribe(observer);
        System.out.println("[" + name + " (PID " + ProcessHandle.current().pid() + ")] Subscribed. Listening for alerts for " + durationSec + "s (logging to " + logFile + ")...");

        try {
            Thread.sleep(durationSec * 1000L);
        } catch (InterruptedException ignored) {
        }

        try {
            clinic.unsubscribe(observer);
        } catch (Exception ignored) {
        }
        UnicastRemoteObject.unexportObject(observer, true);
        System.out.println("[" + name + "] Unsubscribed and unexported cleanly.");
    }

    private static void runPopulate(String[] args) throws Exception {
        int targetCount = args.length >= 2 ? Integer.parseInt(args[1]) : 100;
        String host = args.length >= 3 ? args[2] : "localhost";
        int port = args.length >= 4 ? Integer.parseInt(args[3]) : 1099;

        VetClinicRemote clinic = (VetClinicRemote) LocateRegistry.getRegistry(host, port).lookup("VetClinic");
        int current = clinic.getPatients().size();
        System.out.println("[Populate] Current patient count: " + current + ", target: " + targetCount);
        for (int i = current; i < targetCount; i++) {
            clinic.addPatient("Batch_" + i, "Owner", "Breed", new Species("Canine", 10));
        }
        System.out.println("[Populate] Done. Total patients: " + clinic.getPatients().size());
    }

    private static void runTestSuite(String[] args) throws Exception {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 1099;

        System.out.println("==================================================");
        System.out.println("  HAI704I TP1 — Programme de validation RMI");
        System.out.println("==================================================\n");

        Registry registry = LocateRegistry.getRegistry(host, port);
        VetClinicRemote clinic = (VetClinicRemote) registry.lookup("VetClinic");

        System.out.println("--- [A1] Remote Animal Stubs & RMI Proxies ---");
        System.out.println("Clinic object class: " + clinic.getClass().getName());
        System.out.println("Is Clinic a dynamic proxy? " + Proxy.isProxyClass(clinic.getClass()));

        AnimalRemote rex = clinic.findByName("Rex");
        if (rex != null) {
            System.out.println("Animal object class: " + rex.getClass().getName());
            boolean isAnimalProxy = Proxy.isProxyClass(rex.getClass());
            System.out.println("Is AnimalRemote a dynamic proxy stub? " + isAnimalProxy);
            if (isAnimalProxy && !rex.getClass().getName().contains("AnimalImpl")) {
                System.out.println("[A1] PASS: AnimalRemote is a dynamic RMI proxy stub (not AnimalImpl).");
            } else {
                System.out.println("[A1] FAIL: AnimalRemote is not an RMI dynamic proxy stub.");
            }
        } else {
            System.out.println("[A1] FAIL: Default animal 'Rex' not found on server.");
        }

        System.out.println("\n--- [A2] Serializable Species (Pass-by-Value) ---");
        if (rex != null) {
            Species s1 = rex.getSpecies();
            int origLifespan = s1.getAverageLifespan();
            System.out.println("Original species: " + s1.getName() + ", avg lifespan: " + origLifespan);
            System.out.println("Client s1 identityHashCode: " + System.identityHashCode(s1));

            s1.setAverageLifespan(999);
            System.out.println("Mutated local copy s1 lifespan to: " + s1.getAverageLifespan());

            Species s2 = rex.getSpecies();
            System.out.println("Server returned s2 lifespan: " + s2.getAverageLifespan());
            System.out.println("Client s2 identityHashCode: " + System.identityHashCode(s2));
            System.out.println("Are s1 and s2 the same instance (s1 == s2)? " + (s1 == s2));

            if (s2.getAverageLifespan() == origLifespan && s1 != s2) {
                System.out.println("[A2] PASS: Species transferred by value. Local mutation did not affect the server.");
            } else {
                System.out.println("[A2] FAIL: Species mutation affected server or instances are identical.");
            }
        }

        System.out.println("\n--- [A3] Remote Medical Record (Shared State) ---");
        VetClinicRemote client1Clinic = (VetClinicRemote) LocateRegistry.getRegistry(host, port).lookup("VetClinic");
        VetClinicRemote client2Clinic = (VetClinicRemote) LocateRegistry.getRegistry(host, port).lookup("VetClinic");

        AnimalRemote animalFromClient1 = client1Clinic.findByName("Rex");
        AnimalRemote animalFromClient2 = client2Clinic.findByName("Rex");

        MedicalRecordRemote recordClient1 = animalFromClient1.getMedicalRecord();
        MedicalRecordRemote recordClient2 = animalFromClient2.getMedicalRecord();

        System.out.println("Client 1 record stub: " + recordClient1.getClass().getName());
        System.out.println("Client 2 record stub: " + recordClient2.getClass().getName());
        System.out.println("Is record a dynamic proxy? " + Proxy.isProxyClass(recordClient1.getClass()));

        String newStatus = "Treated & Vaccinated #" + (System.currentTimeMillis() % 10000);
        System.out.println("Client 1 updates health status to: " + newStatus);
        recordClient1.setHealthStatus(newStatus);

        String readStatusByClient2 = recordClient2.getHealthStatus();
        System.out.println("Client 2 reads health status: " + readStatusByClient2);

        String testObs = "Observation logged by Client 1 at " + java.time.LocalTime.now();
        recordClient1.addObservation(testObs);
        List<String> obsReadByClient2 = recordClient2.getObservations();
        System.out.println("Client 2 reads total observations: " + obsReadByClient2.size());
        boolean obsFound = obsReadByClient2.contains(testObs);

        if (newStatus.equals(readStatusByClient2) && obsFound) {
            System.out.println("[A3] PASS (two stubs in one JVM only). For two real JVMs use the a3-writer / a3-reader commands from the README.");
        } else {
            System.out.println("[A3] FAIL: MedicalRecord modifications not shared.");
        }

        System.out.println("\n--- [A4] Veterinary Clinic Listing & Search ---");
        String[] bindings = registry.list();
        System.out.println("Registry bindings: " + Arrays.toString(bindings));
        boolean singleBinding = (bindings.length == 1 && "VetClinic".equals(bindings[0]));
        System.out.println("Only 'VetClinic' is published in registry? " + singleBinding);

        List<AnimalRemote> allPatients = clinic.getPatients();
        System.out.println("Total patients currently in clinic: " + allPatients.size());
        AnimalRemote unknown = clinic.findByName("NonExistentPatientXYZ");
        System.out.println("Search for nonexistent patient returned: " + unknown);

        boolean patientsAreStubs = !allPatients.isEmpty() && Proxy.isProxyClass(allPatients.get(0).getClass());
        System.out.println("getPatients() elements are dynamic proxies? " + patientsAreStubs);
        if (singleBinding && patientsAreStubs && unknown == null) {
            System.out.println("[A4] PASS: Clinic published as single entry point; search returns stubs and null when not found.");
        } else {
            System.out.println("[A4] FAIL: A4 requirements check failed.");
        }

        System.out.println("\n--- [A5] Add Patient via Remote Call ---");
        int countBefore = clinic.getPatients().size();
        String testPatientName = "Patient_A5_" + (System.currentTimeMillis() % 10000);
        Species hamster = new Species("Hamster", 3);

        clinic.addPatient(testPatientName, "Alice", "Roborovski", hamster);
        int countAfter = clinic.getPatients().size();
        System.out.println("Patient count before: " + countBefore + ", after: " + countAfter);

        AnimalRemote retrieved = clinic.findByName(testPatientName);
        boolean countOk = (countAfter == countBefore + 1);
        boolean fieldsOk = (retrieved != null
                && testPatientName.equals(retrieved.getName())
                && "Alice".equals(retrieved.getOwnerName())
                && "Roborovski".equals(retrieved.getBreed())
                && retrieved.getSpecies() != null
                && "Hamster".equals(retrieved.getSpecies().getName())
                && retrieved.getSpecies().getAverageLifespan() == 3);

        if (countOk && fieldsOk) {
            System.out.println("[A5] PASS: addPatient incremented count (before=" + countBefore + ", after=" + countAfter + ") and patient fields match.");
        } else {
            System.out.println("[A5] FAIL: addPatient verification failed (countOk=" + countOk + ", fieldsOk=" + fieldsOk + ").");
        }

        System.out.println("\n--- [A6] Remote Observer & Threshold Semantics ---");
        int currentCount = clinic.getPatients().size();
        System.out.println("Current patient count: " + currentCount);

        if (currentCount < 99) {
            System.out.println("Pre-populating patients up to 99...");
            for (int i = currentCount; i < 99; i++) {
                clinic.addPatient("PrepPatient_" + i, "Owner", "Breed", new Species("Canine", 10));
            }
            currentCount = clinic.getPatients().size();
        }

        if (currentCount == 99) {
            VetObserverImpl obs1 = new VetObserverImpl("Observer-1");
            VetObserverImpl obs2 = new VetObserverImpl("Observer-2");
            clinic.subscribe(obs1);
            clinic.subscribe(obs2);
            System.out.println("Subscribed two independent observers at count 99.");

            clinic.addPatient("ThresholdPatient_100", "Owner", "Breed", new Species("Canine", 10));
            boolean obs1Received = obs1.getAlertCount() >= 1;
            boolean obs2Received = obs2.getAlertCount() >= 1;
            System.out.println("Observer-1 received 100 alert? " + obs1Received);
            System.out.println("Observer-2 received 100 alert? " + obs2Received);

            System.out.println("Simulating Observer-1 becoming dead/unreachable (unexporting)...");
            UnicastRemoteObject.unexportObject(obs1, true);

            int preCount500 = clinic.getPatients().size();
            System.out.println("Adding patients to cross threshold 500 (from " + preCount500 + " to 500)...");
            for (int i = preCount500; i < 500; i++) {
                clinic.addPatient("Batch500_" + i, "Owner", "Breed", new Species("Feline", 12));
            }

            boolean obs2Received500 = obs2.getAlertCount() >= 2;
            System.out.println("Surviving Observer-2 received threshold 500 alert? " + obs2Received500);

            clinic.unsubscribe(obs2);
            UnicastRemoteObject.unexportObject(obs2, true);

            if (obs1Received && obs2Received && obs2Received500) {
                System.out.println("[A6] PASS (single JVM, upward only): both observers received threshold 100; surviving observer received threshold 500. Server-side pruning of a killed client is checked with separate JVMs (see README).");
            } else {
                System.out.println("[A6] FAIL: Observer notifications or dead observer pruning failed.");
            }
        } else {
            System.out.println("[A6] MANUAL CHECK REQUIRED: server already holds " + currentCount + " patients, so the threshold 100 crossing cannot be reproduced. Restart the server and rerun.");
        }
        System.out.println("[Note] Downward threshold crossing is implemented on the server but cannot be demonstrated via normal operations because VetClinicRemote does not define a removePatient method.");

        System.out.println("\n--- [A7] Serialization Experiment ---");
        System.out.println("Step 1: Attempting to send client-only class UnsharedSpecies...");
        boolean failureObserved = false;
        try {
            UnsharedSpecies unshared = new UnsharedSpecies();
            clinic.addPatient("AlienPet", "Dr. Who", "Unknown", unshared);
            System.out.println("UNEXPECTED: Server accepted unshared class without exception.");
        } catch (ServerException se) {
            Throwable root = se;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            failureObserved = root instanceof ClassNotFoundException
                    && root.getMessage() != null
                    && root.getMessage().contains("vet.client.UnsharedSpecies");
            System.out.println(failureObserved ? "Expected failure caught (root cause is ClassNotFoundException for the client-only class):" : "Unexpected ServerException root cause: " + root);
            System.out.println("  ServerException: " + se.getMessage());
            if (se.getCause() instanceof UnmarshalException) {
                UnmarshalException ue = (UnmarshalException) se.getCause();
                System.out.println("  -> Nested UnmarshalException: " + ue.getMessage());
                if (ue.getCause() instanceof ClassNotFoundException) {
                    System.out.println("  -> Root ClassNotFoundException: " + ue.getCause().getMessage());
                }
            }
            System.out.println("Explanation: Server JVM cannot deserialize vet.client.UnsharedSpecies because it is missing from the server classpath.");
        } catch (Exception e) {
            System.out.println("Unexpected exception (not the expected failure): " + e.getClass().getName() + ": " + e.getMessage());
        }

        System.out.println("\nStep 2: Sending shared Dog subclass located in common/...");
        boolean successObserved = false;
        try {
            Dog dog = new Dog();
            clinic.addPatient("Rocky", "Mr. Jones", "Bulldog", dog);
            AnimalRemote rocky = clinic.findByName("Rocky");
            if (rocky != null) {
                successObserved = true;
                System.out.println("PASS: Shared Dog subclass deserialized successfully on server.");
            }
        } catch (Exception e) {
            System.out.println("FAIL: Shared Dog subclass threw exception: " + e.getMessage());
        }

        if (failureObserved && successObserved) {
            System.out.println("[A7] PASS: Experiment demonstrated unshared class failure and shared class resolution.");
        } else {
            System.out.println("[A7] FAIL: Serialization experiment incomplete.");
        }

        System.out.println("\n--- [A8] Interactive CLI ---");
        System.out.println("[A8] MANUAL CHECK REQUIRED: the CLI is interactive. Run 'java -cp common/out:client/out vet.client.Main'.");

        System.out.println("\n==================================================");
        System.out.println("  Fin des tests de validation.");
        System.out.println("==================================================");
    }
}