#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.detect_leaks = True

    def test(self):
        response = self.report.request_plain('admin_action=memory_leak_test')
        self.assertIn('<status>memory_leak_test done</status>', str(response))

        if self.server.sanitizer_type == 'address':
            self.server.must_have_memory_leak = True


if __name__ == '__main__':
    main()
