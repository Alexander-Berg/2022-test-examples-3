# coding: utf-8

from hamcrest import assert_that
import pytest

from yt.wrapper import ypath_join

from market.idx.generation.yatf.envs.pic_maker import PicMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config, PicrobotConfig
from market.idx.generation.yatf.resources.prepare.new_picrobot_state import NewPicrobotStateTable
from market.idx.generation.yatf.resources.prepare.picrobot_meta import PicrobotMetaTable
from market.idx.generation.yatf.resources.prepare.picture import default_picture

from market.idx.pylibrary.datacamp.pictures import make_picture_id
from market.idx.datacamp.picrobot.proto.mds_info_pb2 import TMdsInfo, TMdsId
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import TPicrobotState
from library.python.codecs import dumps as codec_dumps

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
            'yt_new_picrobot_state': ypath_join(home_dir, 'new_state'),
        },
        'avatar_read': {
            'report_namespace': AVATAR_NAMESPACE,
        },
    }


@pytest.yield_fixture(scope="module")
def picrobot_config(picrobot_config_data):
    return PicrobotConfig(**picrobot_config_data)


@pytest.yield_fixture(scope="module")
def pictures():
    return [
        default_picture(make_picture_id("1"), url="1", mds_namespace=AVATAR_NAMESPACE, group_id=1234),
        default_picture(make_picture_id("2"), url="2", mds_namespace=AVATAR_NAMESPACE, group_id=1234),
    ]


def default_state(url, mds_namespace, group_id):
    CODEC_NAME = "zstd_6"
    state = TPicrobotState(
        MdsInfo=[
            TMdsInfo(
                MdsId=TMdsId(Namespace=mds_namespace, GroupId=group_id, ImageName=make_picture_id(url)),
                Width=1234,
                Height=1235,
                OrigFormat="JPEG",
                Crc64="234567897654",
                Factors="{}",
            ),
        ]
    )

    return {"Id": url, "State": codec_dumps(CODEC_NAME, state.SerializeToString()), "Codec": CODEC_NAME}


@pytest.yield_fixture(scope="module")
def new_state():
    return [
        default_state("2", AVATAR_NAMESPACE, 5678),
        default_state("3", AVATAR_NAMESPACE, 5678),
    ]


@pytest.yield_fixture(scope="module")
def picrobot_meta(yt_server, pictures):
    home_dir = get_yt_prefix()
    realpath = ypath_join(home_dir, 'metadata')
    path = ypath_join(home_dir, 'meta')
    return PicrobotMetaTable(yt_server, realpath, path, pictures)


@pytest.yield_fixture(scope="module")
def new_picrobot_state(yt_server, new_state):
    home_dir = get_yt_prefix()
    realpath = ypath_join(home_dir, "new_state_data")
    path = ypath_join(home_dir, "new_state")
    return NewPicrobotStateTable(yt_server, realpath, path, new_state)


@pytest.yield_fixture(scope="module")
def workflow(yt_server, or3_config, picrobot_config, picrobot_meta, new_picrobot_state):
    assert picrobot_meta
    assert new_picrobot_state
    resources = {
        'meta': picrobot_meta,
        'new_state': new_picrobot_state,
        'mi.cfg': or3_config,
        'picrobot.cfg': picrobot_config,
    }
    with PicMakerTestEnv(yt_server, **resources) as env:
        env.verify()
        env.execute()
        yield env


def test_new_picrobot_table_priority(workflow):
    assert_that(
        [(pic['id'], pic['pic']) for pic in workflow.success],
        [(make_picture_id("1"), 1234), (make_picture_id("2"), 5678), (make_picture_id("3"), 5678)],
    )
