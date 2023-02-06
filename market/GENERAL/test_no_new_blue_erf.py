#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Region, WebErfEntry, WebErfFeatures, WebHerfEntry, WebHerfFeatures
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_no_new_erf(cls):
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
                hyperid=1, sku=1, title='мяч', blue_offers=[BlueOffer(waremd5='EpnWVxDQxj4wg7vVI1ElnA', feedid=1)]
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

        cls.settings.generate_old_erf = True

    def test_no_new_erf(self):
        """
        Проверяем, что, если нет blue_erf, будет использоваться erf
        для обратной совместимости
        При этом хостовые фичи тоже работают
        """
        _ = self.report.request_json('place=prime&text=мяч&show-urls=external&rearr-factors=market_metadoc_search=no')

        self.feature_log.expect(
            title_comm=1, f_title_idf_sum=0.5, owner_enough_clicked=1, ware_md5='EpnWVxDQxj4wg7vVI1ElnA'
        )


if __name__ == '__main__':
    main()
