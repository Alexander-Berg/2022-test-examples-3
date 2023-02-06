package ru.yandex.bannerstorage.harvester.queues.processdynamiccode;

import java.util.Arrays;
import java.util.List;

import ru.yandex.bannerstorage.harvester.queues.processdynamiccode.infrastracture.ImageStorageService;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author egorovmv
 */
public class ProcessDynamicCodeTest {
    protected final static String SOURCE_IMG_URL = "http://localhost/source";
    protected final static String DEST_IMG_URL = "http://localhost/dest";
    protected final static String REL_SOURCE_IMG_URL = "path/source.jpg";
    protected final static String REL_DEST_IMG_URL = "path/dest.jpg";
    protected final static String REL_SOURCE_IMG_URL_WITHOUT_EXT = "path/source";
    protected final static String REL_DEST_IMG_URL_WITHOUT_EXT = "path/dest";

    protected static List<String> getValidImageExtensions() {
        return Arrays.asList(".jpg", ".jpeg", ".gif", ".png");
    }

    protected static ImageStorageService.ImageUploader createImageUploader(String resultImageUrl) {
        ImageStorageService.ImageUploader result = mock(ImageStorageService.ImageUploader.class);
        when(result.uploadImage(anyString())).thenReturn(resultImageUrl);
        return result;
    }
}
