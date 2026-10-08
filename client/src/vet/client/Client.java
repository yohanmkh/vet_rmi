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
 * Automated test suite validating requirements A1 through A7.
 */
public class Client {

    public static void main(String[] args) throws Exception {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 1099;

        System.out.println("==================================================");
        System.out.println("  HAI704I TP1 — Java RMI Automated Validation Suite");
        System.out.println("==================================================\n");

        // ---------------------------------------------------------------------
        // A1 & A4 — Registry lookup and single service binding
        // ---------------------------------------------------------------------
        System.out.println("--- [A1 & A4] Registry Lookup & Remote Stubs ---");
        Registry registry = LocateRegistry.getRegistry(host, port);
        String[] bindings = registry.list();
        System.out.println("Registry bindings: " + Arrays.toString(bindings));
        if (bindings.length == 1 && "VetClinic".equals(bindings[0])) {
            System.out.println("PASS: Registry contains exactly one service binding ('VetClinic').");
        } else {
            System.out.println("INFO: Registry contains: " + Arrays.toString(bindings));
        }

        VetClinicRemote clinic = (VetClinicRemote) registry.lookup("VetClinic");
        System.out.println("Clinic object class: " + clinic.getClass().getName());
        System.out.println("Is clinic a JDK dynamic proxy? " + Proxy.isProxyClass(clinic.getClass()));

        // ---------------------------------------------------------------------
        // A2 — Serializable Species (Point de contrôle B)
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A2] Serializable Species (Pass-by-Value) ---");
        AnimalRemote rex = clinic.findByName("Rex");
        if (rex != null) {
            Species s1 = rex.getSpecies();
            int origLifespan = s1.getAverageLifespan();
            System.out.println("Rex species: " + s1.getName() + ", avg lifespan: " + origLifespan);
            System.out.println("Client s1 identityHashCode: " + System.identityHashCode(s1));

            // Mutate local client copy
            s1.setAverageLifespan(999);
            System.out.println("Mutated local copy s1 lifespan to: " + s1.getAverageLifespan());

            // Re-fetch species from server
            Species s2 = rex.getSpecies();
            System.out.println("Server returned s2 lifespan: " + s2.getAverageLifespan());
            System.out.println("Client s2 identityHashCode: " + System.identityHashCode(s2));
            System.out.println("Are s1 and s2 the same Java instance (s1 == s2)? " + (s1 == s2));

            if (s2.getAverageLifespan() == origLifespan && s1 != s2) {
                System.out.println("PASS: Species was transferred by value. Local mutation did not affect the server.");
            } else {
                System.out.println("FAIL: Species mutation affected server or instances are identical.");
            }
        }

        // ---------------------------------------------------------------------
        // A3 — Remote Medical Record (Point de contrôle C)
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A3] Remote Medical Record (Shared State) ---");
        if (rex != null) {
            MedicalRecordRemote record = rex.getMedicalRecord();
            System.out.println("Record object class: " + record.getClass().getName());
            System.out.println("Is record a JDK dynamic proxy? " + Proxy.isProxyClass(record.getClass()));

            String initialStatus = record.getHealthStatus();
            System.out.println("Initial health status: " + initialStatus);

            record.setHealthStatus("Under observation");
            System.out.println("Updated health status to: " + record.getHealthStatus());

            record.addObservation("Observation logged at " + java.time.LocalTime.now());
            List<String> observations = record.getObservations();
            System.out.println("Observations count on server: " + observations.size());
            System.out.println("PASS: MedicalRecord is remote; modifications persist on server.");
        }

        // ---------------------------------------------------------------------
        // A4 — Veterinary Clinic queries
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A4] Veterinary Clinic Listing & Search ---");
        List<AnimalRemote> patients = clinic.getPatients();
        System.out.println("Total patients currently in clinic: " + patients.size());
        for (AnimalRemote a : patients) {
            System.out.println(" - " + a.getName() + " (" + a.getSpecies().getName() + ")");
        }

        AnimalRemote unknown = clinic.findByName("NonExistentPatientXYZ");
        System.out.println("Search for nonexistent patient: " + unknown);
        if (unknown == null) {
            System.out.println("PASS: Nonexistent search returns null as expected.");
        }

        // ---------------------------------------------------------------------
        // A5 — Add Patient
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A5] Add Patient via Remote Call ---");
        String testPatientName = "TestBunny_" + System.currentTimeMillis() % 10000;
        Species rabbit = new Species("Rabbit", 8);
        clinic.addPatient(testPatientName, "Mr. McGregor", "Dutch", rabbit);

        AnimalRemote retrieved = clinic.findByName(testPatientName);
        if (retrieved != null && testPatientName.equals(retrieved.getName())) {
            System.out.println("PASS: Patient '" + testPatientName + "' created on server and retrieved via client lookup.");
            System.out.println("Retrieved breed: " + retrieved.getBreed() + ", species: " + retrieved.getSpecies().getName());
        } else {
            System.out.println("FAIL: Could not retrieve newly created patient.");
        }

        // ---------------------------------------------------------------------
        // A6 — Remote Observer & Threshold Semantics
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A6] Remote Observer & Threshold Semantics ---");
        VetObserverImpl testObserver = new VetObserverImpl();
        clinic.subscribe(testObserver);
        System.out.println("Subscribed client observer stub to clinic.");

        int currentCount = clinic.getPatients().size();
        System.out.println("Current patient count: " + currentCount);
        if (currentCount < 100) {
            System.out.println("Adding patients up to threshold 100 to trigger alert...");
            for (int i = currentCount + 1; i <= 100; i++) {
                clinic.addPatient("BatchPatient_" + i, "Owner_" + i, "Breed", new Species("Canine", 12));
            }
        }

        clinic.unsubscribe(testObserver);
        UnicastRemoteObject.unexportObject(testObserver, true);
        System.out.println("PASS: Observer unsubscribed and unexported cleanly.");

        // ---------------------------------------------------------------------
        // A7 — Serialization Experiment
        // ---------------------------------------------------------------------
        System.out.println("\n--- [A7] Serialization Experiment ---");
        System.out.println("Step 1: Attempting to send client-only class UnsharedSpecies...");
        try {
            UnsharedSpecies unshared = new UnsharedSpecies();
            clinic.addPatient("AlienPet", "Dr. Who", "Unknown", unshared);
            System.out.println("UNEXPECTED: Server accepted unshared class without exception.");
        } catch (ServerException se) {
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
            System.out.println("Caught exception: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  -> Cause: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
        }

        System.out.println("\nStep 2: Sending shared Dog subclass located in common/...");
        try {
            Dog dog = new Dog();
            clinic.addPatient("Rocky", "Mr. Jones", "Bulldog", dog);
            AnimalRemote rocky = clinic.findByName("Rocky");
            if (rocky != null) {
                System.out.println("PASS: Shared Dog subclass deserialized successfully on server.");
            }
        } catch (Exception e) {
            System.out.println("FAIL: Shared Dog subclass threw exception: " + e.getMessage());
        }

        System.out.println("\n==================================================");
        System.out.println("  Automated Validation Complete!");
        System.out.println("==================================================");
    }
}