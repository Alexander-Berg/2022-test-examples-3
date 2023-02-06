package ru.yandex.market.ff.service.implementation;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestItemAttribute;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestItemAttributeEntity;
import ru.yandex.market.ff.model.entity.SkuAttributeEntity;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.service.RequestItemAttributeService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RequestItemAttributeServiceTest extends IntegrationTest {

    @Autowired
    private RequestItemAttributeService requestItemAttributeService;

    @Autowired
    private RequestItemRepository requestItemRepository;

    @Test
    @DatabaseSetup("classpath:service/request-tem-attribute-service/1/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-tem-attribute-service/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void saveWithDuplicateTest() {
        List<SkuAttributeEntity> list = List.of(
                new SkuAttributeEntity(1L, RequestItemAttribute.CTM),
                new SkuAttributeEntity(2L, RequestItemAttribute.CTM),
                new SkuAttributeEntity(3L, RequestItemAttribute.CTM)
        );
        requestItemAttributeService.saveWithDuplicate(list);
    }

    @Test
    @DatabaseSetup("classpath:service/request-tem-attribute-service/2/before.xml")
    @ExpectedDatabase(value = "classpath:service/request-tem-attribute-service/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void setRequestItemAttributeTest() {
        List<RequestItem> list = requestItemRepository.findAll();
        requestItemAttributeService.setRequestItemAttribute(list);
    }

    @Test
    @DatabaseSetup("classpath:service/request-tem-attribute-service/3/before.xml")
    public void getMatchingTest() {
        List<RequestItemAttributeEntity> entities =
                requestItemAttributeService.getMatching(List.of(1L, 2L, 3L), RequestItemAttribute.CTM);

        assertThat(entities, notNullValue());
        assertThat(entities, hasSize(2));
        List<Long> ids = entities.stream()
                .map(RequestItemAttributeEntity::getId)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(ids,  List.of(1L, 2L));
    }
}
