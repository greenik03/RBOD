package com.github.greenik03.rbod.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SettingsObj implements Serializable {
    List<String> names;
    boolean reactOnName, reactOnReply; //TODO: add field for ephemeral messages (and then edit the database)

    public SettingsObj() {
        reactOnName = false;
        reactOnReply = false;
        this.names = new ArrayList<>(List.of(
                "react bot",
                "reactbot",
                "rbod"
        ));
    }

    public List<String> getNames() { return names; }

    public void addName(String name) { names.add(name); }

    public void removeName(String name) { names.remove(name); }

    public boolean containsName(String name) { return names.contains(name); }

    public boolean isReactOnName() { return reactOnName; }

    public void setReactOnName(boolean reactOnName) { this.reactOnName = reactOnName; }

    public boolean isReactOnReply() { return reactOnReply; }

    public void setReactOnReply(boolean reactOnReply) { this.reactOnReply = reactOnReply; }

    //TODO: change string format
    @Override
    public String toString() {
        return "Settings {" +
                "names=" + names +
                ", reactOnName=" + reactOnName +
                ", reactOnReply=" + reactOnReply +
                '}';
    }
}
