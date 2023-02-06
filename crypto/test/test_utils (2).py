import logging

from yt import yson

from crypta.graph.matching.direct.proto.types_pb2 import TDirectEdge
from crypta.lab.proto.describe_pb2 import TYandexuidDatedSample
from crypta.lib.python.yt import schema_utils
from crypta.siberia.bin.make_id_to_crypta_id.lib.maker.id_to_crypta_id_pb2 import TIdToCryptaId

logger = logging.getLogger(__name__)


class Fields(object):
    DATE = 'date'
    CRYPTA_ID = 'CryptaID'
    ID_FIELD = 'id'
    ID_TYPE = 'id_type'


def yuid_to_crypta_id_schema():
    return schema_utils.get_schema_from_proto(TIdToCryptaId)


def strict_src_with_dates_schema():
    return schema_utils.get_schema_from_proto(TYandexuidDatedSample)


def src_with_dates_schema():
    return schema_utils.yt_schema_from_dict({
        'Yandexuid': 'string',
        'Date': 'string',
    })


def matching_schema():
    return schema_utils.get_schema_from_proto(TDirectEdge)


def sample_stats_schema():
    schema = yson.to_yson_type(
        [
            dict(name='Hash', type='uint64', expression='farm_hash(SampleID)', sort_order='ascending'),
            dict(name='SampleID', type='string', required=False, sort_order='ascending'),
            dict(name='Stats', type='string', required=False, sort_order='ascending'),
            dict(name='GroupID', type='string', required=False),
        ],
        attributes=dict(unique_keys=True),
    )
    return schema
