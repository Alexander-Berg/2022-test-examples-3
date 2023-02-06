# coding: utf-8

import pytest
import logging

from market.idx.pylibrary.datacamp.utils import wait_until
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    ValueType
)
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv

from market.idx.datacamp.proto.models.MarketSkuMboContent_pb2 import MarketSkuMboContent
from market.idx.datacamp.proto.models.ModelStatus_pb2 import AboHidingReason, ModelStatus
from market.idx.datacamp.proto.errors.ValidationResult_pb2 import AboReason
from market.idx.yatf.utils.utils import rows_as_table
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampMskuTable
)
from market.pylibrary.proto_utils import message_from_data
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.datacamp.yatf.utils import create_update_meta
from datetime import datetime
from hamcrest import assert_that, equal_to, not_
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampMskuRows
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

# Дата того, что лежит в хранилище
OLD_DATE_TS = 1618763231577

# Дата обновления
NEW_DATE_TS = 1618764231000

# Дата обновления для одной из msku - более старая чем то, что лежит в хранилище
VERY_OLD_DATE_TS = 1618734231000

# Типы msku относящиеся к офферам.
ALLOWED_MSKU_CURRENT_TYPES = [
    "EXPERIMENTAL_SKU",
    "FAST_SKU",
    "PARTNER_SKU",
    "SKU",
]

NOT_ALLOWED_MSKU_CURRENT_TYPES = [
    "YouShallNotPass",
    "and you too",
]


def ts_to_seconds(ts):
    return ts // 1000


def ts_to_meta(ts):
    time_pattern = '%Y-%m-%dT%H:%M:%SZ'
    dt = datetime.utcfromtimestamp(ts_to_seconds(ts))
    return {'timestamp': dt.strftime(time_pattern)}


# То, что улетает в топик
MSKUS_TO_TOPIC = [
    # Обновление
    {
        'id': 1,
        'mbo_content': {
            'meta': ts_to_meta(NEW_DATE_TS),
            'msku': {
                'current_type': 'SKU',
                'id': 1,
                'parameter_values': [
                    {
                        'param_id': 777,
                        'value_type': ValueType.BOOLEAN,
                        'bool_value': True
                    },
                    {
                        'param_id': 778,
                        'value_type': ValueType.STRING,
                        'str_value': [{
                            'isoCode': 'ru',
                            'value': 'SomeValue'
                        }]
                    },
                ]
            }
        }
    },
    # Новая msku
    {
        'id': 2,
        'mbo_content': {
            'meta': ts_to_meta(NEW_DATE_TS),
            'msku': {
                'current_type': 'SKU',
                'id': 2,
                'parameter_values': [
                    {
                        'param_id': 100,
                        'value_type': ValueType.BOOLEAN,
                        'bool_value': True
                    }
                ]
            }
        }
    },
    # Обновление, но более старыми данными чем в хранилище. Эти обновления не должны примениться
    {
        'id': 3,
        'mbo_content': {
            'meta': ts_to_meta(VERY_OLD_DATE_TS),
            'msku': {
                'current_type': 'SKU',
                'id': 3,
                'parameter_values': [
                    {
                        'param_id': 101,
                        'value_type': ValueType.BOOLEAN,
                        'bool_value': True
                    }
                ]
            }
        }
    }
] + [
    {
        'id': i,
        'mbo_content': {
            'meta': ts_to_meta(NEW_DATE_TS),
            'msku': {
                'current_type': msku_current_type,
                'id': i,
                'parameter_values': [
                    {
                        'param_id': i,
                        'value_type': ValueType.BOOLEAN,
                        'bool_value': True
                    }
                ]
            }
        }
    } for msku_current_type, i in zip(ALLOWED_MSKU_CURRENT_TYPES, range(333, 333 + len(ALLOWED_MSKU_CURRENT_TYPES)))
] + [
    {
        'id': i,
        'mbo_content': {
            'meta': ts_to_meta(NEW_DATE_TS),
            'msku': {
                'current_type': msku_current_type,
                'id': i,
                'parameter_values': [
                    {
                        'param_id': i,
                        'value_type': ValueType.BOOLEAN,
                        'bool_value': True
                    }
                ]
            }
        }
    } for msku_current_type, i in zip(NOT_ALLOWED_MSKU_CURRENT_TYPES, range(666, 666 + len(NOT_ALLOWED_MSKU_CURRENT_TYPES)))
]


# То, что есть в хранилище
@pytest.fixture(scope='module')
def datacamp_msku_table_data():
    return [
        {
            'id': 1,
            'status': ModelStatus(abo_status=AboHidingReason(reason=AboReason.FAULTY)).SerializeToString(),
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=1,
                    category_id=101,
                    vendor_id=442,
                    parameter_values=[
                        ParameterValue(
                            param_id=77,
                            value_type=ValueType.STRING,
                            xsl_name='BarCode',
                            str_value=[
                                LocalizedString(
                                    isoCode='ru',
                                    value='123',
                                ),
                                LocalizedString(
                                    isoCode='en',
                                    value='45',
                                ),
                            ]
                        )
                    ]
                ),
                meta=create_update_meta(ts_to_seconds(OLD_DATE_TS))
            ).SerializeToString()
        },
        {
            'id': 3,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=3
                ),
                meta=create_update_meta(ts_to_seconds(OLD_DATE_TS))
            ).SerializeToString()
        },
    ]


@pytest.fixture(scope='module')
def datacamp_msku_table(config, yt_server, datacamp_msku_table_data):
    return DataCampMskuTable(
        yt_server,
        config.yt_datacamp_msku_tablepath,
        data=datacamp_msku_table_data
    )


@pytest.fixture(scope='module')
def mskus_datacamp_messages():
    return [message_from_data({'market_skus': {'msku': MSKUS_TO_TOPIC}}, DatacampMessage())]


@pytest.fixture(scope='module')
def msku_content_input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='module')
def config(yt_server, log_broker_stuff, msku_content_input_topic):
    cfg = {
        'logbroker': {
            'mdm_mbo_msku_content_topic': msku_content_input_topic.topic
        },
        'general': {
            'color': 'white',
        },
        'features': {
            'msku_allowed_current_types': ';'.join(ALLOWED_MSKU_CURRENT_TYPES)
        },
    }

    return PiperConfigMock(yt_server=yt_server,
                           log_broker_stuff=log_broker_stuff,
                           config=cfg)


@pytest.yield_fixture(scope='module')
def piper(yt_server, log_broker_stuff, config, msku_content_input_topic, datacamp_msku_table):
    resources = {
        'config': config,
        'mdm_mbo_msku_content_topic': msku_content_input_topic,
        'datacamp_msku_table': datacamp_msku_table
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='module')
def main_msku_inserter(mskus_datacamp_messages, piper, msku_content_input_topic):
    for message in mskus_datacamp_messages:
        msku_content_input_topic.write(message.SerializeToString())

    wait_until(lambda: piper.msku_processed >= len(mskus_datacamp_messages), timeout=60)


def test_msku_read_update_from_topic(main_msku_inserter, piper, datacamp_msku_table):
    # Напечатаем ка табличку в хранилище после того как всё накатили
    print_datacamp_msku_table(datacamp_msku_table)

    datacamp_msku_table.load()
    assert_that(len(datacamp_msku_table.data), equal_to(3 + len(ALLOWED_MSKU_CURRENT_TYPES)))
    # mbo_content заменяется полностью - в топик приходит полностью готовая msku
    assert_that(datacamp_msku_table.data, HasDatacampMskuRows([
        {
            'id': 1,
            'status': IsSerializedProtobuf(ModelStatus, {'abo_status': {'reason': DTC.FAULTY}}),  # Проверяем, что обновление не потёрло статус
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': {
                    'timestamp': {
                        'seconds': ts_to_seconds(NEW_DATE_TS)
                    }
                },
                'msku': {
                    'id': 1,
                    'parameter_values': [
                        {
                            'param_id': 777,
                            'value_type': ValueType.BOOLEAN,
                            'bool_value': True
                        },
                        {
                            'param_id': 778,
                            'value_type': ValueType.STRING,
                            'str_value': [{
                                'isoCode': 'ru',
                                'value': 'SomeValue'
                            }]
                        },
                    ]
                }
            })
        },
        {
            'id': 2,
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': {
                    'timestamp': {
                        'seconds': ts_to_seconds(NEW_DATE_TS)
                    }
                },
                'msku': {
                    'id': 2,
                    'parameter_values': [
                        {
                            'param_id': 100,
                            'value_type': ValueType.BOOLEAN,
                            'bool_value': True
                        }
                    ]
                }
            })
        },
        {
            'id': 3,
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': {
                    'timestamp': {
                        'seconds': ts_to_seconds(OLD_DATE_TS)
                    }
                },
                'msku': {
                    'id': 3
                }
            })
        },
    ] + [
        {
            'id': i,
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': {
                    'timestamp': {
                        'seconds': ts_to_seconds(NEW_DATE_TS)
                    }
                },
                'msku': {
                    'id': i,
                    'parameter_values': [
                        {
                            'param_id': i,
                            'value_type': ValueType.BOOLEAN,
                            'bool_value': True
                        }
                    ]
                }
            })
        } for msku_current_type, i in zip(ALLOWED_MSKU_CURRENT_TYPES, range(333, 333 + len(NOT_ALLOWED_MSKU_CURRENT_TYPES)))
    ]))

    assert_that(datacamp_msku_table.data, not_(HasDatacampMskuRows([
        {
            'id': i,
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': ts_to_meta(NEW_DATE_TS),
                'msku': {
                    'current_type': msku_current_type,
                    'id': i,
                    'parameter_values': [
                        {
                            'param_id': i,
                            'value_type': ValueType.BOOLEAN,
                            'bool_value': True
                        }
                    ]
                }
            })
        } for msku_current_type, i in zip(NOT_ALLOWED_MSKU_CURRENT_TYPES, range(666, 666 + len(NOT_ALLOWED_MSKU_CURRENT_TYPES)))
    ])))


def print_datacamp_msku_table(datacamp_msku_table):
    log = logging.getLogger('')
    o_table_column_proto = {
        'status': ModelStatus,
        'mbo_content': MarketSkuMboContent
    }
    o_table_column_widths = {
        'status': 50,
        'mbo_content': 100
    }
    o_table_column_alignment = {
        'status': 'l',
        'mbo_content': 'l'
    }
    datacamp_msku_table.load()
    log.info('datacamp_msku_table\n{}\n{}'.format(datacamp_msku_table._path,
                                                  rows_as_table(datacamp_msku_table.data,
                                                                column_to_proto_type_dict=o_table_column_proto,
                                                                column_widths_dict=o_table_column_widths,
                                                                column_alignment_dict=o_table_column_alignment)))
