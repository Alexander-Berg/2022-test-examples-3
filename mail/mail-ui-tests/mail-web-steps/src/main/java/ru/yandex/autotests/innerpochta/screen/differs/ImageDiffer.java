package ru.yandex.autotests.innerpochta.screen.differs;

import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.DiffMarkupPolicy;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageMarkupPolicy;
import ru.yandex.qatools.ashot.comparison.PointsMarkupPolicy;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static ru.yandex.qatools.ashot.util.ImageBytesDiffer.areImagesEqual;
import static ru.yandex.qatools.ashot.util.ImageTool.rgbCompare;

/**
 * @author pavponn
 */
public class ImageDiffer {

    private static final int DEFAULT_COLOR_DISTORTION = 15;

    private int colorDistortion = DEFAULT_COLOR_DISTORTION;
    private DiffMarkupPolicy diffMarkupPolicy = new PointsMarkupPolicy();
    private Color ignoredColor = null;

    /**
     * Specifies the color to be ignored.
     *
     * @param ignoreColor color
     * @return self for fluent style
     */
    public ImageDiffer withIgnoredColor(final Color ignoreColor) {
        this.ignoredColor = ignoreColor;
        return this;
    }

    /**
     * Specifies the color distortion.
     *
     * @param distortion color distortion
     * @return self for fluent style
     */
    public ImageDiffer withColorDistortion(int distortion) {
        this.colorDistortion = distortion;
        return this;
    }

    /**
     * Specifies the diff markup policy.
     *
     * @param diffMarkupPolicy diff markup policy instance
     * @return self for fluent style
     * @see ImageMarkupPolicy
     * @see PointsMarkupPolicy
     */
    public ImageDiffer withDiffMarkupPolicy(final DiffMarkupPolicy diffMarkupPolicy) {
        this.diffMarkupPolicy = diffMarkupPolicy;
        return this;
    }

    /** Make diff image based on two screenshots.
     *
     * @param expected expected screenshot
     * @param actual actual screenshot
     * @return diff image
     */
    public ImageDiff makeDiff(Screenshot expected, Screenshot actual) {
        ImageDiff diff = new ImageDiff(diffMarkupPolicy);

        if (areImagesEqual(expected, actual)) {
            diff.setDiffImage(actual.getImage());
        } else {
            markDiffPoints(expected, actual, diff);
        }

        return diff;
    }

    protected void markDiffPoints(Screenshot expected, Screenshot actual, ImageDiff diff) {
        Coords expectedImageCoords = Coords.ofImage(expected.getImage());
        Coords actualImageCoords = Coords.ofImage(actual.getImage());

        CoordsSet compareCoordsSet = new CoordsSet(CoordsSet.union(actual.getCoordsToCompare(), expected.getCoordsToCompare()));
        CoordsSet ignoreCoordsSet = new CoordsSet(CoordsSet.intersection(actual.getIgnoredAreas(), expected.getIgnoredAreas()));

        int width = Math.max(expected.getImage().getWidth(), actual.getImage().getWidth());
        int height = Math.max(expected.getImage().getHeight(), actual.getImage().getHeight());
        diff.setDiffImage(createDiffImage(expected.getImage(), actual.getImage(), width, height));

        Set<Point> diffPoints = new HashSet<>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (ignoreCoordsSet.contains(i, j)) {
                    continue;
                }
                if (!isInsideBothImages(i, j, expectedImageCoords, actualImageCoords)
                    || compareCoordsSet.contains(i, j) && hasDiffInChannel(expected, actual, i, j)) {
                    // INSTEAD OF ADDING a diff point to diff, we add it to the set of diffPoints
                    // and only after will mark according to AntialiasingComparator
                    diffPoints.add(new Point(i, j));
                }
            }
        }

        // If images sizes are different we don't do antialising comparision
        if (expected.getImage().getWidth() != actual.getImage().getWidth() ||
            expected.getImage().getHeight() != actual.getImage().getHeight()) {

            for (Point p: diffPoints) {
                diff.addDiffPoint(p.x, p.y);
            }

            return;
        }

        final double BRIGHTNESS_TOLERANCE = 2.3;

        Set<Point> realDiff = AntialiasingDiffer.filterDiffPoints(
            expected.getImage(),
            actual.getImage(),
            BRIGHTNESS_TOLERANCE,
            diffPoints
        );

        for (Point p: realDiff) {
            diff.addDiffPoint(p.x, p.y);
        }
    }

    private boolean hasDiffInChannel(Screenshot expected, Screenshot actual, int i, int j) {
        if(ignoredColor != null && rgbCompare(expected.getImage().getRGB(i, j), ignoredColor.getRGB(), 0)) {
            return false;
        }

        return !rgbCompare(expected.getImage().getRGB(i, j), actual.getImage().getRGB(i, j), colorDistortion);
    }

    private BufferedImage createDiffImage(BufferedImage expectedImage, BufferedImage actualImage, int width, int height) {
        BufferedImage diffImage = new BufferedImage(width, height, actualImage.getType());
        paintImage(actualImage, diffImage);
        paintImage(expectedImage, diffImage);
        return diffImage;
    }

    private void paintImage(BufferedImage image, BufferedImage diffImage) {
        Graphics graphics = diffImage.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
    }

    private boolean isInsideBothImages(int i, int j, Coords expected, Coords actual) {
        return expected.contains(i, j) && actual.contains(i, j);
    }

    private static class CoordsSet {

        private final boolean isSingle;
        private final Coords minRectangle;
        private Set<Coords> coordsSet;

        public CoordsSet(Set<Coords> coordsSet) {
            isSingle = coordsSet.size() == 1;
            this.coordsSet = coordsSet;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = 0;
            int maxY = 0;
            for (Coords coords : coordsSet) {
                minX = Math.min(minX, (int) coords.getMinX());
                minY = Math.min(minY, (int) coords.getMinY());
                maxX = Math.max(maxX, (int) coords.getMaxX());
                maxY = Math.max(maxY, (int) coords.getMaxY());
            }
            minRectangle = new Coords(minX, minY, maxX - minX, maxY - minY);
        }

        private boolean contains(int i, int j) {
            return inaccurateContains(i, j) && accurateContains(i, j);
        }

        private boolean inaccurateContains(int i, int j) {
            return minRectangle.contains(i, j);
        }

        private boolean accurateContains(int i, int j) {
            if (isSingle) {
                return true;
            }
            for (Coords coords : coordsSet) {
                if (coords.contains(i, j)) {
                    return true;
                }
            }
            return false;
        }

        private static Set<Coords> intersection(Set<Coords> coordsPool1, Set<Coords> coordsPool2) {
            return Coords.intersection(coordsPool1, coordsPool2);
        }

        private static Set<Coords> union(Set<Coords> coordsPool1, Set<Coords> coordsPool2) {
            Set<Coords> coordsPool = new LinkedHashSet<>();
            coordsPool.addAll(coordsPool1);
            coordsPool.addAll(coordsPool2);
            return coordsPool;
        }
    }
}
