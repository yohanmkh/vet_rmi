package vet.server;

import vet.common.MedicalRecordRemote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordImpl extends UnicastRemoteObject implements MedicalRecordRemote {

    private String healthStatus;
    private List<String> observations;

    public MedicalRecordImpl(String initialStatus) throws RemoteException {
        super();
        this.healthStatus = initialStatus;
        this.observations = new ArrayList<>();
    }

    @Override
    public String getHealthStatus() throws RemoteException {
        return healthStatus;
    }

    @Override
    public void setHealthStatus(String status) throws RemoteException {
        this.healthStatus = status;
    }

    @Override
    public void addObservation(String observation) throws RemoteException {
        observations.add(observation);
    }

    @Override
    public List<String> getObservations() throws RemoteException {
        return new ArrayList<>(observations); // return a copy of the list
    }
}