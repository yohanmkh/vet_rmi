package vet.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MedicalRecordRemote extends Remote {

    String getHealthStatus() throws RemoteException;

    void setHealthStatus(String status) throws RemoteException;

    void addObservation(String observation) throws RemoteException;

    List<String> getObservations() throws RemoteException;

}