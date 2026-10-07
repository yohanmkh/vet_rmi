package vet.server;

import vet.common.AnimalRemote;
import vet.common.Species;
import vet.common.VetClinicRemote;
import vet.common.VetObserverRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import vet.common.MedicalRecordRemote;
import vet.common.VetObserverRemote;

public class VetClinicImpl extends UnicastRemoteObject implements VetClinicRemote {

    private final List<AnimalImpl> patients;

    public VetClinicImpl() throws RemoteException {
        super();
        this.patients = new ArrayList<>();
    }

    public void addPatientLocally(AnimalImpl animal) {
        patients.add(animal);
    }

    @Override
    public List<AnimalRemote> getPatients() throws RemoteException {
        return new ArrayList<>(patients);
    }

    @Override
    public AnimalRemote findByName(String name) throws RemoteException {
        for (AnimalImpl a : patients) {
            if (a.getName().equals(name))
                return a;
        }
        return null; // or throw a custom exception
    }

    private final List<VetObserverRemote> observers = new ArrayList<>();
    private static final int[] THRESHOLDS = { 100, 500, 1000 };

    @Override
    public void subscribe(VetObserverRemote observer) throws RemoteException {
        observers.add(observer);
        System.out.println("New observer subscribed. Total: " + observers.size());
    }

    @Override
    public void unsubscribe(VetObserverRemote observer) throws RemoteException {
        observers.remove(observer);
        System.out.println("Observer unsubscribed. Total: " + observers.size());
    }

    @Override
    public void addPatient(String name, String ownerName, String breed, Species species)
            throws RemoteException {
        AnimalImpl animal = new AnimalImpl(name, ownerName, breed, species);
        patients.add(animal);
        System.out.println("New patient added: " + name);
        checkThresholds();
    }

    private void checkThresholds() {
        int count = patients.size();
        for (int threshold : THRESHOLDS) {
            if (count == threshold) {
                notifyObservers("ALERT: Patient count reached " + threshold + "!");
            }
        }
    }

    private void notifyObservers(String message) {
        List<VetObserverRemote> toRemove = new ArrayList<>();
        for (VetObserverRemote observer : observers) {
            try {
                observer.onAlert(message);
            } catch (RemoteException e) {
                // client is gone — remove it, don't crash
                System.out.println("Observer unreachable, removing it.");
                toRemove.add(observer);
            }
        }
        observers.removeAll(toRemove);
    }
}