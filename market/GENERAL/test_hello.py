#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.common_good.servicesforgoods.mt.env as env


class T(env.ServicesforgoodsSuite):
    def test_json(self):
        response = self.servicesforgoods.request_json('hello?name=shiny user')
        self.assertFragmentIn(response, {
            "greetings": "Hello, shiny user!"
        })

    def test_text(self):
        response = self.servicesforgoods.request_text('hello?name=shiny user&format=text')
        self.assertFragmentIn(response, "Hello, shiny user!")


if __name__ == '__main__':
    env.main()
