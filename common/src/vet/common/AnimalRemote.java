package vet.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AnimalRemote extends Remote {

    String getName() throws RemoteException;

    String getOwnerName() throws RemoteException;

    String getBreed() throws RemoteException;

    String getSpeciesName() throws RemoteException;

    Species getSpecies() throws RemoteException;

    MedicalRecordRemote getMedicalRecord() throws RemoteException;

}