# -*- coding: utf-8 -*-

import os
import pytest
import re
import json

from report.functional.web.base import BaseFuncTest


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestClid(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.ticket('SERP-55203')
    @pytest.mark.parametrize(
        ('clid', 'removed'),
        [
            (2302157, True),
            ( 230215, False),
        ]
    )
    def test_delete_mts_clid(self, query, clid, removed):
        query.add_params({"clid": clid, "safari_remove_clids": 1})
        ctxs = self.json_dump_ctxs(query)

        assert len(ctxs['request']) > 0
        if removed:
            assert 'clid' not in ctxs['request'][0]['params']
        else:
            assert int(ctxs['request'][0]['params']['clid'][0]) == clid
        assert int(ctxs['report'][0]["clid"]) == clid

    @pytest.mark.ticket('SERP-55203')
    @pytest.mark.parametrize(
        ('clid', 'new_clid'),
        [
            (1906723, 2192579),
            (1906724, 2192593),
        ]
    )
    def test_rewrite_clid(self, query, clid, new_clid):
        query.add_params({"clid": clid})
        ctxs = self.json_dump_ctxs(query)

        assert len(ctxs['request']) > 0
        assert int(ctxs['request'][0]['params']['clid'][0]) == new_clid
        assert int(ctxs['report'][0]["clid"]) == clid
