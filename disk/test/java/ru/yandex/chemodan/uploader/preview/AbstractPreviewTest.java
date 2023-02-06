package ru.yandex.chemodan.uploader.preview;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.uploader.config.ImageAnnotationContextConfiguration;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.IoUtils;
import ru.yandex.misc.io.RuntimeIoException;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.io.file.File2;

/**
 * @author metal
 */
@ContextConfiguration(classes = {ImageAnnotationContextConfiguration.class})
public abstract class AbstractPreviewTest extends AbstractTest {
    protected File2 tmpDir;

    @Before
    public void init() {
        tmpDir = File2.createNewTmpDir("kladun-preview-test");
    }

    @After
    public void destroy() {
        tmpDir.deleteRecursive();
    }

    protected File2 getImage(String fileName) {
        try {
            InputStreamSource image = ClassLoaderUtils.streamSourceForResource(getClass(), fileName);
            File2 imageCopy = tmpDir.child(fileName);
            IoUtils.copy(image.getInput(), imageCopy.asOutputStreamTool().getOutput());
            return imageCopy;
        } catch (IOException e) {
            throw new RuntimeIoException(e);
        }
    }
}
