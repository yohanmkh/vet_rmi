package vet.client;

import vet.common.VetObserverRemote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class VetObserverImpl extends UnicastRemoteObject implements VetObserverRemote {

    public VetObserverImpl() throws RemoteException {
        super();
    }

    @Override
    public void onAlert(String message) throws RemoteException {
        System.out.println("\n*** " + message + " ***\n");
    }
}