package vet.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VetObserverRemote extends Remote {
    void onAlert(String message) throws RemoteException;

}