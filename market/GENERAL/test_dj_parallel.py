#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    MarketSku,
    Model,
    Offer,
    RegionalModel,
    YamarecDjDefaultModelsList,
    YamarecPlace,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.dj import DjModel

OMM_PARALLEL_PLACES = [
    'place=omm_parallel',
    'place=omm_parallel&omm_place=yandexapp_vertical',
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.models += [
            Model(hyperid=6000, hid=600),
            Model(hyperid=6001, hid=600),
            Model(hyperid=6002, hid=600),
            Model(hyperid=6003, hid=600),
        ]

        cls.index.offers += [
            Offer(hyperid=6000),
            Offer(hyperid=6001),
            Offer(hyperid=6002),
            Offer(hyperid=6003),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=6000, sku=60000, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6001, sku=60010, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6002, sku=60020, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6003, sku=60030, blue_offers=[BlueOffer()]),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=6000, offers=1),
            RegionalModel(hyperid=6001, offers=1),
            RegionalModel(hyperid=6002, offers=1),
            RegionalModel(hyperid=6003, offers=1),
        ]

    @classmethod
    def prepare_dj_exp(cls):
        cls.dj.on_request(yandexuid='700', exp='dj_parallel').respond([DjModel(id=6001)])

    def test_dj_exp(self):
        '''Test base case.'''

        request = '{place}&rearr-factors=market_blue_omm_parallel={blue};market_dj_exp_for_omm_parallel=dj_parallel&yandexuid=700'
        for place in OMM_PARALLEL_PLACES:

            response = self.report.request_json(request.format(place=place, blue=0))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "product", "id": 6001},
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

            response = self.report.request_json(request.format(place=place, blue=1))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "product", "id": 6001},
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_dj_unistat(cls):
        cls.dj.on_request(yandexuid='701', exp='dj_parallel').respond([])

    def test_dj_unistat(self):
        '''Test that correct unitstat counters filled.'''

        def check_unistat_data(request, unistat_prefix, pumpkin_expected):

            # Сохраняем счётчики до запроса
            tass_data_before = self.report.request_tass()

            # Делаем запрос
            self.report.request_json(request)

            # Сохраняем счётчики после запроса
            tass_data_after = self.report.request_tass()

            # Сравниваем
            # Общее число запросов к нужному плейсу
            self.assertEqual(
                tass_data_before.get(unistat_prefix + '_request_count_dmmm', 0) + 1,
                tass_data_after.get(unistat_prefix + '_request_count_dmmm', 0),
            )
            # Число обращений к тыкве не изменилось
            self.assertEqual(
                tass_data_before.get(unistat_prefix + '_pumpkin_shows_count_dmmm', 0) + int(pumpkin_expected),
                tass_data_after.get(unistat_prefix + '_pumpkin_shows_count_dmmm', 0),
            )
            self.assertEqual(
                tass_data_before.get(unistat_prefix + '_error_count_dmmm', 0),
                tass_data_after.get(unistat_prefix + '_error_count_dmmm', 0),
            )
            # Тайминги как-то считаются
            self.assertIn(unistat_prefix + '_dj_request_time_hgram', tass_data_after.keys())
            self.assertIn(unistat_prefix + '_full_request_time_hgram', tass_data_after.keys())
            # игнорируем ошибки связанные с пустым ответом dj
            self.error_log.ignore(code=3796)

        test_cases = [
            dict(place='place=omm_parallel&omm_place=yandexapp_vertical', prefix='omm_parallel_yandexapp_vertical'),
            dict(
                place='place=omm_parallel&omm_place=yandexapp_vertical&rearr-factors=market_yandexapp_blue_vertical=1',
                prefix='omm_parallel_yandexapp_vertical',
            ),
        ]

        request = '{place}&rearr-factors=market_dj_exp_for_omm_parallel=dj_parallel&yandexuid={yandexuid}'
        for tst in test_cases:
            check_unistat_data(
                request=request.format(place=tst['place'], yandexuid=700),
                unistat_prefix=tst['prefix'],
                pumpkin_expected=False,
            )
            check_unistat_data(
                request=request.format(place=tst['place'], yandexuid=701),
                unistat_prefix=tst['prefix'],
                pumpkin_expected=True,
            )

    @classmethod
    def prepare_dj_pumpkin(cls):
        cls.index.models += [
            Model(hyperid=6100, hid=610),
        ]

        cls.index.offers += [
            Offer(hyperid=6100),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=6100, sku=61000, blue_offers=[BlueOffer()]),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=6100, offers=1),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.DJ_DEFAULT_MODELS_LIST,
                kind=YamarecPlace.Type.DJ_DEFAULT_MODELS_LIST,
                partitions=[
                    YamarecDjDefaultModelsList(dj_default_models_list=[[6100, 'title', 'picture']]),
                ],
            ),
        ]

        cls.dj.on_request(yandexuid='702', exp='null').return_code(503)
        cls.dj.on_request(yandexuid='702', exp='empty').respond([])

    def test_dj_pumpkin(self):
        '''Test dj pumpkin on parallel.'''

        request = '{place}&rearr-factors=market_dj_exp_for_omm_parallel={exp}&yandexuid=702'

        for place in OMM_PARALLEL_PLACES:

            response = self.report.request_json(request.format(place=place, exp='null'))
            self.assertFragmentIn(response, {'results': [{'entity': 'product', 'id': 6100}]}, allow_different_len=False)
            self.error_log.expect(code=3787)

            response = self.report.request_json(request.format(place=place, exp='empty'))
            self.assertFragmentIn(response, {'results': [{'entity': 'product', 'id': 6100}]}, allow_different_len=False)

    @classmethod
    def prepare_enable_reasons_to_buy(cls):
        cls.dj.on_request(yandexuid='704', exp='dj_parallel').respond([DjModel(id=6003)])
        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy(blue=True)
            .new_partition("")
            .add(
                hyperid=6003,
                reasons=[
                    {"id": "viewed_n_times_blue", "value": "100500"},
                    {"id": "bought_n_times_blue", "value": "100"},
                    {"id": "bestseller_blue", "value": "100500"},
                    {"id": "alisa_lives_here_blue", "value": "1"},
                ],
            ),
            YamarecPlaceReasonsToBuy()
            .new_partition("")
            .add(
                hyperid=6003,
                reasons=[
                    {"id": "viewed_n_times", "value": "500"},
                    {"id": "bought_n_times", "value": "10"},
                    {"id": "bestseller", "value": "100500"},
                    {"id": "alisa_lives_here", "value": "5"},
                ],
            ),
        ]

    def test_dj_add_reasons_to_buy(self):
        '''
        Test dj reasons_to_buy.
        '''

        request = '{place}&rgb=blue&rearr-factors=market_blue_omm_parallel=1;market_dj_exp_for_omm_parallel=dj_parallel&yandexuid=704'

        cases = (
            # enable-reasons model_id, reasonsToBuy
            (1, 6003, ["viewed_n_times", "alisa_lives_here"]),
            (0, 6003, None),
            (None, 6003, None),
        )

        for enable_reasons, model, reasons_to_buy in cases:
            final_request = request
            if enable_reasons is not None:
                final_request += "&enable-reasons-to-buy={}".format(enable_reasons)
            for place in OMM_PARALLEL_PLACES:
                response = self.report.request_json(final_request.format(place=place))
                pattern = {'results': [{'entity': 'product', 'id': model}]}
                if reasons_to_buy is not None:
                    pattern["results"][0]["reasonsToBuy"] = [{"id": reason_to_buy} for reason_to_buy in reasons_to_buy]
                self.assertFragmentIn(response, pattern, allow_different_len=False)


if __name__ == '__main__':
    main()
