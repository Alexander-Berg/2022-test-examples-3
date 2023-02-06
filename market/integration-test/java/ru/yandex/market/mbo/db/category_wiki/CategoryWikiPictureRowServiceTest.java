package ru.yandex.market.mbo.db.category_wiki;

import com.google.common.io.ByteStreams;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.mbo.common.imageservice.avatars.AvatarImageDepotService;
import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.configs.category_wiki.CategoryWikiConfiguration;
import ru.yandex.market.mbo.core.images.HyperImageDepotService;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWikiPictureRow;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * @author eremeevvo
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfiguration.class,
    CategoryWikiPictureRowServiceTest.NullBeansConfiguration.class,
    CategoryWikiConfiguration.class
})
public class CategoryWikiPictureRowServiceTest {

    private static final int CATEGORY1_ID = 1;

    private static final int CATEGORY2_ID = 2;

    private static final CategoryWikiPictureRow PICTURE_ROW1 = new CategoryWikiPictureRow()
        .setCategoryId(CATEGORY1_ID).setComments("comment1");

    private static final CategoryWikiPictureRow PICTURE_ROW2 = new CategoryWikiPictureRow()
        .setCategoryId(CATEGORY1_ID).setComments("comment2");

    private static final CategoryWikiPictureRow PICTURE_ROW3 = new CategoryWikiPictureRow()
        .setCategoryId(CATEGORY1_ID).setComments("comment3");

    private static final CategoryWikiPictureRow PICTURE_ROW4 = new CategoryWikiPictureRow()
        .setCategoryId(CATEGORY2_ID).setComments("to_insert");


    private static final int PIC_FILENAME_LENGTH = 10;
    private static final String PIC_FILENAME_CHARS = "abcdefghijklmnopqrstuvwxyz";
    public static final int IMAGE_WIDTH = 600;
    public static final int IMAGE_HEIGHT = 600;

    @Autowired
    private CategoryWikiPictureRowService categoryWikiPictureRowService;

    @Resource
    private AvatarImageDepotService avatarImageDepotService;

    @Before
    @Transactional("categoryWikiTransactionManager")
    public void setUp() throws Exception {
        categoryWikiPictureRowService.insertCategoryWikiPictureRow(PICTURE_ROW1, 0);
        categoryWikiPictureRowService.insertCategoryWikiPictureRow(PICTURE_ROW2, 1);
        categoryWikiPictureRowService.insertCategoryWikiPictureRow(PICTURE_ROW3, 2);
    }

    @Test
    @Transactional("categoryWikiTransactionManager")
    public void getCategoryWikiPictureRowsTest() {
        List<CategoryWikiPictureRow> result = categoryWikiPictureRowService.getCategoryWikiPictureRows(CATEGORY1_ID);
        Assertions.assertThat(result)
            .usingElementComparatorOnFields("comments", "categoryId")
            .containsExactly(PICTURE_ROW1, PICTURE_ROW2, PICTURE_ROW3);
    }

    @Test
    @Transactional("categoryWikiTransactionManager")
    public void deleteCategoryWikiPictureRowsForCategoryTest() {
        categoryWikiPictureRowService.deleteCategoryWikiPictureRowsForCategory(CATEGORY1_ID);
        List<CategoryWikiPictureRow> result = categoryWikiPictureRowService.getCategoryWikiPictureRows(CATEGORY1_ID);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    @Transactional("categoryWikiTransactionManager")
    public void insertCategoryWikiPictureRowTest() {
        categoryWikiPictureRowService.insertCategoryWikiPictureRow(PICTURE_ROW4, 1);
        List<CategoryWikiPictureRow> result = categoryWikiPictureRowService.getCategoryWikiPictureRows(CATEGORY2_ID);
        Assertions.assertThat(result)
            .usingElementComparatorOnFields("comments", "categoryId")
            .containsExactly(PICTURE_ROW4);
    }

    @Test
    @Ignore // Teamcity have troubles in uploading images
    @Transactional("categoryWikiTransactionManager")
    public void testResizeBigImage() throws IOException {
        testResizeImage("/mbo-core/assets/test_big.jpg");
    }

    @Test
    @Ignore // Teamcity have troubles in uploading images
    @Transactional("categoryWikiTransactionManager")
    public void testResizeSmallImage() throws IOException {
        testResizeImage("/mbo-core/assets/test_small.png");
    }

    private void testResizeImage(String imagePath) throws IOException {
        String url = null;
        try {
            byte[] data = loadFromClasspath(imagePath);
            String filename = RandomStringUtils.random(PIC_FILENAME_LENGTH, PIC_FILENAME_CHARS);

            url = categoryWikiPictureRowService.saveImage(data);
            BufferedImage image = ImageIO.read(new URL("http:" + url));

            Assertions.assertThat(image.getWidth()).isEqualTo(IMAGE_WIDTH);
            Assertions.assertThat(image.getHeight()).isEqualTo(IMAGE_HEIGHT);
        } finally {
            if (url != null) {
                String imageId = avatarImageDepotService.getImageId(url);
                avatarImageDepotService.removeImage(imageId);
            }
        }
    }


    private byte[] loadFromClasspath(String file) {
        try (InputStream inputStream = getClass().getResourceAsStream(file)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource " + file + " is not found.");
            }
            return ByteStreams.toByteArray(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Configuration
    public static class NullBeansConfiguration {

        @Bean
        TovarTreeService tovarTreeService() {
            return null;
        }

        @Bean
        IParameterLoaderService parameterLoaderService() {
            return null;
        }

        @Bean
        CategoryWikiPictureRowService categoryWikiPictureRowService() {
            return null;
        }

        @Bean
        HyperImageDepotService hyperImageDepotService() {
            return null;
        }
    }

}
