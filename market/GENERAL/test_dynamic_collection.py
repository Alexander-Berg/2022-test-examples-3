#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    def test_report_collection_restart_admin_action(self):
        response = self.report.request_xml('admin_action=updatedata&which=collection')
        self.assertFragmentIn(response, "<admin-action>collection id must be specified</admin-action>")

        response = self.report.request_xml('admin_action=updatedata&which=collection&id=xxx')
        self.assertFragmentIn(response, "<admin-action>No collection with ID xxx</admin-action>")


if __name__ == '__main__':
    main()
