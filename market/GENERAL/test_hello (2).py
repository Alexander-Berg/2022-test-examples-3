#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.dyno.service.mt.env as env


class T(env.DynoSuite):
    def test_json(self):
        response = self.dyno.request_json('hello?name=shiny user')
        self.assertFragmentIn(response, {
            "greetings": "Hello, shiny user!"
        })

    def test_text(self):
        response = self.dyno.request_text('hello?name=shiny user&format=text')
        self.assertFragmentIn(response, "Hello, shiny user!")


if __name__ == '__main__':
    env.main()
