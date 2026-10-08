package vet.client;

import vet.common.Species;

/**
 * A7 — Serialization Experiment:
 * This class exists ONLY on the client classpath (not in common or server).
 * Sending an instance of this class to the server causes:
 * ServerException -> UnmarshalException -> ClassNotFoundException
 * because the server JVM cannot locate the class bytecode.
 */
public class UnsharedSpecies extends Species {

    private static final long serialVersionUID = 1L;

    public UnsharedSpecies() {
        super("ClientOnlyAlienSpecies", 99);
    }
}
