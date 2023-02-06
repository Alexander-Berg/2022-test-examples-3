#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, YamarecCategoryBanList, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.dj import DjModel

CATEGORY_BAN_LIST = {
    16155381: 'hard',  # Алкоголь,
    6091783: 'soft',  # Товары для взрослых,
}

BANNED_CATEGORIES = list(CATEGORY_BAN_LIST.keys())

REGULAR_CATEGORIES = [
    10604359,  # Компьютеры
]

categories = BANNED_CATEGORIES + REGULAR_CATEGORIES
indices = list(range(2 * len(categories)))


def hid(i):
    return categories[i / 2]


def model(i):
    return 100 + i


def offer(i):
    return 200 + i


def sku(i):
    return 300 + i


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_BAN_LIST,
                kind=YamarecPlace.Type.CATEGORY_BAN_LIST,
                partitions=[YamarecCategoryBanList(ban_list=CATEGORY_BAN_LIST)],
            )
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            ),
        ]

        cls.index.models += [Model(hyperid=model(i), hid=hid(i)) for i in indices]

        # green offers
        cls.index.offers += [Offer(hyperid=model(i)) for i in indices]

        # blue offers
        cls.index.mskus += [MarketSku(hyperid=model(i), sku=sku(i), blue_offers=[BlueOffer()]) for i in indices]

        for i in indices[::2]:
            cls.recommender.on_request_accessory_models(model_id=model(i), item_count=1000, version='1').respond(
                {'models': [str(model(i + 1))]}
            )
            cls.dj.on_request(exp="test_soft_bans", yandexuid="1111", hyperid=str(model(i))).respond(
                [
                    DjModel(id=str(model(i + 1))),
                ]
            )

    def assertBanStatus(self, rgb, hyperid, banned, pp=None, ban_mode=None):

        request = 'place=product_accessories&rgb={rgb}&hyperid={hyperid}&rearr-factors=market_disable_product_accessories=0'.format(
            rgb=rgb, hyperid=hyperid
        )

        if pp is not None:
            request += '&pp=' + str(pp)

        if ban_mode is not None:
            request += '&ban-mode=' + ban_mode

        response = self.report.request_json(request)

        if banned:
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': []}})
        else:
            self.assertFragmentIn(
                response, {'search': {'total': 1, 'results': [{'entity': 'product', 'id': (hyperid + 1)}]}}
            )

    def assertBanStatusPlaceDj(self, hyperid, banned, flags=''):
        request = 'place=dj&dj-place=test_soft_bans&djid=test_soft_bans&hyperid={hyperid}&yandexuid=1111'.format(
            hyperid=hyperid
        )
        if flags:
            request += '&' + flags

        response = self.report.request_json(request)

        if banned:
            self.assertFragmentIn(response, {'search': {'total': 0, 'results': []}})
        else:
            self.assertFragmentIn(
                response, {'search': {'total': 1, 'results': [{'entity': 'product', 'id': (hyperid + 1)}]}}
            )

    def test_green(self):
        '''
        Green market should work as before.
        '''

        self.assertBanStatus('green', pp=None, hyperid=100, banned=True)
        self.assertBanStatus('green', pp=None, hyperid=102, banned=True)
        self.assertBanStatus('green', pp=None, hyperid=104, banned=False)

    def test_blue(self):
        '''
        Beru should show products from soft ban categories only on limited
        number of pps. Hard banned and not banned products should have appear
        usual behaviour.
        '''

        soft_ban_pps = [
            1041,
            1042,
            1048,
            1050,
            1641,
            1642,
            1648,
            1650,
            1741,
            1742,
            1748,
            1750,
            1841,
            1842,
            1848,
            1850,
        ]

        pps = [None, 18, 999] + soft_ban_pps

        for pp in pps:
            self.assertBanStatus('blue', pp=pp, hyperid=100, banned=True)
            self.assertBanStatus('blue', pp=pp, hyperid=102, banned=pp not in soft_ban_pps)
            self.assertBanStatus('blue', pp=pp, hyperid=104, banned=False)

    def test_ban_mode_flag(self):
        '''
        Test behaviour of the new cgi parameter 'ban-mode'
        '''

        for ban_mode in [None, 'hard', 'soft']:
            self.assertBanStatus('blue', ban_mode=ban_mode, hyperid=100, banned=True)
            self.assertBanStatus('blue', ban_mode=ban_mode, hyperid=102, banned=(ban_mode != 'soft'))
            self.assertBanStatus('blue', ban_mode=ban_mode, hyperid=104, banned=False)

    def test_invalid_ban_mode_value(self):
        '''
        In case of invalid value of 'ban-mode' cgi parameter, report should
        behave as if ban mode wasn't specified at all and log an error.
        '''
        error_text = "Key 'foo' not found in enum Market::EBanMode."
        self.error_log.expect((error_text))
        try:
            _ = self.report.request_json(
                'place=product_accessories&ban-mode=foo&hyperid=100&rearr-factors=market_disable_product_accessories=0'
            )
        except RuntimeError as e:
            self.assertIn(error_text, str(e))
        else:
            self.assertTrue(False)

    def test_priority(self):
        '''
        CGI parameter 'ban-mode' should override 'pp'
        '''

        self.assertBanStatus('blue', pp=1042, ban_mode='hard', hyperid=102, banned=True)
        self.assertBanStatus('blue', pp=18, ban_mode='soft', hyperid=102, banned=False)

    def test_dj_ban_mode_is_soft(self):
        self.assertBanStatusPlaceDj(100, True)
        self.assertBanStatusPlaceDj(102, False)
        self.assertBanStatusPlaceDj(102, True, 'rearr-factors=market_recom_enable_soft_bans_for_dj=0')
        self.assertBanStatusPlaceDj(102, True, 'ban-mode=hard')
        self.assertBanStatusPlaceDj(104, False)


if __name__ == '__main__':
    main()
