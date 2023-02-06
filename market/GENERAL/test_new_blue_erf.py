#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    MarketSku,
    MonetOfferEntry,
    MonetSkuEntry,
    Region,
    WebErfEntry,
    WebErfFeatures,
    WebHerfEntry,
    WebHerfFeatures,
)
from core.testcase import TestCase, main
from core.matcher import Round, Absent
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare_new_erf(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                title='мяч',
                blue_offers=[
                    BlueOffer(waremd5='EpnWVxDQxj4wg7vVI1ElnA', feedid=1),
                    BlueOffer(waremd5='BH8EPLtKmdLQhLUasgaOnA', feedid=2, has_gone=True),
                ],
            ),
            MarketSku(
                hyperid=1,
                sku=2,
                title='шар',
                blue_offers=[
                    BlueOffer(waremd5='KXGI8T3GP_pqjgdd7HfoHQ', feedid=3),
                ],
            ),
        ]

        cls.index.web_erf_features += [
            WebErfEntry(
                url='http://pokupki.market.yandex.ru/product/1?offerid=EpnWVxDQxj4wg7vVI1ElnA',
                features=WebErfFeatures(title_comm=1, f_title_idf_sum=0.5),
            ),
        ]

        cls.index.web_herf_features += [
            WebHerfEntry(host='http://pokupki.market.yandex.ru', features=WebHerfFeatures(owner_enough_clicked=1)),
        ]

        cls.index.monet_offer_entries += [
            MonetOfferEntry('http://pokupki.market.yandex.ru/product/1?offerid=EpnWVxDQxj4wg7vVI1ElnA', elasticity=0.52)
        ]

        cls.index.monet_sku_entries += [MonetSkuEntry(sku='1', predict_items_today=1.5, predict_items_in_5d=1.25)]

        cls.settings.generate_old_erf = False  # explicitly, is false by default

    @skip('MARKETOUT-41926')
    def test_new_erf(self):
        """
        Проверяем, что, если есть blue_erf, он будет использоваться
        При этом хостовые фичи работают для всех офферов Беру, а поскушные --
        для всех офферов соответствующего ску.
        """

        self.report.request_json(
            'place=prime&text=мяч&show-urls=external&reqid=1'
            '&rearr-factors=market_search_in_white_offer_shard_all_cpa_docs=0'
        )

        self.feature_log.expect(
            title_comm=1,
            f_title_idf_sum=0.5,
            owner_enough_clicked=1,
            elasticity=Round(0.52, 2),
            predict_items_today=Round(1.5, 2),
            predict_items_in_5d=Round(1.25, 2),
            req_id='1',
            ware_md5='EpnWVxDQxj4wg7vVI1ElnA',
        )

        # Для другого оффера того же ску эластичность и статфакторы уже
        # не посчитаются, зато по-прежнему будут считаться скушные факторы

        self.report.request_json(
            'place=prime&text=мяч&show-urls=external&ignore-has-gone=1&reqid=2'
            '&rearr-factors=market_search_in_white_offer_shard_all_cpa_docs=0'
        )

        self.feature_log.expect(
            title_comm=Absent(),
            f_title_idf_sum=Absent(),
            owner_enough_clicked=1,
            elasticity=Absent(),
            predict_items_today=Round(1.5, 2),
            predict_items_in_5d=Round(1.25, 2),
            req_id='2',
            ware_md5='BH8EPLtKmdLQhLUasgaOnA',
        )

        # Для оффера другого ску продолжают считаться похостовые (beru.ru),
        # но уже не считаются поскушные факторы

        self.report.request_json(
            'place=prime&text=шар&show-urls=external&reqid=3'
            '&rearr-factors=market_search_in_white_offer_shard_all_cpa_docs=0'
        )

        self.feature_log.expect(
            title_comm=Absent(),
            f_title_idf_sum=Absent(),
            owner_enough_clicked=1,
            elasticity=Absent(),
            predict_items_today=Absent(),
            predict_items_in_5d=Absent(),
            req_id='3',
            ware_md5='KXGI8T3GP_pqjgdd7HfoHQ',
        )


if __name__ == '__main__':
    main()
