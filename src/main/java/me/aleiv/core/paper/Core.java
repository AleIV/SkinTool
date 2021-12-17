package me.aleiv.core.paper;

import co.aikar.commands.PaperCommandManager;
import com.twodevsstudio.simplejsonconfig.SimpleJSONConfig;
import kr.entree.spigradle.annotations.SpigotPlugin;
import lombok.Getter;
import me.aleiv.core.paper.skins.SkinCMD;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

@SpigotPlugin
public class Core extends JavaPlugin {

  private static @Getter Core instance;
  private @Getter static final MiniMessage miniMessage = MiniMessage.get();
  private @Getter PaperCommandManager commandManager;

  @Override
  public void onEnable() {
    instance = this;
    SimpleJSONConfig.INSTANCE.register(this);
    // COMMANDS
    commandManager = new PaperCommandManager(this);
    commandManager.registerCommand(new SkinCMD(this));
  }

  @Override
  public void onDisable() {}
}
