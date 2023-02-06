package ru.yandex.autotests.direct.cmd.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtils {

    public enum ImageFormat {
        JPG, PNG, GIF, BMP;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public static BufferedImage createRandomImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (Math.random() < 0.5) {
                    image.setRGB(x, y, Color.BLUE.getRGB());
                }
            }
        }
        drawSignificantLines(image, Color.RED, new BasicStroke());
        return image;
    }

    public static void writeToFile(BufferedImage img, ImageFormat format, File f) {
        try {
            ImageIO.write(img, format.toString(), f);
        } catch (IOException e) {
            throw new IllegalStateException("ошибка сохранения картинки в файл", e);
        }
    }

    public static byte[] writeToByteArray(BufferedImage img, ImageFormat format) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(img, format.toString(), stream);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("ошибка сохранения картинки в массив байтов", e);
        }
    }

    public static File createImageInTempFile(int width, int height, ImageFormat format) {
        File imageFile = createTempFile(width, height, format.toString());
        writeToFile(createRandomImage(width, height), format, imageFile);
        return imageFile;
    }

    private static File createTempFile(int width, int height, String format) {
        try {
            return File.createTempFile(width + "x" + height + "__", "." + format);
        } catch (IOException e) {
            throw new IllegalStateException("ошибка создания временного файла", e);
        }
    }

    private static void drawSignificantLines(BufferedImage image, Color color, Stroke stroke) {
        int width = image.getWidth();
        int height = image.getHeight();
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setColor(color);
        g2d.setStroke(stroke);
        // lines from top-left
        g2d.drawLine(0, 0, width/2, height);
        g2d.drawLine(0, 0, width, height/2);
        // lines from bottom-left
        g2d.drawLine(0, height, width/2, 0);
        g2d.drawLine(0, height, width, height/2);
        // lines from bottom-right
        g2d.drawLine(width, height, 0, height/2);
        g2d.drawLine(width, height, width/2, 0);
        // lines from top-right
        g2d.drawLine(width, 0, 0, height/2);
        g2d.drawLine(width, 0, width/2, height);
        g2d.dispose();
    }
}
