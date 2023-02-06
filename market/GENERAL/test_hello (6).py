#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.search.shop.mt.env as env


class T(env.ShopSuite):
    def test_json(self):
        response = self.shop.request_json('hello?name=shiny user')
        self.assertFragmentIn(response, {
            "greetings": "Hello, shiny user!"
        })

    def test_text(self):
        response = self.shop.request_text('hello?name=shiny user&format=text')
        self.assertFragmentIn(response, "Hello, shiny user!")


if __name__ == '__main__':
    env.main()
