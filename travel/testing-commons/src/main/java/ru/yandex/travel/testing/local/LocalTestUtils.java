package ru.yandex.travel.testing.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.yandex.travel.testing.misc.TestResources;

public class LocalTestUtils {
    public static Path getHomeDir() {
        return Path.of(System.getProperty("user.home"));
    }

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String readLocalToken(String homeBasedPath) {
        // just in case we have some Windows users
        homeBasedPath = String.join(File.separator, homeBasedPath.split("/"));
        return TestResources.readFile(LocalTestUtils.getHomeDir().resolve(homeBasedPath));
    }

    public static String readYavSecret(String secretVersion, String secretField) {
        // todo(tlg-13): poor man's yav client, for local tests only!
        // should replace this temporary hack with the proper client:
        // ru.yandex.travel.acceptance.orders.vault.VaultClient (move it out into common libraries)
        try {
            StringBuilder sb = new StringBuilder();
            // most likely won't work as is on Windows
            Process p = Runtime.getRuntime().exec("ya vault get version -n " + secretVersion + " -o " + secretField);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                throw new TimeoutException("Failed to execute the command in 10 seconds.");
            }
            if (p.exitValue() != 0) {
                throw new RuntimeException("Process finished with an error; code " + p.exitValue());
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read secret: " + secretVersion + "; field " + secretField, e);
        }
    }
}
