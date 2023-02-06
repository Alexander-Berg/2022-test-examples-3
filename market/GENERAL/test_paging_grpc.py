#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
from market.access.puller.mt.env import AccessPullerSuite, main
from market.pylibrary.lite.matcher import NotEmpty, Capture


class T(AccessPullerSuite):
    @classmethod
    def prepare(cls):
        cls.access_puller.config.StoragePullers.Dummy = True

    def test_paging(self):
        for i in range(5):
            self.access_puller.create_resource_puller(name=str(i), resource_name='some_res')

        response = self.access_puller.list_resource_pullers(page_size=3)
        next_page = Capture()
        self.assertFragmentIn(response, {
            'next_page_token': NotEmpty(capture=next_page),
            'pullers': [
                {'name': '0'},
                {'name': '1'},
                {'name': '2'}
            ]})

        response = self.access_puller.list_resource_pullers(page_size=3, page_token=next_page.value)
        self.assertFragmentIn(response, {
            'pullers': [
                {'name': '3'},
                {'name': '4'}
            ]})


if __name__ == '__main__':
    main()
