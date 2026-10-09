package vet.server;

import vet.common.MedicalRecordRemote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class MedicalRecordImpl extends UnicastRemoteObject implements MedicalRecordRemote {

    private String healthStatus;
    private List<String> observations;

    public MedicalRecordImpl(String initialStatus) throws RemoteException {
        super();
        this.healthStatus = initialStatus;
        this.observations = new ArrayList<>();
    }

    @Override
    public synchronized String getHealthStatus() throws RemoteException {
        return healthStatus;
    }

    @Override
    public synchronized void setHealthStatus(String status) throws RemoteException {
        this.healthStatus = status;
    }

    @Override
    public synchronized void addObservation(String observation) throws RemoteException {
        observations.add(observation);
    }

    @Override
    public synchronized List<String> getObservations() throws RemoteException {
        return new ArrayList<>(observations);
    }
}