package com.example.knifeclient;

import java.util.ArrayList;
import java.util.List;

public final class ClientModule {
    public final String id, name, desc;
    public final boolean toggleable;
    public String category = "Misc";
    public boolean enabled;
    private final boolean defEnabled;
    public final List<Setting> settings = new ArrayList<>();

    public ClientModule(String id, String name, String desc, boolean toggleable, boolean defEnabled) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.toggleable = toggleable;
        this.defEnabled = defEnabled;
        this.enabled = defEnabled;
    }

    public ClientModule cat(String c) {
        this.category = c;
        return this;
    }

    public <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public void resetSettings() {
        for (Setting s : settings) s.reset();
    }

    public boolean defaultEnabled() { return defEnabled; }
}
