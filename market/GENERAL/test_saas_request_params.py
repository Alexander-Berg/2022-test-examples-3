#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid


class T(env.MediaAdvIncutSearchSuite):

    @classmethod
    def prepare_saas_request_kps(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=1001,
                id=1,
                vendor_id=1,
                datasource_id=1,
                models=[
                    ModelWithBid(model_id=1000+i) for i in range(1, 4)
                ],
                bid=90,
            ),
            IncutModelsList(
                hid=1001,
                id=2,
                vendor_id=2,
                datasource_id=2,
                models=[
                    ModelWithBid(model_id=1010+i) for i in range(1, 4)
                ],
                bid=45,
                kps=555,
            ),
        ]

    # врезка отдается из дефолтного неймспейса
    def test_saas_request_default_kps(self):
        response = self.request({
            'hid': 1001,
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasId': 1,
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 1,
                    },
                },
            },
        })

    # с флагом врезка отдается из запрошенного неймспейса
    def test_saas_request_kps_in_flag(self):
        response = self.request({
            'hid': 1001,
        }, exp_flags={
            'market_madv_saas_request_prefix': '555',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasId': 2,
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 2,
                    },
                },
            },
        })

    # с несуществующим неймспейсом в флаге врезка пустая
    def test_saas_request_invalid_kps_in_flag(self):
        response = self.request({
            'hid': 1001,
        }, exp_flags={
            'market_madv_saas_request_prefix': '333',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'Empty',
                    },
                },
            },
        })

    @classmethod
    def prepare_saas_request_page(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=1003,
                id=3,
                vendor_id=3,
                datasource_id=3,
                models=[
                    ModelWithBid(model_id=1000+i) for i in range(1, 4)
                ],
                bid=90,
            ),
            IncutModelsList(
                hid=1003,
                id=4,
                vendor_id=4,
                datasource_id=4,
                models=[
                    ModelWithBid(model_id=1010+i) for i in range(1, 4)
                ],
                bid=45,
                page=1,
            ),
        ]

    # по умолчанию врезка отдается для поиска
    def test_saas_request_default_page(self):
        response = self.request({
            'hid': 1003,
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasId': 3,
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 3,
                    },
                },
            },
        })

    # при заданном параметре врезка отдается соответствующая
    def test_saas_request_modelcard_page(self):
        response = self.request({
            'hid': 1003,
            'target_page': 'modelcard',
        })
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasId': 4,
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 4,
                    },
                },
            },
        })

    @classmethod
    def prepare_saas_request_vendor(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=1005,
                id=5,
                vendor_id=5,
                datasource_id=5,
                models=[
                    ModelWithBid(model_id=1000+i) for i in range(1, 4)
                ],
                bid=90,
                vendor_ids=[1],
            ),
            IncutModelsList(
                hid=1005,
                id=6,
                vendor_id=6,
                datasource_id=6,
                models=[
                    ModelWithBid(model_id=1010+i) for i in range(1, 4)
                ],
                bid=45,
            ),
        ]

    # по умолчанию приходят врезки для любого вендора (0),
    # врезка с id=5 саасом не отдается
    def test_saas_request_any_vendors(self):
        response = self.request({
            'hid': 1005,
        }, debug=True)
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasId': 6,
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 6,
                    },
                },
            },
            'debug': {'counters': {'incuts': {
                'Total': 1,
                'Passed': 1,
            }}},
        })

    # с указанным вендором из сааса придут обе, победит id=5
    def test_saas_request_vendor_id_1(self):
        response = self.request({
            'hid': 1005,
            'vendor': 1,
        }, debug=True)
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasId': 5,
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 5,
                    },
                },
            },
            'debug': {'counters': {'incuts': {
                'Total': 2,
                'Passed': 2,
            }}},
        })

    # с указанным несуществующим вендором
    # из сааса придет только с id=6 (без списка вендоров)
    def test_saas_request_vendor_id_2(self):
        response = self.request({
            'hid': 1005,
            'vendor': 2,
        }, debug=True)
        self.assertFragmentIn(response, {
            'incutLists': [[{
                'entity': 'incut',
                'id': '1',
            }]],
            'entities': {
                'incut': {
                    '1': {
                        'incutType': 'ModelsList',
                        'saasId': 6,
                    },
                },
                'vendor': {
                    '1': {
                        'vendorId': 6,
                    },
                },
            },
            'debug': {'counters': {'incuts': {
                'Total': 1,
                'Passed': 1,
            }}},
        })


if __name__ == '__main__':
    env.main()
