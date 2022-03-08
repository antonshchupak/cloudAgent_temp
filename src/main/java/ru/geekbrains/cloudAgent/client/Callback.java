package ru.geekbrains.cloudAgent.client;

@FunctionalInterface
public interface Callback {
    void callback(Object... args);
}
