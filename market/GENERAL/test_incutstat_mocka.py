#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Region,
    Vendor,
    MarketSku,
    BlueOffer,
    Shop,
    Currency,
    Tax,
)
from core.testcase import main
from market.report.proto.ReportState_pb2 import TCommonReportState  # noqa pylint: disable=import-error
from test_media_adv import TestMediaAdv
from google.protobuf.json_format import MessageToJson
from market.media_adv.incut_search.proto.grpc.incut_stat_grpc_pb2 import (
    TIncutStatResponse,
    TIncutStatData,
    TMinModelCounts,
)


class T(TestMediaAdv):
    @classmethod
    def prepare_mock_data(cls):
        """
        Подготовка дерева регионов для тест моки ручки incutstat, ответа моки
        """
        incut_id = '987654'
        cls.index.regiontree += [
            Region(rid=50, name='Край Питоновск', region_type=Region.FEDERATIVE_SUBJECT),
            Region(rid=230, name='Область плюсовая', region_type=Region.FEDERATIVE_SUBJECT),
        ]
        mock_str_valid_incutstat = MessageToJson(
            TIncutStatResponse(
                IncutStat={
                    987654: TIncutStatData(
                        ModelIds=[545, 676, 898],
                        RegionStat={
                            50: 0,
                            230: 2,
                        },
                    )
                }
            ),
        )
        cls.media_advertising.on_request_media_adv_incutstat(incut_ids=incut_id).respond(mock_str_valid_incutstat)

    def test_incutstat_mock(self):
        """
        Проверка работы моки для ручки incutstats
        Запрашиваем incut_id и проверяем ответ
        В ответе должна быть информация о регионах, где врезка отображается
        """
        params = {}
        params['place'] = 'madv_incut'
        params['madv-incut-ids'] = '987654'
        response = self.report.request_json(self.get_request(params, {}))
        self.assertFragmentIn(
            response,
            {
                'incutStat': {
                    '987654': {
                        'brief': {
                            'total': 2,
                        },
                        'regions': {
                            '50': {'status': 0, 'name': 'Край Питоновск'},  # NoFilter
                            '230': {
                                'status': 2,  # MRS
                                'name': 'Область плюсовая',
                            },
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_mock_data_with_error(cls):
        """
        Подготовка пустого ответа моки для проверки пустого ответа
        """
        incut_id = '121315'
        mock_data_empty = ''
        cls.media_advertising.on_request_media_adv_incutstat(incut_ids=incut_id).respond(mock_data_empty)

    def test_empty_data(self):
        """
        Если по запрошенному incut_id ничего не нашлось - в ответе должно быть сообщение об ошибке - ResponseIsEmpty
        """
        params = {}
        params['place'] = 'madv_incut'
        params['madv-incut-ids'] = '121315'
        response = self.report.request_json(self.get_request(params, {}))
        self.assertFragmentIn(
            response,
            {
                'error': 1,  # ResponseIsEmpty
            },
        )

    @classmethod
    def prepare_mock_data_invalid(cls):
        """
        Подготовка невалидного ответа моки
        """
        incut_id = '404'
        mock_data_invalid = 'This MOCK shoUld be _ invlid =)'
        cls.media_advertising.on_request_media_adv_incutstat(incut_ids=incut_id).respond(mock_data_invalid)

    def test_invalid_data(self):
        """
        Если при запрошенном incut_id возвращается невалидный ответ - в ответе также должно быть сообщение об ошибке - ResponseIsInvalid
        """
        params = {}
        params['place'] = 'madv_incut'
        params['madv-incut-ids'] = '404'
        response = self.report.request_json(self.get_request(params, {}))
        self.assertFragmentIn(
            response,
            {
                'error': 2,  # ResponseIsInvalid
            },
        )

    @classmethod
    def prepare_incutstat_for_skus(cls):
        hid = 100
        start_hyper_id = 2000
        start_sku_id = 1000
        fesh_id = 613

        mock_str_valid_incutstat = MessageToJson(
            TIncutStatResponse(
                IncutStat={
                    1001: TIncutStatData(
                        ModelIds=[],
                        SkuIds=[1000 + i for i in range(10)],
                        RegionStat={
                            50: 0,
                            230: 2,
                        },
                        MinModelCounts=TMinModelCounts(Normal=6, Degraded=3),
                    ),
                    3001: TIncutStatData(
                        ModelIds=[],
                        SkuIds=[3000 + i for i in range(10)],
                        RegionStat={
                            50: 0,
                            230: 2,
                        },
                        MinModelCounts=TMinModelCounts(Normal=6, Degraded=3),
                    ),
                    5001: TIncutStatData(
                        ModelIds=[],
                        SkuIds=[5000 + i for i in range(10)],
                        RegionStat={
                            50: 0,
                            230: 2,
                        },
                        MinModelCounts=TMinModelCounts(Normal=6, Degraded=3),
                    ),
                }
            ),
        )
        cls.media_advertising.on_request_media_adv_incutstat(incut_ids='1001,3001,5001').respond(
            mock_str_valid_incutstat
        )

        vendor_id = 45
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        cls.index.shops += [
            Shop(
                fesh=fesh_id - 1,
                datafeed_id=fesh_id - 1,
                priority_region=50,
                regions=[50],
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),  # virtual shop
            Shop(
                fesh=fesh_id,
                datafeed_id=fesh_id,
                priority_region=50,
                regions=[50],
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
            ),  # supplier
        ]

        cls.index.mskus += [
            MarketSku(
                sku=start_sku_id + x,
                hyperid=start_hyper_id + x if x < 5 else None,
                vendor_id=vendor_id,
                hid=hid,
                blue_offers=[
                    BlueOffer(
                        offerid=start_sku_id + x,
                        feedid=fesh_id,
                    )
                ],
            )
            for x in range(10)
        ]
        cls.index.mskus += [
            MarketSku(
                sku=start_sku_id + x + 2000,
                hyperid=start_hyper_id + x + 2000 if x < 2 else None,
                vendor_id=vendor_id,
                hid=hid,
                blue_offers=[
                    BlueOffer(
                        offerid=start_sku_id + x + 2000,
                        feedid=fesh_id,
                    )
                ],
            )
            for x in range(5)
        ]

    def test_incutstat_for_skus(self):
        """
        Проверяем запрос статистики и фильтрацию врезок с скю
        """
        params = {}
        params['place'] = 'madv_incut'
        params['madv-incut-ids'] = '1001,3001,5001'
        response = self.report.request_json(self.get_request(params, {}))
        self.assertFragmentIn(
            response,
            {
                'incutStat': {
                    '1001': {
                        'brief': {
                            'total': 2,
                        },
                        'regions': {
                            '50': {'status': 0, 'name': 'Край Питоновск'},  # NoFilter
                            '230': {
                                'status': 2,  # MRS
                                'name': 'Область плюсовая',
                            },
                        },
                    },
                    '3001': {
                        'brief': {
                            'total': 2,
                        },
                        'regions': {
                            '50': {'status': 1, 'name': 'Край Питоновск'},  # Degraded
                            '230': {
                                'status': 2,  # MRS
                                'name': 'Область плюсовая',
                            },
                        },
                    },
                    '5001': {
                        'brief': {
                            'total': 2,
                        },
                        'regions': {
                            '50': {'status': 3, 'name': 'Край Питоновск'},  # Index
                            '230': {
                                'status': 2,  # MRS
                                'name': 'Область плюсовая',
                            },
                        },
                    },
                },
            },
        )


if __name__ == '__main__':
    main()
