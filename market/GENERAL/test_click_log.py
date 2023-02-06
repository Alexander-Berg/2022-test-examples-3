#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import urllib
import json

from core.cpc import Cpc, Cpa
from core.click_context import ClickContext
from core.types import (
    BlueOffer,
    ClickType,
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryOption,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Shop,
    Tax,
    UrlType,
    Vat,
    Vendor,
)
from core.types.market_pp import MarketPp
from core.testcase import TestCase, main
from core.matcher import (
    NotEmpty,
    NoKey,
    LikeUrl,
    LessEq,
    Absent,
    Contains,
    ElementCount,
    Wildcard,
)

import six

if six.PY3:
    from urllib.parse import quote
else:
    from urllib import quote


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=444, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=555, output_type=HyperCategoryType.CLUSTERS, visual=True),
        ]

        cls.index.offers = [
            Offer(
                title='samsung',
                fesh=1,
                cpa=Offer.CPA_REAL,
                waremd5='lJ6RKOrkpvarYA06_0Zmdg',
                offerid=100,
                hyperid=1000,
                feedid=200,
            ),
            Offer(title='potato', fesh=1, offerid="offer/id/with/slashes"),
            Offer(title='iphone', fesh=2),
            Offer(
                title='cheeter',
                fesh=1,
                url='apple.com/store?ymclid=123&_openstat=mama&frommarket=obama',
                waremd5='xMpCOKC5I4INzFCab3WEmw',
            ),
            Offer(title='MARKETOUT-7696', fesh=3, url='http://stoktrade.ru/gostinaya-legenda'),
            Offer(title='sony', fesh=4, url='http://sony.ru/'),
            Offer(title='sony', fesh=5, url='http://sony.ru/'),
            Offer(title='sony', fesh=6, url='http://sony.ru/'),
            Offer(
                title='long',
                fesh=6,
                url='http://long-url.ru/with/long/path?and={long}&long=query'.format(long='-'.join(['very'] * 161)),
            ),
            Offer(title='nokia', fesh=7, cpa=Offer.CPA_REAL),
            Offer(title='meow', fesh=7, cpa=Offer.CPA_REAL, price=100, discount=7),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                open_stat=True,
                yclid=True,
                from_market=True,
                priority_region=2,
                regions=[213],
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=2, from_market=True, priority_region=2, cpa=Shop.CPA_REAL),
            Shop(fesh=3, yclid=False, priority_region=2, cpa=Shop.CPA_REAL),
            Shop(fesh=4, open_stat=False, yclid=False, from_market=True, priority_region=2, cpa=Shop.CPA_REAL),
            Shop(fesh=5, open_stat=True, yclid=False, from_market=True, priority_region=2, cpa=Shop.CPA_REAL),
            Shop(fesh=6, open_stat=False, yclid=True, from_market=True, priority_region=2),
            Shop(fesh=7, yclid=False, priority_region=2, cpa=Shop.CPA_REAL),
            Shop(fesh=222, yclid=False, priority_region=2, cpa=Shop.CPA_REAL),
            Shop(fesh=333, yclid=False, priority_region=2, cpa=Shop.CPA_REAL),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=444, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
            CpaCategory(hid=555, regions=[225], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.mskus += [
            MarketSku(title='cpa-offer', sku=123456),
        ]

        # cpa-pof & checkouter-url
        cls.index.offers += [
            Offer(
                title='clickUrl cpa-offer',
                feedid=123,
                offerid=456,
                fesh=222,
                hyperid=333,
                hid=444,
                waremd5='09lEaAKkQll1XTjm0WPoIA',
                cpa=Offer.CPA_REAL,
                price=10000000,
                randx=500,
                sku='123456',
                delivery_options=[DeliveryOption(price=100000, day_from=1, day_to=3, order_before=24)],
            ),
            Offer(
                title='clickUrl cpa-offer',
                feedid=12345,
                offerid=67890,
                fesh=333,
                hyperid=333,
                hid=444,
                waremd5='XXXXXXXXXXXXXXXXXXXXXX',
                cpa=Offer.CPA_REAL,
                price=10000000,
                randx=100,
                delivery_options=[DeliveryOption(price=100000, day_from=1, day_to=3, order_before=24)],
            ),
            Offer(
                feedid=234,
                offerid=567,
                fesh=333,
                vclusterid=1000000004,
                hid=555,
                waremd5='79lEaAKkQll1XTjm0WPoIA',
                cpa=Offer.CPA_REAL,
                price=20000000,
                delivery_options=[DeliveryOption(price=100000, day_from=1, day_to=3, order_before=24)],
            ),
        ]

    def test_drop_clicks_for_pp(self):
        self.report.request_json(
            'place=prime&text=samsung&rids=2&show-urls=external&rearr-factors=market_drop_clicks_for_pp=1,2&pp=2'
        )
        self.click_log.expect(ClickType.EXTERNAL).never()

    def test_shopid(self):
        self.report.request_json('place=prime&text=samsung&rids=2&show-urls=external,showPhone')
        self.click_log.expect(ClickType.EXTERNAL, shop_id=1, url_type=UrlType.EXTERNAL)
        self.click_log.expect(ClickType.SHOW_PHONE, shop_id=1, url_type=UrlType.SHOW_PHONE)

        self.report.request_json('place=prime&text=iphone&rids=2&show-urls=external')
        self.click_log.expect(ClickType.EXTERNAL, shop_id=2, url_type=UrlType.EXTERNAL)

    def test_discount(self):
        self.report.request_json('place=prime&text=meow&rids=2')
        self.click_log.expect(discount_ratio="0.07", discount="1")
        self.report.request_json('place=prime&text=nokia&rids=2')
        self.click_log.expect(discount_ratio=None, discount=None)

    def test_discount_in_reverse_exp(self):
        self.report.request_json('place=prime&text=meow&rids=2&rearr-factors=market_remove_promos=1')
        self.click_log.expect(discount_ratio="0.07", discount="1")

    def test_source_base_field(self):
        self.report.request_json('place=productoffers&hyperid=333&client=sovetnik&source_base=avito.ru')
        self.click_log.expect(src_bs='avito.ru')

    def test_shop_url_fields(self):
        self.report.request_json(
            'place=prime&text=samsung&referer=www.samsung.com&rids=2&show-urls=external,showPhone,cpa'
        )
        uids = {
            ClickType.EXTERNAL: "04884192001117778888800001",
            ClickType.SHOW_PHONE: None,
        }

        for clicktype in uids.keys():
            self.click_log.expect(
                clicktype,
                data_url=LikeUrl(
                    url_quoted=True,
                    url_params={
                        '_openstat': 'bWFya2V0LnlhbmRleC5ydTtzYW1zdW5nO2xKNlJLT3JrcHZhcllBMDZfMFptZGc7',
                        'frommarket': 'www.samsung.com',
                        'ymclid': uids[clicktype],
                    },
                )
                if uids[clicktype] is not None
                else None,
                uid=uids[clicktype],
                link_id=uids[ClickType.EXTERNAL],
                url_type=clicktype,
            )

    def test_ymclid_disabled(self):
        self.report.request_json('place=prime&text=nokia&rids=2&show-urls=external,showPhone,cpa')

        uids = {
            ClickType.EXTERNAL: "04884192001117778888800001",
            ClickType.SHOW_PHONE: "04884192001117778888807001",
        }

        for clicktype, uid in uids.items():
            self.click_log.expect(
                clicktype,
                data_url=LikeUrl(url_quoted=True, url_params={'ymclid': None}),
                uid=uid,
                link_id=uids[ClickType.EXTERNAL],
                url_type=clicktype,
            )

    def test_no_ymclid_if_too_long_url(self):
        self.report.request_json(
            'place=prime&text=samsung&referer=www.samsung.com&rids=2&'
            'show-urls=external,showPhone,cpa&test-buckets=1,2,3&rearr-factors=market_add_ymclid_to_url_any_length=0&'
            'test_tag=test_123{tag}&test_bits=101010{long}'.format(tag='TAG' * 800, long='10' * 1000)
        )

        self.click_log.expect(
            ClickType.EXTERNAL,
            data_url=LikeUrl(url_quoted=True, url_params={'ymclid': None}),
            uid="04884192001117778888800001",
            link_id="04884192001117778888800001",
            url_type=ClickType.EXTERNAL,
        )

    def test_ymclid_if_too_long_url_and_rearr_factor(self):
        self.report.request_json(
            'place=prime&text=samsung&referer=www.samsung.com&rids=2&'
            'show-urls=external,showPhone,cpa&'
            'test-buckets=1,2,3&test_tag=test_123{tag}&test_bits=101010{long}'.format(tag='TAG' * 800, long='10' * 1000)
        )

        self.click_log.expect(
            ClickType.EXTERNAL,
            data_url=LikeUrl(url_quoted=True, url_params={'ymclid': "04884192001117778888800001"}),
            uid="04884192001117778888800001",
            link_id="04884192001117778888800001",
            url_type=ClickType.EXTERNAL,
        )

    def test_urls_with_https(self):
        self.report.request_json('place=prime&text=iphone&referer=yandex.ru&rids=2&show-urls=external')
        self.click_log.expect(data_url=LikeUrl(url_quoted=True, url_params={'frommarket': 'yandex.ru'}))

    def test_frommarket_rude_cut_non_market_urls(self):
        long_referer = quote('https://apple.com/search?iphone=5S&dummy=' + 'z' * 3000)
        self.report.request_json('place=prime&text=iphone&rids=2&referer={}&show-urls=external'.format(long_referer))
        self.click_log.expect(
            click_url=ElementCount(2000),
            data_url=LikeUrl(
                url_quoted=True,
                url_params={'frommarket': Contains('https://apple.com/search?iphone=5S&dummy=' + 'z' * 1000)},
            ),
        )

    def test_frommarket_keeps_required_params(self):
        long_referer = quote(
            'https://market.yandex.ru?hid={hid}&nid={nid}&glfilter={glfilter}&text={text}&how={how}&dummy={dummy}'.format(
                hid='H' * 250, nid='N' * 300, glfilter='G' * 200, text='T' * 200, how='O' * 200, dummy='X' * 2000
            )
        )

        self.report.request_json('place=prime&text=iphone&rids=2&referer={}&show-urls=external'.format(long_referer))
        self.click_log.expect(
            data_url=LikeUrl(
                url_quoted=True,
                url_len=LessEq(2000),
                url_params={
                    'frommarket': LikeUrl(
                        url_params={
                            'hid': 'H' * 250,
                            'nid': 'N' * 300,
                            'glfilter': 'G' * 200,
                            'text': 'T' * 200,
                            'how': 'O' * 200,
                            'dummy': None,
                        }
                    )
                },
            )
        )

    def test_frommarket_keeps_shortest_params(self):
        long_referer = quote(
            'https://market.yandex.ru?hid={hid}&short={short}&medium={medium}&long={long}'.format(
                hid='H' * 900,
                short='S' * 10,
                medium='M' * 100,
                long='L' * 800,
            )
        )

        self.report.request_json('place=prime&text=iphone&rids=2&referer={}&show-urls=external'.format(long_referer))
        self.click_log.expect(
            data_url=LikeUrl(
                url_quoted=True,
                url_len=LessEq(2000),
                url_params={
                    'frommarket': LikeUrl(
                        url_params={'hid': 'H' * 900, 'short': 'S' * 10, 'medium': 'M' * 100, 'long': None}
                    )
                },
            )
        )

    def test_frommarket_rude_cut_bad_url(self):
        bad_referer = quote('{left}{middle}{right}'.format(left='L' * 500, middle='M' * 500, right='R' * 500))
        self.report.request_json('place=prime&text=iphone&rids=2&referer={}&show-urls=external'.format(bad_referer))
        self.click_log.expect(
            click_url=ElementCount(2000),
            data_url=LikeUrl(
                url_quoted=True,
                url_params={
                    'frommarket': Contains(
                        '{left}{middle}{right}'.format(left='L' * 500, middle='M' * 500, right='R' * 100)
                    )
                },
            ),
        )

    def test_frommarket_cut_because_of___baseline(self):
        long_referer = quote(
            'https://market.yandex.ru?hid={hid}&short={short}&medium={medium}&long={long}'.format(
                hid='H' * 600,
                short='S' * 10,
                medium='M' * 100,
                long='L' * 400,
            )
        )

        self.report.request_json(
            'place=prime&text=sony&fesh=4&rids=2&reqid=&referer={}&show-urls=external'.format(long_referer)
        )
        self.click_log.expect(
            data_url=LikeUrl(
                url_quoted=True,
                url_len=LessEq(2000),
                url_params={
                    'frommarket': LikeUrl(
                        url_params={'hid': 'H' * 600, 'short': 'S' * 10, 'medium': 'M' * 100, 'long': 'L' * 400}
                    )
                },
            )
        )

    def test_frommarket_cut_because_of_openstat(self):
        long_referer = quote(
            'https://market.yandex.ru?hid={hid}&short={short}&medium={medium}&long={long}'.format(
                hid='H' * 800,
                short='S' * 10,
                medium='M' * 100,
                long='L' * 400,
            )
        )

        self.report.request_json(
            'place=prime&text=sony&fesh=5&rids=2&referer={}&show-urls=external'.format(long_referer)
        )
        self.click_log.expect(
            data_url=LikeUrl(
                url_quoted=True,
                url_len=LessEq(2000),
                url_params={
                    'frommarket': LikeUrl(
                        url_params={'hid': 'H' * 800, 'short': 'S' * 10, 'medium': 'M' * 100, 'long': None}
                    )
                },
            )
        )

    def test_frommarket_cut_because_of_ymclid(self):
        long_referer = quote(
            'https://market.yandex.ru?hid={hid}&short={short}&medium={medium}&long={long}'.format(
                hid='H' * 800,
                short='S' * 10,
                medium='M' * 100,
                long='L' * 400,
            )
        )

        self.report.request_json(
            'place=prime&text=sony&fesh=6&rids=2&referer={}&show-urls=external'.format(long_referer)
        )
        self.click_log.expect(
            data_url=LikeUrl(
                url_quoted=True,
                url_len=LessEq(2000),
                url_params={
                    'frommarket': LikeUrl(
                        url_params={'hid': 'H' * 800, 'short': 'S' * 10, 'medium': 'M' * 100, 'long': None}
                    )
                },
            )
        )

    def test_cut_with_bad_url_symbols(self):
        """
        Проверяем, что большое количество символов, требующих url encoding в реферере не приводят к незапланирвоанному выкидыванию ymclid
        """
        self.error_log.ignore('')
        long_referer = quote(
            'https://market.yandex.ru?param={param}'.format(
                param='*' * 300,
            )
        )

        self.report.request_json(
            'place=prime&text=long&fesh=6&rids=2&referer={}&show-urls=external'.format(long_referer)
        )
        self.click_log.expect(
            data_url=LikeUrl(
                url_quoted=True,
                url_params={
                    'ymclid': NotEmpty(),
                },
            )
        )

    def test_rewrites_reserved_params(self):
        self.report.request_json('place=prime&text=cheeter&rids=2&referer=yandex.ru&show-urls=external')
        self.click_log.expect(
            data_url=LikeUrl(
                url_quoted=True,
                url_params={
                    '_openstat': 'bWFya2V0LnlhbmRleC5ydTtjaGVldGVyO3hNcENPS0M1STRJTnpGQ2FiM1dFbXc7',
                    'frommarket': 'yandex.ru',
                    'ymclid': "04884192001117778888800001",
                },
            )
        )

    def test_no_trailing_question_mark(self):
        self.report.request_json('place=prime&rids=2&text=MARKETOUT-7696&show-urls=external')
        self.click_log.expect(data_url='http%3A%2F%2Fstoktrade.ru%2Fgostinaya-legenda')

    def test_cpa_clickurl_pof(self):
        # classic behavior: single pof parameter
        self.report.request_json('place=prime&text=cpa-offer&pof=somepof&rids=2&show-urls=cpa,external')
        self.click_log.expect(clicktype=ClickType.EXTERNAL, pof='somepof', pof_debug=Absent())

        # intermediate behavior: cpa-pof and pof params. pof-cpa MUST win for CPA-offers
        self.report.request_json('place=prime&text=cpa-offer&pof=oldpof&cpa-pof=newpof&rids=2&show-urls=cpa,external')
        self.click_log.expect(clicktype=ClickType.EXTERNAL, pof='oldpof', pof_debug=Absent())

        # new behavior: cpa-pof only
        self.report.request_json('place=prime&text=cpa-offer&cpa-pof=cpapof&rids=2&show-urls=cpa,external')
        self.click_log.expect(clicktype=ClickType.EXTERNAL, pof=Absent(), pof_debug=Absent())

        self.click_log.expect(clicktype=ClickType.CPA).times(21)

    def test_offer_id(self):
        self.report.request_json('place=prime&text=samsung&rids=2&show-urls=external,showPhone')
        self.click_log.expect(ClickType.EXTERNAL, offer_id=100)
        self.click_log.expect(ClickType.SHOW_PHONE, offer_id=100)

    def test_offer_id_with_slashes(self):
        self.report.request_json('place=prime&text=potato&rids=2&show-urls=external,showPhone')
        self.click_log.expect(ClickType.EXTERNAL, offer_id="offer%2Fid%2Fwith%2Fslashes")
        self.click_log.expect(ClickType.SHOW_PHONE, offer_id="offer%2Fid%2Fwith%2Fslashes")

    def test_feed_id(self):
        self.report.request_json('place=prime&text=samsung&rids=2&show-urls=external,showPhone')
        self.click_log.expect(ClickType.EXTERNAL, feed_id=200)
        self.click_log.expect(ClickType.SHOW_PHONE, feed_id=200)

    def test_bid_type(self):
        self.report.request_json('place=prime&text=samsung&show-urls=external&how=aprice&rids=2')
        self.click_log.expect(ClickType.EXTERNAL, bid_type="minbid")

        self.report.request_json('place=productoffers&hyperid=1000&rids=213&show-urls=external&rids=2')
        self.click_log.expect(ClickType.EXTERNAL, bid_type="cbid")

    def test_subreqid(self):
        self.report.request_json('place=prime&text=nokia&subreqid=11111&rids=2&show-urls=external')
        self.click_log.expect(ClickType.EXTERNAL, sub_request_id=11111)

    def test_client_android(self):
        self.report.request_json('place=prime&text=nokia&client=ANDROID&rids=2&show-urls=external')
        self.click_log.expect(ClickType.EXTERNAL, client="ANDROID")
        self.show_log.expect(client="ANDROID")

    def test_client_lavka(self):
        self.report.request_json(
            'place=prime&text=samsung&client=lavka&rids=213&show-urls=external&regset=1&&entities=offer&allow-collapsing=0&market-force-business-id=1&ignore-has-gone=1&complete-query=lavka'
        )
        self.click_log.expect(ClickType.EXTERNAL, client="lavka")
        self.show_log.expect(client="lavka")

    def test_client_eats(self):
        self.report.request_json(
            'place=prime&text=potato&client=eats&rids=213&show-urls=external&regset=1&entities=offer&allow-collapsing=0&market-force-business-id=1&ignore-has-gone=1'
        )
        self.click_log.expect(ClickType.EXTERNAL, client="eats")
        self.show_log.expect(client="eats")

    @classmethod
    def prepare_test_not_modify_original_params(cls):
        """
        Создаем два магазина:
         - в ссылки на первый надо добавить один зарезервировнный параметр (ymclid)
         - в ссылки на второй надо добавить два зарезервированных параметра (ymclid + frommarket)
        """
        cls.index.shops += [
            Shop(fesh=100, yclid=True, priority_region=2, regions=[213]),
            Shop(fesh=101, yclid=True, from_market=True, priority_region=2, regions=[213]),
        ]

        cls.index.offers += [
            # Офферы для тестирования бага MARKETOUT-10585. Параметр (param1) в урле без знака '='
            Offer(
                title='param_without_assign_not_cheater',
                fesh=100,
                url="http://notcheater.com/index.html?param1&param2=value",
                waremd5='1KDIXe2MjWqGXFLInwQzZg',
            ),
            Offer(
                title='param_without_assign_cheater',
                fesh=100,
                url="http://cheater.com/index.html?param1&ymclid=value",
                waremd5='RZDfQx7aGC31VLFpEwKjrQ',
            ),
            # Офферы для тестирования бага MARKETOUT-10865. В урле кодируются пробелы кодом '%20'
            Offer(
                title='param_with_encoded_space_not_cheater',
                fesh=100,
                url="http://notcheater.com/index.html?param1=%20value",
                waremd5='7nHcAfIkOkZ8UVghrvacdQ',
            ),
            Offer(
                title='param_with_encoded_space_cheater',
                fesh=100,
                url="http://cheater.com/index.html?param1=%20value&ymclid=value",
                waremd5='NddnYR1RMa4ccY586VHgFA',
            ),
            # Офферы для тестирования случая, когда в урл надо вставить более одного параметра
            Offer(
                title='multiple_reserved_params_not_cheater',
                fesh=101,
                url="http://notcheater.com/index.html?param1",
                waremd5='2BjW6Mf9CmtvPMH18k7qBA',
            ),
            Offer(
                title='multiple_reserved_params_cheater',
                fesh=101,
                url="http://cheater.com/index.html?param1&frommarket=value",
                waremd5='P8ZRinGdUMUvvk2qbc1xeQ',
            ),
        ]

    def test_not_modify_original_params(self):
        # Если в урле нет зарезервированного параметра, мы его дописываем в конец урла, сам урл магазина остается прежним
        # (у param1 не появляется '=') MARKETOUT-10585
        self.report.request_json('place=prime&offerid=1KDIXe2MjWqGXFLInwQzZg&rids=2&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote(
                'http://notcheater.com/index.html?param1&param2=value&ymclid=04884192001117778888800001', safe=''
            )
        )

        # Если в урле есть зарезервированный параметр, мы его подменяем, при этом урл магазина может измениться
        # (у param1 появляется '=') MARKETOUT-10585
        self.report.request_json('place=prime&offerid=RZDfQx7aGC31VLFpEwKjrQ&rids=2&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote('http://cheater.com/index.html?param1=&ymclid=04884192001117778888800001', safe='')
        )

        # Если в урле нет зарезервированного параметра, мы его дописываем в конец урла, сам урл магазина остается прежним
        # (закодированный пробел (%20) остается как %20) MARKETOUT-10865
        self.report.request_json('place=prime&offerid=7nHcAfIkOkZ8UVghrvacdQ&rids=2&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote(
                'http://notcheater.com/index.html?param1=%20value&ymclid=04884192001117778888800001', safe=''
            )
        )

        # Если в урле есть зарезервированный параметр, мы его подменяем, при этом урл магазина может измениться
        # (закодированный пробел (%20) превращается в +) MARKETOUT-10865
        self.report.request_json('place=prime&offerid=NddnYR1RMa4ccY586VHgFA&rids=2&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote(
                'http://cheater.com/index.html?param1=+value&ymclid=04884192001117778888800001', safe=''
            )
        )

        # Если в урле нет зарезервированных параметров, мы его дописываем в конец урла, сам урл магазина остается прежним
        # (у param1 не появляется '=') MARKETOUT-10585
        self.report.request_json(
            'place=prime&offerid=2BjW6Mf9CmtvPMH18k7qBA&referer=yandex.ru&rids=2&show-urls=external'
        )
        self.click_log.expect(
            data_url=urllib.quote(
                'http://notcheater.com/index.html?param1&frommarket=yandex.ru&ymclid=04884192001117778888800001',
                safe='',
            )
        )

        # Если в урле есть зарезервированный параметр, мы его подменяем и дописываем еще один
        # при этом урл магазина может измениться
        # (у param1 появляется '=') MARKETOUT-10585
        self.report.request_json(
            'place=prime&offerid=P8ZRinGdUMUvvk2qbc1xeQ&referer=yandex.ru&rids=2&show-urls=external'
        )
        self.click_log.expect(
            data_url=urllib.quote(
                'http://cheater.com/index.html?param1=&frommarket=yandex.ru&ymclid=04884192001117778888800001', safe=''
            )
        )

    @classmethod
    def prepare_test_empty_querystring(cls):
        cls.index.offers += [
            Offer(
                title='no_question_mark_and_slash',
                fesh=100,
                url="http://url1.com/somepage",
                waremd5='4hmaothQdATNP1PMPtoyHQ',
            ),
            Offer(
                title='no_question_mark_wish_slash',
                fesh=100,
                url="http://url2.com/somepage/",
                waremd5='j17xH2RV5uU0VvNs9VvJRA',
            ),
            Offer(
                title='question_mark_only', fesh=100, url="http://url3.com/somepage?", waremd5='TPlqCTKJq0ABaKiJSc0NSA'
            ),
            Offer(
                title='question_mark_with_empty_param',
                fesh=100,
                url="http://url4.com/somepage?param",
                waremd5='Rt3jfDHHxFX7wsW7ifeokA',
            ),
            Offer(title='domain_only_without_slash', fesh=100, url="http://url5.com", waremd5='rLDrbHFvmeous3ga28kOAg'),
            Offer(title='domain_only_with_slash', fesh=100, url="http://url6.com/", waremd5='JBzQQHtYGiY9X1ufXKA4Xw'),
        ]

    def test_empty_querystring(self):
        """Проверяем, что ymclid дописывается корректно, если в урле пустой querystring"""
        self.report.request_json('place=prime&offerid=4hmaothQdATNP1PMPtoyHQ&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote('http://url1.com/somepage?ymclid=04884192001117778888800001', safe='')
        )

        self.report.request_json('place=prime&offerid=j17xH2RV5uU0VvNs9VvJRA&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote('http://url2.com/somepage/?ymclid=04884192001117778888800001', safe='')
        )

        self.report.request_json('place=prime&offerid=TPlqCTKJq0ABaKiJSc0NSA&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote('http://url3.com/somepage?ymclid=04884192001117778888800001', safe='')
        )

        self.report.request_json('place=prime&offerid=Rt3jfDHHxFX7wsW7ifeokA&show-urls=external')
        self.click_log.expect(
            data_url=urllib.quote('http://url4.com/somepage?param&ymclid=04884192001117778888800001', safe='')
        )

        self.report.request_json('place=prime&offerid=rLDrbHFvmeous3ga28kOAg&show-urls=external')
        self.click_log.expect(data_url=urllib.quote('http://url5.com/?ymclid=04884192001117778888800001', safe=''))

        self.report.request_json('place=prime&offerid=JBzQQHtYGiY9X1ufXKA4Xw&show-urls=external')
        self.click_log.expect(data_url=urllib.quote('http://url6.com/?ymclid=04884192001117778888800001', safe=''))

    @classmethod
    def prepare_clicklog_in_wizard(cls):
        """
        Создаем 5 офферов которые попадут в топ6 модели и 2 оффера, которые не попадут
        """
        cls.index.cpa_categories += [
            CpaCategory(hid=200, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]
        cls.index.navtree += [NavCategory(nid=300, hid=200)]
        cls.index.models += [
            Model(hyperid=301090, hid=200, title="lenovo p780"),
        ]
        cls.index.offers += [
            Offer(
                title='lenovo p780 cpa offer 1',
                hid=200,
                hyperid=301090,
                fesh=1,
                cpa=Offer.CPA_REAL,
                price=8000,
                waremd5='H__rIZIhXqNM4Kq2NlBTSg',
                randx=90,
            ),
            Offer(
                title='lenovo p780 cpa offer 2',
                hid=200,
                hyperid=301090,
                fesh=2,
                cpa=Offer.CPA_REAL,
                price=8000,
                waremd5='RxxQrRoVbXJW7d_XR9d5MQ',
                randx=80,
            ),
            Offer(
                title='lenovo p780 cpa offer 3',
                hid=200,
                hyperid=301090,
                fesh=3,
                cpa=Offer.CPA_REAL,
                price=8000,
                waremd5='1ZSxqUW11kXlniH4i9LYOw',
                randx=70,
            ),
            Offer(
                title='lenovo p780 cpa offer 4',
                hid=200,
                hyperid=301090,
                fesh=4,
                cpa=Offer.CPA_REAL,
                price=8000,
                waremd5='SlM1kXY9-nQ6E6_6sahXkw',
                randx=60,
            ),
            Offer(
                title='lenovo p780 cpa offer 5',
                hid=200,
                hyperid=301090,
                fesh=5,
                cpa=Offer.CPA_REAL,
                price=8000,
                waremd5='ZD3nz3unacdGbMvErf1_rA',
                randx=50,
            ),
            Offer(
                title='lenovo p780 cpc offer 1',
                hid=200,
                hyperid=301090,
                fesh=6,
                price=8000,
                waremd5='5RNatoyv6c_AyxpGFgp1og',
                randx=40,
            ),
            Offer(
                title='lenovo p780 cpc offer 2',
                hid=200,
                hyperid=301090,
                fesh=7,
                price=8000,
                waremd5='tsW-9atNBM2fCzqVZD3Efw',
                randx=30,
            ),
        ]

    def test_clicklog_in_wizard(self):
        """
        Проверка попадания записей в click.log при клике по урлам из колдунщика
        Поля, которые проверяются в кликлоге: cpa, dtype, hyper_cat_id, hyper_id, nav_cat_id, onstock, position, price,
        shop_id, show_block_id, type_id, geo_id, pof, pp, reqid, url_type, uuid, waremd5, wprid, yandexuid, sub_request_id
        """
        # TODO Уберу или раскомментирую соотвествующие строчки после выяснения как должно быть
        self.report.request_bs(
            'place=parallel&text=lenovo p780&rids=2&pof=pof&reqid=55555&uuid=789&wprid=101112&yandexuid=131415&subreqid=161718'
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='market',
            hyper_cat_id=200,
            hyper_id=301090,
            nav_cat_id=300,
            onstock=1,
            position=1,
            price=8000,
            shop_id=1,
            show_block_id="048841920011177788888",
            type_id=0,
            geo_id=2,
            # reqid=55555,
            # pof='pof',
            pp=404,
            url_type=0,
            # uuid=789,
            ware_md5='H__rIZIhXqNM4Kq2NlBTSg',
            wprid=101112,
            yandexuid=131415,
            # sub_request_id=161718,
        )

        # Проверка для тача, pp=402
        self.report.request_bs(
            'place=parallel&text=lenovo p780&rids=2&pof=pof&reqid=55555&uuid=789&wprid=101112&yandexuid=131415&subreqid=161718'
            '&touch=1'
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='market',
            hyper_cat_id=200,
            hyper_id=301090,
            nav_cat_id=300,
            onstock=1,
            position=1,
            price=8000,
            shop_id=1,
            show_block_id="048841920011177788888",
            type_id=0,
            geo_id=2,
            # reqid=55555,
            # pof='pof',
            pp=402,
            url_type=0,
            # uuid=789,
            ware_md5='H__rIZIhXqNM4Kq2NlBTSg',
            wprid=101112,
            yandexuid=131415,
            # sub_request_id=161718,
        )

    @classmethod
    def prepare_test_model_clicks(cls):
        '''
        Создаем модели со ставкой и без, модель со ставкой привязываем к вендору
        '''
        cls.index.navtree += [NavCategory(hid=101, nid=201)]
        cls.index.vendors += [Vendor(vendor_id=103)]

        cls.index.models += [
            Model(
                title="model_with_vendor_bid",
                hyperid=100,
                hid=101,
                vbid=102,
                vendor_id=103,
                datasource_id=104,
                ts=11111,
            ),
            Model(title="model_without_vendor_bid", ts=22222),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11111).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 22222).respond(0.2)

    def test_model_with_vendor_bid(self):
        # Все необходимые параметры записываются в клик-лог, при передаче параметра show-urls=productVendorBid
        response = self.report.request_json(
            'place=prime&text=model_with_vendor_bid&rearr-factors=market_force_use_vendor_bid=1&yandexuid=131415&'
            'show-urls=productVendorBid&pof=pof&rids=2&ip-rids=191&rearr-factors=disable_panther_quorum=0;market_money_return_vendor_urls_even_for_zero_v_bid=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {'raw': 'model_with_vendor_bid'},
                            'urls': {
                                'encrypted': Contains(
                                    '/redir/dtype=modelcard', '/hyper_id=100/', '/vendor_price=1/vc_bid=102/'
                                )
                            },
                        },
                        # Модель, у которой нет вендорных ставок, не показывает в выдаче ссылки для клика.
                        {'titles': {'raw': 'model_without_vendor_bid'}, 'urls': {'encrypted': Absent()}},
                    ]
                }
            },
            preserve_order=False,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            uid="04884192001117778888816002",
            yandexuid=131415,
            pp=18,
            pof='pof',
            url=urllib.quote('//market.yandex.ru/product/100?hid=101&nid=201', safe=''),
            hyper_id=100,
            hyper_cat_id=101,
            brand_id=103,
            vendor_ds_id=104,
            vendor_price=1,  # Автоброкер
            vc_bid=102,
            geo_id=2,
            ip_geo_id=191,
            position=2,
        )

    def test_model_without_vendor_bid(self):
        """
        Проверяем, что всё корректно работает, когда на оффер есть нулевая вендорская ставка, и включен флаг market_money_return_vendor_urls_even_for_zero_v_bid
        """
        response = self.report.request_json(
            'place=prime&text=model_with_vendor_bid&rearr-factors=market_force_use_vendor_bid=1&yandexuid=131415&'
            'show-urls=productVendorBid&pof=pof&rids=2&ip-rids=191&rearr-factors=disable_panther_quorum=0;market_money_return_vendor_urls_even_for_zero_v_bid=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {'raw': 'model_with_vendor_bid'},
                            'urls': {
                                'encrypted': Contains(
                                    '/redir/dtype=modelcard', '/hyper_id=100/', '/vendor_price=1/vc_bid=102/'
                                )
                            },
                        },
                        # Модель, у которой нет вендорных ставок, но ссылки для клика всё равно создаются
                        {
                            'titles': {'raw': 'model_without_vendor_bid'},
                            'urls': {
                                'encrypted': Contains(
                                    "/redir/dtype=modelcard",
                                    "vendor_ds_id=0",
                                    "vendor_price=0",
                                    "vc_bid=0",
                                    "url_type=16",
                                ),
                            },
                        },
                    ]
                }
            },
            preserve_order=False,
        )
        # В клик лог запишется клик с нулевой ставкой
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            yandexuid=131415,
            pp=18,
            pof='pof',
            vendor_ds_id=0,
            vendor_price=0,
            vc_bid=0,
            position=1,
        ).times(1)

    def test_model_with_vendor_bid_with_cpc(self):
        # Все необходимые параметры записываются в клик-лог, но данные по ставке, зашифрованные в cpc, имееют бОльший приоритет
        # над ставками в индексе

        cpc = str(Cpc.create_for_model(model_id=100, vendor_bid=1000, vendor_click_price=999))
        self.report.request_json(
            'place=prime&text=model_with_vendor_bid&rearr-factors=market_force_use_vendor_bid=1&yandexuid=131415'
            '&show-urls=productVendorBid&pof=pof&rids=2&ip-rids=191&rearr-factors=disable_panther_quorum=0'
            '&cpc={}'.format(cpc)
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            uid="04884192001117778888816002",
            yandexuid=131415,
            pp=18,
            pof='pof',
            url=urllib.quote('//market.yandex.ru/product/100?hid=101&nid=201', safe=''),
            hyper_id=100,
            hyper_cat_id=101,
            brand_id=103,
            vendor_ds_id=104,
            vendor_price=999,
            vc_bid=1000,
            geo_id=2,
            ip_geo_id=191,
            position=2,
        )

    def __test_fast_order_url_exists(self, request, pof, feeShow):
        response = self.report.request_json(
            request + "&offerid=09lEaAKkQll1XTjm0WPoIA&show-urls=fastOrder&pof={}&rids=2".format(json.dumps(pof))
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'wareId': '09lEaAKkQll1XTjm0WPoIA',
                        'cpa': 'real',
                        'feeShow': '{}'.format(feeShow),
                        'urls': {'fastOrder': NotEmpty()},
                    }
                ]
            },
        )

        url_params = {'fee': feeShow, 'offerid': '09lEaAKkQll1XTjm0WPoIA', 'lr': 2}

        for key, value in pof.items():
            url_params[key] = value

        self.click_log.expect(
            clicktype=ClickType.FAST_ORDER,
            dtype='cpa',
            pp=18,
            pof=json.dumps(pof),
            data_url=LikeUrl(
                url_host='market.yandex.ru', url_path='/checkout/fast', url_params=url_params, url_quoted=True
            ),
            hyper_id=333,
            msku='123456',
        )

    def test_fast_order_url(self):
        """
        Что тестируем: формат выдачи, зашифрованной для кликдемона, ссылки ведущей от партнерского api к быстрому заказу checkout/fast. MARKETOUT-12764
        Ссылки формируются только для оферов с CPA. Для получения ссылки требуется запросить show-urls=fastOrder
        """

        feeShow = Cpa.create_for_offer(
            fee="0.0000",
            fee_sum="0",
            show_block_id="048841920011177788888",
            ware_md5="09lEaAKkQll1XTjm0WPoIA",
            pp=18,
            show_uid="04884192001117778888806001",
        )

        pof = {'clid': ['clid_AAA'], 'mclid': 'mclid_BBB', 'distr_type': '2'}
        self.__test_fast_order_url_exists('place=prime&text=cpa-offer&rids=2', pof, feeShow)

        feeShow = Cpa.create_for_offer(
            fee="0.0000",
            fee_sum="0",
            show_block_id="048841920011177788888",
            ware_md5="09lEaAKkQll1XTjm0WPoIA",
            pp=18,
            show_uid="04884192001117778888806001",
        )
        self.__test_fast_order_url_exists('place=productoffers&hyperid=333&rids=2', pof, feeShow)

        feeShow = Cpa.create_for_offer(
            fee="0.0000",
            fee_sum="0",
            show_block_id="048841920011177788888",
            ware_md5="09lEaAKkQll1XTjm0WPoIA",
            pp=18,
            show_uid="04884192001117778888806000",
        )
        self.__test_fast_order_url_exists('place=offerinfo&rids=2&regset=2', pof, feeShow)

    def test_fast_order_url_double_clid(self):
        """Проверяем наличие в ссылке двух clid"""

        feeShow = Cpa.create_for_offer(
            fee="0.0000",
            fee_sum="0",
            show_block_id="048841920011177788888",
            ware_md5="09lEaAKkQll1XTjm0WPoIA",
            pp=18,
            show_uid="04884192001117778888806000",
        )

        pof = {'clid': ['clid_AAA', 'clid_CCC'], 'mclid': 'mclid_BBB', 'distr_type': '2'}
        self.__test_fast_order_url_exists('place=offerinfo&rids=2&regset=2', pof, feeShow)

    def test_fast_order_url_without_clid(self):
        """Проверяем отсутствие в ссылке clid"""

        feeShow = Cpa.create_for_offer(
            fee="0.0000",
            fee_sum="0",
            show_block_id="048841920011177788888",
            ware_md5="09lEaAKkQll1XTjm0WPoIA",
            pp=18,
            show_uid="04884192001117778888806000",
        )

        pof = {'mclid': 'mclid_BBB', 'distr_type': '2'}
        self.__test_fast_order_url_exists('place=offerinfo&rids=2&regset=2', pof, feeShow)

    def test_fast_order_url_without_mclid(self):
        """Проверяем отсутствие в ссылке mclid"""

        feeShow = Cpa.create_for_offer(
            fee="0.0000",
            fee_sum="0",
            show_block_id="048841920011177788888",
            ware_md5="09lEaAKkQll1XTjm0WPoIA",
            pp=18,
            show_uid="04884192001117778888806000",
        )

        pof = {'clid': ['clid_AAA', 'clid_CCC'], 'distr_type': '2'}
        self.__test_fast_order_url_exists('place=offerinfo&rids=2&regset=2', pof, feeShow)

    def test_fast_order_url_without_distr_type(self):
        """Проверяем отсутствие в ссылке distr_type"""

        feeShow = Cpa.create_for_offer(
            fee="0.0000",
            fee_sum="0",
            show_block_id="048841920011177788888",
            ware_md5="09lEaAKkQll1XTjm0WPoIA",
            pp=18,
            show_uid="04884192001117778888806000",
        )

        pof = {
            'clid': ['clid_AAA', 'clid_CCC'],
            'mclid': 'mclid_BBB',
        }
        self.__test_fast_order_url_exists('place=offerinfo&rids=2&regset=2', pof, feeShow)

    def test_fast_order_url_missed_without_cpa(self):
        """Проверяем отсутствие ссылки у офера без CPA"""
        response = self.report.request_json("place=prime&text=iphone&show-urls=fastOrder")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'iphone'},
                        'cpa': NoKey('cpa'),
                        'urls': {'fastOrder': NoKey('fastOrder')},
                    }
                ]
            },
        )

    @classmethod
    def prepare_use_direct_model_url(cls):
        cls.index.models += [
            Model(hid=111222, vbid=4321, datasource_id=54321),
        ]

    def test_use_direct_model_url(self):
        """Проверяем, что ссылка на модель является safeclick"""
        _ = self.report.request_json(
            "place=prime&hid=111222&rearr-factors=market_safeclick_model_url=1&show-urls=productVendorBid"
        )
        self.click_log.expect(clicktype=ClickType.EXTERNAL, click_url=Wildcard('/safeclick/*'))

    def test_use_direct_phone_url(self):
        """Проверяем, что phone-ссылка является safeclick"""
        _ = self.report.request_json(
            "place=prime&pp=18&text=samsung&rearr-factors=market_safeclick_phone_url=1&show-urls=showPhone,callPhone"
        )
        self.click_log.expect(clicktype=ClickType.SHOW_PHONE, click_url=Wildcard('/safeclick/*'))
        self.click_log.expect(clicktype=ClickType.PHONE, click_url=Wildcard('/safeclick/*'))

    def test_x_yandex_icookie(self):
        """
        Проверяем, что зашифрованное значение icookie, попадает в клик-лог в расшифрованном виде
        BCEmkyAbCsICEPzTQsKKZiwaEphOTfJYcplJsb6WeCNz9ThbLjUw5pw1K8G40cyJPs%2BVrWxAzPzzs34zCBQWvGkphV4%3D => 6774478491508471626
        """
        self.report.request_json('place=prime&text=iphone&show-urls=external&x-yandex-icookie=6774478491508471626')
        self.click_log.expect(ClickType.EXTERNAL, icookie='6774478491508471626')

    def test_x_yandex_icookie_absent(self):
        """
        Проверяем, что если icookie отсутствует в запросе, то в клик-лог оно не попадает
        """
        self.report.request_json('place=prime&text=iphone&show-urls=external')
        self.click_log.expect(ClickType.EXTERNAL, icookie=None)

    def test_puid(self):
        self.report.request_json('place=productoffers&hyperid=1000&yandexuid=1')
        self.click_log.expect(puid=Absent(), yandexuid='1')

        self.report.request_json('place=productoffers&hyperid=1000&yandexuid=2&puid=100500')
        self.click_log.expect(puid='100500', yandexuid='2')

    @classmethod
    def prepare_pp_billing(cls):
        cls.index.offers += [
            Offer(hyperid=100009, price=198, bid=12),
        ]

        cls.index.market_pp_data += [MarketPp(pp_id=7, is_free=False), MarketPp(pp_id=123, is_free=True)]

    def test_free_pp_billing(self):
        """
        Проверяем, что PP указаные в конфиге как бесплатные не билятся
        """
        self.report.request_json('place=productoffers&hyperid=100009&pp=123')
        self.click_log.expect(ClickType.EXTERNAL, hyper_id=100009, min_bid=1, cb=1, cp=0, cpbbc=0, price=198)

    def test_non_free_pp_billing(self):
        """
        Проверяем, что PP указаные в конфиге как платные билятся
        """
        self.report.request_json('place=productoffers&hyperid=100009&pp=7')
        self.click_log.expect(ClickType.EXTERNAL, hyper_id=100009, min_bid=1, cb=1, cp=1, cpbbc=1, price=198)

    def test_pp_not_in_config_billing(self):
        """
        Проверяем, что PP не указаные в конфиге билятся
        """
        self.report.request_json('place=productoffers&hyperid=100009&pp=17')
        self.click_log.expect(ClickType.EXTERNAL, hyper_id=100009, min_bid=1, cb=1, cp=1, cpbbc=1, price=198)

    def test_no_direct_urls_by_default(self):
        """
        Проверяем, что прямая ссылка не прилетает по умолчанию
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=100009&pp=17&rearr-factors=market_no_direct_links_by_default=1'
        )
        self.assertFragmentNotIn(response, {'urls': {'direct': NotEmpty()}})

    def test_no_direct_urls_by_default_direct_ulr_requested(self):
        """
        Проверяем, что прямая ссылка прилетает, если её запросили
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=100009&pp=17&show-urls=direct&rearr-factors=market_no_direct_links_by_default=1'
        )
        self.assertFragmentIn(response, {"results": [{'entity': 'offer', 'urls': {'direct': NotEmpty()}}]})

    @classmethod
    def prepare_is_recommended_by_vendor(cls):
        cls.index.offers += [
            Offer(hyperid=100010, price=199, bid=12, is_recommended=True),
            Offer(hyperid=100011, price=299, bid=12),
        ]

    def test_is_recommended_by_vendor(self):
        """
        Проверяем, что правильно пишетв кликс лог поле is_recommended_by_vendor
        """
        self.report.request_json('place=productoffers&hyperid=100010')
        self.click_log.expect(is_recom_vnd=1, price=199)

        self.report.request_json('place=productoffers&hyperid=100011')
        self.click_log.expect(is_recom_vnd=0, price=299)

    def test_linefeed_in_log(self):
        """
        Проверяем, что \n не пролезает в логи
        """

        def check(linefeed):
            response = self.report.request_json(
                'place=productoffers&hyperid=100010&test_tag=cool{}hacker'.format(linefeed)
            )
            self.assertFragmentNotIn(response, {'urls': {'encrypted': Contains('\n')}})
            self.error_log.expect("Illegal clickdaemon symbol")

        check("\n")
        check("%0A")
        check("%0a")

    @classmethod
    def prepare_test_previous_pp(cls):
        cls.index.navtree += [NavCategory(hid=1017, nid=2017)]
        cls.index.vendors += [Vendor(vendor_id=1037)]

        cls.index.models += [
            Model(
                title="amn_ral_mal_ist_ohm",
                hyperid=1007,
                hid=1017,
                vbid=1027,
                vendor_id=1037,
                datasource_id=1047,
                ts=11117,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='tal_thul_ort_amn',
                hyperid=1000119,
                price=2999,
                bid=129,
                fesh=100,
                url="https://some-shop.aq/catalog",
                waremd5='xMpCOKC5I43434Cab3WEmw',
            ),
            Offer(hyperid=1000117, price=2997, bid=127),
            Offer(title="ral_tir_tal_sol", hyperid=1000118, price=2998, bid=128),
        ]

    def test_previous_pp(self):
        cpc = str(Cpc.create_for_model(model_id=31, pp=246))
        response = self.report.request_json(
            'place=prime&text=amn_ral_mal_ist_ohm&rearr-factors=market_force_use_vendor_bid=1'
            '&yandexuid=131415&show-urls=productVendorBid&pof=pof&rids=2&ip-rids=191&rearr-factors'
            '=disable_panther_quorum=0;market_report_click_context_enabled=0&pp=247&cpc=' + cpc
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {'raw': 'amn_ral_mal_ist_ohm'},
                            'urls': {
                                'encrypted': Contains(
                                    '/redir/dtype=modelcard', '/hyper_id=1007/', '/vendor_price=1/vc_bid=1027/'
                                )
                            },
                        }
                    ]
                }
            },
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            pp=247,
            prev_pp=246,
            pof='pof',
            url=urllib.quote('//market.yandex.ru/product/1007?hid=1017&nid=2017', safe=''),
            hyper_id=1007,
            hyper_cat_id=1017,
            brand_id=1037,
            vendor_ds_id=1047,
        )
        cpc = str(Cpc.create_for_offer(click_price=71, offer_id='xMpCOKC5I43434Cab3WEmw', bid=80, shop_id=71, pp=240))
        self.report.request_json(
            'place=prime&offerid=xMpCOKC5I43434Cab3WEmw&rids=2&show-urls=external&pp=241&rearr-factors=market_report_click_context_enabled=0&cpc='
            + cpc
        )
        self.click_log.expect(price=2999, hyper_id=1000119, dtype='market', pp=241, prev_pp=240)

        cpc = str(Cpc.create_for_offer(click_price=71, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=80, shop_id=71, pp=240))
        self.report.request_json(
            'place=prime&text=ral_tir_tal_sol&show-urls=external&pp=241&rearr-factors=market_report_click_context_enabled=0&cpc='
            + cpc
        )
        self.click_log.expect(price=2998, hyper_id=1000118, dtype='market', pp=241, prev_pp=240)

        cpc = str(Cpc.create_for_offer(click_price=71, offer_id='RcSMzi4tf73qGvxRx8atJg', bid=80, shop_id=71, pp=248))
        self.report.request_json(
            'place=productoffers&hyperid=1000117&pp=249&rearr-factors=market_report_click_context_enabled=0&cpc=' + cpc
        )
        self.click_log.expect(price=2997, hyper_id=1000117, dtype='market', pp=249, prev_pp=248)

    def test_previous_pp_from_cc(self):
        cc = str(ClickContext(pp=246))
        response = self.report.request_json(
            'place=prime&text=amn_ral_mal_ist_ohm&rearr-factors=market_force_use_vendor_bid=1'
            '&yandexuid=131415&show-urls=productVendorBid&pof=pof&rids=2&ip-rids=191&rearr-factors'
            '=disable_panther_quorum=0;market_report_click_context_enabled=1&pp=247&cc=' + cc
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'titles': {'raw': 'amn_ral_mal_ist_ohm'},
                            'urls': {
                                'encrypted': Contains(
                                    '/redir/dtype=modelcard', '/hyper_id=1007/', '/vendor_price=1/vc_bid=1027/'
                                )
                            },
                        }
                    ]
                }
            },
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            pp=247,
            prev_pp=246,
            pof='pof',
            url=urllib.quote('//market.yandex.ru/product/1007?hid=1017&nid=2017', safe=''),
            hyper_id=1007,
            hyper_cat_id=1017,
            brand_id=1037,
            vendor_ds_id=1047,
        )
        cc = str(ClickContext(pp=240))
        self.report.request_json(
            'place=prime&offerid=xMpCOKC5I43434Cab3WEmw&rids=2&show-urls=external&pp=241&rearr-factors=market_report_click_context_enabled=1&cc='
            + cc
        )
        self.click_log.expect(price=2999, hyper_id=1000119, dtype='market', pp=241, prev_pp=240)

        cc = str(ClickContext(pp=240))
        self.report.request_json(
            'place=prime&text=ral_tir_tal_sol&show-urls=external&pp=241&rearr-factors=market_report_click_context_enabled=1&cc='
            + cc
        )
        self.click_log.expect(price=2998, hyper_id=1000118, dtype='market', pp=241, prev_pp=240)

        cc = str(ClickContext(pp=248))
        self.report.request_json(
            'place=productoffers&hyperid=1000117&pp=249&rearr-factors=market_report_click_context_enabled=1&cc=' + cc
        )
        self.click_log.expect(price=2997, hyper_id=1000117, dtype='market', pp=249, prev_pp=248)

    @classmethod
    def prepare_ymclid_presense(cls):
        cls.index.models += [
            Model(hyperid=462, hid=461, title='Gnusmas Galaxy S10'),
        ]

        cls.index.shops += [
            Shop(
                fesh=464,
                datafeed_id=464,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                yclid=True,
            ),
            Shop(
                fesh=463,
                datafeed_id=463,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=462,
                main_fesh=462,
                business_fesh=462,
                yclid=True,
                datafeed_id=462,
                cpa=Shop.CPA_REAL,
                warehouse_id=147,
                blue=Shop.BLUE_NO,
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=False,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Gnusmas Galaxy S10 Black',
                hyperid=462,
                sku=3,
                feedid=462,
                fesh=462,
                blue_offers=[
                    BlueOffer(
                        price=11,
                        vat=Vat.VAT_10,
                        offerid='Shop1_sku3',
                        feedid=463,
                        waremd5='gwgfZ3JflzQelx9tFVgDqQ',
                        is_fulfillment=False,
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='Gnusmas Galaxy S10 Black',
                hyperid=462,
                sku=463,
                price=12,
                bid=129,
                fesh=462,
                url="https://another-shop.aq/catalog",
                vat=Vat.VAT_10,
                offerid='Shop2_sku3',
                feedid=3,
                waremd5='gwgfZ3xXxXxelx9tFVgDqQ',
            ),
        ]

    def test_ymclid_presense(self):
        response = self.report.request_json(
            'place=prime&text=Gnusmas&rids=0&pp=18&show-urls=encrypted&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "encrypted": LikeUrl(
                        url_quoted=True,
                        url_params={
                            'data': LikeUrl(url_quoted=True, url_params={'ymclid': NotEmpty()}),
                        },
                    ),
                    "direct": "https://another-shop.aq/catalog",
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "encrypted": LikeUrl(
                        url_quoted=True,
                        url_params={
                            'data': LikeUrl(
                                url_quoted=True,
                                url_params={'offerid': "gwgfZ3JflzQelx9tFVgDqQ"},
                            ),
                        },
                        no_params=['ymclid'],
                    ),
                    "direct": LikeUrl(url_host="pokupki.market.yandex.ru", url_path="/product/3"),
                }
            },
        )

    @classmethod
    def prepare_test_ip(cls):
        cls.index.offers += [
            Offer(hyperid=6217, cpa=Offer.CPA_REAL),
        ]

    def test_ip(self):
        response = self.report.request_json('place=productoffers&hyperid=6217&ip=77.88.8.8&show-urls=cpa,encrypted')
        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "encrypted": Contains('/ip=77.88.8.8'),
                    "cpa": Contains('/ip=77.88.8.8'),
                },
            },
        )
        self.click_log.expect(hyper_id=6217, dtype='market', ip='77.88.8.8')

    def test_search_params(self):
        response = self.report.request_json(
            'place=prime&utm_source_service=web&src_pof=703&x-yandex-src-icookie=7111647341623827291&baobab_event_id=kuiggrsq9q&hyperid=6217'
        )
        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "cpa": Contains('/src_pof=703'),
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "cpa": Contains('/utm_source_service=web'),
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "cpa": Contains('/src_icookie=7111647341623827291'),
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "urls": {
                    "cpa": Contains('/baobab_event_id=kuiggrsq9q'),
                },
            },
        )

    def test_antirobot_degradation(self):
        self.report.request_json(
            'place=productoffers&hyperid=6217&show-urls=cpa,encrypted', headers={'X-Yandex-Antirobot-Degradation': '1'}
        )
        self.click_log.expect(ClickType.EXTERNAL, ar_deg=1)
        self.click_log.expect(ClickType.CPA, ar_deg=1)


if __name__ == '__main__':
    main()
