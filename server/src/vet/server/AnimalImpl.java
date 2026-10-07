package vet.server;

import vet.common.AnimalRemote;
import vet.common.MedicalRecordRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import vet.common.Species;

public class AnimalImpl extends UnicastRemoteObject implements AnimalRemote {

    private final String name;
    private final String ownerName;
    private final String breed;
    private final Species species;
    private final MedicalRecordRemote medicalRecord;

    public AnimalImpl(String name, String ownerName, String breed, Species species)
            throws RemoteException {
        super();
        this.name = name;
        this.ownerName = ownerName;
        this.breed = breed;
        this.species = species;
        this.medicalRecord = new MedicalRecordImpl("Healthy");
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    // implement the other getters
    @Override
    public String getOwnerName() throws RemoteException {
        return ownerName;
    }

    @Override
    public String getBreed() throws RemoteException {
        return breed;
    }

    @Override
    public String getSpeciesName() throws RemoteException {
        return species.getName();
    }

    @Override
    public Species getSpecies() throws RemoteException {
        System.out.println("Server side hash: " + System.identityHashCode(species));
        return species;
    }

    @Override
    public MedicalRecordRemote getMedicalRecord() throws RemoteException {
        return medicalRecord;
    }

}