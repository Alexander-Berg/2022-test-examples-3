#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import time

from core.types import BlueOffer, MarketSku, Offer, RtyOffer, RtyOfferHelper, Shop, Tax
from core.testcase import TestCase, main
from core.types.currency import Currency, CurrencyIsoCodes


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, name='test_shop', currency=Currency.RUR, tax_system=Tax.OSN)
        ]

        cls.index.offers += [
            Offer(fesh=1, title='iphone1', feedid=15, offerid='offer1', price=100.5),
            Offer(fesh=1, title='galaxy2', feedid=25, offerid='offer2', price=10),
            Offer(fesh=1, title='nexus99', feedid=99, offerid='offer99', price=10),
            Offer(fesh=1, title='endpoint', feedid=0, offerid='offerZ', price=10),
            Offer(fesh=1, title='offerForBid', feedid=1, offerid='offerForBid', price=10, bid=10),
            Offer(fesh=1, title='offerForDelivery', feedid=1, offerid='offerForDelivery', price=10),
            Offer(fesh=1, title='offerForStock', feedid=1, offerid='offerForStock', price=10),
        ]
        cls.index.mskus += [
            MarketSku(
                title="blue offer",
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(
                        price=30,
                        feedid=3,
                        offerid='pull/partner_offer',
                        is_push_partner=False,
                    ),
                    BlueOffer(
                        price=40,
                        feedid=4,
                        offerid='push/partner_offer',
                        is_push_partner=True,
                    ),
                ],
            )
        ]

    def tearDown(self):
        # Send test offers
        self.rty.flush()

        # Send special offer for finish of waiting
        now = int(time.time())
        self.rty.offers += [
            RtyOffer(
                feedid=0,
                offerid='offerZ',
                price=10,
                download_time=now,
                modification_time=now,
                changed_states=['api_price_deleted'],
            )
        ]
        self.rty_stats_log.expect(feed_id=0, offer_id='offerZ', hub_ts=now)

        # Send test offer and wait
        self.rty.flush()
        self.rty_stats_log.wait_last_expectation()
        super(T, self).tearDown()

    def test_changed_price_fields(self):
        """
        Проверяем корректность заполнения полей в логе
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(
                feedid=15,
                offerid='offer1',
                price=100.5,
                old_price=80.1034,
                currency=Currency.RUR,
                download_time=1539157690,
                changed_states=['feed_price'],
            ),
            helper.make_offer(
                feedid=25,
                offerid='offer2',
                price=10,
                currency=Currency.EUR,
                download_time=1539157680,
                changed_states=['api_price'],
            ),
        ]

        mtimes = helper.get_mtimes()
        self.rty_stats_log.expect(
            feed_id=15,
            offer_id='offer1',
            price=100.5,
            old_price=80.1034,
            download_ts=1539157690,
            hub_ts=next(mtimes),
            currency=CurrencyIsoCodes[Currency.RUR],
            type='feed_price',
        ).once()
        self.rty_stats_log.expect(
            feed_id=25,
            offer_id='offer2',
            price=10,
            currency=CurrencyIsoCodes[Currency.EUR],
            download_ts=1539157680,
            hub_ts=next(mtimes),
            type='api_price',
        ).once()

    def test_changed_price_wrong_documents(self):
        """
        Проверяем обработку некорректных документов
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            # absent field 'changed_states'
            helper.make_offer(feedid=99, offerid='offer99', price=10),
            # 'download_ts'
            helper.make_offer(feedid=99, offerid='offer99', price=10, download_time=0),
            # empty field 'feedid'
            helper.make_offer(feedid='', offerid='offer99', price=10, changed_states=['price']),
            # empty field 'offerid'
            helper.make_offer(feedid=99, offerid='', price=10, changed_states=['price']),
            # empty timestamp of current change
            helper.make_offer(
                feedid=99,
                offerid='offer99',
                price=10,
                currency=Currency.RUR,
                download_time=1539157680,
                changed_states=['offer_has_gone'],
            ),
        ]

        self.rty_stats_log.expect(feed_id=99).never()
        self.rty_stats_log.expect(offer_id='offer99').never()

    def test_multiply_changed_in_one_document(self):
        """
        Проверяем корректность записей в логе при нескольких изменениях в одном документе
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(
                feedid=15, offerid='offer1', price=100, currency=Currency.RUR, changed_states=['feed_price,api_price']
            ),
        ]

        mtime = next(helper.get_mtimes())
        self.rty_stats_log.expect(
            feed_id=15,
            offer_id='offer1',
            price=100,
            currency=CurrencyIsoCodes[Currency.RUR],
            hub_ts=mtime,
            type='feed_price',
        ).once()
        self.rty_stats_log.expect(
            feed_id=15,
            offer_id='offer1',
            price=100,
            currency=CurrencyIsoCodes[Currency.RUR],
            hub_ts=mtime,
            type='api_price',
        ).once()

    def test_offer_has_gone_ts(self):
        """
        Проверяем корректность поля download ts для изменения offer has gone.
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(
                feedid=15,
                offerid='offer1',
                price=10,
                currency=Currency.RUR,
                flags=2048,
                flags_ts=1539157690,
                download_time=1539157680,
                changed_states=['flags,offer_has_gone'],
            ),
        ]

        mtimes = helper.get_mtimes()
        self.rty_stats_log.expect(
            feed_id=15, offer_id='offer1', download_ts=1539157690, hub_ts=next(mtimes), type='offer_has_gone'
        ).once()

    def test_offer_disabled_ts(self):
        """
        Проверяем корректность поля download ts для изменения offer disabled.
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(
                feedid=25,
                offerid='offer2',
                price=10,
                currency=Currency.RUR,
                disabled=1,
                disabled_ts=1539157690,
                download_time=1539157680,
                changed_states=['offer_disabled'],
            ),
        ]

        mtimes = helper.get_mtimes()
        self.rty_stats_log.expect(
            feed_id=25, offer_id='offer2', download_ts=1539157690, hub_ts=next(mtimes), type='offer_disabled'
        ).once()

    def test_bid_ts(self):
        """
        Проверяем корректность поля ts для изменения bid.
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(
                feedid=1,
                offerid='offerForBid',
                price=10,
                bid_and_flags=15,
                bid_and_flags_ts=1539157690,
                changed_states=['bid'],
            ),
        ]

        mtimes = helper.get_mtimes()
        matcher = self.rty_stats_log.expect(
            feed_id=1, offer_id='offerForBid', download_ts=1539157690, hub_ts=next(mtimes), type='bid'
        )
        matcher.once()

    def test_delivery_ts(self):
        """
        Проверяем корректность поля ts для изменения delivery.
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(
                feedid=1, offerid='offerForDelivery', price=10, delivery_ts=1539157000, changed_states=['delivery']
            ),
        ]

        mtimes = helper.get_mtimes()
        matcher = self.rty_stats_log.expect(
            feed_id=1, offer_id='offerForDelivery', download_ts=1539157000, hub_ts=next(mtimes), type='delivery'
        )
        matcher.once()

    def test_processing_type(self):
        """
        Проверяем корректность поля processing_type.
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(feedid=3, offerid='pull/partner_offer', price=31, changed_states=['feed_price']),
            helper.make_offer(feedid=4, offerid='push/partner_offer', price=41, changed_states=['api_price']),
        ]
        mtimes = helper.get_mtimes()
        self.rty_stats_log.expect(
            feed_id=3, offer_id='pull/partner_offer', hub_ts=next(mtimes), type='feed_price', processing_type='PULL'
        ).once()
        self.rty_stats_log.expect(
            feed_id=4, offer_id='push/partner_offer', hub_ts=next(mtimes), type='api_price', processing_type='PUSH'
        ).once()

    def test_offer_disabled_stock(self):
        """
        Проверяем, что поля разделенные по типам скрытий логируются
        """
        helper = RtyOfferHelper()
        self.rty.offers += [
            helper.make_offer(
                feedid=1,
                offerid='offerForStock',
                price=10,
                currency=Currency.RUR,
                disabled=1,
                disabled_ts=1539157690,
                download_time=1539157680,
                changed_states=['offer_disabled,offer_disabled_stock'],
            ),
        ]

        mtimes = helper.get_mtimes()
        self.rty_stats_log.expect(
            feed_id=1,
            offer_id='offerForStock',
            download_ts=1539157690,
            hub_ts=next(mtimes),
            type='offer_disabled_stock',
        ).once()


if __name__ == '__main__':
    main()
