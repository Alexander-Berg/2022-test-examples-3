#!/usr/bin/env python
# coding: utf-8

import pytest
from market.idx.generation.yatf.test_envs.sentences_length_indexer import IndexsentTestEnv


@pytest.yield_fixture(scope="module")
def workflow():
    resources = {}
    with IndexsentTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_sent_index(workflow):
    pass
