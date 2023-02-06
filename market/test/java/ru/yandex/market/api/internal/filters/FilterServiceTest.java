package ru.yandex.market.api.internal.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.netty.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.category.FilterService;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.guruass.FilterControls;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.GuruassTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.ApiMatchers.map;
import static ru.yandex.market.api.ApiMatchers.returns;
import static ru.yandex.market.api.internal.filters.FilterControlsAssert.assertEquals;

/**
 * @author dimkarp93
 */
public class FilterServiceTest extends BaseTest {

    @Inject
    private FilterService filterService;
    @Inject
    private GuruassTestClient guruassTestClient;
    @Inject
    ReportTestClient reportTestClient;

    private static final Map<String, String> AUDI_A5_2_2_PARAMS = ImmutableMap.of(
        "mark", "6",
        "model", "6",
        "generation", "2",
        "tech-param", "2"
    );

    private static final FilterControls.FilterControl AUDI_A5_2_2_RESULT = new FilterControls.FilterControl(FilterControls.FilterControlType.RADIO)
        .setValues(Arrays.asList(
            new FilterControls.FilterControlValue().setName("225/50 R17").setSelected(true).setGroup(FilterControls.ControlValueGroup.MAIN)
                .setCriteria(Arrays.asList(
                    new Criterion("5065223", "12104307", null),
                    new Criterion("5065225", "12104333", null),
                    new Criterion("5065227", "12104350", null)
                )),
            new FilterControls.FilterControlValue().setName("245/40 R18").setGroup(FilterControls.ControlValueGroup.MAIN)
                .setCriteria(Arrays.asList(
                    new Criterion("5065223", "12104309", null),
                    new Criterion("5065225", "12104331", null),
                    new Criterion("5065227", "12104351", null)
                )),
            new FilterControls.FilterControlValue().setName("255/35 R19").setGroup(FilterControls.ControlValueGroup.MAIN)
                .setCriteria(Arrays.asList(
                    new Criterion("5065223", "12104310", null),
                    new Criterion("5065225", "12104330", null),
                    new Criterion("5065227", "12104352", null)
                )),
            new FilterControls.FilterControlValue().setName("265/30 R20").setGroup(FilterControls.ControlValueGroup.MAIN)
                .setCriteria(Arrays.asList(
                    new Criterion("5065223", "12104311", null),
                    new Criterion("5065225", "12104329", null),
                    new Criterion("5065227", "12104353", null)
                )),
            new FilterControls.FilterControlValue().setName("235/45 R17").setGroup(FilterControls.ControlValueGroup.ALT)
                .setCriteria(Arrays.asList(
                    new Criterion("5065223", "12104308", null),
                    new Criterion("5065225", "12104332", null),
                    new Criterion("5065227", "12104350", null)
                ))
        ));

    /**
     * Проверка того что при запросе с неподдерживаемоей категорией отдается {@link NotFoundException}
     */
    @Test
    public void testGetFilterControlsUnsupportedCategory() {

        guruassTestClient.getControlIds(2343);

        Future<FilterControls> future = filterService.getCategoryFilterControls(2343, Collections.emptyMap(),
            new ValidationErrors());
        Futures.wait(future);

        Assert.assertFalse(future.isSuccess());
        assertEquals(NotFoundException.class, future.cause().getClass());
    }

    /**
     * Проверка выдачи при нормальном полном ответе
     */
    @Test
    public void testGetFilterControlsNormalResponse() {

        guruassTestClient.getControlIds(GuruassTestClient.TYRES_CATEGORY);
        guruassTestClient.getControls(GuruassTestClient.TYRES_CATEGORY, AUDI_A5_2_2_PARAMS,
            "guruass_Audi_A5_2_2.json");

        Map<String, String[]> requestParam = Maps.newHashMap();
        requestParam.put("geo_id", new String[]{ "213" });
        AUDI_A5_2_2_PARAMS.forEach((k, v) -> requestParam.put(k, new String[]{ v }));

        Future<FilterControls> future = filterService.getCategoryFilterControls(GuruassTestClient.TYRES_CATEGORY,
            requestParam, new ValidationErrors());
        FilterControls filterControls = Futures.waitAndGet(future);

        // Проверки
        Assert.assertNotNull(filterControls);

        FilterControls.FilterControl resultControl = filterControls.getResultControl();
        assertEquals(AUDI_A5_2_2_RESULT, resultControl);
        assertEquals(AUDI_A5_2_2_RESULT.getValues(), resultControl.getValues());

        List<FilterControls.FilterControl> inputControls = filterControls.getInputControls();
        assertEquals(4, inputControls.size());

        FilterControls.FilterControl input = inputControls.get(0);
        assertEquals(new FilterControls.FilterControl(FilterControls.FilterControlType.SELECT).setId("mark").setName("Марка"), input);
        assertEquals(92, input.getValues().size());
        FilterControls.FilterControlValue fv = Iterables.find(input.getValues(), v -> "6".equals(v.getId()));
        assertEquals(new FilterControls.FilterControlValue().setId("6").setName("Audi").setSelected(true), fv);

        input = inputControls.get(1);
        assertEquals(new FilterControls.FilterControl(FilterControls.FilterControlType.SELECT).setId("model").setName("Модель"), input);
        assertEquals(33, input.getValues().size());
        fv = Iterables.find(input.getValues(), v -> "6".equals(v.getId()));
        assertEquals(new FilterControls.FilterControlValue().setId("6").setName("A5").setSelected(true), fv);

        input = inputControls.get(2);
        assertEquals(new FilterControls.FilterControl(FilterControls.FilterControlType.SELECT).setId("generation").setName("Поколение"), input);
        assertEquals(3, input.getValues().size());
        fv = Iterables.find(input.getValues(), v -> "2".equals(v.getId()));
        assertEquals(new FilterControls.FilterControlValue().setId("2").setName("A5 Typ 8T restyle • 2011–2016").setSelected(true), fv);

        input = inputControls.get(3);
        assertEquals(new FilterControls.FilterControl(FilterControls.FilterControlType.SELECT).setId("tech-param").setName("Модификация"), input);
        assertEquals(16, input.getValues().size());
        fv = Iterables.find(input.getValues(), v -> "-1".equals(v.getId()));
        assertEquals(new FilterControls.FilterControlValue().setId("-1").setName("Любая модификация"), fv);
        fv = Iterables.find(input.getValues(), v -> "2".equals(v.getId()));
        assertEquals(new FilterControls.FilterControlValue().setId("2").setName("1.8 л. бензин, 170 л.с.").setSelected(true), fv);
    }

    @Test
    public void testCheckFilters() {
        int hid = 91491;

        reportTestClient.checkFilters(hid, Arrays.asList("123:456,789", "7893318:111,222"), "check_filters_with_vendor.json");

        Map<String, String> filters = ImmutableMap.<String, String>builder()
                .put("123", "456,789")
                .put("-11", "111,222")
                .put("-1", "100~1000")
                .put("onstock", "1")
                .build();
        assertThat(filterService.checkFilters(hid, filters), returns(allOf(
                map(Map::keySet, "keys", hasSize(4)),
                hasEntry("123", "234"),
                hasEntry("7893318", "111"),
                hasEntry("-1", "100~1000"),
                hasEntry("onstock", "1")
        )));
    }

    @Test
    public void testCheckInvalidFilters() {
        int hid = 91491;

        reportTestClient.checkFilters(hid, Arrays.asList("123:456,789", "7893318:111,222"), "check_filters_invalid.json");

        Map<String, String> filters = ImmutableMap.<String, String>builder()
                .put("123", "456,789")
                .put("-11", "111,222")
                .put("-1", "100~1000")
                .put("onstock", "1")
                .build();
        assertThat(filterService.checkFilters(hid, filters), returns(allOf(
                map(Map::keySet, "keys", hasSize(2)),
                hasEntry("-1", "100~1000"),
                hasEntry("onstock", "1")
        )));
    }
}
