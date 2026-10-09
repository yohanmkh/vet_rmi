package vet.client;

import vet.common.VetClinicRemote;

import java.lang.reflect.Proxy;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 1099;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port \"" + args[1] + "\", using default 1099.");
            }
        }

        VetClinicRemote clinic;
        try {
            System.out.println("Connecting to RMI registry at " + host + ":" + port + "...");
            Registry registry = LocateRegistry.getRegistry(host, port);
            clinic = (VetClinicRemote) registry.lookup("VetClinic");

            System.out.println("[RMI proof] Connected to VetClinic stub : " + clinic.getClass().getName());
            System.out.println("[RMI proof] Is JDK dynamic proxy?       : " + Proxy.isProxyClass(clinic.getClass()));
        } catch (Exception e) {
            System.err.println("Could not connect to the RMI registry at " + host + ":" + port + ".");
            System.err.println("Make sure the server is running. Details: " + e.getMessage());
            System.exit(1);
            return;
        }

        CliLogic logic = new CliLogic(clinic);
        CliConsole console = new CliConsole(logic);
        console.run();
    }
}
