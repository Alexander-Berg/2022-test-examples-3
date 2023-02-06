#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, HyperCategory, MarketSku, Model, Offer, Region
from core.matcher import Regex


from core.report import DefaultFlags


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.reqwizard.on_default_request().respond()

        cls.index.hypertree += [
            HyperCategory(hid=999),
        ]

        cls.index.regiontree += [Region(rid=213, name='Москва')]

        cls.index.models += [Model(title="dress", hid=999, hyperid=1), Model(hid=998, hyperid=2)]

        """
        Офер, у которого нет поля hidd
        """

        cls.index.offers += [
            Offer(
                hyperid=1,
                waremd5='DPBf1fM7GEYfSiYSJaWtIQ',
                dont_save_fields=['hidd'],
                title='NoHidd',
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue_no_hidd",
                hyperid=2,
                sku=1,
                dont_save_fields=['hidd'],
                blue_offers=[
                    BlueOffer(
                        waremd5='Sku1Price50-iLVm1Goleg',
                    )
                ],
            )
        ]

    def test_has_request_info(self):
        # Первый случай написания url_hash
        _ = self.report.request_json(
            'place=prime&hid=999&ip=127.0.0.17&rearr-factors=market_use_childs_for_nid_to_hid=0',
            strict=False,
            add_defaults=DefaultFlags.BS_FORMAT,
        )

        self.error_log.expect(code=1010, url_hash=Regex("[0-9a-f]{32}"), place='prime', rgb='GREEN')

        # Второй случай написания url_hash
        _ = self.report.request_json('place=productoffers&pp=18&hyperid=2&rgb=blue')

        self.base_logs_storage.error_log.expect(
            message='No HIDD attributes in document',
            url_hash=Regex("[0-9a-f]{32}"),
            place='productoffers',
            rgb='BLUE',
        )
        self.base_logs_storage.error_log.expect(code=3924, place='productoffers')

    def test_hyperid_parse_error(self):
        for request in ('place=prime&hyperid=-1', 'place=prime&hyperid=bcb', 'place=prime&vclusterid=bcb'):
            self.report.request_json(request, strict=False)
            self.error_log.expect(code=3639, message=Regex('.*Failed to parse hyperid and vclusterid params.*'))

    def test_non_empty_url_hash_when_3621(self):
        self.report.request_json('place=prime&hid=100500&hyperid=1')  # some unknown hid
        self.error_log.expect(code=3621, url_hash=Regex('[0-9a-f]{32}'), place='prime')
        self.base_logs_storage.error_log.expect(code=3646, place='prime')
        self.base_logs_storage.error_log.expect(code=3924, place='prime')

    def test_meta_resource_header(self):
        self.report.request_json(
            'place=prime&hyperid=-1',
            strict=False,
            headers={
                'resource-meta': '%7B%22client%22%3A%22pokupki.touch%22%2C%22pageId%22%3A%22blue-market_product%22%2C%22scenario%22%3A%22fetchSkusWithProducts%22%7D'
            },
        )
        self.error_log.expect(
            code=3639,
            resource_meta='{"client":"pokupki.touch","pageId":"blue-market_product","scenario":"fetchSkusWithProducts"}',
            client='pokupki.touch',
            client_page_id='blue-market_product',
            client_scenario='fetchSkusWithProducts',
        )

    def test_suspiciousness_header(self):
        self.report.request_json('place=prime&hid=999', strict=False, headers={'x-antirobot-suspiciousness-y': '1.0'})
        self.base_logs_storage.error_log.expect(is_suspicious=1)

    def test_is_antirobot_degradation_header(self):
        self.report.request_json('place=prime&hid=999', strict=False, headers={'x-yandex-antirobot-degradation': '1'})
        self.base_logs_storage.error_log.expect(is_antirobot_degradation=1)

    def test_warming_up_param(self):
        self.report.request_json('place=prime&hid=999&mini-tank=1', strict=False)
        self.base_logs_storage.error_log.expect(is_warming_up=1)


if __name__ == '__main__':
    main()
