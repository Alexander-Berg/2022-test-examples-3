#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty, Capture


class T(env.AccessSuite):
    def test_publisher_paging(self):
        for i in range(5):
            self.access.create_publisher(name=str(i))

        response = self.access.list_publishers(page_size=3)
        next_page = Capture()
        self.assertFragmentIn(response, {
            'next_page_token': NotEmpty(capture=next_page),
            'publisher': [
                {'name': '0'},
                {'name': '1'},
                {'name': '2'}
            ]})

        response = self.access.list_publishers(page_size=3, page_token=next_page.value)
        self.assertFragmentIn(response, {
            'publisher': [
                {'name': '3'},
                {'name': '4'}
            ]})


if __name__ == '__main__':
    env.main()
