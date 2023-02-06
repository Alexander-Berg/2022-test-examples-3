package ru.yandex.market.billing;

import java.lang.reflect.Field;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.common.util.terminal.RemoteControlServer;
import ru.yandex.market.tms.quartz2.remote.config.BaseRemoteControlServerConfig;

/**
 * @author i-milyaev
 */
@Configuration
public class BillingRemoteServerFunctionalTestConfig extends BaseRemoteControlServerConfig {

    private static final String TERMINAL_NAME = "mbi-billing";
    private static final int NO_OPEN_FILES_ACCEPT_DELAY = 2000;

    @Bean
    @DependsOn("remoteControlServer")
    public int port(RemoteControlServer remoteControlServer) throws NoSuchFieldException, IllegalAccessException {
        Field port = remoteControlServer.getClass().getDeclaredField("port");
        port.setAccessible(true);
        return (int) port.get(remoteControlServer);
    }

    @Bean
    public RemoteControlServer remoteControlServer() {
        RemoteControlServer remoteControlServer = new RemoteControlServer();
        remoteControlServer.setTerminalName(TERMINAL_NAME);
        remoteControlServer.setCommandExecutor(commandExecutor());
        remoteControlServer.setNoOpenFilesAcceptDelay(NO_OPEN_FILES_ACCEPT_DELAY);
        return remoteControlServer;
    }
}
