import market.mars.lite.env as env
from market.pylibrary.lite.matcher import Regex
from market.library.shiny.lite.log import Severity

PRIME_RESPONSE = {
    "search": {
        "results": [{"entity": "product", "id": 2, "offers": {"items": [{"supplier": {"warehouseId": 100654}}]}}]
    }
}

COMPLEMENTARY_RESPONSE = {
    "search": {
        "results": [
            {
                "entity": "group",
                "results": [
                    {"entity": "product", "id": 1, "offers": {"items": [{"supplier": {"warehouseId": 100654}}]}}
                ],
            }
        ]
    }
}

OFFERINFO_RESPONSE = {
    "search": {
        "results": [
            {
                "entity": "offer",
                "wareId": "SIAcerpCBfCfomxNI3uiQg",
                "supplier": {"entity": "shop", "id": 1189377, "warehouseId": 100654},
                "model": {"id": 31337},
            }
        ]
    }
}

MERGED_RESPONSE = {
    "search": {
        "results": [
            {"entity": "product", "id": 1, "offers": {"items": [{"supplier": {"warehouseId": 100654}}]}},
            {"entity": "product", "id": 2, "offers": {"items": [{"supplier": {"warehouseId": 100654}}]}},
        ]
    }
}

ERROR_RESPONSE = {'MARS Error': 'No report answer'}


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {'main_report': cls.mars.main_report}

    @classmethod
    def prepare(cls):
        prime = cls.mars.main_report.prime()
        prime.set_path_response(
            'yandsearch?place=offerinfo&rids=213&pp=18&regset=2&offerid=some_offer_00000111112&bsformat=2',
            OFFERINFO_RESPONSE,
            file_format='json',
        )
        prime.set_path_response(
            'yandsearch?place=prime&rids=213&pp=18&numdoc=10&cpa=real&allow-collapsing=1&warehouse-id=100654'
            '&filter-express-delivery=1&regset=2&entity=product&cart=some_offer_00000111112&new-picture-format=1',
            PRIME_RESPONSE,
            file_format='json',
        )
        prime.set_path_response(
            'yandsearch?place=complementary_product_groups&rids=213&pp=18&numdoc=10&cpa=real&warehouse-id=100654'
            '&filter-express-delivery=1&regset=2&cart=some_offer_00000111112&hyperid=31337'
            '&show-all-complementary-products=1&new-picture-format=1',
            COMPLEMENTARY_RESPONSE,
            file_format='json',
        )

    def test_mars_filters_via_warehouse_id(self):
        """Проверяем основной пайплайн работы: запрос в offerinfo за warehouseId, запрос в prime с этим warehouseId"""
        response = self.mars.request_json(
            'go-express/cart-recom?rids=213&pp=18&numdoc=10&cpa=real&allow-collapsing=1'
            '&filter-express-delivery=1&supplier-id=1189377&regset=2&cart=some_offer_00000111112&mars-debug=1'
        )

        self.assertFragmentIn(response, MERGED_RESPONSE)

    def test_mars_filters_via_warehouse_id_without_optional_parameters(self):
        """Проверяем случай когда supplier-id и filter-express-delivery не переданы"""
        response = self.mars.request_json(
            'go-express/cart-recom?rids=213&pp=18&numdoc=10&cpa=real&allow-collapsing=1&regset=2'
            '&cart=some_offer_00000111112&mars-debug=1'
        )

        self.assertFragmentIn(response, MERGED_RESPONSE)
        self.trace_log.expect(http_code=200, type='OUT')

    def test_no_report_answer(self):
        """Проверяем, что MARS пишет ошибку в случае неответа репорта"""
        response = self.mars.request_json('go-express/cart-recom?some_random_params', fail_on_error=False)
        self.assertFragmentIn(response, ERROR_RESPONSE)
        self.common_log.expect(message=Regex('.*No report answer.*'), severity=Severity.ERROR)
        self.trace_log.expect(http_code=400, type='IN')


if __name__ == '__main__':
    env.main()
