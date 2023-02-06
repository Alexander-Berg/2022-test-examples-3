import pytest

from test_utils import TestParser


class TestYandexGoodsDjRecommendParser(TestParser):
    def test_grouping_size(self):
        components = self.read_components("test_response.json")
        assert len(components) == 3

    @pytest.mark.parametrize("path, idx, sku_id, product_id, title, image_url, page_url", [
        ("test_response.json", 0, "101434432970", 1427396992,
         "Джинсы Haze&Finn IMIQ Regular Fit Stretch Jeans, размер 31, рост 32, rinsed",
         "http://avatars.mds.yandex.net/get-mpic/5235128/img_id7274210198462265981.jpeg/orig",
         "http://yandex.ru/products/product/1427396992/sku/101434432970"),
        ("test_response.json", 1, "101349454789", 982601083,
         "Джинсы Levi's 501® ORIGINAL FIT, размер 31, рост 32, key west sky",
         "http://avatars.mds.yandex.net/get-mpic/5363183/img_id7250498092429885978.jpeg/orig",
         "http://yandex.ru/products/product/982601083/sku/101349454789"),
    ])
    def test_component_sku_id(self, path, idx, sku_id, product_id, title, image_url, page_url):
        components = self.read_components(path)
        assert components[idx]["json.serpData"]["id"] == sku_id
        assert components[idx]["json.serpData"]["productId"] == product_id
        assert components[idx]["text.title"] == title
        assert components[idx]["imageadd"]["url"] == image_url
        assert components[idx]["componentUrl"]["pageUrl"] == page_url

    @pytest.mark.parametrize("path, idx, sku_id, generator", [
        ("test_response.json", 0, "101434432970", "PN_Goods#ET_NNLN_V12_ENC_MARKET_V4_192_hnsw_candidate_generator"),
        ("test_response.json", 1, "101349454789", "PN_Goods#Title_hnsw_candidate_generator"),
    ])
    def test_sku_data(self, path, idx, sku_id, generator):
        components = self.read_components(path)
        assert components[idx]["json.serpData"]["id"] == sku_id
        assert components[idx]["json.serpData"]["sku_data"]["generator"] == generator
