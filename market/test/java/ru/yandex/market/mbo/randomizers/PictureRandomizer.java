package ru.yandex.market.mbo.randomizers;

import com.github.javafaker.Faker;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 28.08.2018
 */
public class PictureRandomizer implements Randomizer<Picture> {

    private final EnhancedRandom random;
    private Faker faker;

    private PictureRandomizer(long seed) {
        this.random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(seed)
            .build();

        this.faker = new Faker(new Random(seed));
    }

    public static PictureRandomizer aNewRandomizer(long seed) {
        return new PictureRandomizer(seed);
    }

    @Override
    public Picture getRandomValue() {
        return getRandomImage(ImageType.XL_PICTURE);
    }

    private Picture getRandomImage(ImageType imageType) {
        Picture picture = random.nextObject(Picture.class, "parameterValues");
        picture.setXslName(XslNames.XL_PICTURE);
        picture.setHeight(randomSize(imageType));
        picture.setWidth(randomSize(imageType));
        picture.setOrigMd5(md5());
        picture.setIsWhiteBackground(faker.bool().bool());
        picture.setUrl("://" + faker.internet().url());
        picture.setUrlOrig("://" + faker.internet().url());
        picture.setUrlSource("://" + faker.internet().url() + "/" + faker.internet().slug());
        picture.setModificationSource(random.nextObject(ModificationSource.class));
        return picture;
    }

    protected int randomSize(ImageType imageType) {
        return random.ints(imageType.getMinLongWidth(), imageType.getMaxWidth() + 1).findFirst().getAsInt();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private String md5() {
        return random.ints(4).mapToObj(Integer::toHexString).collect(Collectors.joining());
    }
}
