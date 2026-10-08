package vet.client;

import vet.common.VetObserverRemote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VetObserverImpl extends UnicastRemoteObject implements VetObserverRemote {

    private final String name;
    private final List<String> receivedAlerts;

    public VetObserverImpl() throws RemoteException {
        this("DefaultObserver");
    }

    public VetObserverImpl(String name) throws RemoteException {
        super();
        this.name = name;
        this.receivedAlerts = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public void onAlert(String message) throws RemoteException {
        receivedAlerts.add(message);
        System.out.println("\n*** [" + name + "] " + message + " ***\n");
    }

    public String getName() {
        return name;
    }

    public List<String> getReceivedAlerts() {
        synchronized (receivedAlerts) {
            return new ArrayList<>(receivedAlerts);
        }
    }

    public int getAlertCount() {
        return receivedAlerts.size();
    }
}