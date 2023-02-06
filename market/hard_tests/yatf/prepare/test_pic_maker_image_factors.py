# coding: utf-8

from hamcrest import assert_that, equal_to
import pytest
import json

from yt.wrapper import ypath_join

from market.idx.generation.yatf.envs.pic_maker import PicMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config, PicrobotConfig
from market.idx.generation.yatf.resources.prepare.new_picrobot_state import NewPicrobotStateTable
from market.idx.generation.yatf.resources.prepare.picrobot_meta import PicrobotMetaTable

from market.idx.pylibrary.datacamp.pictures import make_picture_id
from market.idx.datacamp.picrobot.proto.mds_info_pb2 import TMdsInfo, TMdsId
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import TPicrobotState
from library.python.codecs import dumps as codec_dumps

from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)

AVATAR_NAMESPACE = 'YATF_AVATAR_MDS_NAMESPACE'

IMAGE2TEXT_V10_SIZE = 200
IMAGE2TEXT_V10_MIN_BOUND = -0.197   # см. kernel/dssm_applier/decompession/decompression.h


def _get_raw_image2text_v10_str(val):
    l = [str(val) for i in xrange(IMAGE2TEXT_V10_SIZE)]
    return " ".join(l)


def _get_compressed_image2text_v10_str(hex_val):
    l = [str(hex_val) for i in xrange(IMAGE2TEXT_V10_SIZE)]
    return "".join(l)


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
        'pic_maker' : {
            'enable_image2text_v10': 'true',
        }
    }


@pytest.yield_fixture(scope="module")
def picrobot_config(picrobot_config_data):
    return PicrobotConfig(**picrobot_config_data)


@pytest.yield_fixture(scope="module")
def pictures():
    return []


def default_state(url, mds_namespace, group_id, factors="{}"):
    CODEC_NAME = "zstd_6"
    state = TPicrobotState(
        MdsInfo=[
            TMdsInfo(
                MdsId=TMdsId(Namespace=mds_namespace, GroupId=group_id, ImageName=make_picture_id(url)),
                Width=1234,
                Height=1235,
                OrigFormat="JPEG",
                Crc64="234567897654",
                Factors=factors,
            ),
        ]
    )

    return {"Id": url, "State": codec_dumps(CODEC_NAME, state.SerializeToString()), "Codec": CODEC_NAME}


def get_factors(image2text_v10):
    factors = {
        "prod_v10_enc_i2t_v12_200_img": image2text_v10,
    }
    return json.dumps(factors)


@pytest.yield_fixture(scope="module")
def new_state():
    return [
        default_state("1", AVATAR_NAMESPACE, 5678),                                                            # нет факторов
        default_state("2", AVATAR_NAMESPACE, 5678, factors=get_factors(_get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND))),   # норм image2text_v10
        default_state("3", AVATAR_NAMESPACE, 5678, factors=get_factors("0.5 0.5 0.5")),                        # неверная длина массива эмбедов
        default_state("4", AVATAR_NAMESPACE, 5678, factors=get_factors(_get_raw_image2text_v10_str("text"))),  # в image2text_v10 не float
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


def test_factors(workflow):
    actual_embeds = [(pic['id'], pic['image2text_v10_embedding_str'] if 'image2text_v10_embedding_str' in pic else None) for pic in workflow.success]
    expected = [
        (make_picture_id("1"), None),
        (make_picture_id("2"), _get_compressed_image2text_v10_str('\x81')),  # для чисел <= нижней границы сжатия получаем -127 = 0x81. (см. NDssmApplier::Scale)
        (make_picture_id("3"), None),
        (make_picture_id("4"), None),
    ]
    assert_that(
        actual_embeds, equal_to(sorted(expected))
    )
