package ru.geekbrains.cloudAgent.server;

public interface AuthenticationProvider {
    void init();
    boolean isUsernameBusy(String username);
    void shutdown();
}
