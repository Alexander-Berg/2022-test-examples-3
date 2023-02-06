#!/usr/bin/env python
# -*- coding: utf-8 -*-
# to run:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long
# to run and canonize:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long -Z
#  svn commit ...

from extsearch.images.library.testlib import packtest

inputdoc_pack = []


class UsertrieMerge(packtest.ProcessDescription):
    desc = 'usertrie Merge'
    cmd = '''{program} Merge
                    --server {mr_server}
                    --index-prefix //sandbox/images
                    --index-state 20221212-131313'''
    input_tables = [
        '//sandbox/images/index/20221212-131313/userindex/factors/queryattrs',
        '//sandbox/images/index/20221212-131313/userindex/factors/querygreenurlctr',
        '//sandbox/images/index/20221212-131313/userindex/factors/querygreenurlctr',
        '//sandbox/images/index/20221212-131313/userindex/factors/querywizardctr'
    ]
    output_tables = [
        '//sandbox/images/index/20221212-131313/userindex/result/usertrie'
    ]

inputdoc_pack.append(UsertrieMerge)


class TestUserTrie(packtest.YtRegressionTestBase):
    pack = inputdoc_pack
