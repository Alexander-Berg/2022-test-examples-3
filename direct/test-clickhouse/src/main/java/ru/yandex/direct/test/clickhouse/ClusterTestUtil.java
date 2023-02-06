package ru.yandex.direct.test.clickhouse;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigException;
import ru.yandex.direct.db.config.DbConfigFactory;

@ParametersAreNonnullByDefault
public class ClusterTestUtil {
    private ClusterTestUtil() {
    }

    public static Map<String, DbConfig> getDbConfigChildrenRecursively(DbConfigFactory dbConfigFactory, String root) {
        Map<String, DbConfig> result = new HashMap<>();
        fillDbConfigNodesRecursively(result, dbConfigFactory, root);
        return result;
    }

    private static void fillDbConfigNodesRecursively(Map<String, DbConfig> result, DbConfigFactory dbConfigFactory,
                                                     String root) {
        try {
            result.put(root, dbConfigFactory.get(root));
        } catch (DbConfigException ignored) {
            // Если причиной ошибки было то, что root не является листовым узлом, то следующий код успешно отработает.
            // Если причина в другом (например, нет такого пути), то код упадёт.
        }
        for (String child : dbConfigFactory.getChildNames(root)) {
            fillDbConfigNodesRecursively(result, dbConfigFactory, root + ":" + child);
        }
    }
}
