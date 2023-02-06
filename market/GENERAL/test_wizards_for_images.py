#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, MnPlace, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import LikeUrl, Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, name='Киянки всем'),
            Shop(fesh=2, priority_region=213),
            Shop(fesh=3, priority_region=213),
            Shop(fesh=4, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title='kiyanka 1',
                fesh=1,
                ts=1,
                url='http://kiyanochnaya.ru/kiyanki?id=1',
                waremd5='f7_BYaO4c78hGceI7ZPR9A',
            ),
            Offer(title='kiyanka 2', fesh=2, ts=2),
            Offer(title='kiyanka 3', fesh=3, ts=3),
            Offer(title='kiyanka 4', fesh=4, ts=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.6)

    def test_offers_wizard_pp(self):
        """Проверяем, что под флагом market_offers_wizard_for_images
        в ссылках офферного колдунщика используется pp 42 и 642, добавляется параметр pof
        https://st.yandex-team.ru/MARKETOUT-31449
        https://st.yandex-team.ru/MARKETOUT-33947
        https://st.yandex-team.ru/MARKETOUT-34039
        """
        request = 'place=parallel&text=kiyanka&rearr-factors=market_offers_wizard_for_images=1;market_offers_wizard_incut_url_type=External'

        show_block_id = "048841920011177788888"
        base_show_uid = "{}{:02}{:03}".format(show_block_id, ClickType.EXTERNAL, 1)
        pof = '/pof={\"clid\":[\"2322165\"],\"distr_type\":\"1\"}/'

        # Для десктопа
        response = self.report.request_bs_pb(request + '&reqid=1&rearr-factors=device=desktop')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {"text": "Киянки всем", "domain": "kiyanochnaya.ru"},
                                "title": {
                                    "text": {"__hl": {"text": "kiyanka 1", "raw": True}},
                                    "url": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "urlTouch": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard/", "/pp=42/", pof
                                    ),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/pp=42/", pof
                                    ),
                                },
                                "thumb": {
                                    "url": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "urlTouch": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard/", "/pp=42/", pof
                                    ),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/pp=42/", pof
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

        self.show_log.expect(
            reqid=1,
            url=LikeUrl.of('http://kiyanochnaya.ru/kiyanki?id=1', ignore_len=False),
            show_block_id=show_block_id,
            show_uid=base_show_uid,
            ware_md5='f7_BYaO4c78hGceI7ZPR9A',
            pp=42,
            pof='{"clid":["2322165"],"distr_type":"1"}',
        )

        # click.log не пишется для запросов request_bs_pb
        response = self.report.request_bs(request + '&reqid=1&rearr-factors=device=desktop')
        self.click_log.expect(
            reqid=1,
            clicktype=ClickType.EXTERNAL,
            dtype='market',
            data_url=LikeUrl.of('http://kiyanochnaya.ru/kiyanki?id=1', unquote=True, ignore_len=False),
            ware_md5='f7_BYaO4c78hGceI7ZPR9A',
            pp=42,
            pof='{\\"clid\\":[\\"2322165\\"],\\"distr_type\\":\\"1\\"}',
        )

        # Для тача
        response = self.report.request_bs_pb(request + '&reqid=2&touch=1&rearr-factors=device=touch;offers_touch=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "greenUrl": {"text": "Киянки всем", "domain": "kiyanochnaya.ru"},
                                "title": {
                                    "text": {"__hl": {"text": "kiyanka 1", "raw": True}},
                                    "url": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "urlTouch": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard/", "/pp=642/", pof
                                    ),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/pp=642/", pof
                                    ),
                                },
                                "thumb": {
                                    "url": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "urlTouch": LikeUrl.of("http://kiyanochnaya.ru/kiyanki?id=1", ignore_len=False),
                                    "offercardUrl": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=offercard/", "/pp=642/", pof
                                    ),
                                    "urlForCounter": Contains(
                                        "//market-click2.yandex.ru/redir/dtype=market/", "/pp=642/", pof
                                    ),
                                },
                            }
                        ]
                    }
                }
            },
        )

        self.show_log.expect(
            reqid=2,
            url=LikeUrl.of('http://kiyanochnaya.ru/kiyanki?id=1', ignore_len=False),
            show_block_id=show_block_id,
            show_uid=base_show_uid,
            ware_md5='f7_BYaO4c78hGceI7ZPR9A',
            pp=642,
            pof='{"clid":["2322165"],"distr_type":"1"}',
        )

        # click.log не пишется для запросов request_bs_pb
        response = self.report.request_bs(request + '&reqid=2&touch=1&rearr-factors=device=touch;offers_touch=1')
        self.click_log.expect(
            reqid=2,
            clicktype=ClickType.EXTERNAL,
            dtype='market',
            data_url=LikeUrl.of('http://kiyanochnaya.ru/kiyanki?id=1', unquote=True, ignore_len=False),
            ware_md5='f7_BYaO4c78hGceI7ZPR9A',
            pp=642,
            pof='{\\"clid\\":[\\"2322165\\"],\\"distr_type\\":\\"1\\"}',
        )


if __name__ == '__main__':
    main()
