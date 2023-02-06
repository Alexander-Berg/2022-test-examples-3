#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import datetime
import hashlib
import random
import unittest

from core.types import Offer
from core.types import autogen
from core.testcase import TestCase, main
from core.matcher import Greater

from market.pylibrary.lite.log import parse_tskv


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [Offer(title='iphone')]
        cls.emergency_flags.add_flags(log_every_n_base_request=1)
        cls.emergency_flags.save()
        random.seed(autogen.get_seed())

    def wait_log_line(self, req_id):
        '''Ожидам нужную строчку в логе'''
        # Когда мета научится пробрасывать X-Market-Req-ID в базоый, эту функцию можно будет удалить.
        # При удалении нужно будет добавить тест на проверку url_meta_hash и включить test_market_request_id.
        log_line = self.access_log.wait_line('x_market_req_id={}'.format(req_id))
        url_hash = parse_tskv(log_line)['url_hash']
        log_line = self.base_access_log.wait_line('url_meta_hash={}'.format(url_hash))
        return parse_tskv(log_line)

    def update_flag(self, value):
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(log_every_n_base_request=value)
        self.emergency_flags.save()
        self.base_access_log.backend.reopen()
        self.restart_report()

    def test_event_time(self):
        req_id = 'test_event_time'
        self.report.request_json('place=prime&text=iphone', headers={'X-Market-Req-ID': req_id})
        log_line = self.wait_log_line(req_id)
        unix_time = int(log_line['unixtime'])
        event_time = datetime.datetime.fromtimestamp(unix_time).strftime('[%c]')
        self.assertEqual(event_time, log_line['event_time'])
        self.assertEqual(int(unix_time), int(log_line['unixtime_ms']) / 1000)

    def test_url_hash(self):
        req_id = 'test_url_hash'
        self.report.request_json('place=prime&text=iphone', headers={'X-Market-Req-ID': req_id})
        log_line = self.wait_log_line(req_id)
        url = log_line['url']
        url_hash = hashlib.md5(url.encode()).hexdigest()
        self.assertEqual(url_hash, log_line['url_hash'])

    def test_search_elapsed(self):
        self.report.request_json('place=prime&text=iphone')
        self.base_access_log.expect(search_elapsed=Greater(0))

    @unittest.skip('meta search doesn\'t send X-Market-Req-ID to base')
    def test_market_request_id(self):
        request_id = '4124bc0a9335c27f086f24ba207a4912'
        self.report.request_json('place=prime&text=iphone', headers={'X-Market-Req-ID': request_id})
        self.base_access_log.expect(x_market_req_id=request_id)

    def test_z_write_log_frequency(self):
        '''Проверяем, что размер лога зависит от значени флага (last test in suite)'''

        def send_requests(request_count):
            for count in range(request_count - 1):
                self.report.request_json('place=prime&text=iphone')
            req_id = str(random.randint(0, 10000))
            self.report.request_json('place=prime&text=iphone', headers={'X-Market-Req-ID': req_id})
            self.wait_log_line(req_id)

        # Пропускаем все лишнее
        [_ for _ in self.base_access_log.backend]

        request_count = 15
        send_requests(request_count)
        # Сейчас 1 prime генерит 2 запроса на базовый, запоминаем сколько логов генерит request_count запросов.
        total_records = len([r for r in self.base_access_log.backend])

        self.update_flag(3)  # Кратно request_count'у, чтобы wait log правильно сработал
        send_requests(request_count)
        lines = [r for r in self.base_access_log.backend]
        self.assertEqual(len(lines), total_records / 3)


if __name__ == '__main__':
    main()
