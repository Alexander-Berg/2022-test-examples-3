package ru.yandex.direct.ydb.testutils.ydbinfo;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class YdbInfoFactory {

    private static volatile boolean inited = false;
    private static YdbInfo ydbInfo;

    private YdbInfoFactory() {

    }

    public static YdbInfo getExecutor() {
        if (!inited) {
            synchronized (YdbInfoFactory.class) {
                if (inited) {
                    return ydbInfo;
                }

                ydbInfo = getInstance();
                ydbInfo.init();
                inited = true;
            }
        }

        return ydbInfo;
    }

    private static YdbInfo getInstance() {
        if (ru.yandex.devtools.test.Paths.getSandboxResourcesRoot() != null
                && "Linux".equals(System.getProperty("os.name"))) {
            return new SandboxYdbInfo();
        } else {
            return new DockerYdbInfo();
        }
    }
}
