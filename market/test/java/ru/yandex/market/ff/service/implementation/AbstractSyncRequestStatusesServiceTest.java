package ru.yandex.market.ff.service.implementation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.ShopRequest;

public abstract class AbstractSyncRequestStatusesServiceTest extends IntegrationTest {

    @Autowired
    protected SyncRequestStatusesService syncRequestStatusesService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    protected void syncRequests(Set<Long> requestIds, boolean isXDoc) {
        transactionTemplate.execute(status -> {
            List<ShopRequest> requests = new ArrayList<>(shopRequestFetchingService.getRequests(requestIds));
            Map<Long, Map<RequestType, List<ShopRequest>>> groupedRequests = requests.stream()
                    .collect(Collectors.groupingBy(
                            ShopRequest::getServiceId,
                            Collectors.mapping(Function.identity(), Collectors.groupingBy(ShopRequest::getType))
                    ));
            groupedRequests.forEach((service, typeToRequests) ->
                    typeToRequests.forEach((type, batch) -> {
                        batch.sort(Comparator.comparing(ShopRequest::getId));
                        syncRequestStatusesService.processBatchAndGetFailureMessages(batch, isXDoc);
                    }));
            return null;
        });
    }
}
