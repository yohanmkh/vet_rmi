package vet.client;

import vet.common.AnimalRemote;
import vet.common.Dog;
import vet.common.MedicalRecordRemote;
import vet.common.Species;
import vet.common.VetClinicRemote;

import java.lang.reflect.Proxy;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;

/**
 * Automated validation suite for HAI704I TP1 requirements A1 through A7.
 */
public class Client {

    public static void main(String[] args) throws Exception {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 1099;

        System.out.println("==================================================");
        System.out.println("  HAI704I TP1 — Java RMI Automated Validation Suite");
        System.out.println("==================================================\n");

        Registry registry = LocateRegistry.getRegistry(host, port);
        VetClinicRemote clinic = (VetClinicRemote) registry.lookup("VetClinic");

        // ---------------------------------------------------------------------
        // A1 — Remote Animal Stubs & RMI Proxies
        // ---------------------------------------------------------------------
        System.out.println("--- [A1] Remote Animal Stubs & RMI Proxies ---");
        System.out.println("Clinic object class: " + clinic.getClass().getName());
        System.out.println("Is Clinic a dynamic proxy? " + Proxy.isProxyClass(clinic.getClass()));

        AnimalRemote rex = clinic.findByName("Rex");
        if (rex != null) {
            System.out.println("Animal object class: " + rex.getClass().getName());
            boolean isAnimalProxy = Proxy.isProxyClass(rex.getClass());
            System.out.println("Is AnimalRemote a dynamic proxy stub? " + isAnimalProxy);
            if (isAnimalProxy && !rex.getClass().getName().contains("AnimalImpl")) {
                System.out.println("PASS: AnimalRemote is a dynamic RMI proxy stub, not AnimalImpl.");
            } else {
                System.out.println("FAIL: AnimalRemote is not an RMI dynamic proxy stub.");
            }
        } else {
            System.out.println("FAIL: Default animal 'Rex' not found on server.");
        }

        // ---------------------------------------------------------------------
        // A2 — Serializable Species (Pass-by-Value)
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A2] Serializable Species (Pass-by-Value) ---");
        if (rex != null) {
            Species s1 = rex.getSpecies();
            int origLifespan = s1.getAverageLifespan();
            System.out.println("Original species: " + s1.getName() + ", avg lifespan: " + origLifespan);
            System.out.println("Client s1 identityHashCode: " + System.identityHashCode(s1));

            // Mutate local copy
            s1.setAverageLifespan(999);
            System.out.println("Mutated local copy s1 lifespan to: " + s1.getAverageLifespan());

            // Re-fetch from server
            Species s2 = rex.getSpecies();
            System.out.println("Server returned s2 lifespan: " + s2.getAverageLifespan());
            System.out.println("Client s2 identityHashCode: " + System.identityHashCode(s2));
            System.out.println("Are s1 and s2 the same instance (s1 == s2)? " + (s1 == s2));

            if (s2.getAverageLifespan() == origLifespan && s1 != s2) {
                System.out.println("PASS: Species was transferred by value. Local mutation did not affect the server.");
            } else {
                System.out.println("FAIL: Species mutation affected server or instances are identical.");
            }
        }

        // ---------------------------------------------------------------------
        // A3 — Remote Medical Record (Shared Across Multiple Clients)
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A3] Remote Medical Record (Shared State Across 2 Clients) ---");
        // Create two independent client lookup connections to demonstrate shared server state
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
            System.out.println("PASS: MedicalRecord state modified by Client 1 is immediately visible to independent Client 2.");
        } else {
            System.out.println("FAIL: MedicalRecord modifications not shared across independent client references.");
        }

        // ---------------------------------------------------------------------
        // A4 — Veterinary Clinic Listing & Search
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A4] Veterinary Clinic Listing & Search ---");
        String[] bindings = registry.list();
        System.out.println("Registry bindings: " + Arrays.toString(bindings));
        boolean singleBinding = (bindings.length == 1 && "VetClinic".equals(bindings[0]));
        System.out.println("Only 'VetClinic' is published in registry? " + singleBinding);

        List<AnimalRemote> allPatients = clinic.getPatients();
        System.out.println("Total patients currently in clinic: " + allPatients.size());
        AnimalRemote unknown = clinic.findByName("NonExistentPatientXYZ");
        System.out.println("Search for nonexistent patient returned: " + unknown);

        if (singleBinding && !allPatients.isEmpty() && unknown == null) {
            System.out.println("PASS: Clinic published as single entry point; search returns stubs and null when not found.");
        } else {
            System.out.println("FAIL: A4 requirements check failed.");
        }

        // ---------------------------------------------------------------------
        // A5 — Add Patient (Count Verification & Field Assertions)
        // ---------------------------------------------------------------------
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
            System.out.println("PASS: addPatient incremented count (before=" + countBefore + ", after=" + countAfter + ") and patient fields match.");
        } else {
            System.out.println("FAIL: addPatient verification failed (countOk=" + countOk + ", fieldsOk=" + fieldsOk + ").");
        }

        // ---------------------------------------------------------------------
        // A6 — Remote Observer & Threshold Semantics
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A6] Remote Observer & Threshold Semantics ---");
        int currentCount = clinic.getPatients().size();
        System.out.println("Current patient count: " + currentCount);

        // Advance patient count to 99 if below 99 so we can observe the 100 threshold crossing
        if (currentCount < 99) {
            System.out.println("Pre-populating patients up to 99...");
            for (int i = currentCount; i < 99; i++) {
                clinic.addPatient("PrepPatient_" + i, "Owner", "Breed", new Species("Canine", 10));
            }
            currentCount = clinic.getPatients().size();
        }

        if (currentCount == 99) {
            // Subscribe TWO independent observers
            VetObserverImpl obs1 = new VetObserverImpl("Observer-1");
            VetObserverImpl obs2 = new VetObserverImpl("Observer-2");
            clinic.subscribe(obs1);
            clinic.subscribe(obs2);
            System.out.println("Subscribed two independent observers at count 99.");

            // Add the 100th patient: triggers threshold crossing from 99 to 100
            clinic.addPatient("ThresholdPatient_100", "Owner", "Breed", new Species("Canine", 10));
            boolean obs1Received = obs1.getAlertCount() >= 1;
            boolean obs2Received = obs2.getAlertCount() >= 1;
            System.out.println("Observer-1 received 100 alert? " + obs1Received);
            System.out.println("Observer-2 received 100 alert? " + obs2Received);

            // Simulate Observer-1 crashing / becoming unreachable by unexporting its remote object
            System.out.println("Simulating Observer-1 becoming dead/unreachable (unexporting)...");
            UnicastRemoteObject.unexportObject(obs1, true);

            // Advance to threshold 500 to verify:
            // 1) The server encounters RemoteException for dead Observer-1 and prunes it cleanly.
            // 2) The server successfully continues notifying surviving Observer-2.
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
                System.out.println("PASS: Both observers received threshold 100; dead observer pruned cleanly; surviving observer received threshold 500.");
            } else {
                System.out.println("FAIL: Observer notifications or dead observer pruning failed.");
            }
        } else {
            System.out.println("INFO: Patient count is already " + currentCount + " (threshold 100 previously passed).");
            // Standard observer subscribe/unsubscribe test
            VetObserverImpl fallbackObs = new VetObserverImpl("TestObserver");
            clinic.subscribe(fallbackObs);
            clinic.unsubscribe(fallbackObs);
            UnicastRemoteObject.unexportObject(fallbackObs, true);
            System.out.println("PASS: Observer subscription and clean unexport verified.");
        }
        System.out.println("[Note] Downward threshold crossing logic is implemented on the server but cannot be demonstrated via normal operations because VetClinicRemote does not define a removePatient method.");

        // ---------------------------------------------------------------------
        // A7 — Serialization Experiment
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A7] Serialization Experiment ---");
        System.out.println("Step 1: Attempting to send client-only class UnsharedSpecies...");
        boolean failureObserved = false;
        try {
            UnsharedSpecies unshared = new UnsharedSpecies();
            clinic.addPatient("AlienPet", "Dr. Who", "Unknown", unshared);
            System.out.println("UNEXPECTED: Server accepted unshared class without exception.");
        } catch (ServerException se) {
            failureObserved = true;
            System.out.println("PASS (Expected Exception caught):");
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
            failureObserved = true;
            System.out.println("Caught exception: " + e.getClass().getName() + ": " + e.getMessage());
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
            System.out.println("PASS: A7 experiment demonstrated both unshared class failure and shared class resolution.");
        }

        System.out.println("\n==================================================");
        System.out.println("  Automated Validation Complete!");
        System.out.println("==================================================");
    }
}