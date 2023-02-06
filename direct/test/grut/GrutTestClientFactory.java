package ru.yandex.direct.test.grut;

import ru.yandex.grut.client.GrutClient;
import ru.yandex.grut.client.GrutGrpcClient;
import ru.yandex.grut.client.SingleHostServiceHolder;
import ru.yandex.grut.client.SingleHostServiceHolderKt;
import ru.yandex.grut.client.testlib.ClientProvider;

public class GrutTestClientFactory {
    public static GrutClient getGrutClient() {
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            // для Mac c M1 докер-образа пока нет
            // но можно попробовать воспользоваться удалённым докер-демоном, запущенным на линуксовой виртуалке
            return getGrutClientFromDocker();
        } else if (ru.yandex.devtools.test.Paths.getSandboxResourcesRoot() != null
                && "Linux".equals(System.getProperty("os.name"))) {
            return getGrutClientFromRecipe();
        } else {
            return getGrutClientFromDocker();
        }
    }

    private static GrutClient getGrutClientFromDocker() {
        return new DockerGrutClientProvider().getGrutClient();
    }

    private static GrutClient getGrutClientFromRecipe() {
        return new GrutGrpcClient(
                ClientProvider.createServiceHolder(null),
                null,
                null
        );
    }

    private static GrutClient buildLocalGrutClient() {
        var serviceHolder = new SingleHostServiceHolder("localhost", SingleHostServiceHolderKt.DEFAULT_PORT);
        return new GrutGrpcClient(serviceHolder, null, null);
    }
}
