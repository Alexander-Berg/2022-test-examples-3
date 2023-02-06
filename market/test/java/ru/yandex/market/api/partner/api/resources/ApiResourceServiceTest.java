package ru.yandex.market.api.partner.api.resources;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.resource.ApiLimit;
import ru.yandex.market.api.resource.ApiLimitType;
import ru.yandex.market.api.resource.ApiResource;
import ru.yandex.market.api.resource.ApiResourceAccessLevel;
import ru.yandex.market.api.resource.CalculatedApiLimit;
import ru.yandex.market.api.resource.MemCachedApiLimitsAgentService;
import ru.yandex.market.api.resource.MemCachedApiResourceService;
import ru.yandex.market.api.resource.ResourceUserLimit;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.PROPERTY_TABLE_TYPE,
                value = "TABLE, VIEW"
        )
})
class ApiResourceServiceTest extends FunctionalTest {

    @Autowired
    private MemCachedApiResourceService partnerApiResourceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemCachedApiLimitsAgentService memCachedApiLimitsAgentService;

    @Test
    @DisplayName("Достается лимит как прописан в pl_limit")
    @DbUnitDataSet(before = "apiResourceServiceTestPlLimits.before.csv")
    void checkLoadPlLimits() {
        final CalculatedApiLimit limit = partnerApiResourceService.getResourceLimit(ApiLimitType.PRICE_LABS, 38, 10774);
        ApiLimit plLimit = limit.getApiLimit();
        assertEquals(Integer.valueOf(500000), plLimit.getLimit());
        assertEquals(Integer.valueOf(2), plLimit.getTimePeriod());
        assertEquals(TimeUnit.MINUTES, plLimit.getTimeUnit());
        assertEquals(ApiLimitType.PRICE_LABS, limit.getCalculatedApiLimitType());
    }

    @Test
    @DisplayName("Достается лимит как прописан в pl2_limit")
    @DbUnitDataSet(before = "apiResourceServiceTestPlLimits.before.csv")
    void checkLoadPl2Limits() {
        final CalculatedApiLimit limit = partnerApiResourceService.getResourceLimit(ApiLimitType.PRICE_LABS_V2, 38,
                10774);
        ApiLimit plLimit = limit.getApiLimit();
        assertEquals(Integer.valueOf(170000), plLimit.getLimit()); // На самом деле, это не 138 * 17, а 10000
        assertEquals(Integer.valueOf(2), plLimit.getTimePeriod());
        assertEquals(TimeUnit.MINUTES, plLimit.getTimeUnit());
        assertEquals(ApiLimitType.PRICE_LABS_V2, limit.getCalculatedApiLimitType());
    }

    @ParameterizedTest
    @EnumSource(ApiLimitType.class)
    @DisplayName("Проверяем, что мы без ошибок обрабатываем все доступные ApiLimitType, доставая дефолтный лимит")
    @DbUnitDataSet(before = "apiResourceServiceTestPlLimits.before.csv")
    void checkAllPossibleLimitTypes(ApiLimitType apiLimitType) {
        final CalculatedApiLimit limit = partnerApiResourceService.getResourceLimit(apiLimitType, 5, 10774);
        ApiLimit generalLimit = limit.getApiLimit();
        assertEquals(Integer.valueOf(10000), generalLimit.getLimit());
        assertEquals(Integer.valueOf(1), generalLimit.getTimePeriod());
        assertEquals(TimeUnit.DAYS, generalLimit.getTimeUnit());
        assertEquals(ApiLimitType.DEFAULT, limit.getCalculatedApiLimitType());
    }

    @Test
    @DisplayName("Достаются все ресурсы из БД")
    @DbUnitDataSet(before = "apiResourceServiceTestPlLimits.before.csv")
    void checkLoadApiResources() {
        Collection<ApiResource> apiResourceCollection = partnerApiResourceService.getApiResources();
        List<ApiResource> expected = ImmutableList.of(
                new ApiResource(5, 5, "/campaigns/*/offers", "GET", null, ApiResourceAccessLevel.READ_WRITE),
                new ApiResource(30, 29, "/campaigns/*/orders/*/status", "PUT", null, ApiResourceAccessLevel.READ_WRITE),
                new ApiResource(42, 42, "/without/*/*/group", "GET", null, ApiResourceAccessLevel.READ_WRITE),
                new ApiResource(59, 38, "/campaigns/*/hidden-offers", "POST", null, ApiResourceAccessLevel.READ_WRITE),
                new ApiResource(60, 38, "/campaigns/*/hidden-offers", "DELETE", null, ApiResourceAccessLevel.READ_WRITE)
        );
        ReflectionAssert.assertReflectionEquals(expected, apiResourceCollection);
    }

    @Test
    @DisplayName("Проверяем, что без ошибок достаются параллельные лимиты")
    @DbUnitDataSet(before = "apiResourceServiceTestPlLimits.before.csv")
    void test() {
        assertParallelLimit(29, 5);
        assertParallelLimit(38, 3);
        assertParallelLimit(5, null);
    }

    @Test
    @DisplayName("Проверяем, что лимиты обновляются корректно")
    @DbUnitDataSet(before = "apiResourceServiceTestPlLimits.before.csv")
    void testResourceLimitsUpdate() {
        MemCachedApiResourceService service = spy(partnerApiResourceService);
        doAnswer(invocation -> updateApiLimits(getApiLimits())).when(service).callProcedureUpdateResourceLimits();
        OnDeleteAnswer onDelete = new OnDeleteAnswer(service);
        MemCachedApiLimitsAgentService mockAgent = spy(memCachedApiLimitsAgentService);
        doAnswer(onDelete).when(mockAgent).deleteFromCache(anyString());
        service.setMemCachedApiLimitsAgentService(mockAgent);
        service.rebuildResourceLimits();
        assertEquals(getApiLimitsToDelete().size(), onDelete.count.size());
        assertTrue(onDelete.count.stream()
                .allMatch(count -> count == Stream.of(ApiLimitType.values())
                        .filter(type -> type != ApiLimitType.PARALLEL)
                        .count()));
    }

    @Test
    @DisplayName("Проверяем, что при наличии кастомных лимитов они перепишут стандартные")
    @DbUnitDataSet(before = "apiResourceServiceTestPlLimits.before.csv")
    void testCustomLimits() {
        assertEquals(36000, getLimit(38, 1001L));
        assertEquals(10000, getLimit(29, 1001L));
        assertEquals(72000, getLimit(38, 1002L));
        assertEquals(100, getLimit(29, 1002L));
    }

    @Test
    @DbUnitDataSet(
            before = "ApiResourceServiceTest.testLimitModels.before.csv",
            after = "ApiResourceServiceTest.testLimitModels.after.csv"
    )
    void testLimitModels() {
        // проверяется вьюха. полностью через dbunit
    }

    private Integer getLimit(int resourceGroupId, long campaignId) {
        return partnerApiResourceService.getResourceLimit(ApiLimitType.DEFAULT, resourceGroupId, campaignId)
                .getApiLimit().getLimit();
    }

    private List<ResourceUserLimit> getApiLimits() {
        return Stream.concat(getApiLimitsToDelete().stream(),
                        getApiLimitsToStay().stream())
                .collect(Collectors.toList());
    }

    private List<ResourceUserLimit> getApiLimitsToStay() {
        return Collections.singletonList(
                new ResourceUserLimit(63, 1036804L, new ApiLimit(21300, 1, TimeUnit.DAYS))
        );
    }

    private List<ResourceUserLimit> getApiLimitsToDelete() {
        return List.of(
                new ResourceUserLimit(63, 1035049L, new ApiLimit(98400, 1, TimeUnit.DAYS)),
                new ResourceUserLimit(37, 21254012L, new ApiLimit(15750, 1, TimeUnit.DAYS))
        );
    }

    private Object updateApiLimits(List<ResourceUserLimit> apiLimits) {
        jdbcTemplate.update("truncate table shops_web.api_limits");
        jdbcTemplate.batchUpdate("insert into shops_web.api_limits" +
                "(group_id, subject_id, \"LIMIT\", time_period, time_unit) " +
                "values (?, ?, ?, ?, ?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ResourceUserLimit limit = apiLimits.get(i);
                ApiLimit apiLimit = limit.getApiLimit();
                ps.setInt(1, limit.getGroupId());
                ps.setLong(2, limit.getSubjectId());
                ps.setInt(3, apiLimit.getLimit());
                ps.setInt(4, apiLimit.getTimePeriod());
                ps.setString(5, apiLimit.getTimeUnit().toString());
            }

            @Override
            public int getBatchSize() {
                return apiLimits.size();
            }
        });
        return null;
    }

    private void assertParallelLimit(int groupId, Integer expectedLimit) {
        CalculatedApiLimit calculatedApiLimit = partnerApiResourceService.getResourceLimit(ApiLimitType.PARALLEL,
                groupId, -1);
        assertNotNull(calculatedApiLimit.getApiLimit());
        if (expectedLimit == null) {
            assertEquals(ApiLimitType.DEFAULT, calculatedApiLimit.getCalculatedApiLimitType());
        } else {
            assertEquals(expectedLimit, calculatedApiLimit.getApiLimit().getLimit());
        }
    }

    private class OnDeleteAnswer implements Answer<Object> {
        private final MemCachedApiResourceService service;
        private final List<ResourceUserLimit> toDelete = getApiLimitsToDelete();
        public List<Integer> count = toDelete.stream().map(e -> 0).collect(Collectors.toList());

        public OnDeleteAnswer(MemCachedApiResourceService service) {
            this.service = service;
        }

        private boolean filterByKey(ResourceUserLimit res, String key) {
            return Stream.of(ApiLimitType.values())
                    .filter(type -> type != ApiLimitType.PARALLEL)
                    .map(ApiLimitType::getMemcachedPrefix)
                    .anyMatch(prefix ->
                            service.getApiLimitKeyBuilder().buildKey(prefix,
                                    Objects.requireNonNull(res.getGroupId()),
                                    Objects.requireNonNull(res.getSubjectId()),
                                    res.getApiLimit()).equals(key));
        }

        @Override
        public Object answer(InvocationOnMock invocation) {
            String key = invocation.getArgument(0);
            List<ResourceUserLimit> found = toDelete.stream()
                    .filter(res -> filterByKey(res, key))
                    .collect(Collectors.toList());
            assertEquals(1, found.size());
            int index = toDelete.indexOf(found.iterator().next());
            count.set(index, count.get(index) + 1);
            return null;
        }
    }

}
