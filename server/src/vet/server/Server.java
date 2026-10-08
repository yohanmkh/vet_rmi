package vet.server;

import vet.common.Species;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    public static void main(String[] args) throws Exception {
        int port = 1099;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default 1099.");
            }
        }

        // Create the clinic
        VetClinicImpl clinic = new VetClinicImpl();

        // Add initial patients
        Species dog = new Species("Dog", 13);
        Species cat = new Species("Cat", 15);
        clinic.addPatientLocally(new AnimalImpl("Rex", "Dr. Martin", "Labrador", dog));
        clinic.addPatientLocally(new AnimalImpl("Whiskers", "Mrs. Dupont", "Siamese", cat));

        // Create registry or reuse existing if rmiregistry is running
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (Exception e) {
            registry = LocateRegistry.getRegistry(port);
        }

        registry.rebind("VetClinic", clinic);
        System.out.println("Server ready on port " + port + ". Clinic published.");
    }
}