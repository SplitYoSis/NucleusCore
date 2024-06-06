package dev.splityosis.nucleuscore.commands;

import dev.splityosis.nucleuscore.Nucleus;
import dev.splityosis.commandsystem.SYSCommandBranch;

public class NucleusCommandBranch extends SYSCommandBranch {

    private final Nucleus nucleus;

    public NucleusCommandBranch(Nucleus nucleus, String basePermission, String... names) {
        super(names);
        this.nucleus = nucleus;
        setPermission(basePermission);


    }

    public Nucleus getNucleus() {
        return nucleus;
    }
}
