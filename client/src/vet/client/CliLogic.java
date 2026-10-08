package vet.client;

import vet.common.AnimalRemote;
import vet.common.MedicalRecordRemote;
import vet.common.Species;
import vet.common.VetClinicRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CliLogic — encapsulates all RMI communications for the CLI.
 * Converts remote objects (stubs) into lightweight views/DTOs
 * so the console layer does not perform any remote invocations directly.
 */
public class CliLogic {

    private final VetClinicRemote clinic;
    private VetObserverImpl observer = null;
    private boolean subscribed = false;

    public CliLogic(VetClinicRemote clinic) {
        this.clinic = clinic;
    }

    // ── DTOs for the Presentation Layer ────────────────────────────────────────

    public static class PatientView {
        private final String name;
        private final String owner;
        private final String breed;
        private final String speciesName;
        private final int lifespan;

        public PatientView(String name, String owner, String breed, String speciesName, int lifespan) {
            this.name = name;
            this.owner = owner;
            this.breed = breed;
            this.speciesName = speciesName;
            this.lifespan = lifespan;
        }

        public String getName() { return name; }
        public String getOwner() { return owner; }
        public String getBreed() { return breed; }
        public String getSpeciesName() { return speciesName; }
        public int getLifespan() { return lifespan; }
    }

    public static class MedicalRecordView {
        private final String healthStatus;
        private final List<String> observations;

        public MedicalRecordView(String healthStatus, List<String> observations) {
            this.healthStatus = healthStatus;
            this.observations = observations != null ? observations : Collections.emptyList();
        }

        public String getHealthStatus() { return healthStatus; }
        public List<String> getObservations() { return observations; }
    }

    // ── Patient Queries & Commands ─────────────────────────────────────────────

    public List<PatientView> getPatients() throws RemoteException {
        List<AnimalRemote> stubs = clinic.getPatients();
        List<PatientView> views = new ArrayList<>();
        for (AnimalRemote stub : stubs) {
            Species sp = stub.getSpecies();
            views.add(new PatientView(
                    stub.getName(),
                    stub.getOwnerName(),
                    stub.getBreed(),
                    sp != null ? sp.getName() : "Unknown",
                    sp != null ? sp.getAverageLifespan() : 0
            ));
        }
        return views;
    }

    public PatientView findByName(String name) throws RemoteException {
        AnimalRemote stub = clinic.findByName(name);
        if (stub == null) {
            return null;
        }
        Species sp = stub.getSpecies();
        return new PatientView(
                stub.getName(),
                stub.getOwnerName(),
                stub.getBreed(),
                sp != null ? sp.getName() : "Unknown",
                sp != null ? sp.getAverageLifespan() : 0
        );
    }

    public void addPatient(String name, String owner, String breed,
                           String speciesName, int lifespan) throws RemoteException {
        Species species = new Species(speciesName, lifespan);
        clinic.addPatient(name, owner, breed, species);
    }

    // ── Medical Record Commands ────────────────────────────────────────────────

    public MedicalRecordView getMedicalRecord(String patientName) throws RemoteException {
        AnimalRemote stub = clinic.findByName(patientName);
        if (stub == null) {
            return null;
        }
        MedicalRecordRemote record = stub.getMedicalRecord();
        if (record == null) {
            return new MedicalRecordView("No record found", Collections.emptyList());
        }
        return new MedicalRecordView(record.getHealthStatus(), record.getObservations());
    }

    public boolean updateHealthStatus(String patientName, String status) throws RemoteException {
        AnimalRemote stub = clinic.findByName(patientName);
        if (stub == null) {
            return false;
        }
        MedicalRecordRemote record = stub.getMedicalRecord();
        if (record == null) {
            return false;
        }
        record.setHealthStatus(status);
        return true;
    }

    public boolean addObservation(String patientName, String observation) throws RemoteException {
        AnimalRemote stub = clinic.findByName(patientName);
        if (stub == null) {
            return false;
        }
        MedicalRecordRemote record = stub.getMedicalRecord();
        if (record == null) {
            return false;
        }
        record.addObservation(observation);
        return true;
    }

    // ── Observer / Alerts ──────────────────────────────────────────────────────

    public void subscribe() throws RemoteException {
        if (subscribed) {
            return;
        }
        observer = new VetObserverImpl();
        clinic.subscribe(observer);
        subscribed = true;
    }

    public void unsubscribe() throws RemoteException {
        if (!subscribed) {
            return;
        }
        try {
            clinic.unsubscribe(observer);
        } finally {
            try {
                UnicastRemoteObject.unexportObject(observer, true);
            } catch (Exception ignored) {
            }
            observer = null;
            subscribed = false;
        }
    }

    public boolean isSubscribed() {
        return subscribed;
    }
}
