package vet.client;

import vet.common.AnimalRemote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

/**
 * CliConsole — interactive menu loop.
 * Zero direct RMI calls; delegates everything to CliLogic.
 * Only java.rmi import allowed: RemoteException.
 */
public class CliConsole {

    private static final String MENU =
            "\n=== Vet Clinic ===\n" +
            "1. List all patients\n" +
            "2. Search patient by name\n" +
            "3. Add new patient\n" +
            "4. View medical record\n" +
            "5. Update health status\n" +
            "6. Add observation\n" +
            "7. Subscribe to alerts\n" +
            "8. Unsubscribe from alerts\n" +
            "9. Quit\n" +
            "Choice: ";

    private final CliLogic logic;
    private final Scanner scanner;

    public CliConsole(CliLogic logic) {
        this.logic = logic;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            System.out.print(MENU);
            String line = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input — please enter a number between 1 and 9.");
                continue;
            }

            switch (choice) {
                case 1: handleListPatients();       break;
                case 2: handleSearch();             break;
                case 3: handleAddPatient();         break;
                case 4: handleViewMedicalRecord();  break;
                case 5: handleUpdateHealth();       break;
                case 6: handleAddObservation();     break;
                case 7: handleSubscribe();          break;
                case 8: handleUnsubscribe();        break;
                case 9: handleQuit(); return;
                default:
                    System.out.println("Unknown option — choose between 1 and 9.");
            }
        }
    }

    // ── Handlers ───────────────────────────────────────────────────────────────

    private void handleListPatients() {
        try {
            List<AnimalRemote> patients = logic.getPatients();
            if (patients.isEmpty()) {
                System.out.println("No patients registered.");
                return;
            }
            System.out.println("Patients (" + patients.size() + "):");
            for (AnimalRemote a : patients) {
                System.out.println("  - " + a.getName()
                        + " | Owner: " + a.getOwnerName()
                        + " | Breed: " + a.getBreed()
                        + " | Species: " + a.getSpecies().getName());
            }
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleSearch() {
        System.out.print("Enter patient name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        try {
            AnimalRemote animal = logic.findByName(name);
            if (animal == null) {
                System.out.println("No patient found with name \"" + name + "\".");
            } else {
                System.out.println("Found: " + animal.getName()
                        + " | Owner: " + animal.getOwnerName()
                        + " | Breed: " + animal.getBreed()
                        + " | Species: " + animal.getSpecies().getName());
            }
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleAddPatient() {
        System.out.print("Patient name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        System.out.print("Owner name: ");
        String owner = scanner.nextLine().trim();
        if (owner.isEmpty()) { System.out.println("Owner name cannot be empty."); return; }

        System.out.print("Breed: ");
        String breed = scanner.nextLine().trim();
        if (breed.isEmpty()) { System.out.println("Breed cannot be empty."); return; }

        System.out.print("Species name: ");
        String speciesName = scanner.nextLine().trim();
        if (speciesName.isEmpty()) { System.out.println("Species name cannot be empty."); return; }

        System.out.print("Average lifespan (years): ");
        String lifespanStr = scanner.nextLine().trim();
        int lifespan;
        try {
            lifespan = Integer.parseInt(lifespanStr);
            if (lifespan <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid lifespan — must be a positive integer.");
            return;
        }

        try {
            logic.addPatient(name, owner, breed, speciesName, lifespan);
            System.out.println("Patient \"" + name + "\" added successfully.");
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleViewMedicalRecord() {
        AnimalRemote animal = promptForAnimal();
        if (animal == null) return;
        try {
            System.out.println("Health status : " + logic.getHealthStatus(animal));
            List<String> obs = logic.getObservations(animal);
            if (obs.isEmpty()) {
                System.out.println("Observations  : (none)");
            } else {
                System.out.println("Observations  :");
                for (int i = 0; i < obs.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + obs.get(i));
                }
            }
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleUpdateHealth() {
        AnimalRemote animal = promptForAnimal();
        if (animal == null) return;
        System.out.print("New health status: ");
        String status = scanner.nextLine().trim();
        if (status.isEmpty()) { System.out.println("Status cannot be empty."); return; }
        try {
            logic.updateHealthStatus(animal, status);
            System.out.println("Health status updated.");
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleAddObservation() {
        AnimalRemote animal = promptForAnimal();
        if (animal == null) return;
        System.out.print("Observation: ");
        String obs = scanner.nextLine().trim();
        if (obs.isEmpty()) { System.out.println("Observation cannot be empty."); return; }
        try {
            logic.addObservation(animal, obs);
            System.out.println("Observation added.");
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleSubscribe() {
        if (logic.isSubscribed()) {
            System.out.println("Already subscribed to alerts.");
            return;
        }
        try {
            logic.subscribe();
            System.out.println("Subscribed to clinic alerts.");
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleUnsubscribe() {
        if (!logic.isSubscribed()) {
            System.out.println("Not currently subscribed.");
            return;
        }
        try {
            logic.unsubscribe();
            System.out.println("Unsubscribed from clinic alerts.");
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
        }
    }

    private void handleQuit() {
        if (logic.isSubscribed()) {
            try {
                logic.unsubscribe();
                System.out.println("Unsubscribed before exit.");
            } catch (RemoteException e) {
                System.out.println("Could not cleanly unsubscribe: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Prompts for a patient name, looks it up, and returns the remote object.
     * Returns null (and prints a message) on any failure.
     */
    private AnimalRemote promptForAnimal() {
        System.out.print("Patient name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return null;
        }
        try {
            AnimalRemote animal = logic.findByName(name);
            if (animal == null) {
                System.out.println("No patient found with name \"" + name + "\".");
            }
            return animal;
        } catch (RemoteException e) {
            System.out.println("Remote error: " + e.getMessage());
            return null;
        }
    }
}
