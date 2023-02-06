package ru.yandex.common.util.locks.distributed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Collection of the utilities for the properites handling.
 *
 * @author maxkar
 *
 */
class PropertiesUtils {

    /**
     * Private constructor of the utilities. Just to prevent instantiation.
     */
    private PropertiesUtils() {
        // nothing, just to prevent instantiation.
    }

    /**
     * Loads a list of properties, applies overrides and combines all properties
     * to a single collection.
     * <p>
     * Properties are loaded from the urls in the specified orders. Later
     * locations have higher priority and overrides properties with the same
     * name in the previous locations.
     * <p>
     * If a file does not exists, then it is ignored.
     * <p>
     * <code>null</code> files are ignored. If there is no files, or
     * <code>File[]</code> is <code>null</code>, then this methods returns
     * an instance of the {@link Properties} without any properties set.
     *
     * @see #load(java.io.File[])
     *
     * @param files
     *            files to read properties from.
     * @return a merged properties.
     * @throws IOException
     *             if some I/O error happens.
     */
    public static Properties loadFromFiles(String... files) throws IOException {
        if (files == null || files.length == 0)
            return new Properties();
        final File[] fls = new File[files.length];
        for (int i = 0; i < fls.length; i++)
            fls[i] = files[i] == null ? null : new File(files[i]);
        return load(fls);
    }

    /**
     * Loads a list of properties, applies overrides and combines all properties
     * to a single collection.
     * <p>
     * Properties are loaded from the urls in the specified orders. Later
     * locations have higher priority and overrides properties with the same
     * name in the previous locations.
     * <p>
     * If a file does not exists, then it is ignored.
     * <p>
     * <code>null</code> files are ignored. If there is no files, or
     * <code>File[]</code> is <code>null</code>, then this methods returns
     * an instance of the {@link Properties} without any properties set.
     *
     * @see #load(java.net.URL[])
     *
     * @param files
     *            files to read properties from.
     * @return a merged properties.
     * @throws IOException
     *             if some I/O error happens.
     */
    public static Properties load(File... files) throws IOException {
        if (files == null || files.length == 0)
            return new Properties();
        final URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++)
            urls[i] = files[i] == null ? null : files[i].toURI().toURL();
        return load(urls);
    }

    /**
     * Loads a list of properties, applies overrides and combines all properties
     * to a single collection.
     * <p>
     * Properties are loaded from the urls in the specified orders. Later
     * locations have higher priority and overrides properties with the same
     * name in the previous locations.
     * <p>
     * If a connection to the some location can't be opened (thrown I/O
     * exception during opening), then this location is ignored. It is useful
     * for a set of the properties overrides (for example, system settings, user
     * settings, and project settings).
     * <p>
     * <code>null</code> locations are ignored. If there is no locations, or
     * <code>URL[]</code> is <code>null</code>, then this methods returns
     * an instance of the {@link Properties} without any properties set.
     *
     * @param locations
     *            location of the properties.
     * @return a merged existing properties.
     * @throws IOException
     *             if some I/O error happens during the read.
     */
    public static Properties load(URL... locations) throws IOException {
        final Properties result = new Properties();
        if (locations == null)
            return result;
        for (URL url : locations)
            loadInto(url, result);
        return result;
    }

    /**
     * Loads a set of properties into the specified {@link Properties}. Call to
     * this method overrides an existing collection of properties with all the
     * data, contained at the specified <code>url</code>.
     * <p>
     * If <code>url</code> is <code>null</code> or can't be opened (throwed
     * {@link IOException} during {@link URL#openStream()}, then this method
     * does nothing.
     *
     * @param url
     *            url to load properties from.
     * @param result
     *            properties to load data into.
     * @throws IOException
     *             if connection was opened (<code>URL.openStream()</code>
     *             succeded), but then there was an I/O exception.
     */
    private static void loadInto(URL url, Properties result)
            throws IOException {
        if (url == null)
            return;
        final InputStream connStream;
        try {
            connStream = url.openStream();
        } catch (IOException e) {
            /*
             * Just as specified, ignore broken connections.
             */
            return;
        }
        /*
         * There is no guaranties, that java.util.Properties does not clean
         * itself during the call to the load() method. We must use separate
         * instance.
         */
        final Properties overrides;
        try {
            overrides = new Properties();
            overrides.load(connStream);
        } finally {
            connStream.close();
        }
        result.putAll(overrides);
    }
}
