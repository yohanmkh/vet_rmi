package vet.client;

import vet.common.Species;

public class UnsharedSpecies extends Species {

    private static final long serialVersionUID = 1L;

    public UnsharedSpecies() {
        super("ClientOnlyAlienSpecies", 99);
    }
}
