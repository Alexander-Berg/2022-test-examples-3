package ru.yandex.market.mboc.app.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.mboc.app.security.SecuredRolesIgnore;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.yt.YtOffersReader;
import ru.yandex.market.springmvctots.annotations.TsIgnore;

@TsIgnore
@RestController
@SecuredRolesIgnore(reason = "Integration test dev handles")
@RequestMapping("/api/int-test")
public class IntegrationTestController {

    private final JdbcTemplate jdbcTemplate;
    private final YtOffersReader ytOffersReader;

    IntegrationTestController(JdbcTemplate jdbcTemplate,
                              YtOffersReader ytOffersReader) {
        this.jdbcTemplate = jdbcTemplate;
        this.ytOffersReader = ytOffersReader;
    }

    @GetMapping(value = "/cleanup")
    public String intTestCleanup(@RequestParam(name = "supplierId") Integer supplierId,
                                 @RequestParam(name = "shopSkuId") String shopSkuId) {
        jdbcTemplate.update("delete from mbo_category.offer where supplier_id = ? and shop_sku = ?",
            supplierId, shopSkuId);
        ytOffersReader.deleteOffers(Collections.singletonList(new ShopSkuKey(supplierId, shopSkuId)));
        return "Done";
    }

    @GetMapping(value = "/check-yt-offer")
    public String intTestCheckYtOffer(@RequestParam(name = "supplierId") Integer supplierId,
                                      @RequestParam(name = "shopSkuId") String shopSkuId) {
        List<Long> stamps = new ArrayList<>();
        ytOffersReader.readOffer(supplierId, shopSkuId, node -> stamps.add(node.getLong("stamp")));
        if (stamps.isEmpty()) {
            return String.valueOf(0L);
        } else {
            return String.valueOf(stamps.get(stamps.size() - 1));
        }
    }

    @GetMapping(value = "/update-processing-status")
    public String intTestUpdateProcessingStatus(@RequestParam(name = "offerId") Long offerId,
                                                @RequestParam(name = "processingStatus") String processingStatus) {
        jdbcTemplate.update("update mbo_category.offer" +
            " set processing_status = ?::mbo_category.offer_processing_status" +
            " where id = ?", processingStatus, offerId);
        return "Done";
    }
}
