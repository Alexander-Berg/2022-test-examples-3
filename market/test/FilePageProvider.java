package ru.yandex.common.util.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.http.ActionResolver;
import ru.yandex.common.util.http.FileActionResolver;
import ru.yandex.common.util.http.Location;
import ru.yandex.common.util.http.Page;
import ru.yandex.common.util.http.charset.RussianCharsetDetector;
import ru.yandex.common.util.http.loader.PageProvider;
import ru.yandex.common.util.io.IOInterruptedException;

/**
 * User: konst06
 * Date: 20.02.2009
 */
public class FilePageProvider extends PageProvider {
    private static final Logger log = LoggerFactory.getLogger(FilePageProvider.class);

    public Page fetch(Location location) throws IOException, IOInterruptedException {
        try {
            final RussianCharsetDetector detector = new RussianCharsetDetector();
            final byte[] source = IOUtils.readWholeFileToBytes(locationToFile(location));
            final Charset charset = detector.detectActualCharset(source, Charset.defaultCharset());
            log.debug("Detected charset: " + charset.name());
            final String content = new String(source, charset);
            return new Page(location, content);
        } catch (Exception e) {
            log.warn("Can't load page: " + location.getName(), e);
            return new Page(location, "");
        }
    }

    protected String locationToFile(final Location location) throws MalformedURLException {
        return location.unsafeGetAbsoluteURL();
    }

    @Override
    public ActionResolver getActionResolver() {
        return new FileActionResolver();
    }
}
