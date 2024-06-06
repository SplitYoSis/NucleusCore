package dev.splityosis.nucleuscore.commands;

import com.octanepvp.splityosis.commandsystem.SYSCommandBranch;
import dev.splityosis.nucleuscore.Nucleus;

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
