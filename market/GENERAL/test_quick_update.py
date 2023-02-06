#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    def test_call_without_update_file(self):
        response = self.report.request_xml('admin_action=updatedata&which=qindex')
        self.assertFragmentIn(response, 'ok', preserve_order=True)


if __name__ == '__main__':
    main()
