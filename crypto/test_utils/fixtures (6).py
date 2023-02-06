import os

import mock
import pytest
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.proto.user_embedding_pb2 import TUserEmbedding
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames

YT_NODE_NAMES = TYtNodeNames()


pytest_plugins = [
    'crypta.lib.python.nirvana.test_helpers.fixtures',
]


@pytest.fixture
def patched_yt_client(yt_client, yql_client):
    with mock.patch('crypta.lookalike.lib.python.utils.utils.get_yt_client', return_value=yt_client), \
            mock.patch('crypta.lookalike.lib.python.utils.utils.get_yql_client', return_value=yql_client):
        yield yt_client


@pytest.fixture
def model_version():
    def get_model_version(date, monthly=False, versions_dir=None, test=None):
        applier_test, embeddings_test = (tests.Exists(), tests.TableIsNotChanged()) if test is None else (test, test)
        if versions_dir is None:
            versions_dir = config.LOOKALIKE_VERSIONS_DIRECTORY
            if monthly:
                versions_dir = config.LOOKALIKE_MONTHLY_VERSIONS_DIRECTORY

        versions_dir = os.path.join(versions_dir, date)
        model_applier = files.YtFile(
            yatest.common.work_path('dssm_lal_model.applier'),
            os.path.join(versions_dir, YT_NODE_NAMES.DssmModelFile),
        )

        segments_dict = files.YtFile(
            yatest.common.work_path('segments_dict'),
            os.path.join(versions_dir, YT_NODE_NAMES.SegmentsDictFile),
        )

        data = [(model_applier, applier_test), (segments_dict, None)]
        if monthly:
            return data

        user_embeddings = tables.get_yson_table_with_schema(
            'user_embeddings.yson',
            os.path.join(versions_dir, YT_NODE_NAMES.UserEmbeddingsTable),
            schema=schema_utils.get_schema_from_proto(TUserEmbedding),
        )
        return data + [(user_embeddings, embeddings_test)]

    return get_model_version
