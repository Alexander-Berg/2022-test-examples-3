#!/usr/bin/python

import unittest
import os
import common

class TestPlaneshift(unittest.TestCase):

    def test_no_local_host_in_snippet_url(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh-ps01d.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh-ps01d.market.yandex.net',
                    ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        self.assertTrue(yandsearch is not None)
        remote = yandsearch.find_section('SearchSource', lambda sec: 'basesearch16-1' in sec['CgiSearchPrefix'])
        self.assertTrue(remote is not None)
        self.assertEqual(len(remote['CgiSearchPrefix'].split()), 1)
        self.assertTrue('://localhost:' not in remote['CgiSnippetPrefix'])
