package me.aleiv.core.paper;

import org.bukkit.plugin.java.JavaPlugin;

import co.aikar.commands.PaperCommandManager;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Getter;
import me.aleiv.core.paper.skins.SkinCMD;
import net.kyori.adventure.text.minimessage.MiniMessage;

@SpigotPlugin
public class Core extends JavaPlugin {

    private static @Getter Core instance;
    private @Getter PaperCommandManager commandManager;
    private @Getter static MiniMessage miniMessage = MiniMessage.get();

    @Override
    public void onEnable() {
        instance = this;

        //COMMANDS
        commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new SkinCMD(this));

    }

    @Override
    public void onDisable() {

    }

}