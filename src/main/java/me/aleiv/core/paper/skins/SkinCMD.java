package me.aleiv.core.paper.skins;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.destroystokyo.paper.profile.ProfileProperty;


import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import com.mojang.authlib.properties.Property;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import me.aleiv.core.paper.Core;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo;

/**
 * A command to interact with the skin-tool app from minecraft.
 * 
 * @author jcedeno
 */
@CommandPermission("admin.skin.cmd")
@CommandAlias("skin")
public class SkinCMD extends BaseCommand {
    Core instance;

    public SkinCMD(Core instance) {
        this.instance = instance;
        // register command completion
        instance.getCommandManager().getCommandCompletions().registerStaticCompletion("variants",
                List.of("civilian", "guard", "participant", "tux", "original"));
        // register the command itself
        instance.getCommandManager().registerCommand(this);
    }

    @Subcommand("add")
    @CommandCompletion("@players")
    public void addQueue(CommandSender sender, String... names) {

        List<UUID> users = new ArrayList<>();

        CompletableFuture.supplyAsync(() -> {
            try {
                for (String name : names) {

                    var uuid = SkinToolApi.getUserProfile(name);

                    if (uuid == null) {
                        sender.sendMessage(ChatColor.RED + "Player " + name + " doesn't exist.");
                        return false;
                    }
                    users.add(uuid);

                    sender.sendMessage(ChatColor.DARK_AQUA + name + " added to the queue.");
                }

                SkinToolApi.addSkinsToComputeQueue(users);

            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "An error ocurred.");
            }

            return false;
        });

    }

    @Subcommand("random")
    @CommandCompletion("@variants")
    public void randomSkins(CommandSender sender, String variant) {

        CompletableFuture.supplyAsync(() -> {
            // Ask the api for all skins of this type
            var optionalSkins = SkinToolApi.getAllVariant(variant);
            if (optionalSkins.isPresent()) {
                // Fitler out the not signed skins.
                var skins = optionalSkins.get().stream().filter(PlayerSkin::isItSigned).toList();
                if (skins.size() > 0) {
                    // Take a snapshot o of all players online to change their skins.
                    var players = Bukkit.getOnlinePlayers().stream().map(m -> m.getPlayer()).toList();
                    int skinIndex = 0;
                    // Get an iterator to easily loop through the players.
                    var playerIter = players.iterator();

                    while (playerIter.hasNext()) {
                        var nextPlayer = playerIter.next();
                        // Get current index and add 1
                        var skin = skins.get(skinIndex++);
                        // Ensure skin not null.
                        if (skin != null) {
                            Bukkit.getScheduler().runTask(instance, task -> {
                                SkinCMD.skinSwapper(nextPlayer, skin.getValue(), skin.getSignature()); // Swap the
                                                                                                       // Player's
                                // skin
                            });
                        }

                        // Handle possible out of bounds exception.
                        if (skinIndex > skins.size() - 1)
                            skinIndex = 0;
                    }
                }
            }
            return false;
        }).whenComplete((action, ex) -> {
            sender.sendMessage(ChatColor.DARK_AQUA + "Task skins random completed.");
        });

    }

    @Subcommand("set-other")
    @CommandCompletion("@players @players @variants")
    public void changeSkinOther(CommandSender sender, String playerTarget, String skinSourcePlayer,
            @Default("original") String variant) {
        CompletableFuture.supplyAsync(() -> {
            try {

                var player = Bukkit.getPlayer(playerTarget);

                if (player != null && player.isOnline()) {
                    UUID id = null;

                    var ofP = Bukkit.getPlayer(skinSourcePlayer);

                    if (ofP != null && ofP.getUniqueId() != null) {
                        id = ofP.getUniqueId();
                    } else {
                        id = SkinToolApi.getUserProfile(skinSourcePlayer);
                    }

                    if (id != null) {
                        if (variant.equalsIgnoreCase("original")) {
                            var skin = SkinToolApi.getCurrentUserSkin(id, false);
                            Bukkit.getScheduler().runTask(Core.getInstance(),
                                    () -> skinSwapper(player, skin.getValue(), skin.getSignature()));

                        } else {
                            SkinToolApi.getElseComputeSkins(id).whenComplete((skins, exception) -> {
                                if (exception != null) {
                                    sender.sendMessage("Command ended exceptionally: " + exception.getMessage());
                                    exception.printStackTrace();
                                } else {
                                    var skin = skins.stream().filter(s -> s.getName().equalsIgnoreCase(variant))
                                            .findFirst();
                                    if (skin.isPresent()) {
                                        var actualSkin = skin.get();
                                        Bukkit.getScheduler().runTask(Core.getInstance(), () -> skinSwapper(player,
                                                actualSkin.getValue(), actualSkin.getSignature()));

                                    } else {
                                        sender.sendMessage("Skin not found");
                                    }
                                }

                            });
                        }
                    } else {
                        sender.sendMessage("§cThe player you specified does not have a skin set.");
                    }

                } else {
                    sender.sendMessage("§cPlayer not found.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage("Error changin skins: " + e.getMessage());
            }

            return false;
        });
    }

    /**
     * Function that swaps a player's skin for a different one.
     * 
     * @param player    The player to swap the skin for.
     * @param texture   The texture to apply to the player.
     * @param signature The signature of the texture.
     */
    public static void skinSwapper(Player player, String texture, String signature) {
        player.sendMessage(Core.getMiniMessage().parse("<yellow>Changing your skin..."));
        var entityPlayer = ((CraftPlayer) player.getPlayer()).getHandle();
        var prof = entityPlayer.getProfile();
        var con = entityPlayer.playerConnection;

        con.sendPacket(
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer));
        prof.getProperties().removeAll("textures");
        prof.getProperties().put("textures", new Property("textures", texture, signature));
        con.sendPacket(
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer));

        var profilePurpur = player.getPlayerProfile();
        profilePurpur.setProperty(new ProfileProperty(player.getName(), texture, signature));

        player.setPlayerProfile(profilePurpur);
        player.sendMessage(Core.getMiniMessage().parse("<green>Skin changed!"));
        player.setPlayerTime(0, true);
        // Reset player time 2 ticks later.
        Bukkit.getScheduler().runTaskLater(Core.getInstance(), () -> player.resetPlayerTime(), 2);
    }

}
