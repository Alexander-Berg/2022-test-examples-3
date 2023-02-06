#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
import os
import errno
import json


class T(TestCase):
    @classmethod
    def create_gencfg_cache_file(cls, path, datacenters, group_suffix, service_port, lite_shade_handler_suffix):
        cache = {}

        for datacenter in datacenters:
            group = '_'.join([datacenter, group_suffix])
            request_pattern = '/tags/test_tag/searcherlookup/groups/' + group + '/instances'

            cache[request_pattern] = json.dumps(
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
                }
            )

        with open(path, 'w') as file:
            json.dump(cache, file)

    @classmethod
    def prepare(cls):
        cls.settings.gencfg_enabled = False

        cache_dir = cls.meta_paths.gencfg_backup

        try:
            os.mkdir(cache_dir)
        except OSError as exc:
            if exc.errno != errno.EEXIST:
                raise
            pass

        snippet_file = os.path.join(cache_dir, 'market_snippet_gencfg_cache.txt')
        snippet_file_lb = os.path.join(cache_dir, 'market_snippets_lb_balancing_gencfg_cache.txt')
        snippet_file_balancing = os.path.join(cache_dir, 'market_snippet_balancing_gencfg_cache.txt')
        ugc_file = os.path.join(cache_dir, 'saas_ugc_gencfg_cache.txt')

        datacenters = ['SAS', 'VLA', 'IVA', 'MAN']

        for filename in [snippet_file, snippet_file_lb, snippet_file_balancing]:
            cls.create_gencfg_cache_file(
                path=filename,
                datacenters=datacenters,
                group_suffix='SAAS_CLOUD_SEARCHPROXY_STABLE_MARKET',
                service_port=cls.snippets.port,
                lite_shade_handler_suffix='/saas/market_snippet',
            )

        cls.create_gencfg_cache_file(
            path=ugc_file,
            datacenters=datacenters,
            group_suffix='SAAS_MMETA',
            service_port=cls.saas_ugc.port,
            lite_shade_handler_suffix='/saas_ugc',
        )

    def test_use_cache(self):
        self.common_log.expect('Snippet over search proxy client initialized for groups')
        self.common_log.expect('UGC saas searcher is initialized and hosts are discovered')


if __name__ == '__main__':
    main()
