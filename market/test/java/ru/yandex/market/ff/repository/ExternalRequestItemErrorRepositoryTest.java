package ru.yandex.market.ff.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.ExternalRequestItemError;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static ru.yandex.market.ff.client.enums.ExternalRequestItemErrorSource.MBO;

public class ExternalRequestItemErrorRepositoryTest extends IntegrationTest {

    @Autowired
    private RequestItemRepository requestItemRepository;
    @Autowired
    private ExternalRequestItemErrorRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/external_request_item_error/before.xml")
    public void retrieveErrorWithAttributes() {
        Collection<ExternalRequestItemError> allByRequestItemIdIn =
            repository.findAllByRequestItemIdInAndSource(ImmutableList.of(1L), MBO);

        assertions.assertThat(allByRequestItemIdIn).hasSize(1);

        Optional<ExternalRequestItemError> optionalError = allByRequestItemIdIn.stream()
            .findAny();
        assertions.assertThat(optionalError).isPresent();

        ExternalRequestItemError error = optionalError.get();
        assertions.assertThat(error.getErrorCode()).isEqualTo("code1");
        assertions.assertThat(error.getTemplate()).isEqualTo("{}");
        assertions.assertThat(error.getErrorParams()).isEqualTo("TestData");
        assertions.assertThat(error.getHidden()).isFalse();
    }

    @Test
    @DatabaseSetup("classpath:repository/external_request_item_error/before.xml")
    public void findItemIdsWithErrorsForRequest() {
        Set<Long> itemIdsWithErrors = repository.findAllItemIdsForRequest(2);

        assertions.assertThat(itemIdsWithErrors).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    @DatabaseSetup("classpath:repository/external_request_item_error/before-delete-item.xml")
    @ExpectedDatabase(value = "classpath:repository/external_request_item_error/after-delete-item.xml",
            assertionMode = NON_STRICT)
    void deleteErrorsWithItems() {
        var requestItems = requestItemRepository.findAll(List.of(9L));
        requestItemRepository.deleteInBatch(requestItems);
    }

}
