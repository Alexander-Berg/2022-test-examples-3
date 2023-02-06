package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.domain.v2.Image;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.market.api.ApiMatchers.map;

public class ImageMatcher {
    public static Matcher<? extends Image> image(String url, int width, int height) {
        return allOf(
            map(Image::getUrl, "'url'", is(url)),
            map(Image::getWidth, "'width'", is(width)),
            map(Image::getHeight, "'height'", is(height))
        );
    }

    public static String toStr(Image image) {
        if (null == image) {
            return "null";
        }
        return MoreObjects.toStringHelper(Image.class)
            .add("url", image.getUrl())
            .add("width", image.getWidth())
            .add("height", image.getHeight())
            .toString();
    }
}
