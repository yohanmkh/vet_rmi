package vet.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface VetClinicRemote extends Remote {

    // Get all patients
    List<AnimalRemote> getPatients() throws RemoteException;

    // Find one by name — returns null if not found (you can also throw an
    // exception, your choice)
    AnimalRemote findByName(String name) throws RemoteException;

    void addPatient(String name, String ownerName, String breed, Species species)
            throws RemoteException;

    void subscribe(VetObserverRemote observer) throws RemoteException;

    void unsubscribe(VetObserverRemote observer) throws RemoteException;
}