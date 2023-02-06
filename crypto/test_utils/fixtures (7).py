import os

import mock
import pytest
import yatest.common

import crypta.lib.python.yql.client as yql_helpers
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
)
from crypta.lookalike.proto import yt_node_names_pb2
from crypta.siberia.bin.custom_audience.lib.python.clustering import test_utils


@pytest.fixture
def patched_environ(local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)


@pytest.fixture
def patched_yt_client(local_yt, patched_environ):
    yt_client = local_yt.get_yt_client()
    with mock.patch('crypta.siberia.bin.custom_audience.lib.python.clustering.utils.get_yt_client', return_value=yt_client):
        yield yt_client


@pytest.fixture
def patched_yql_client(local_yt, patched_environ):
    yql_client = yql_helpers.create_yql_client(
        yt_proxy=local_yt.get_server(),
        pool='fake_pool',
        token=os.getenv('YQL_TOKEN'),
    )
    with mock.patch('crypta.siberia.bin.custom_audience.lib.python.clustering.utils.get_yql_client', return_value=yql_client):
        yield yql_client


@pytest.fixture
def dssm_lal_model_with_sandbox_link_attr():
    def get_segment_dict(segments_dict_path, resources_path=test_utils.resources_path):
        return files.YtFile(
            yatest.common.build_path(
                os.path.join(
                    resources_path,
                    'crypta_look_alike_model/segments_dict',
                ),
            ),
            segments_dict_path,
        )

    def get_dssm_lal_model(dssm_lal_model_path, mocked_sandbox_dssm_lookalike_model, resources_path=test_utils.resources_path):
        return files.YtFile(
            yatest.common.build_path(
                os.path.join(
                    resources_path,
                    'crypta_look_alike_model/dssm_lal_model.applier',
                ),
            ),
            dssm_lal_model_path,
            on_write=tables.OnWrite(
                attributes={
                    yt_node_names_pb2.TYtNodeNames().DssmSandboxLinkAttr: test_utils.get_dssm_lookalike_url(mocked_sandbox_dssm_lookalike_model),
                },
            ),
        )
    return get_segment_dict, get_dssm_lal_model
