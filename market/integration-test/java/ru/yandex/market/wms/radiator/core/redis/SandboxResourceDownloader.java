package ru.yandex.market.wms.radiator.core.redis;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SandboxResourceDownloader {
    private static final Logger logger = LoggerFactory.getLogger(SandboxResourceDownloader.class);

    public static void download(long resourceId, File target) {
        String url = "https://proxy.sandbox.yandex-team.ru/" + resourceId;
        int i = 0;
        while(true) {
            i+=1;
            logger.error("Downloading " + url + " to " + target.getAbsolutePath() + ", try " + i);
            try {
                FileUtils.copyURLToFile(new URL(url), target, 60 * 1000, 60 * 1000);
                logger.error("Download successful");
                return;
            } catch (IOException e) {
                logger.error("Download failed ", e);
                if (i >= 3) {
                    ExceptionUtils.rethrow(e);
                    return;
                }
            }
        }
    }
}
