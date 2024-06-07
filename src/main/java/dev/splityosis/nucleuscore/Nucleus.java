package dev.splityosis.nucleuscore;

import dev.splityosis.nucleuscore.commands.CommandsCommandBranch;
import dev.splityosis.nucleuscore.commands.ModulesCommandBranch;
import dev.splityosis.nucleuscore.commands.NucleusCommandBranch;
import dev.splityosis.nucleuscore.exceptions.UnsupportedDatabaseType;
import dev.splityosis.nucleuscore.module.ModuleLoader;
import dev.splityosis.nucleuscore.storage.DatabaseConnectionManager;
import dev.splityosis.nucleuscore.storage.HikariDatabaseConnectionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class Nucleus {

    private final String LOG_PREFIX;

    private ModuleLoader moduleLoader;
    private JavaPlugin plugin;
    private NucleusCommandBranch nucleusCommandBranch;
    private CommandsCommandBranch commandsCommandBranch;
    private File settingsFile;
    private FileConfiguration settingsConfig;
    private File localDatabaseFile;
    private DatabaseConnectionManager databaseConnectionManager;


    public Nucleus(JavaPlugin plugin, String LOG_PREFIX, File modulesFolder, File localDatabaseFile) {
        this.plugin = plugin;
        this.localDatabaseFile = localDatabaseFile;
        moduleLoader = new ModuleLoader(this, modulesFolder);
        this.LOG_PREFIX = LOG_PREFIX;
    }

    public void initializeSettings(File settingsFile){
        this.settingsFile = settingsFile;
        if (!settingsFile.exists()) {
            try {
                writeToOutputStream(getSettingsYmlInputStream(), getSettingsFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
    }

    public void initializeDatabase(){
        try {
            String databaseType = settingsConfig.getString("database.type");
            String address = settingsConfig.getString("database.info.address");
            String name = settingsConfig.getString("database.info.name");
            String username = settingsConfig.getString("database.info.username");
            String password = settingsConfig.getString("database.info.password");

            log("&7Connecting to database...");
            databaseConnectionManager = new HikariDatabaseConnectionManager(this, databaseType, address, name, username, password);
            databaseConnectionManager.setup();
            log("&7Successfully connected to database!");
        } catch (UnsupportedDatabaseType e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeCommandBranch(String basePermission, String... branchNames){
        this.nucleusCommandBranch = new NucleusCommandBranch(this, basePermission, branchNames);
        this.commandsCommandBranch = new CommandsCommandBranch(this, basePermission);

        this.nucleusCommandBranch.addBranch(new ModulesCommandBranch(this, basePermission));
        this.nucleusCommandBranch.addBranch(commandsCommandBranch);

        this.nucleusCommandBranch.registerCommandBranch(plugin);
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getLOG_PREFIX() {
        return LOG_PREFIX;
    }

    public CommandsCommandBranch getCommandsCommandBranch() {
        return commandsCommandBranch;
    }

    public NucleusCommandBranch getNucleusCommandBranch() {
        return nucleusCommandBranch;
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public FileConfiguration getSettingsConfig() {
        return settingsConfig;
    }

    public File getLocalDatabaseFile() {
        return localDatabaseFile;
    }

    public DatabaseConnectionManager getDatabaseConnectionManager() {
        return databaseConnectionManager;
    }

    protected InputStream getSettingsYmlInputStream() {
        return getClass().getClassLoader().getResourceAsStream("settings.yml");
    }



    private static void writeToOutputStream(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public void log(String message){
        NucleusUtil.sendMessage(Bukkit.getConsoleSender(), LOG_PREFIX + " &7" + message);
    }

    public void log(Collection<String> message){
        for (String s : message) {
            log(s);
        }
    }

    public void sendPrefixedMessage(CommandSender to, String message){
        to.sendMessage(NucleusUtil.colorize(LOG_PREFIX + " " +message));
    }

    public void sendPrefixedMessage(CommandSender to, List<String> message){
        message.forEach(s -> {
            sendPrefixedMessage(to, s);
        });
    }

    public void sendMessage(CommandSender to, String message){
        to.sendMessage(NucleusUtil.colorize(message));
    }

    public void sendMessage(CommandSender to, List<String> message){
        message.forEach(s -> {
            sendMessage(to, s);
        });
    }
}
