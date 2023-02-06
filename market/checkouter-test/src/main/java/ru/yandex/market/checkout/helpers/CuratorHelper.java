package ru.yandex.market.checkout.helpers;

import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Assertions;

import ru.yandex.market.checkout.common.TestHelper;

@TestHelper
public class CuratorHelper {

    private final CuratorFramework curator;

    public CuratorHelper(CuratorFramework curator) {
        this.curator = curator;
    }

    public int queueSize(String path) throws Exception {
        return curator.checkExists().forPath(path).getNumChildren();
    }

    public void waitForEmptyChildren(String path) throws Exception {
        int count = 0;
        while (curator.checkExists().forPath(path).getNumChildren() != 0) {
            // Нужно подождать пока задачку достанут из очереди.
            Thread.sleep(100);
            if (count++ > 100) {
                Assertions.fail("Sleep is too long");
            }
        }
    }

    public void waitForEmptyChildrenInCycle(String path) throws Exception {
        // Цикл вместо sleep
        for (int i = 0; i < 10_000; i++) {
            if (curator.getChildren().forPath(path).size() == 0) {
                break;
            }
        }
    }

    public void waitForQueuedTasksPropagation(String path) throws Exception {
        List<String> currentChildren = curator.getChildren().forPath(path);
        if (currentChildren.size() == 0) {
            return;
        }

        int count = 0;
        List<String> children;
        do {
            children = curator.getChildren().forPath(path);
            Thread.sleep(100);
            if (count++ > 50) {
                Assertions.fail("Sleep is too long");
            }
        } while (children != null && !Collections.disjoint(children, currentChildren));
    }
}
