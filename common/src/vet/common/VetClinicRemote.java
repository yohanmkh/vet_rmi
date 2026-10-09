package vet.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface VetClinicRemote extends Remote {

    List<AnimalRemote> getPatients() throws RemoteException;

    AnimalRemote findByName(String name) throws RemoteException;

    void addPatient(String name, String ownerName, String breed, Species species)
            throws RemoteException;

    void subscribe(VetObserverRemote observer) throws RemoteException;

    void unsubscribe(VetObserverRemote observer) throws RemoteException;
}