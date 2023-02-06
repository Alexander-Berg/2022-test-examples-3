#!/usr/bin/env python
# coding: utf-8

import pytest
from market.idx.generation.yatf.test_envs.annotation_indexer import IndexannTestEnv


@pytest.yield_fixture(scope="module")
def workflow():
    resources = {}
    with IndexannTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_ann_index(workflow):
    pass
