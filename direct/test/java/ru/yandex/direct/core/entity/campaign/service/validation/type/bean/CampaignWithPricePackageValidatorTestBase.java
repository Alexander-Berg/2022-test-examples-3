package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;

@ParametersAreNonnullByDefault
class CampaignWithPricePackageValidatorTestBase {

    CpmPriceCampaign validCampaign() {
        return new CpmPriceCampaign()
                .withCurrency(CurrencyCode.RUB)
                .withStartDate(LocalDate.of(2020, 1, 1))
                .withEndDate(LocalDate.of(2020, 12, 1))
                .withPricePackageId(1L)
                .withFlightOrderVolume(700L)
                .withFlightTargetingsSnapshot(new PriceFlightTargetingsSnapshot()
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE))
                        .withAllowExpandedDesktopCreative(true));
    }

    PricePackage defaultPricePackage(ClientId clientId) {
        return new PricePackage()
                .withId(1L)
                .withTitle("Default Package")
                .withTrackerUrl("http://ya.ru")
                .withPrice(BigDecimal.valueOf(4000))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(500L)
                .withOrderVolumeMax(1000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new TargetingsCustom())
                .withStatusApprove(StatusApprove.YES)
                .withLastUpdateTime(LocalDateTime.parse("2019-08-09T00:11:04"))
                .withDateStart(LocalDate.of(2020, 1, 1))
                .withDateEnd(LocalDate.of(2020, 12, 1))
                .withIsPublic(false)
                .withIsArchived(false)
                .withClients(List.of(allowedPricePackageClient(clientId.asLong())));
    }

}
