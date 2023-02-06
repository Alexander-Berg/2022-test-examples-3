package ru.yandex.autotests.innerpochta.screen.differs;

import jxl.format.RGB;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * @author pavponn
 */
public class AntialiasingDiffer {
    private final static double DEFAULT_BRIGHTNESS_TOLERANCE = 0;
    private BufferedImage image1;
    private BufferedImage image2;
    private Set<Point> diffCoordinates;
    private Data data;
    private double brightnessTolerance;

    /** Filter possible diff points taking antialiasing into account.
     *
     * @param image1 first image to compare
     * @param image2 second image to compare
     * @param brightnessTolerance brightness tolerance to determine if pixel is antialiased
     * @param diffCoordinates possible
     * @return Set of diff points
     */
    public static Set<Point> filterDiffPoints(BufferedImage image1, BufferedImage image2, double brightnessTolerance, Set<Point> diffCoordinates) {
        return new  AntialiasingDiffer(image1, image2, brightnessTolerance, diffCoordinates).getPointsWhichLookDifferent();
    }

    private AntialiasingDiffer(BufferedImage image1, BufferedImage image2, double brightnessTolerance, Set<Point> diffCoords) {
        this.image1 = image1;
        this.image2 = image2;
        this.data = new Data(image1);
        this.diffCoordinates = diffCoords;
        this.brightnessTolerance = brightnessTolerance;
    }


    private Set<Point> getPointsWhichLookDifferent() {
        Set<Point> resultDiffPoints = new HashSet<>();
        for (Point pixel: diffCoordinates) {
            if (!isAntialiased(pixel.x, pixel.y)) {
                resultDiffPoints.add(pixel);
            }
        }

        return resultDiffPoints;
    }

    private boolean isAntialiased(int x, int y) {
        return isAntialiased(image1, x, y, image2) || isAntialiased(image2, x, y, image1);
    }

    private class Data {
        int height;
        int width;

        Data(int height, int width) {
            this.height = height;
            this.width = width;
        }

        Data(BufferedImage image) {
            this(image.getHeight(), image.getWidth());
        }
    }

    private boolean isAntialiased(BufferedImage img1, int x1, int y1, BufferedImage img2) {
        final RGB color = intToRGB(img1.getRGB(x1, y1));
        final int x0 = Math.max(x1 - 1, 0);
        final int y0 = Math.max(y1 - 1, 0);
        final int x2 = Math.min(x1 + 1, data.width - 1);
        final int y2 = Math.min(y1 + 1, data.height - 1);

        int zeroes = 0, positives = 0, negatives = 0;
        double min = 0, max = 0;

        int minX = 0, minY = 0, maxX = 0, maxY = 0;

        final boolean checkExtremePixels = (img2 == null);
        final double brightnessTolerance = checkExtremePixels ? this.brightnessTolerance : DEFAULT_BRIGHTNESS_TOLERANCE;

        for (int y = y0; y <= y2; y++) {
            for (int x = x0; x <= x2; x++) {
                if (x == x1 && y == y1) {
                    continue;
                }
                final double delta = brightnessDelta(intToRGB(image1.getRGB(x, y)), color);

                if (Math.abs(delta) <= brightnessTolerance) {
                    zeroes++;
                } else if (delta > brightnessTolerance) {
                    positives++;
                } else {
                    negatives++;
                }

                // if found more than 2 equal siblings, it's definitely not anti-aliasing
                if (zeroes > 2) {
                    return false;
                }

                // remember the darkest pixel
                if (delta < min) {
                    min = delta;
                    minX = x;
                    minY = y;
                }

                // remember the brightest pixel
                if (delta > max) {
                    max = delta;
                    maxX = x;
                    maxY = y;
                }
            }
        }

        if (checkExtremePixels) {
            return true;
        }

        // if there are no both darker and brighter pixels among siblings, it's not anti-aliasing
        if (negatives == 0 || positives == 0) {
            return false;
        }

        // if either the darkest or the brightest pixel has more than 2 equal siblings in both images
        // (definitely not anti-aliased), this pixel is anti-aliased
        return (!isAntialiased(img1, minX, minY, null) && !isAntialiased(img2, minX, minY, null)) ||
            (!isAntialiased(img1, maxX, maxY, null) && !isAntialiased(img2, maxX, maxY, null));

    }

    private double brightnessDelta(RGB color1, RGB color2) {
        return rgbToY(color1) - rgbToY(color2);
    }

    private double rgbToY(RGB rgb) {
        return rgb.getRed() * 0.29889531 + rgb.getGreen() * 0.58662247 + rgb.getBlue() * 0.11448223;
    }

    private static RGB intToRGB(int intRGB) {
        final int blue = intRGB & 255;
        final int green = (intRGB >> 8) & 255;
        final int red = (intRGB >> 16) & 255;
        return new RGB(red, green, blue);
    }
}
