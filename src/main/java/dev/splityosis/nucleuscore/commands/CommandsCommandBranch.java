package dev.splityosis.nucleuscore.commands;

import dev.splityosis.nucleuscore.Nucleus;
import dev.splityosis.commandsystem.SYSCommandBranch;

public class CommandsCommandBranch extends SYSCommandBranch {

    private Nucleus nucleus;

    public CommandsCommandBranch(Nucleus nucleus, String basePermission) {
        super("Commands", "Command", "Cmd", "Cmds");
        this.nucleus = nucleus;

        setPermission(basePermission + ".commands");
    }
}
