#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import test_conf as tc
import sys
import yatest.common

def test_features():
    cfg = tc.TestConfig(yatest.common.build_path(), yatest.common.data_path())
    binary = yatest.common.binary_path("search/web/personalization/test_features/test_features")
    params = [
        "--rc"    , cfg.get_rearr_ctx(),
        "--uc"    , cfg.get_user_ctx(),
        "--cp"    , cfg.get_cp_trie()
    ]
    return yatest.common.canonical_execute(binary, params)
