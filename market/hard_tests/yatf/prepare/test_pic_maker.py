# coding: utf-8

from hamcrest import (
    assert_that,
    equal_to,
)
import pytest

from yt.wrapper import ypath_join

from market.idx.generation.yatf.envs.pic_maker import PicMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config, PicrobotConfig
from market.idx.generation.yatf.resources.prepare.picrobot_meta import PicrobotMetaTable
from market.idx.generation.yatf.resources.prepare.picture import default_picture

from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)

AVATAR_NAMESPACE = 'YATF_AVATAR_MDS_NAMESPACE'


@pytest.fixture(scope="module")
def or3_config_data():
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def picrobot_config_data():
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
            'yt_picrobot_meta': ypath_join(home_dir, 'meta'),
        },
        'avatar_read': {
            'report_namespace': AVATAR_NAMESPACE,
        }
    }


@pytest.yield_fixture(scope="module")
def picrobot_config(picrobot_config_data):
    return PicrobotConfig(**picrobot_config_data)


@pytest.yield_fixture(scope="module")
def pictures():
    return [
        default_picture('1', mds_namespace=None),
        default_picture('2', mds_namespace=''),
        default_picture('3', mds_namespace='marketpic'),
        default_picture('4', mds_namespace='marketpic2'),
        default_picture('5', mds_namespace='marketpic-testing', main_color='#808080'),
    ]


@pytest.yield_fixture(scope="module")
def picrobot_meta(yt_server, pictures):
    home_dir = get_yt_prefix()
    realpath = ypath_join(home_dir, 'metadata')
    path = ypath_join(home_dir, 'meta')
    return PicrobotMetaTable(yt_server, realpath, path, pictures)


@pytest.yield_fixture(scope="module")
def workflow(yt_server, or3_config, picrobot_config, picrobot_meta):
    assert picrobot_meta
    resources = {
        'meta': picrobot_meta,
        'mi.cfg': or3_config,
        'picrobot.cfg': picrobot_config,
    }
    with PicMakerTestEnv(yt_server, **resources) as env:
        env.verify()
        env.execute()
        yield env


def test_namespace(workflow, pictures):
    expected_ns = [
        (pic.get('mds_namespace', None) or AVATAR_NAMESPACE)
        for pic in pictures
    ]

    actual_ns_success = [
        pic['pic']['namespace']
        for pic in workflow.success
    ]
    assert_that(expected_ns, equal_to(actual_ns_success))

    actual_ns_full = [
        pic['pic']['namespace']
        for pic in workflow.full
    ]
    assert_that(expected_ns, equal_to(actual_ns_full))


def test_main_color(workflow, pictures):
    expected = [
        (pic.get('main_color', None) or '')
        for pic in pictures
    ]

    actual_success = [
        pic['pic']['main_color']
        for pic in workflow.success
    ]
    assert_that(expected, equal_to(actual_success))

    actual_full = [
        pic['pic']['main_color']
        for pic in workflow.full
    ]
    assert_that(expected, equal_to(actual_full))
