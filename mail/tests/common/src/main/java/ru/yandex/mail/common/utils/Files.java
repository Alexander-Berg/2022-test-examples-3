package ru.yandex.mail.common.utils;

import io.restassured.specification.RequestSpecification;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static java.lang.System.getProperty;

public class Files {
    private static File getLogFileWithoutLogEvent(String prefix, String ext, String hash, boolean strictName) throws IOException {
        String localPrefix = (null != prefix) ? prefix : "logfile_";
        String localExt = (null != ext) ? ext : "txt";

        String dotExt = "." + localExt;
        String logsDir;
        if (getProperty("test.report.dir") == null) {
            logsDir = "files";
        } else {
            logsDir = getProperty("test.report.dir");
        }

        File lDir = new File(logsDir);
        lDir.mkdirs();

        if (strictName) {
            return new File(lDir, localPrefix + dotExt + "_" + hash);
        } else {
            return File.createTempFile(localPrefix, dotExt, lDir);
        }
    }

    private static File impl(String name, RequestSpecification spec, boolean strictName) throws IOException {
        byte[] bytes = spec.log().uri().log().parameters().get().body().asByteArray();

        File file = getLogFileWithoutLogEvent("dowloaded_", name, Integer.toString(Arrays.hashCode(bytes)), strictName);

        LogManager.getLogger(Files.class).info("See log in " + file.getAbsolutePath());
        FileUtils.writeByteArrayToFile(file, bytes);

        return file;
    }

    public static byte[] downloadBytes(String url, String sid, String name) {
        RequestSpecification spec = given().redirects().follow(true);

        return spec.baseUri(url).queryParam("sid", sid).queryParam("name", name).log().uri().log().parameters().get().body().asByteArray();
    }

    public static File downloadFile(String url, String name) throws IOException {
        return impl(name, given().redirects().follow(true).baseUri(url), false);
    }

    public static File downloadFile(String url, String sid, String name) throws IOException {
        return impl(name, given().redirects().follow(true).baseUri(url).queryParam("sid", sid), false);
    }

    public static File downloadFileWithNonrandomFilename(String url, String sid, String name) throws IOException {
        return impl(name, given().redirects().follow(true).baseUri(url).queryParam("sid", sid), true);
    }

    public static File downloadFileWithNonrandomFilename(String url, String name) throws IOException {
        return impl(name, given().redirects().follow(true).baseUri(url), true);
    }
}
