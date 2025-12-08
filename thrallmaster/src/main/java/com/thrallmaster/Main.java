package com.thrallmaster;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.thrallmaster.IO.NBTExporter;
import com.thrallmaster.Protocols.ThrallProtocol;

public class Main extends JavaPlugin {

   public static Main plugin;
   public static FileConfiguration config;
   public static ThrallManager manager;
   public static NBTExporter saver;
   public static boolean hadError;

   @Override
   public void onLoad() {
      super.onLoad();
      ThrallProtocol.onLoad(this);
   }

   @Override
   public void onEnable() {
      plugin = this;
      config = getConfig();
      saveDefaultConfig();

      try {
         Settings.loadConfig(plugin);
         ThrallManager.logger = getLogger();

         manager = new ThrallManager();
         manager.restorePlayers();
         
         // Registrar el comando
         Commands commands = new Commands();
         this.getCommand("thrall").setExecutor(commands);
         this.getCommand("thrall").setTabCompleter(commands);
         
         this.getServer().getPluginManager().registerEvents(manager, this);
      } catch (Exception e) {
         hadError = true;
         throw e;
      }

      getLogger().info("Thrall Master plugin enabled.");
   }

   @Override
   public void onDisable() {
      super.onDisable();

      if (!hadError) {
         getLogger().info("Saving Thrall NBT state.");
         manager.savePlayers(true);
      } else {
         getLogger().warning("ThrallMaster had an error!");
         getLogger().info("Skipping saving NBT state to disk.");
      }
   }

   public static void reload() {
      plugin.reloadConfig();
      config = plugin.getConfig();
      Settings.loadConfig(plugin);
   }
}