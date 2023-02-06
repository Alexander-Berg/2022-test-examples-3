import pytest

from crypta.lib.proto.user_data.user_data_stats_pb2 import TUserDataStats
from crypta.lib.python import proto
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.siberia.bin.common.proto.crypta_id_user_data_pb2 import TCryptaIdUserData
from crypta.siberia.bin.make_id_to_crypta_id.lib.maker.id_to_crypta_id_pb2 import TIdToCryptaId


pytest_plugins = [
    'crypta.lib.python.nirvana.test_helpers.fixtures',
    'crypta.lookalike.lib.python.test_utils.fixtures',
    'crypta.siberia.bin.common.test_helpers.fixtures',
]

YT_NODE_NAMES = TYtNodeNames()


@pytest.fixture
def describe_input():
    crypta_id_user_data = tables.YsonTable(
        file_path='crypta_id_user_data.yson',
        cypress_path=config.FOR_DESCRIPTION_BY_CRYPTAID_TABLE,
        on_write=tables.OnWrite(
            sort_by=['crypta_id'],
            attributes={'schema': schema_utils.get_schema_from_proto(TCryptaIdUserData)},
            row_transformer=proto.row_transformer(TCryptaIdUserData),
        ),
    )

    id_to_crypta_id = tables.YsonTable(
        file_path='id_to_crypta_id.yson',
        cypress_path='//home/crypta/production/siberia/id_to_crypta_id',
        on_write=tables.OnWrite(
            sort_by=['id', 'id_type'],
            attributes={'schema': schema_utils.get_schema_from_proto(TIdToCryptaId)},
        ),
    )

    user_data_stats = tables.YsonTable(
        file_path='user_data_stats.yson',
        cypress_path='//home/crypta/testing/lab/data/crypta_id/UserDataStats',
        on_write=tables.OnWrite(
            attributes={'schema': schema_utils.get_schema_from_proto(TUserDataStats)},
            row_transformer=proto.row_transformer(TUserDataStats),
        ),
    )

    return [
        (crypta_id_user_data, tests.TableIsNotChanged()),
        (id_to_crypta_id, tests.TableIsNotChanged()),
        (user_data_stats, tests.TableIsNotChanged()),
    ]


@pytest.fixture
def date():
    return '2022-01-19'
