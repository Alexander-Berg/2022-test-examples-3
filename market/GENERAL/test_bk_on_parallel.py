#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'parallel'
        cls.index.offers += [
            Offer(title='offer 1', hyperid=1),
        ]

    def test_source_role(self):
        """
        Проверяем наличие поля source_role в логах, которое позволяет отличить бк и параллельный
        """

        def check_log_fields(req_params, headers, source_role):
            self.report.request_bs('place=parallel&hyperid=1&{}'.format(req_params), headers=headers)
            self.access_log.expect(source_role=source_role)
            self.report.request_json('place=prime&hyperid=1&hid=100500&{}'.format(req_params), headers=headers)
            self.access_log.expect(source_role=source_role)
            self.error_log.expect(code=3621, source_role=source_role)

        check_log_fields('', {}, 'parallel')
        check_log_fields('reqid=123-hamster-456', {}, 'bk')
        check_log_fields('', {'Host': 'search.preport.tst.vs.market.yandex.net'}, 'bk')


if __name__ == '__main__':
    main()
