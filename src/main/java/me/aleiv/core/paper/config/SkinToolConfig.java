package me.aleiv.core.paper.config;

import com.twodevsstudio.simplejsonconfig.api.Config;
import com.twodevsstudio.simplejsonconfig.interfaces.Configuration;

@Configuration("skinToolConfig.json")
public class SkinToolConfig extends Config {

  private final String skinToolAddress = "127.0.0.1";
  private final String skinToolPort = "8069";
  private final String protocol = "http";

  public String getUri() {
    return protocol + "://" + skinToolAddress + ":" + skinToolPort;
  }
}
