package ru.yandex.market.checkout.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;
import org.apache.zookeeper.ZooKeeperMain;
import org.apache.zookeeper.cli.CliException;
import org.springframework.core.io.Resource;

public class ZooScriptExecutor {
    private final ZooKeeperMain zooKeeperMain;
    private final Resource file;

    public ZooScriptExecutor(ZooKeeperMain zooKeeperMain, Resource file) {
        this.zooKeeperMain = zooKeeperMain;
        this.file = file;
    }

    @PostConstruct
    public void init() throws IOException {
        IOUtils.readLines(file.getInputStream(), StandardCharsets.UTF_8).forEach(line -> {
            if (!line.trim().isEmpty()) {
                try {
                    zooKeeperMain.executeLine(line);
                }  catch (InterruptedException | CliException | IOException e) {
                    throw Throwables.propagate(e);
                }
            }
        });
    }
}
