from copy import deepcopy

import pytest

from market.backbone.offers_store.yt_sync.settings.tables import TABLES
from market.backbone.offers_store.yt_sync.test.libs.helpers import gen_path


@pytest.fixture
def clusters(yt_cluster):
    return yt_cluster.primary, yt_cluster.secondary


@pytest.fixture
def clients(clusters):
    return tuple(x.get_yt_client() for x in clusters)


@pytest.fixture
def table_desc():
    return deepcopy(TABLES)


@pytest.fixture
def path(table):
    return gen_path(table)
