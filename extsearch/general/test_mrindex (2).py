#!/usr/bin/env python
# -*- coding: utf-8 -*-
# to run:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long
# to run and canonize:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long -Z
#  svn commit ...

import os
import sys

print sys.path
print os.environ

import testlib


class RunDirtyTagsDesc(testlib.IndexProcessDescription):
    desc = 'mr_build_tags'
    cmd = '''{program} BuildDirtyTagsMetadoc
                           --shard-number 0
                           --gzt-bin {config_dir}/tags.gzt.bin
                           --homonyms {config_dir}/homonyms.initial.map
                           --shard-number 0
                           --fio-finder-input {config_dir}/fio_finder_input_data.tar.gz
                           --job-memory-limit 4147483648
                           --tags-prefix images
                           --tags-state 66666666-666666
                           --server {mr_server}
                           --index-prefix //images/tags/66666666-666666/input
                           --index-state 99999999-999999'''

    input_tables = [
        '//images/tags/66666666-666666/input/index/99999999-999999/metadoc/0/metadoc'
    ]
    output_tables = [
        '//images/tags/66666666-666666/group.2.dirtytags',
        '//images/tags/66666666-666666/homonyms'
    ]


class TestDirtyTags(testlib.YtRegressionTestBase):
    config_dir = 'tags/20190615-103123/data'
    pack = [RunDirtyTagsDesc]
