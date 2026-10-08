package vet.client;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

/**
 * CliConsole — interactive menu loop.
 * Contains ZERO direct RMI calls on remote stubs; delegates everything to CliLogic.
 */
public class CliConsole {

    private static final String MENU =
            "\n=== Vet Clinic Management ===\n" +
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
            List<CliLogic.PatientView> patients = logic.getPatients();
            if (patients.isEmpty()) {
                System.out.println("No patients registered in clinic.");
                return;
            }
            System.out.println("Patients (" + patients.size() + "):");
            for (CliLogic.PatientView p : patients) {
                System.out.println("  - " + p.getName()
                        + " | Owner: " + p.getOwner()
                        + " | Breed: " + p.getBreed()
                        + " | Species: " + p.getSpeciesName()
                        + " (avg lifespan: " + p.getLifespan() + " yrs)");
            }
        } catch (RemoteException e) {
            System.out.println("Remote communication error: " + e.getMessage());
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
            CliLogic.PatientView p = logic.findByName(name);
            if (p == null) {
                System.out.println("No patient found with name \"" + name + "\".");
            } else {
                System.out.println("Found: " + p.getName()
                        + " | Owner: " + p.getOwner()
                        + " | Breed: " + p.getBreed()
                        + " | Species: " + p.getSpeciesName()
                        + " (avg lifespan: " + p.getLifespan() + " yrs)");
            }
        } catch (RemoteException e) {
            System.out.println("Remote communication error: " + e.getMessage());
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
            System.out.println("Remote communication error: " + e.getMessage());
        }
    }

    private void handleViewMedicalRecord() {
        System.out.print("Patient name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        try {
            CliLogic.MedicalRecordView record = logic.getMedicalRecord(name);
            if (record == null) {
                System.out.println("No patient found with name \"" + name + "\".");
                return;
            }
            System.out.println("Health status : " + record.getHealthStatus());
            List<String> obs = record.getObservations();
            if (obs.isEmpty()) {
                System.out.println("Observations  : (none)");
            } else {
                System.out.println("Observations  :");
                for (int i = 0; i < obs.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + obs.get(i));
                }
            }
        } catch (RemoteException e) {
            System.out.println("Remote communication error: " + e.getMessage());
        }
    }

    private void handleUpdateHealth() {
        System.out.print("Patient name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        System.out.print("New health status: ");
        String status = scanner.nextLine().trim();
        if (status.isEmpty()) { System.out.println("Status cannot be empty."); return; }

        try {
            boolean ok = logic.updateHealthStatus(name, status);
            if (ok) {
                System.out.println("Health status updated successfully for \"" + name + "\".");
            } else {
                System.out.println("No patient found with name \"" + name + "\".");
            }
        } catch (RemoteException e) {
            System.out.println("Remote communication error: " + e.getMessage());
        }
    }

    private void handleAddObservation() {
        System.out.print("Patient name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        System.out.print("Observation: ");
        String obs = scanner.nextLine().trim();
        if (obs.isEmpty()) { System.out.println("Observation cannot be empty."); return; }

        try {
            boolean ok = logic.addObservation(name, obs);
            if (ok) {
                System.out.println("Observation added successfully for \"" + name + "\".");
            } else {
                System.out.println("No patient found with name \"" + name + "\".");
            }
        } catch (RemoteException e) {
            System.out.println("Remote communication error: " + e.getMessage());
        }
    }

    private void handleSubscribe() {
        if (logic.isSubscribed()) {
            System.out.println("Already subscribed to alerts.");
            return;
        }
        try {
            logic.subscribe();
            System.out.println("Subscribed to clinic alerts (threshold crossings at 100, 500, 1000).");
        } catch (RemoteException e) {
            System.out.println("Remote communication error: " + e.getMessage());
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
            System.out.println("Remote communication error: " + e.getMessage());
        }
    }

    private void handleQuit() {
        if (logic.isSubscribed()) {
            try {
                logic.unsubscribe();
                System.out.println("Cleanly unsubscribed from alerts.");
            } catch (RemoteException e) {
                System.out.println("Could not cleanly unsubscribe: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }
}
