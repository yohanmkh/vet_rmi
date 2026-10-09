package vet.server;

import vet.common.AnimalRemote;
import vet.common.Species;
import vet.common.VetClinicRemote;
import vet.common.VetObserverRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("serial")
public class VetClinicImpl extends UnicastRemoteObject implements VetClinicRemote {

    private final List<AnimalImpl> patients;
    private final List<VetObserverRemote> observers;
    private static final int[] THRESHOLDS = { 100, 500, 1000 };

    public VetClinicImpl() throws RemoteException {
        super();
        this.patients = Collections.synchronizedList(new ArrayList<>());
        this.observers = new CopyOnWriteArrayList<>();
    }

    public synchronized void addPatientLocally(AnimalImpl animal) {
        int prevCount = patients.size();
        patients.add(animal);
        int newCount = patients.size();
        checkThresholds(prevCount, newCount);
    }

    @Override
    public List<AnimalRemote> getPatients() throws RemoteException {
        synchronized (patients) {
            return new ArrayList<>(patients);
        }
    }

    @Override
    public AnimalRemote findByName(String name) throws RemoteException {
        if (name == null) {
            return null;
        }
        synchronized (patients) {
            for (AnimalImpl a : patients) {
                if (a.getName().equalsIgnoreCase(name)) {
                    return a;
                }
            }
        }
        return null;
    }

    @Override
    public void subscribe(VetObserverRemote observer) throws RemoteException {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            System.out.println("New observer subscribed. Total: " + observers.size());
        }
    }

    @Override
    public void unsubscribe(VetObserverRemote observer) throws RemoteException {
        if (observer != null) {
            observers.remove(observer);
            System.out.println("Observer unsubscribed. Total: " + observers.size());
        }
    }

    @Override
    public synchronized void addPatient(String name, String ownerName, String breed, Species species)
            throws RemoteException {
        int prevCount = patients.size();
        AnimalImpl animal = new AnimalImpl(name, ownerName, breed, species);
        patients.add(animal);
        int newCount = patients.size();
        System.out.println("New patient added: " + name + " (total: " + newCount + ")");
        checkThresholds(prevCount, newCount);
    }

    private void checkThresholds(int prevCount, int newCount) {
        for (int threshold : THRESHOLDS) {
            if (prevCount < threshold && newCount >= threshold) {
                notifyObservers("ALERT: Patient count crossed threshold " + threshold + " upwards (now " + newCount + ")!");
            } else if (prevCount >= threshold && newCount < threshold) {
                notifyObservers("ALERT: Patient count crossed threshold " + threshold + " downwards (now " + newCount + ")!");
            }
        }
    }

    private void notifyObservers(String message) {
        List<VetObserverRemote> deadObservers = new ArrayList<>();
        for (VetObserverRemote observer : observers) {
            try {
                observer.onAlert(message);
            } catch (RemoteException e) {
                System.out.println("Observer unreachable, removing from subscriber list.");
                deadObservers.add(observer);
            }
        }
        if (!deadObservers.isEmpty()) {
            observers.removeAll(deadObservers);
        }
    }
}