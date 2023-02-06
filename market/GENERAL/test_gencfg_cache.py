#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
import os
import json
import re


class T(TestCase):
    def check_file(self, path, service_port, lite_shade_handler_suffix):
        with open(path, 'r') as file:
            response = json.loads('\n'.join(file.readlines()))

        request_pattern = re.compile('/tags/.*/searcherlookup/groups/(.*)/instances')
        for group, hosts in response.items():
            self.assertTrue(request_pattern.match(group) is not None)
            self.assertEqual(
                json.loads(hosts),
                {
                    "instances": [
                        {
                            "lite_shade_handler_suffix": lite_shade_handler_suffix,
                            "domain": ".search.yandex.net",
                            "power": 380.0,
                            "hbf": {"interfaces": {"backbone": {"hostname": "localhost"}}},
                            "dc": "vla",
                            "location": "vla",
                            "port": service_port,
                        }
                    ]
                },
            )

    def test_cache_files_creation(self):
        cache_dir = self.meta_paths.gencfg_backup
        at_least_one_exist = False

        for filename in os.listdir(cache_dir):
            if not filename.endswith('cache.txt'):
                continue

            at_least_one_exist = True

            service_port = self.snippets.port
            lite_shade_handler_suffix = '/saas/market_snippet'

            if 'ugc' in filename:
                service_port = self.saas_ugc.port
                lite_shade_handler_suffix = '/saas_ugc'

            self.check_file(
                path=os.path.join(cache_dir, filename),
                service_port=service_port,
                lite_shade_handler_suffix=lite_shade_handler_suffix,
            )

        self.assertTrue(at_least_one_exist)


if __name__ == '__main__':
    main()
