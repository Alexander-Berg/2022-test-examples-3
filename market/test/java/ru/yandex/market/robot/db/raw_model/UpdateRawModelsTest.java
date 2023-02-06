package ru.yandex.market.robot.db.raw_model;

import com.google.common.collect.Sets;
import io.github.benas.randombeans.randomizers.text.StringRandomizer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;
import ru.yandex.market.robot.db.raw_model.tables.ModelPictureTable;
import ru.yandex.market.robot.shared.raw_model.Picture;
import ru.yandex.market.robot.shared.raw_model.RawModel;
import ru.yandex.market.robot.shared.raw_model.Version;
import ru.yandex.market.test.db.DatabaseTester;
import ru.yandex.market.test.util.random.RandomBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
@RunWith(JUnitParamsRunner.class)
public class UpdateRawModelsTest {
    private static final int SOURCE_ID = 33;
    private static final int ROBOT_AUTHOR_ID = 1;
    private static final String RANDOM_NEW_URL = StringRandomizer.aNewStringRandomizer().getRandomValue();
    private static final Timestamp RANDOM_NEW_TIMESTAMP = Timestamp.from(Instant.now());
    private static final String RANDOM_NEW_DOWNLOAD_ERROR = StringRandomizer.aNewStringRandomizer().getRandomValue();

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();


    @Autowired
    DatabaseTester dataBase;

    @Autowired
    RawModelStorage rawModelStorage;

    @Test
    @Parameters(source = DownloadStatusValuesProvider.class)
    public void whenUpdatePicturesUrlsShouldUpdateModelPictureTable(Picture.DownloadStatus status) {
        RawModel model = saveModel(randomRawModel());

        Picture picture = model.getPictureUrls().iterator().next();
        picture.setUrl(RANDOM_NEW_URL);
        picture.setDownloadTimestamp(RANDOM_NEW_TIMESTAMP);
        picture.setDownloadError(RANDOM_NEW_DOWNLOAD_ERROR);
        picture.setDownloadStatus(status);
        rawModelStorage.updatePicturesUrls(Collections.singletonList(picture));

        dataBase.checkStateMatches(ModelPictureTable.entryFor(model.getId(), picture, false));
    }

    @Test
    public void modelsFoundByRawIdsAfterSaving() {
        RawModel firstModel = saveModel(randomRawModel());

        RawModel secondModel = randomRawModel();
        secondModel.setSourceId(firstModel.getSourceId());
        secondModel.setSourceName(firstModel.getSourceName());
        secondModel.setVendor(firstModel.getVendor());

        secondModel =  saveModel(secondModel);

        List<String> rawIds = Arrays.asList(firstModel.getRawId(), secondModel.getRawId());

        Collection<RawModel> currentModels = rawModelStorage.getModels(
            firstModel.getSourceId(), firstModel.getVendor(), rawIds, false
        );

        Assert.assertEquals(
            Sets.newHashSet(firstModel, secondModel),
            Sets.newHashSet(currentModels)
        );
    }

    private RawModel randomRawModel() {
        RawModel randomModel = RandomBean.generateComplete(RawModel.class);
        Picture randomPicture = RandomBean.generateComplete(Picture.class);
        randomModel.getPictureUrls().clear();
        randomModel.addPictureUrl(randomPicture);
        return randomModel;
    }

    private RawModel saveModel(RawModel rawModel) {
        List<RawModel> savedModels = saveModels(Collections.singletonList(rawModel));

        if (savedModels.size() != 1) {
            throw new IllegalStateException("Saved model not found");
        }

        return savedModels.get(0);
    }

    private List<RawModel> saveModels(List<RawModel> rawModels) {
        Version version = rawModelStorage.createNewVersion(SOURCE_ID, ROBOT_AUTHOR_ID, new Date());
        rawModelStorage.updateModels(version, rawModels);
        IntSet ids = rawModels.stream()
            .mapToInt(RawModel::getId)
            .collect(
                IntOpenHashSet::new,
                IntOpenHashSet::add,
                IntOpenHashSet::addAll
            );
        return rawModelStorage.getModelsByIds(ids);
    }

    public static class DownloadStatusValuesProvider {
        public static Object[] provide() {
            return Arrays.stream(Picture.DownloadStatus.values()).map(value -> new Object[]{value}).toArray();
        }
    }
}
