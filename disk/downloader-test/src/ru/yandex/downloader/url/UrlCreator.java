package ru.yandex.downloader.url;

import com.ning.http.util.Base64;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.downloader.crypt.AesCrypt;
import ru.yandex.downloader.crypt.HmacSha256Digest;
import ru.yandex.downloader.crypt.ZaberunSecrets;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.lang.CharsetUtils;
import ru.yandex.misc.lang.StringUtils;

/**
 * @author akirakozov
 */
public class UrlCreator {
    private static final String DOWNLOADER_HOST = "http://downloader.dst.yandex.ru";
    //private static final String DOWNLOADER_HOST = "http://downloader.dsd.yandex.ru";
    //private static final String DOWNLOADER_HOST = "http://akirakozov.haze.yandex.net";

    private static final HmacSha256Digest tokenCalculator = new HmacSha256Digest(ZaberunSecrets.TOKEN_SECRET);
    private static final AesCrypt targetRefEncrypter = new AesCrypt(ZaberunSecrets.STID_ENCRYPT_SECRET);

    public static String createPreviewUrl(BaseUrlParams params) {
        return createBaseUrl("preview", params);
    }

    public static String createDiskUrl(BaseUrlParams params) {
        return createBaseUrl("disk", params);
    }

    public static String createInstallerUrl(BaseUrlParams params) {
        return createBaseUrl("share", params);
    }

    public static String createZipUrl(BaseUrlParams params) {
        return createBaseUrl("zip", params);
    }

    public static String createZipAlbumUrl(BaseUrlParams params) {
        return createBaseUrl("zip-album", params);
    }

    public static String createZipFilesUrl(BaseUrlParams params) {
        return createBaseUrl("zip-files", params);
    }

    public static String createBaseUrl(String handleName, BaseUrlParams params) {
        String token = calculateToken(params);
        String encryptedTargetRef = calculateEncryptedTargetRef(params.targetRef);
        String url = String.format(DOWNLOADER_HOST + "/" + handleName + "/%s/%s/%s",
                token, params.getHexTimestamp(), encryptedTargetRef);

        MapF<String, Object> paramsToAdd =
                Cf.<String, Object>map("uid", params.getUidAsString())
                        .plus1("filename", params.fileName)
                        .plus1("disposition", params.disposition.name().toLowerCase())
                        .plus1("tknv", "v2");

        if (!params.contentType.equals("application/zip")) {
            paramsToAdd = paramsToAdd.plus1("content_type", params.contentType);
        }
        if (params.hash.isDefined()) {
            paramsToAdd = paramsToAdd.plus1("hash", params.hash.get());
        }
        if (params.limit.isDefined()) {
            paramsToAdd = paramsToAdd.plus1("limit", params.getLimitAsString());
        }
        if (params.autoLogin.isDefined()) {
            paramsToAdd = paramsToAdd.plus1("al", params.getAutoLoginAsString());
        }
        if (params.src.isDefined()) {
            paramsToAdd = paramsToAdd.plus1("src", params.src.get());
        }

        return UrlUtils.addParameters(url, paramsToAdd);
    }

    private static String calculateEncryptedTargetRef(TargetReference targetRef) {
        if (targetRef instanceof MulcaTargetId) {
            return UrlUtils.urlEncode(targetRefEncrypter.encryptInBase64(targetRef.getValue()));
        } else if (targetRef instanceof StringTargetId) {
            return UrlUtils.urlEncode(Base64.encode(targetRef.getValue().getBytes(CharsetUtils.UTF8_CHARSET)));
        } else {
            throw new IllegalArgumentException("");
        }
    }

    public static String calculateToken(BaseUrlParams params) {
        return UrlUtils.urlEncode(tokenCalculator.getDigestInHex(
                StringUtils.join(params.getParamsForTokenAsListOfStrings(), "-")));
    }
}
