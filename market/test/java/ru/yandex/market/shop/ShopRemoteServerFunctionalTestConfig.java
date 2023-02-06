package ru.yandex.market.shop;

import java.lang.reflect.Field;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.common.util.terminal.RemoteControlServer;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.tms.quartz2.remote.config.BaseRemoteControlServerConfig;

import static org.mockito.Mockito.mock;

@Configuration
public class ShopRemoteServerFunctionalTestConfig extends BaseRemoteControlServerConfig {

    private static final int NO_OPEN_FILES_ACCEPT_DELAY = 2000;
    @Value("${servant.name}")
    private String terminalName;

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
        remoteControlServer.setTerminalName(terminalName);
        remoteControlServer.setCommandExecutor(commandExecutor());
        remoteControlServer.setNoOpenFilesAcceptDelay(NO_OPEN_FILES_ACCEPT_DELAY);
        return remoteControlServer;
    }

    @Bean
    public PilotSupplierYtDao pilotSupplierYtDao() {
        return mock(PilotSupplierYtDao.class);
    }
}
