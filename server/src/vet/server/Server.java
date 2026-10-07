package vet.server;

import vet.common.AnimalRemote;
import vet.common.Species;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import vet.common.VetClinicRemote;

import java.util.ArrayList;

public class Server {
    public static void main(String[] args) throws Exception {

        // Create the clinic
        VetClinicImpl clinic = new VetClinicImpl();

        // Add some initial patients
        Species dog = new Species("Dog", 13);
        Species cat = new Species("Cat", 15);
        clinic.addPatientLocally(new AnimalImpl("Rex", "Dr. Martin", "Labrador", dog));
        clinic.addPatientLocally(new AnimalImpl("Whiskers", "Mrs. Dupont", "Siamese", cat));
        // Registry — only one binding now
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind("VetClinic", clinic);

        System.out.println("Server ready. Clinic published.");
    }
}