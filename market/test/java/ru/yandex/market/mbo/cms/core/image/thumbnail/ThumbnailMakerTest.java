package ru.yandex.market.mbo.cms.core.image.thumbnail;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThumbnailMakerTest {

    @Test
    public void guessImageFormat() throws Exception {
        assertEquals("gif", ThumbnailMaker.guessImageFormat(picAsBytes("gif_pic.gif")));
        assertEquals("gif", ThumbnailMaker.guessImageFormat(picAsBytes("gif_pic.jpg")));
        assertEquals("gif", ThumbnailMaker.guessImageFormat(picAsBytes("gif_pic.png")));
        assertEquals("jpeg", ThumbnailMaker.guessImageFormat(picAsBytes("jpg_pic.gif")));
        assertEquals("jpeg", ThumbnailMaker.guessImageFormat(picAsBytes("jpg_pic.jpg")));
        assertEquals("jpeg", ThumbnailMaker.guessImageFormat(picAsBytes("jpg_pic.png")));
        assertEquals("png", ThumbnailMaker.guessImageFormat(picAsBytes("png_pic.gif")));
        assertEquals("png", ThumbnailMaker.guessImageFormat(picAsBytes("png_pic.jpg")));
        assertEquals("png", ThumbnailMaker.guessImageFormat(picAsBytes("png_pic.png")));
    }

    private static byte[] picAsBytes(String picFile) throws IOException {
        return IOUtils.toByteArray(ThumbnailMaker.class.getClassLoader().getResourceAsStream("images/" + picFile));
    }
}
