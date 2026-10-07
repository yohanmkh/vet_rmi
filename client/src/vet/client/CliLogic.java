package vet.client;

import vet.common.AnimalRemote;
import vet.common.MedicalRecordRemote;
import vet.common.Species;
import vet.common.VetClinicRemote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * CliLogic — wraps all RMI calls for the CLI.
 * No Scanner, no System.out menus here.
 */
public class CliLogic {

    private final VetClinicRemote clinic;
    private VetObserverImpl observer = null;
    private boolean subscribed = false;

    public CliLogic(VetClinicRemote clinic) {
        this.clinic = clinic;
    }

    // ── Patient queries ────────────────────────────────────────────────────────

    public List<AnimalRemote> getPatients() throws RemoteException {
        return clinic.getPatients();
    }

    public AnimalRemote findByName(String name) throws RemoteException {
        return clinic.findByName(name);
    }

    public void addPatient(String name, String owner, String breed,
                           String speciesName, int lifespan) throws RemoteException {
        Species species = new Species(speciesName, lifespan);
        clinic.addPatient(name, owner, breed, species);
    }

    // ── Medical record ─────────────────────────────────────────────────────────

    public String getHealthStatus(AnimalRemote animal) throws RemoteException {
        return animal.getMedicalRecord().getHealthStatus();
    }

    public List<String> getObservations(AnimalRemote animal) throws RemoteException {
        return animal.getMedicalRecord().getObservations();
    }

    public void updateHealthStatus(AnimalRemote animal, String status) throws RemoteException {
        animal.getMedicalRecord().setHealthStatus(status);
    }

    public void addObservation(AnimalRemote animal, String observation) throws RemoteException {
        animal.getMedicalRecord().addObservation(observation);
    }

    // ── Observer / alerts ──────────────────────────────────────────────────────

    /**
     * Subscribes to clinic alerts.
     * Creates and exports a VetObserverImpl, then registers it with the server.
     * Safe to call even if already subscribed (no-op in that case).
     */
    public void subscribe() throws RemoteException {
        if (subscribed) {
            return;
        }
        observer = new VetObserverImpl();
        clinic.subscribe(observer);
        subscribed = true;
    }

    /**
     * Unsubscribes from clinic alerts and unexports the observer so the JVM can exit.
     * Safe to call even if not currently subscribed (no-op in that case).
     */
    public void unsubscribe() throws RemoteException {
        if (!subscribed) {
            return;
        }
        try {
            clinic.unsubscribe(observer);
        } finally {
            // Always unexport, even if the server call failed, so the JVM can exit.
            try {
                UnicastRemoteObject.unexportObject(observer, true);
            } catch (Exception ignored) {
                // Nothing useful to do here.
            }
            observer = null;
            subscribed = false;
        }
    }

    public boolean isSubscribed() {
        return subscribed;
    }
}
