#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.media_adv.incut_search.mt.env as env


class T(env.MediaAdvIncutSearchSuite):

    @classmethod
    def setUpClass(cls):
        """
        переопределенный метод для дополнительного вызова настроек
        """
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    def test_json(self):
        response = self.media_adv_incut_search.request_json('hello?name=shiny user')
        self.assertFragmentIn(response, {
            "greetings": "Hello, shiny user!"
        })

    def test_text(self):
        response = self.media_adv_incut_search.request_text('hello?name=shiny user&format=text')
        self.assertFragmentIn(response, "Hello, shiny user!")


if __name__ == '__main__':
    env.main()
