package ru.yandex.market.pers.grade.ugc.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.client.dto.FactorType;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.core.FactorCreator;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactor;
import ru.yandex.market.pers.grade.core.ugc.model.RadioFactorValue;
import ru.yandex.market.pers.grade.ugc.api.dto.ShopGradeFactorDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author korolyov
 * 10.04.17
 */
public class FactorControllerTest extends MockedPersGradeTest {
    private static final int CATEGORY_ID = 1;
    private static final long FASHION_CATEGORY_ID = 7812157L;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FactorCreator factorCreator;

    @Before
    public void clearNonShopFactors() {
        pgJdbcTemplate.update("delete from grade_factor where category_id is not null");
    }

    @Test
    public void getModelFactorsTest() throws Exception {
        String response = invokeAndRetrieveResponse(
            get("/api/grade/factors/category/" + CATEGORY_ID)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        List<GradeFactor> gradeFactors = objectMapper.readValue(response, new TypeReference<List<GradeFactor>>() {
        });
        Assert.assertNotNull(gradeFactors);
    }

    @Test
    public void testGetModelFactors() throws Exception {
        factorCreator.addFactorAndReturnId("Имя фактор", CATEGORY_ID, 10);
        String response = invokeAndRetrieveResponse(
            get("/api/grade/factors/category/" + CATEGORY_ID)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        List<GradeFactor> gradeFactors = objectMapper.readValue(response, new TypeReference<List<GradeFactor>>() {
        });
        Assert.assertNotNull(gradeFactors);
        Assert.assertEquals(1, gradeFactors.size());

        GradeFactor gradeFactor = gradeFactors.get(0);
        Assert.assertEquals(FactorType.STARS, gradeFactor.getType());
        Assert.assertNull(gradeFactor.getRadioValues());
    }

    @Test
    public void testGetModelFactorsV2() throws Exception {
        String response = invokeAndRetrieveResponse(
            get("/api/grade/factors/v2/category/" + CATEGORY_ID)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        List<GradeFactor> gradeFactors = objectMapper.readValue(response, new TypeReference<List<GradeFactor>>() {
        });
        Assert.assertNotNull(gradeFactors);
    }

    @Test
    public void testGetModelFactorsWithRadio() throws Exception {
        factorCreator.addFactorAndReturnId("Другой размер", FASHION_CATEGORY_ID, 10);
        factorCreator.addRadioFactorAndReturnId("Вещь соответствует заявленному размеру?",
            FASHION_CATEGORY_ID,
            10,
            true,
            List.of(
                new RadioFactorValue(0, 0, "Да"),
                new RadioFactorValue(1, 10, "Нет, маломерит"),
                new RadioFactorValue(2, 20, "Нет, большемерит"),
                new RadioFactorValue(3, 30, "Нет, другое: в комментарии")
            ));

        String response = invokeAndRetrieveResponse(
            get("/api/grade/factors/v2/category/" + FASHION_CATEGORY_ID)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());

        JSONAssert.assertEquals(
            IOUtils.readInputStream(getClass().getResourceAsStream("/data/factor/model_factors_with_radio.json")),
            response, JSONCompareMode.LENIENT);
    }

    @Test
    public void getShopFactorsTest() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/shopFactors")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    @Test
    public void resetShopFactorsTest() throws Exception {
        resetFactorCache();
    }

    @Test
    public void testFactorRetrievalAndCacheCleanse() throws Exception {
        // cleanse shop_factors table
        cleanShopFactors();
        // check that table actually clean and initialize cache
        Assert.assertEquals(0, getActualFactors().size());
        // add factors
        factorCreator.addShopFactorAndReturnId("name0", "description0", 0, Delivery.DELIVERY, true);
        factorCreator.addShopFactorAndReturnId("name1", "description1", 1, Delivery.PICKUP, false);
        // check that still receive empty list trough cache
        Assert.assertEquals(0, getActualFactors().size());
        // reset cache
        resetFactorCache();


        List<ShopGradeFactorDto> factors = getActualFactors();
        Assert.assertEquals(2, factors.size());
        assertFactor(factors.get(0), 0, Delivery.DELIVERY, true);
        assertFactor(factors.get(1), 1, Delivery.PICKUP, false);
    }

    @Test
    public void testActualFactors() throws Exception {
        List<ShopGradeFactorDto> expected = getExpectedFactors();
        List<ShopGradeFactorDto> actual = getActualFactors();
        // DB not guarantees exact order
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(expected.containsAll(actual));
        Assert.assertTrue(actual.containsAll(expected));
    }

    private List<ShopGradeFactorDto> getExpectedFactors() {
        List<ShopGradeFactorDto> result = new ArrayList<>();
        result.add(new ShopGradeFactorDto(0L,"Скорость обработки заказа","Как быстро с вами связались для подтверждения заказа?",Delivery.DELIVERY, false));
        result.add(new ShopGradeFactorDto(0L,"Скорость обработки заказа","Как быстро с вами связались для подтверждения заказа?",Delivery.PICKUP,false));
        result.add(new ShopGradeFactorDto(0L,"Скорость обработки заказа","Как быстро с вами связались для подтверждения заказа?",Delivery.INSTORE,false));
        result.add(new ShopGradeFactorDto(1L,"Качество доставки или выдачи товара","Всё ли было хорошо в процессе получения заказа?",Delivery.DELIVERY,true));
        result.add(new ShopGradeFactorDto(1L,"Качество доставки или выдачи товара","Всё ли было хорошо в процессе получения заказа?",Delivery.PICKUP,true));
        result.add(new ShopGradeFactorDto(1L,"Качество доставки или выдачи товара","Всё ли было хорошо в процессе получения заказа?",Delivery.INSTORE,true));
        result.add(new ShopGradeFactorDto(2L,"Общение","Вежливо ли с вами общались?",Delivery.INSTORE,false));
        result.add(new ShopGradeFactorDto(2L,"Общение","Вежливо ли с вами общались?",Delivery.PICKUP,false));
        result.add(new ShopGradeFactorDto(2L,"Общение","Вежливо ли с вами общались?",Delivery.DELIVERY,false));
        result.add(new ShopGradeFactorDto(3L,"Соответствие товара описанию","Вы получили то, что ожидали?",Delivery.INSTORE,false));
        result.add(new ShopGradeFactorDto(3L,"Соответствие товара описанию","Вы получили то, что ожидали?",Delivery.DELIVERY,false));
        result.add(new ShopGradeFactorDto(3L,"Соответствие товара описанию","Вы получили то, что ожидали?",Delivery.PICKUP,false));
        return result;
    }

    private void assertFactor(ShopGradeFactorDto shopGradeFactorDto, int i, Delivery delivery, boolean feedbackFactor) {
        Assert.assertEquals(shopGradeFactorDto.getTitle(), "name" + i);
        Assert.assertEquals(shopGradeFactorDto.getDescription(), "description" + i);
        Assert.assertEquals(shopGradeFactorDto.getDelivery(), delivery);
        Assert.assertEquals(shopGradeFactorDto.isFeedbackFactor(), feedbackFactor);
    }

    private void resetFactorCache() throws Exception {
        invokeAndRetrieveResponse(
            put("/api/grade/shopFactors/reset")
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    private List<ShopGradeFactorDto> getActualFactors() throws Exception {
        return objectMapper.readValue(
            mockMvc.perform(get("/api/grade/shopFactors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ShopGradeFactorDto>>() {
            }
        );
    }

    private void cleanShopFactors() {
        pgJdbcTemplate.execute("delete from grade_factor_shop");
    }
}
