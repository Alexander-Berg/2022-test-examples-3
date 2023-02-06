package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageClient;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.testing.data.TestPricePackages;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PricePackageInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.dbschema.ppcdict.tables.CpmPricePackages.CPM_PRICE_PACKAGES;

public class PricePackageSteps {

    private final PricePackageRepository pricePackageRepository;
    private final DslContextProvider dslContextProvider;

    @Autowired
    public PricePackageSteps(PricePackageRepository pricePackageRepository,
                             DslContextProvider dslContextProvider) {
        this.pricePackageRepository = pricePackageRepository;
        this.dslContextProvider = dslContextProvider;
    }

    public PricePackageInfo createNewPricePackage() {
        return createPricePackage(new PricePackageInfo()
                .withPricePackage(defaultPricePackage().withStatusApprove(StatusApprove.NEW)));
    }

    public PricePackageInfo createApprovedPricePackage() {
        return createPricePackage(new PricePackageInfo()
                .withPricePackage(defaultPricePackage().withStatusApprove(StatusApprove.YES)));
    }

    public PricePackageInfo createApprovedPricePackageWithClients(ClientInfo... clients) {
        return createPricePackage(new PricePackageInfo()
                .withPricePackage(defaultPricePackage()
                        .withCurrency(clients.length > 0 ? clients[0].getClient().getWorkCurrency() : CurrencyCode.RUB)
                        .withStatusApprove(StatusApprove.YES)
                        .withClients(StreamEx.of(clients)
                                .map(TestPricePackages::allowedPricePackageClient)
                                .toList())));
    }

    public PricePackageInfo createPricePackage(PricePackage pricePackage) {
        return createPricePackage(new PricePackageInfo()
                .withPricePackage(pricePackage));
    }

    public PricePackageInfo createPricePackage(PricePackageInfo pricePackageInfo) {
        if (pricePackageInfo.getPricePackage() == null) {
            pricePackageInfo.withPricePackage(defaultPricePackage());
        }
        if (pricePackageInfo.getPricePackageId() == null) {
            var pricePackageId =
                    pricePackageRepository.addPricePackages(List.of(pricePackageInfo.getPricePackage())).get(0);
            List<PricePackageClient> clients = pricePackageInfo.getPricePackage().getClients();
            if (clients != null) {
                pricePackageRepository.addPackagesToClients(dslContextProvider.ppcdict(),
                        Map.of(pricePackageId, clients));
            }
            pricePackageInfo.getPricePackage().setId(pricePackageId);
        }
        return pricePackageInfo;
    }

    public void deletePricePackage(Long id) {
        dslContextProvider.ppcdictTransaction(configuration ->
                pricePackageRepository.deletePricePackages(configuration.dsl(), List.of(id)));
    }

    public void clearPricePackages() {
        dslContextProvider.ppcdict()
                .deleteFrom(CPM_PRICE_PACKAGES)
                .execute();
    }

}
