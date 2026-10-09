package vet.common;

import java.io.Serializable;

public class Species implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int averageLifespan;

    public Species(String name, int averageLifespan) {
        this.name = name;
        this.averageLifespan = averageLifespan;
    }

    public String getName() {
        return name;
    }

    public int getAverageLifespan() {
        return averageLifespan;
    }

    public void setAverageLifespan(int years) {
        this.averageLifespan = years;
    }

    @Override
    public String toString() {
        return name + " (avg lifespan: " + averageLifespan + " years)";
    }
}