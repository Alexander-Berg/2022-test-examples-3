package ru.yandex.autotests.market.stat.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import ru.yandex.autotests.market.common.attacher.Attacher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by entarrion on 09.02.15.
 */
public class FileReaderUtils {

    private static final int MAX_LINES_TO_READ = 10000;

    public static Map<String, List<String>> getMatches(String stringUrl, String... masks) {
        return getMatches(stringUrl, Arrays.asList(masks));
    }

    public static Map<String, List<String>> getMatches(String stringUrl, List<String> masks) {
        Attacher.attachRequest(stringUrl);
        return getMatches(stringUrl, getPattern(masks));
    }

    private static Pattern getPattern(List<String> masks) {
        Pattern pattern = Pattern.compile(".*");
        if (masks != null && masks.size() > 0) {
            pattern = Pattern.compile(masks.size() > 1 ? StringUtils.join(masks, "|") : masks.get(0));
        }
        return pattern;
    }

    private static Map<String, List<String>> getMatches(String stringUrl, Pattern stringsPattern) {
        Pattern pattern = Pattern.compile("\\.txt|\\.db|\\.md5|.ctr*|_sort|_ratio");
        return getMatchesFromTarGz(stringUrl, pattern, stringsPattern);
    }

    public static List<String> getDataFromFile(String path, int lines) {
        try {
            File file = new File(path);
            FileInputStream stream = new FileInputStream(file);
            Pattern pattern = Pattern.compile(".*");
            return searchItemsInFile(pattern, stream, lines);
        } catch (IOException e) {
            throw new IllegalArgumentException("Problems while reading from file " + path);
        }
    }

    public static List<String> getDataFromFile(String path) {
        return getDataFromFile(path, MAX_LINES_TO_READ);
    }

    public static List<String> getMatchesFromGzFileHttp(String stringUrl, List<String> masks) {
        HttpURLConnection urlConnection = getHttpURLConnection(stringUrl);
        return getMatchesFromTarGzipConnection(masks, urlConnection);
    }

    public static Map<String, List<String>> getMatchesFromZipFileHttp(String stringUrl, List<String> masks) {
        HttpURLConnection urlConnection = getHttpURLConnection(stringUrl);
        return getMatchesFromZipConnection(masks, urlConnection);
    }

    private static List<String> getMatchesFromTarGzipConnection(List<String> masks, HttpURLConnection urlConnection) {
        List<String> matches = new ArrayList<>();
        try {
            InputStream inputStream = urlConnection.getInputStream();
            matches = getMatchesFromGzipFileStream(masks, inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            urlConnection.disconnect();
        }
        return matches;
    }

    private static Map<String, List<String>> getMatchesFromZipConnection(List<String> masks, HttpURLConnection urlConnection) {
        Map<String, List<String>> matches = new HashMap<>();
        try {
            InputStream inputStream = urlConnection.getInputStream();
            matches = getMatchesFromZipFileStream(Pattern.compile(".*"), masks, inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            urlConnection.disconnect();
        }
        return matches;
    }


    public static Map<String, List<String>> getMatchesFromTarGz(String stringUrl, Pattern fileNamePattern, Pattern stringsPattern) {
        HttpURLConnection urlConnection = getHttpURLConnection(stringUrl);
        Map<String, List<String>> result = new HashMap<>();
        try {
            result = getMatchesFromTagGzFileStream(fileNamePattern, stringsPattern, urlConnection.getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            urlConnection.disconnect();
        }
        return result;
    }

    public static List<String> getMatchesFromGzipFileStream(List<String> masks, InputStream inputStream) {
        Pattern stringsPattern = getPattern(masks);
        List<String> matches;
        try {
            InputStream in = new BufferedInputStream(inputStream);
            GZIPInputStream gzipInputStream = new GZIPInputStream(in);
            matches = searchItemsInFile(stringsPattern, gzipInputStream, MAX_LINES_TO_READ);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        return matches;
    }

    public static Map<String, List<String>> getMatchesFromZipFileStream(Pattern fileNamePattern, List<String> masks, InputStream inputStream) throws IOException {
        Map<String, List<String>> result = new HashMap<>();
        Pattern stringsPattern = getPattern(masks);
        InputStream in = new BufferedInputStream(inputStream);
        ZipInputStream zipInputStream = new ZipInputStream(in);
        ZipEntry entryx;
        while ((entryx = zipInputStream.getNextEntry()) != null) {
            if (!entryx.isDirectory()) {
                if (fileNamePattern != null && fileNamePattern.matcher(entryx.getName()).find()) {
                    List<String> matches = searchItemsInFile(stringsPattern, zipInputStream, MAX_LINES_TO_READ);
                    result.put(entryx.getName().replaceAll("\\./", ""), matches);
                }
            }
        }
        return result;
    }

    public static Map<String, Long> getFileSizesFromZipFileStream(String stringUrl) {
        Map<String, Long> result = new HashMap<>();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(getHttpURLConnection(stringUrl).getInputStream()));
            ZipEntry entryx;
            while ((entryx = zipInputStream.getNextEntry()) != null) {
                if (!entryx.isDirectory()) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int b;
                    while ((b = zipInputStream.read()) != -1)
                        os.write(b);
                    result.put(entryx.getName().replaceAll("\\./", ""), Long.valueOf(os.size()));
                }
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public static Map<String, List<String>> getMatchesFromTagGzFileStream(String filename, List<String> masks, InputStream istream) throws IOException {
        return getMatchesFromTagGzFileStream(getPattern(Collections.singletonList(filename)), getPattern(masks), istream);
    }

    public static Map<String, List<String>> getMatchesFromTagGzFileStream(Pattern fileNamePattern, Pattern stringsPattern, InputStream istream) throws IOException {
        Map<String, List<String>> result = new HashMap<>();
        InputStream in = new BufferedInputStream(istream);
        TarInputStream is = new TarInputStream(new GZIPInputStream(in));
        TarEntry entryx;
        while ((entryx = is.getNextEntry()) != null) {
            if (!entryx.isDirectory()) {
                if (fileNamePattern != null && fileNamePattern.matcher(entryx.getName()).find()) {
                    List<String> matches = searchItemsInFile(stringsPattern, is, MAX_LINES_TO_READ);
                    result.put(entryx.getName().replaceAll("\\./", ""), matches);
                }
            }
        }
        return result;
    }

    public static List<String> getMatchesFromFile(String stringUrl, Pattern stringsPattern, int maxLines) {
        List<String> result = new ArrayList<>();
        HttpURLConnection urlConnection = getHttpURLConnection(stringUrl);
        try {
            result = searchItemsInFile(stringsPattern, urlConnection.getInputStream(), maxLines);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            urlConnection.disconnect();
        }
        return result;
    }

    public static Long getFileSize(String stringUrl) {
        return getHttpURLConnection(stringUrl).getContentLengthLong();
    }

    public static List<String> getMatchesFromFile(String stringUrl, Pattern stringsPattern) {
        return getMatchesFromFile(stringUrl, stringsPattern, MAX_LINES_TO_READ);
    }

    public static List<String> getMatchesFromFile(String stringUrl, String... masks) {
        return getMatchesFromFile(stringUrl, Arrays.asList(masks));
    }

    public static List<String> getMatchesFromFile(String stringUrl, List<String> masks) {
        return getMatchesFromFile(stringUrl, getPattern(masks));
    }

    private static List<String> searchItemsInFile(Pattern stringsPattern, InputStream is, int maxLines) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String str;
        List<String> matches = new ArrayList<>();
        while ((str = reader.readLine()) != null && matches.size() < maxLines) {
            if (stringsPattern != null && stringsPattern.matcher(str).find()) {
                matches.add(str);
            }
        }
        return matches;
    }

    public static HttpURLConnection getHttpURLConnection(String stringUrl) {
        HttpURLConnection urlConnection;
        try {
            URL url = new URL(stringUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Wrong url.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Can't open connection.", e);
        }
        return urlConnection;
    }

}
