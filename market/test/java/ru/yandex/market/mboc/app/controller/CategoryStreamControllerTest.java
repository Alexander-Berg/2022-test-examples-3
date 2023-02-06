package ru.yandex.market.mboc.app.controller;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.common.categorystream.CategoryStream;
import ru.yandex.market.mboc.common.categorystream.CategoryStreamInfo;
import ru.yandex.market.mboc.common.categorytocategorystream.CategoryToCategoryStreamData;
import ru.yandex.market.mboc.common.repository.categorystream.B2BCategoryStreamRepository;
import ru.yandex.market.mboc.common.repository.categorystream.CategoryStreamRepository;
import ru.yandex.market.mboc.common.repository.categorystream.CategoryToCategoryStreamRepository;
import ru.yandex.market.mboc.common.services.category_stream.CategoryStreamService;
import ru.yandex.market.mboc.common.web.Result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


public class CategoryStreamControllerTest extends BaseMbocAppTest {
    private final static Long DEFAULT_CATEGORY_STREAM_ID = 100500L;
    private final static String DEFAULT_CATEGORY_STREAM_NAME = "aboba";
    private final static Long DEFAULT_CATEGORY_ID = 666L;
    private final static Long DEFAULT_B2B_CATEGORY_STREAM_ID = 5L;
    private final static int SIZE = 10;
    private CategoryStreamController categoryStreamController;
    @Autowired
    private CategoryStreamRepository categoryStreamRepository;
    @Autowired
    private CategoryToCategoryStreamRepository categoryToCategoryStreamRepository;
    @Autowired
    private B2BCategoryStreamRepository b2BCategoryStreamRepository;

    private CategoryStreamService categoryStreamService;

    @Before
    public void before() {
        categoryStreamService = new CategoryStreamService(
            categoryStreamRepository,
            b2BCategoryStreamRepository,
            categoryToCategoryStreamRepository);
        categoryStreamController = new CategoryStreamController(categoryStreamService);
        categoryStreamRepository.deleteAll();
        categoryToCategoryStreamRepository.deleteAll();
    }

    @Test
    public void getAllCategoryStreamShouldReturnEmptyList() {
        assertTrue(categoryStreamController.getAllCategoryStreams().isEmpty());
    }

    @Test
    public void getAllCategoryStreamShouldReturnValue() {
        CategoryStream categoryStream = defaultCategoryStream();
        categoryStreamRepository.save(categoryStreamService.categoryStreamMapper(categoryStream));
        var categoryStreamWithTimestamp =
            categoryStreamRepository.findById(DEFAULT_CATEGORY_STREAM_ID);
        assertTrue(categoryStreamWithTimestamp.isPresent());
        assertTrue(categoryStreamController.getAllCategoryStreams().contains(
            categoryStreamService.categoryStreamMapper(categoryStreamWithTimestamp.get()))
        );
    }

    @Test
    public void getAllCategoryToCategoryStreamsShouldReturnEmptyList() {
        assertTrue(categoryStreamController.getAllCategoryToCategoryStreams().isEmpty());
    }

    @Test
    public void getAllCategoryToCategoryStreamsShouldReturnValue() {
        categoryStreamRepository.save(categoryStreamService.categoryStreamMapper(defaultCategoryStream()));
        CategoryToCategoryStreamData categoryToCategoryStreamData = defaultCategoryToCategoryStream();
        categoryToCategoryStreamRepository.save(categoryStreamService.categoryToCategoryStreamMapper(categoryToCategoryStreamData));
        var categoryToCategoryStreamWithTimestamp =
            categoryToCategoryStreamRepository.findById(DEFAULT_CATEGORY_ID);
        assertTrue(categoryToCategoryStreamWithTimestamp.isPresent());
        assertTrue(categoryStreamController.getAllCategoryToCategoryStreams()
            .contains(categoryStreamService.categoryToCategoryStreamMapper(categoryToCategoryStreamWithTimestamp.get())));
    }

    @Test
    public void updateCategoryToCategoryStreamShouldReturnError() {
        CategoryStreamInfo categoryStreamInfoNullNull = categoryStreamController.updateCategoryToCategoryStream(
            null, null, DEFAULT_B2B_CATEGORY_STREAM_ID);
        assertSame(Result.ResultStatus.ERROR, categoryStreamInfoNullNull.getStatus());
        assertEquals("Category id is null", categoryStreamInfoNullNull.getErrorDescription());
        CategoryStreamInfo categoryStreamInfoDefaultNull = categoryStreamController.updateCategoryToCategoryStream(
            DEFAULT_CATEGORY_ID, null, DEFAULT_B2B_CATEGORY_STREAM_ID);
        assertSame(Result.ResultStatus.ERROR, categoryStreamInfoDefaultNull.getStatus());
        assertEquals("Category stream id is null", categoryStreamInfoDefaultNull.getErrorDescription());
        CategoryStreamInfo categoryStreamInfoNullDefault = categoryStreamController.updateCategoryToCategoryStream(
            null, DEFAULT_CATEGORY_STREAM_ID, DEFAULT_B2B_CATEGORY_STREAM_ID);
        assertSame(Result.ResultStatus.ERROR, categoryStreamInfoNullDefault.getStatus());
        assertEquals("Category id is null", categoryStreamInfoNullDefault.getErrorDescription());

        // No new category_stream_id by fk
        categoryStreamRepository.save(categoryStreamService.categoryStreamMapper(defaultCategoryStream()));
        CategoryToCategoryStreamData categoryToCategoryStreamData = defaultCategoryToCategoryStream();
        categoryToCategoryStreamRepository.save(categoryStreamService.categoryToCategoryStreamMapper(categoryToCategoryStreamData));
        assertSame(Result.ResultStatus.ERROR,
            categoryStreamController.updateCategoryToCategoryStream(
                DEFAULT_CATEGORY_ID, DEFAULT_CATEGORY_STREAM_ID + 1, DEFAULT_B2B_CATEGORY_STREAM_ID).getStatus());

        // No value with such category_id
        assertSame(Result.ResultStatus.ERROR,
            categoryStreamController.updateCategoryToCategoryStream(
                DEFAULT_CATEGORY_ID + 111L, DEFAULT_CATEGORY_STREAM_ID, DEFAULT_B2B_CATEGORY_STREAM_ID).getStatus()
        );
    }

    @Test
    public void updateCategoryToCategoryStreamShouldReturnSuccess() {
        IntStream.range(0, SIZE).forEach(i -> {
            categoryStreamRepository.save(categoryStreamService.categoryStreamMapper(
                CategoryStream.builder()
                    .id(DEFAULT_CATEGORY_STREAM_ID + i)
                    .name(DEFAULT_CATEGORY_STREAM_NAME)
                    .build()
            ));
            categoryToCategoryStreamRepository.save(categoryStreamService.categoryToCategoryStreamMapper(
                CategoryToCategoryStreamData.builder()
                    .categoryId(DEFAULT_CATEGORY_ID + i)
                    .b2cCategoryStreamId(DEFAULT_CATEGORY_STREAM_ID + i)
                    .build()
            ));
        });
        IntStream.range(0, SIZE).forEach(i ->
            assertSame(Result.ResultStatus.SUCCESS,
                categoryStreamController.updateCategoryToCategoryStream(DEFAULT_CATEGORY_ID + i,
                    DEFAULT_CATEGORY_STREAM_ID + (SIZE - i - 1), DEFAULT_B2B_CATEGORY_STREAM_ID
                ).getStatus()
            )
        );
    }

    @Test
    public void saveCategoryToCategoryStreamShouldReturnSuccess() {
        categoryStreamRepository.save(categoryStreamService.categoryStreamMapper(defaultCategoryStream()));
        CategoryStreamInfo categoryStreamInfo =
            categoryStreamController.updateCategoryToCategoryStream(DEFAULT_CATEGORY_ID,
                DEFAULT_CATEGORY_STREAM_ID, DEFAULT_B2B_CATEGORY_STREAM_ID);
        List<CategoryToCategoryStreamData> allCategoryToCategoryStreamData =
            categoryStreamController.getAllCategoryToCategoryStreams();
        assertNotNull(allCategoryToCategoryStreamData);
        assertFalse(allCategoryToCategoryStreamData.isEmpty());
        CategoryToCategoryStreamData categoryToCategoryStreamData = allCategoryToCategoryStreamData.get(0);
        assertNotNull(categoryToCategoryStreamData);
        assertEquals(DEFAULT_CATEGORY_ID, categoryToCategoryStreamData.getCategoryId());
        assertEquals(DEFAULT_CATEGORY_STREAM_ID, categoryToCategoryStreamData.getB2cCategoryStreamId());
        assertEquals(DEFAULT_B2B_CATEGORY_STREAM_ID, categoryToCategoryStreamData.getB2bCategoryStreamId());
        assertSame(categoryStreamInfo.getStatus(), Result.ResultStatus.SUCCESS);
    }

    @Test
    public void saveCategoryToCategoryStreamShouldReturnError() {
        // save categoryToCategoryStream with nullable category id
        CategoryStreamInfo categoryStreamInfo = categoryStreamController.updateCategoryToCategoryStream(null,
            DEFAULT_CATEGORY_STREAM_ID, DEFAULT_B2B_CATEGORY_STREAM_ID);
        List<CategoryToCategoryStreamData> allCategoryStreams =
            categoryStreamController.getAllCategoryToCategoryStreams();
        assertNotNull(allCategoryStreams);
        assertTrue(allCategoryStreams.isEmpty());
        assertEquals(Result.ResultStatus.ERROR, categoryStreamInfo.getStatus());
        assertEquals("Category id is null", categoryStreamInfo.getErrorDescription());

        // save categoryToCategoryStream with nullable category stream id
        categoryStreamInfo = categoryStreamController.updateCategoryToCategoryStream(DEFAULT_CATEGORY_ID, null,
            DEFAULT_B2B_CATEGORY_STREAM_ID);
        allCategoryStreams = categoryStreamController.getAllCategoryToCategoryStreams();
        assertNotNull(allCategoryStreams);
        assertTrue(allCategoryStreams.isEmpty());
        assertEquals(Result.ResultStatus.ERROR, categoryStreamInfo.getStatus());
        assertEquals("Category stream id is null", categoryStreamInfo.getErrorDescription());

        // no category_stream_id by fk
        categoryStreamRepository.save(categoryStreamService.categoryStreamMapper(defaultCategoryStream()));
        categoryStreamInfo = categoryStreamController.updateCategoryToCategoryStream(DEFAULT_CATEGORY_ID,
            DEFAULT_CATEGORY_STREAM_ID + 1, DEFAULT_B2B_CATEGORY_STREAM_ID);
        assertEquals(Result.ResultStatus.ERROR, categoryStreamInfo.getStatus());

        // no b2b_category_stream_id by fk
        categoryStreamInfo = categoryStreamController.updateCategoryToCategoryStream(DEFAULT_CATEGORY_ID,
            DEFAULT_CATEGORY_STREAM_ID, DEFAULT_B2B_CATEGORY_STREAM_ID + 1);
        assertEquals(Result.ResultStatus.ERROR, categoryStreamInfo.getStatus());
    }

    private CategoryStream defaultCategoryStream() {
        return CategoryStream.builder()
            .id(DEFAULT_CATEGORY_STREAM_ID)
            .name(DEFAULT_CATEGORY_STREAM_NAME)
            .build();
    }

    private CategoryToCategoryStreamData defaultCategoryToCategoryStream() {
        return CategoryToCategoryStreamData.builder()
            .categoryId(DEFAULT_CATEGORY_ID)
            .b2cCategoryStreamId(DEFAULT_CATEGORY_STREAM_ID)
            .b2bCategoryStreamId(DEFAULT_B2B_CATEGORY_STREAM_ID)
            .build();
    }
}
