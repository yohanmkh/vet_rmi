package vet.client;

import vet.common.AnimalRemote;
import vet.common.Dog;
import vet.common.MedicalRecordRemote;
import vet.common.Species;
import vet.common.VetClinicRemote;
import vet.common.VetObserverRemote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class Client {
    public static void main(String[] args) throws Exception {

        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        VetClinicRemote clinic = (VetClinicRemote) registry.lookup("VetClinic");

        // existing A4 code
        List<AnimalRemote> patients = clinic.getPatients();
        System.out.println("Patients (" + patients.size() + "):");
        for (AnimalRemote a : patients) {
            System.out.println(" - " + a.getName() + " (" + a.getSpecies() + ")");
        }

        AnimalRemote found = clinic.findByName("Rex");
        if (found != null) {
            System.out.println("Found: " + found.getName());
            System.out.println("Health: " + found.getMedicalRecord().getHealthStatus());
        } else {
            System.out.println("Not found.");
        }

        AnimalRemote notFound = clinic.findByName("Unknown");
        System.out.println("Unknown search result: " + notFound);
        System.out.println("Registry bindings: " + java.util.Arrays.toString(registry.list()));

        // A5 — Add a new patient from the client
        Species rabbit = new Species("Rabbit", 8);
        clinic.addPatient("Bugs", "Mr. Smith", "Angora", rabbit);

        System.out.println("\nAfter adding Bugs:");
        for (AnimalRemote a : clinic.getPatients()) {
            System.out.println(" - " + a.getName());
        }

        AnimalRemote bugs = clinic.findByName("Bugs");
        System.out.println("Found: " + bugs.getName());
        System.out.println("Health: " + bugs.getMedicalRecord().getHealthStatus());

        // A6 — Subscribe to alerts
        VetObserverImpl observer = new VetObserverImpl();
        clinic.subscribe(observer);
        System.out.println("Subscribed to alerts. Waiting 10 seconds...");
        Thread.sleep(10000);

        // Get current count and add until we hit 100
        int currentCount = clinic.getPatients().size();
        System.out.println("Current patient count: " + currentCount);
        for (int i = currentCount + 1; i <= 100; i++) {
            clinic.addPatient("Patient" + i, "Owner" + i, "Breed" + i, new Species("Dog", 10));
        }

        clinic.unsubscribe(observer);
        System.out.println("Unsubscribed.");
        UnicastRemoteObject.unexportObject(observer, true);

        // A7 — Serialization experiment
        Dog dog = new Dog();
        clinic.addPatient("Rocky", "Mr. Jones", "Bulldog", dog);
        System.out.println("Rocky added successfully.");
    }

}