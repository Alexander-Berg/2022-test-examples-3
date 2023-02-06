package ru.yandex.mail.things.matchers;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 20.03.13
 * Time: 13:31
 */
public class FileCompareMatcher extends TypeSafeMatcher<File> {

    private File expected;
    private String md5Actual;
    private String md5Expected;

    public FileCompareMatcher(File expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(File item) {
        try {
            md5Actual = Files.hash(item, Hashing.md5()).toString();
            md5Expected = Files.hash(expected, Hashing.md5()).toString();

            return md5Actual.equals(md5Expected);
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("файл размером ").appendValue(expected.length())
                .appendText(", md5: ").appendValue(md5Expected);
        try {
            BufferedImage expectedImage = ImageIO.read(expected);
            if (expectedImage != null) {
                description.appendText(String.format("\nразрешение изображения: %s x %s",
                        expectedImage.getHeight(), expectedImage.getWidth()))
                        .appendText(String.format("\nтип изображения: %s",
                                expectedImage.getType()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Factory
    public static FileCompareMatcher hasSameMd5As(File file) {
        return new FileCompareMatcher(file);
    }
}
