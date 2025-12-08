package com.thrallmaster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.ThrallUtils;

public class Commands implements CommandExecutor, TabCompleter {
    static ThrallManager manager = Main.manager;
    
    private static final List<String> SUBCOMMANDS = List.of(
        "reload", "spawn", "ally", "transfer", "select_all", "list", "inspect"
    );
    
    private static final List<String> ALLY_SUBCOMMANDS = List.of(
        "add", "remove", "list"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "spawn":
                return handleSpawn(sender, args);
            case "ally":
                return handleAlly(sender, args);
            case "transfer":
                return handleTransfer(sender, args);
            case "select_all":
                return handleSelectAll(sender);
            case "list":
                return handleList(sender);
            case "inspect":
                return handleInspect(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== ThrallMaster Commands ===");
        sender.sendMessage("§7/thrall reload §8- §fReload configuration");
        sender.sendMessage("§7/thrall spawn [player] [x y z] §8- §fSpawn a thrall");
        sender.sendMessage("§7/thrall ally add <player> §8- §fAdd an ally");
        sender.sendMessage("§7/thrall ally remove <player> §8- §fRemove an ally");
        sender.sendMessage("§7/thrall ally list §8- §fList allies");
        sender.sendMessage("§7/thrall transfer <player> §8- §fTransfer selected thralls");
        sender.sendMessage("§7/thrall select_all §8- §fSelect all your thralls");
        sender.sendMessage("§7/thrall list §8- §fList your thralls");
        sender.sendMessage("§7/thrall inspect [player] §8- §fInspect thralls");
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("thrall.reload")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        sender.sendMessage("§aReloading ThrallMaster configuration...");
        Main.reload();
        sender.sendMessage("§aConfiguration reloaded!");
        return true;
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thrall.spawn")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        Player owner = null;
        Location location = null;

        // Determinar el dueño
        if (sender instanceof Player) {
            Player player = (Player) sender;
            owner = player;
            location = player.getLocation();
        }

        // Procesar argumentos
        if (args.length > 1) {
            // Intentar obtener el jugador del primer argumento
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                owner = target;
            }
            
            // Intentar parsear ubicación
            if (args.length >= 4) {
                try {
                    double x, y, z;
                    World world = owner != null ? owner.getWorld() : Bukkit.getWorlds().get(0);
                    
                    // Determinar qué argumentos son coordenadas
                    if (target != null && args.length >= 5) {
                        // Formato: /thrall spawn <jugador> <x> <y> <z>
                        x = Double.parseDouble(args[2]);
                        y = Double.parseDouble(args[3]);
                        z = Double.parseDouble(args[4]);
                    } else if (!(sender instanceof Player)) {
                        // Consola sin jugador especificado
                        x = Double.parseDouble(args[1]);
                        y = Double.parseDouble(args[2]);
                        z = Double.parseDouble(args[3]);
                        world = Bukkit.getWorlds().get(0); // Mundo por defecto
                    } else {
                        // Jugador con coordenadas pero sin especificar otro jugador
                        x = Double.parseDouble(args[1]);
                        y = Double.parseDouble(args[2]);
                        z = Double.parseDouble(args[3]);
                    }
                    
                    location = new Location(world, x, y, z);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid coordinates. Usage: /thrall spawn [player] [x y z]");
                    return true;
                }
            }
        }

        // Validaciones finales
        if (owner == null) {
            sender.sendMessage("§cAn owner must be specified when using console.");
            sender.sendMessage("§cUsage: /thrall spawn <player> [x y z]");
            return true;
        }

        if (location == null) {
            location = owner.getLocation();
        }

        manager.spawnThrall(location, owner);
        sender.sendMessage("§aThrall spawned for " + owner.getName() + "!");
        return true;
    }

    private boolean handleAlly(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /thrall ally <add|remove|list> [player]");
            return true;
        }

        String subAlly = args[1].toLowerCase();

        switch (subAlly) {
            case "add":
                return handleAllyAdd(player, args);
            case "remove":
                return handleAllyRemove(player, args);
            case "list":
                return handleAllyList(player);
            default:
                player.sendMessage("§cUsage: /thrall ally <add|remove|list> [player]");
                return true;
        }
    }

    private boolean handleAllyAdd(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /thrall ally add <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        manager.getOwnerData(player.getUniqueId()).addAlly(target.getUniqueId());
        player.sendMessage("§a" + target.getName() + " is now an ally!");
        return true;
    }

    private boolean handleAllyRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /thrall ally remove <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        manager.getOwnerData(player.getUniqueId()).removeAlly(target.getUniqueId());
        player.sendMessage("§a" + target.getName() + " is no longer an ally.");
        return true;
    }

    private boolean handleAllyList(Player player) {
        List<String> allies = manager.getOwnerData(player.getUniqueId()).getAllies()
            .map(id -> Bukkit.getOfflinePlayer(id))
            .filter(offlinePlayer -> offlinePlayer != null && offlinePlayer.getName() != null)
            .map(offlinePlayer -> offlinePlayer.getName())
            .collect(Collectors.toList());

        if (allies.isEmpty()) {
            player.sendMessage("§7You have no allies.");
        } else {
            player.sendMessage("§6=== Your Allies ===");
            allies.forEach(name -> player.sendMessage("§7- §f" + name));
        }
        return true;
    }

    private boolean handleTransfer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /thrall transfer <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        var selected = manager.getThralls(player.getUniqueId())
                .filter(state -> state.isSelected() && state.isValidEntity())
                .collect(Collectors.toList());
        
        if (selected.isEmpty()) {
            player.sendMessage("§cYou don't have any thralls selected.");
            return true;
        }

        selected.forEach(state -> {
            manager.unregister(state.getEntityID());
            manager.registerThrall((AbstractSkeleton) state.getEntity(), target);
        });

        player.sendMessage("§aYou transferred §e" + selected.size() + " §athralls to §e" + target.getName());
        target.sendMessage("§aYou received §e" + selected.size() + " §athralls from §e" + player.getName());
        return true;
    }

    private boolean handleSelectAll(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        
        long count = manager.getThralls(player.getUniqueId())
            .peek(state -> state.setSelected(true))
            .count();
            
        player.sendMessage("§aSelected §e" + count + " §athralls.");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        
        List<ThrallState> thralls = manager.getThralls(player.getUniqueId())
            .filter(state -> state.isValidEntity())
            .sorted(Comparator.comparingDouble(state -> ThrallUtils.distanceToOwner(state)))
            .collect(Collectors.toList());

        if (thralls.isEmpty()) {
            player.sendMessage("§7You have no thralls.");
            return true;
        }

        player.sendMessage("§6=== Your Thralls ===");
        thralls.forEach(state -> {
            LivingEntity entity = (LivingEntity) state.getEntity();
            int distance = (int) ThrallUtils.distanceToOwner(state);
            String status = state.isSelected() ? "§a✓" : "§7✗";
            
            player.sendMessage(String.format("%s §e%s §7(%d❤) §8- §f%s §7(%dm)", 
                status,
                entity.getName(),
                (int) entity.getHealth(),
                state.getBehavior().getBehaviorName(),
                distance
            ));
        });
        
        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thrall.inspect")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }

            List<ThrallState> thralls = manager.getThralls(target.getUniqueId())
                .collect(Collectors.toList());

            sender.sendMessage("§6=== " + target.getName() + "'s Thralls ===");
            sender.sendMessage("§7Total: §e" + thralls.size());
            thralls.forEach(state -> {
                sender.sendMessage("§7- §f" + state.getBehavior().getBehaviorName());
            });
        } else {
            sender.sendMessage("§6=== All Thrall Owners ===");
            manager.getOwners().forEach(owner -> {
                String playerName = Bukkit.getOfflinePlayer(owner.getPlayerID()).getName();
                sender.sendMessage("§7" + playerName + ": §e" + owner.getCount() + " §7thralls");
            });
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Completar subcomandos principales
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "spawn":
                case "transfer":
                case "inspect":
                    // Completar nombres de jugadores en línea
                    completions.addAll(getOnlinePlayerNames());
                    break;
                case "ally":
                    // Completar subcomandos de ally
                    StringUtil.copyPartialMatches(args[1], ALLY_SUBCOMMANDS, completions);
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("ally") && 
                (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                // Completar nombres de jugadores para add/remove ally
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("spawn")) {
                // Si ya tenemos un jugador, sugerir coordenadas
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    Location loc = target.getLocation();
                    completions.add(String.valueOf((int) loc.getX()));
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("spawn")) {
            // Completar coordenada Y
            completions.add("0");
            completions.add("64");
            completions.add("100");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("spawn")) {
            // Completar coordenada Z
            completions.add("0");
            completions.add("100");
            completions.add("200");
        }
        
        return completions;
    }
    
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
    }
}