package me.lorenzo.presence;

import lombok.Getter;
import me.lorenzo.presence.entity.service.EBIService;
import me.lorenzo.presence.vault.DataVault;
import me.lorenzo.services.service.holder.Services;

@Getter
public class Presence {
    @Getter
    private static Presence instance;
    private final DataVault vault;

    public Presence(DataVault vault) {
        instance = this;
        this.vault = vault;

        Services.register(new EBIService());
    }

}
