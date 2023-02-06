package ru.yandex.autotests.direct.cmd.steps.images;

import org.hamcrest.Matcher;
import ru.yandex.autotests.directapi.beans.images.ImagesFormats;
import ru.yandex.autotests.directapi.darkside.model.ImageType;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

public class BannerImageUtils {

    public static Matcher<ImagesFormats> getImagesFormatsMatcher(ImageType imageType) {
        switch (imageType) {
            case SMALL: return getSmallTypeFormatsMatcher();
            case REGULAR: return getRegularTypeFormatsMatcher();
            case WIDE: return getWideTypeFormatsMatcher();
            default: throw new IllegalArgumentException("невозможно построить матчер для типа картинки " + imageType.getName());
        }
    }

    public static Matcher<ImagesFormats> getSmallTypeFormatsMatcher() {
        BeanDifferMatcher<ImagesFormats> imagesFormatsMatcher = BeanDifferMatcher.beanDiffer(ImageType.SMALL.getImagesFormats());

        DefaultCompareStrategy strategy = DefaultCompareStrategies.allFields().
                forFields(newPath("x80", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("x90", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y65", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y80", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y90", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y110", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y129", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y150", "width")).useMatcher(not(isEmptyOrNullString()));

        return imagesFormatsMatcher.useCompareStrategy(strategy);
    }

    public static Matcher<ImagesFormats> getRegularTypeFormatsMatcher() {
        BeanDifferMatcher<ImagesFormats> imagesFormatsMatcher = BeanDifferMatcher.beanDiffer(ImageType.REGULAR.getImagesFormats());

        DefaultCompareStrategy strategy = DefaultCompareStrategies.allFields().
                forFields(newPath("x80", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("x90", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("x160", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("x180", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("x450", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y65", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y80", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y90", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y110", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y129", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y150", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y160", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y180", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y300", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("y450", "width")).useMatcher(not(isEmptyOrNullString()));

        return imagesFormatsMatcher.useCompareStrategy(strategy);
    }

    public static Matcher<ImagesFormats> getWideTypeFormatsMatcher() {
        BeanDifferMatcher<ImagesFormats> imagesFormatsMatcher = BeanDifferMatcher.beanDiffer(ImageType.WIDE.getImagesFormats());

        DefaultCompareStrategy strategy = DefaultCompareStrategies.allFields().
                forFields(newPath("wx300", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("wx600", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("wx1080", "height")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("wy150", "width")).useMatcher(not(isEmptyOrNullString())).
                forFields(newPath("wy300", "width")).useMatcher(not(isEmptyOrNullString()));

        return imagesFormatsMatcher.useCompareStrategy(strategy);
    }
}
