from market.content_storage_service.lite.core.types.model import Model, Sku
from market.content_storage_service.lite.core.types.hid import Hid
from market.content_storage_service.lite.core.types.nid import Nid
from market.content_storage_service.lite.core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.market_content_storage.with_saas_server = True
        cls.saas = T.market_content_storage.saas_server.connect()

        # Models creation example:
        cls.index.models += [
            Model(hyperid=4242, hid=200500),
            Model(hyperid=4343, hid=200501),
        ]

        # Sku creation example:
        cls.index.skus += [
            Sku(sku_id=90001, model_id=4242, hid=200500),
            Sku(sku_id=90002, model_id=4343, hid=200501),
        ]

        cls.index.nids += [
            Nid(nid=100, hid=200, main=True, parent_id=0),
            Nid(nid=1005, hid=2005, main=True, parent_id=100),
            Nid(nid=10050, hid=20050, main=True, parent_id=1005),
            Nid(nid=100500, hid=200500, main=True, parent_id=10050),
            Nid(nid=100501, hid=200501, main=False),
            Nid(nid=100502, hid=200500, main=False),
        ]
        cls.index.hids += [
            Hid(hid=200),
            Hid(hid=2005),
            Hid(hid=20050),
            Hid(hid=200500),
            Hid(hid=200501),
        ]

    def test_sku_fast_ids(self):
        response = self.market_content_storage.request_json(
            'fast_ids?market_sku=90001&model_id=4242&model_id=4343&market_sku=90002',
            method='GET',
        )

        self.assertFragmentIn(
            response,
            {
                "models": [
                    {"id": 4242, "navNodes": [100500, 10050, 1005, 100]},
                    {"id": 4343, "navNodes": []},  # only main nids can be found
                ],
                "mskus": [
                    {"id": 90001, "modelId": 4242, "navNodes": [100500, 10050, 1005, 100]},
                    {"id": 90002, "modelId": 4343, "navNodes": []},  # only main nids can be found
                ],
            },
        )


if __name__ == '__main__':
    main()
