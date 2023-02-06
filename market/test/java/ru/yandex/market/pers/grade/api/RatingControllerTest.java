package ru.yandex.market.pers.grade.api;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.core.db.DbRatingService;
import ru.yandex.market.pers.grade.ugc.api.dto.ShopRatingDto;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 16.01.2019
 */
public class RatingControllerTest extends MockedPersGradeTest {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void loadShopRatingTestStartDate() throws Exception {
        long testShop = -128391;

        int start = 12;
        int count = 10;
        int shift = 3;
        String dayPrefix = "2019-11-";

        IntStream.range(start, start + count).forEach(idx -> {
            createRecord(testShop, dayPrefix, idx);
        });

        List<ShopRatingDto> shopRatings = getRating(
            testShop,
            x -> x.param("dateFrom", buildTestDate(dayPrefix, start + shift))
        );

        assertEquals(count - shift, shopRatings.size());

        IntStream.range(0, count - shift).forEach(idx -> {
            assertRating(dayPrefix, shopRatings.get(idx), start +shift + idx);
        });
    }

    @Test
    public void loadShopRatingTestToDate() throws Exception {
        long testShop = -128391;

        int start = 12;
        int count = 10;
        int shift = 4;
        String dayPrefix = "2019-11-";

        IntStream.range(start, start + count).forEach(idx -> {
            createRecord(testShop, dayPrefix, idx);
        });

        List<ShopRatingDto> shopRatings = getRating(
            testShop,
            x -> x.param("dateTo", buildTestDate(dayPrefix, start + shift))
                .param("sortOrder", "desc")
        );

        assertEquals(shift + 1, shopRatings.size());

        IntStream.range(0, shift + 1).forEach(idx -> {
            assertRating(dayPrefix, shopRatings.get(idx), start + shift - idx);
        });
    }

    @Test
    public void loadShopRatingTestPaging() throws Exception {
        long testShop = -128391;

        int start = 12;
        int count = 10;
        int pageSize = 3;
        String dayPrefix = "2019-11-";

        IntStream.range(start, start + count).forEach(idx -> {
            createRecord(testShop, dayPrefix, idx);
        });

        List<ShopRatingDto> shopRatings = getRating(
            testShop,
            x -> x.param("pageNum", "2")
                .param("pageSize", String.valueOf(pageSize))
        );

        assertEquals(pageSize, shopRatings.size());

        IntStream.range(0, pageSize).forEach(idx -> {
            assertRating(dayPrefix, shopRatings.get(idx), start + pageSize + idx);
        });
    }

    private void assertRating(String dayPrefix, ShopRatingDto rating, int dayNum) {
        assertEquals(buildTestDate(dayPrefix, dayNum), parseDate(rating.getDate()));
        assertEquals(dayNum, rating.getGrades().getLast90Days().getTotal().getVerified().intValue());
    }

    @NotNull
    private String parseDate(Date date) {
        return RatingController.DATE_FORMAT.get().format(date);
    }

    private List<ShopRatingDto> getRating(long shopId,
                                          Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> fun) throws Exception {
        return OBJECT_MAPPER.readValue(
            mockMvc.perform(fun.apply(get("/api/rating/shop/" + shopId))
            ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ShopRatingDto>>() {
            });
    }

    private void createRecord(long testShop, String dayPrefix, int idx) {
        pgJdbcTemplate.update(
            "insert into shop_rating_history(" + DbRatingService.SHOP_FIELDS + ") " +
                "values (to_date(?, 'yyyy-mm-dd hh:mi'), ?, 0,3.1,0,0,0,0,?,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)",
            buildTestDateTime(dayPrefix, idx),
            testShop,
            idx);
    }

    @NotNull
    private String buildTestDate(String dayPrefix, int idx) {
        return dayPrefix + idx;
    }

    @NotNull
    private String buildTestDateTime(String dayPrefix, int idx) {
        return dayPrefix + idx + " 05:32";
    }
}
