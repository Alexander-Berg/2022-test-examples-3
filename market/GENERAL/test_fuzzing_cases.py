#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import requests

from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [Offer(title='iphone')]

    def test_large_content_length(self):
        port = self.base_search_client.connection.port
        host = self.base_search_client.connection.host
        requests.request(
            "patch", "http://{}:{}/tass".format(host, port), headers={"content-length": "0000645970003280702122"}
        )


if __name__ == '__main__':
    main()
