#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main
from core.matcher import NotEmpty
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(title='kijanka'),
            Offer(title='kijanka'),
            Offer(title='kijanka'),
            Offer(title='kijanka'),
            Offer(title='kijanka'),
        ]

    def test_tskv_log(self):
        self.report.request_bs(
            'place=parallel&text=kijanka&stat-block-id=test&reqid=abc123', headers={'X-Market-Req-ID': 'test_req_id'}
        )
        self.access_log.expect(
            tskv_format='market-report-access-log',
            unixtime_ms=NotEmpty(),
            unixtime=NotEmpty(),
            event_time=NotEmpty(),
            url=NotEmpty(),
            search_elapsed=NotEmpty(),
            remote_addr=NotEmpty(),
            full_elapsed=NotEmpty(),
            total_documents_processed='5',
            total_documents_accepted='5',
            total_rendered='1',
            req_wiz_time='-1',
            wizards='market_offers_wizard,market_offers_wizard_center_incut',
            reqid='abc123',
            redirect_info='0',
            base_search_elapsed=NotEmpty(),
            meta_search_elapsed=NotEmpty(),
            have_trimmed_field='0',
            fetch_time=NotEmpty(),
            snippet_requests_made=NotEmpty(),
            snippets_fetched=NotEmpty(),
            url_hash=NotEmpty(),
            fuzzy_search_used='0',
            x_market_req_id='test_req_id',
        )

    def test_tskv_log_escaping(self):
        self.report.request_bs('place=parallel&text=kijanka&reqid=\\test')
        self.access_log.expect(
            reqid='\\\\test',
        )

    def test_tskv_log_trim(self):
        self.report.request_bs('place=parallel&text=kijanka&reqid=' + 'a' * 1001)
        self.access_log.expect(reqid='a' * 1000, have_trimmed_field='1')

    def test_x_yandex_icookie(self):
        """
        Проверяем, что зашифрованное значение icookie, попадает в аксес-лог в расшифрованном виде
        BCEmkyAbCsICEPzTQsKKZiwaEphOTfJYcplJsb6WeCNz9ThbLjUw5pw1K8G40cyJPs%2BVrWxAzPzzs34zCBQWvGkphV4%3D => 6774478491508471626
        """
        self.report.request_json('place=prime&text=kijanka&x-yandex-icookie=6774478491508471626')
        self.access_log.expect(icookie='6774478491508471626')

    def test_no_icookie(self):
        self.report.request_json('place=prime&text=kijanka')
        self.access_log.expect(icookie='')

    @skip("https://st.yandex-team.ru/MARKETOUT-21963")
    def test_empty_body(self):
        """
        На обычные запросы (не apphost) тело должно быть NONE
        """
        self.report.request_json('place=prime&text=kijanka')
        self.access_log.expect(request_body='NONE')


if __name__ == '__main__':
    main()
