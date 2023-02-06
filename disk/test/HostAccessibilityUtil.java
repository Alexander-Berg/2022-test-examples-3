package ru.yandex.chemodan.app.dataapi.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class HostAccessibilityUtil {
    private static final Logger logger = LoggerFactory.getLogger(HostAccessibilityUtil.class);

    public static boolean isAccessible(URI url) {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.toURL().openConnection();
            connection.setConnectTimeout(500);
        } catch (IOException e) {
            throw ExceptionUtils.translate(e);
        }

        try {
            connection.setRequestMethod("HEAD");
            connection.connect();
            logger.info("Connection to {} successful", url);
            return true;
        } catch (ProtocolException e) {
            throw ExceptionUtils.translate(e);
        } catch (IOException e) {
            Option<String> messageO = Option.ofNullable(e.getMessage());
            if (messageO.isMatch(m -> m.toLowerCase().contains("connect"))) {
                logger.info("Could not connect to {}", url);
                return false;
            }

            logger.warn("Error while querying url = {}", url, e);
            // TODO: retry
        } finally {
            connection.disconnect();
        }

        return false;
    }
}
