# -*- coding: utf-8 -*-
import pytest
from hamcrest import assert_that, is_not

from async_publishing import (
    HostConfig,
    DistGenerationMeta
)


@pytest.fixture()
def host_config():
    return HostConfig(
        fqdn="iva-001.net",
        group="my_cool_group",
        cluster_id=1,
        dists={
            "marketsearch3": ["search-part-1"]
        },
        generations_prefix="/publisher/generations",
    )


def test_host_config_need_dist_matches_dist(host_config):
    dist_meta = DistGenerationMeta(
        generation_name="20190101_0101",
        dist_name="search-part-1",
        service_name="marketsearch3"
    )

    assert_that(host_config.need_dist(dist_meta))


def test_host_config_need_dist_wrong_service(host_config):
    dist_meta = DistGenerationMeta(
        generation_name="20190101_0101",
        dist_name="search-part-1",
        service_name="marketsearchblue"
    )

    assert_that(is_not, host_config.need_dist(dist_meta))


def test_host_config_need_dist_wrong_dist_name(host_config):
    dist_meta = DistGenerationMeta(
        generation_name="20190101_0101",
        dist_name="search-part-0",
        service_name="marketsearch3"
    )

    assert_that(is_not, host_config.need_dist(dist_meta))
