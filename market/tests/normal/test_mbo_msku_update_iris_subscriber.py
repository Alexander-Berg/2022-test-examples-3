# coding: utf-8
import pytest

from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    ValueType
)
from market.idx.datacamp.proto.models.MarketSkuMboContent_pb2 import MarketSkuMboContent
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from hamcrest import assert_that
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

# Дата того, что лежит в хранилище
OLD_DATE_TS = 1618763231577
# Дата обновления
NEW_DATE_TS = 1618764231000

IRIS_SUBSCRIPTION_PARAMETER_ID=15838982
NOT_IRIS_SUBSCRIPTION_PARAMETER_ID=888
CARGO_TYPE_PARAMETER_ID=16619527
CARGO_TYPE_XSL_NAME='cargoType970'


def ts_to_seconds(ts):
    return ts / 1000


# То, что есть в хранилище
@pytest.fixture(scope='module')
def datacamp_msku_table_data():
    return [
        {
            'id': 1,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=1,
                    export_ts=OLD_DATE_TS,
                    parameter_values=[
                        ParameterValue(
                            param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
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
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
        {
            'id': 3,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=3,
                    export_ts=OLD_DATE_TS,
                    parameter_values=[
                        ParameterValue(
                            param_id=NOT_IRIS_SUBSCRIPTION_PARAMETER_ID,
                            value_type=ValueType.BOOLEAN,
                            bool_value=False
                        )
                    ]
                ),
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
        {
            'id': 4,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=4,
                    export_ts=OLD_DATE_TS,
                    parameter_values=[
                        ParameterValue(
                            param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
                            value_type=ValueType.BOOLEAN,
                            bool_value=True
                        ),
                    ]
                ),
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
        {
            'id': 5,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=5,
                    export_ts=OLD_DATE_TS,
                    parameter_values=[
                        ParameterValue(
                            param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
                            value_type=ValueType.STRING,
                            xsl_name='BarCode',
                            str_value=[
                                LocalizedString(
                                    isoCode='ru',
                                    value='123000',
                                )
                            ]
                        ),
                    ]
                ),
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
        {
            'id': 6,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=6,
                    export_ts=OLD_DATE_TS,
                    parameter_values=[]
                ),
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
    ]


# То, что накатываем
@pytest.fixture(scope='module')
def mbo_msku_table_data():
    return [
        # Изменился параметр iris подписки
        {
            'model_id': 1,
            'data': ExportReportModel(
                id=1,
                export_ts=NEW_DATE_TS,
                category_id=101,
                parameter_values=[
                    ParameterValue(
                        param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
                        value_type=ValueType.STRING,
                        xsl_name='BarCode',
                        str_value=[
                            LocalizedString(
                                isoCode='ru',
                                value='123000',  # Изменено значение параметра на который подписан IRIS
                            ),
                            LocalizedString(
                                isoCode='en',
                                value='45',
                            ),
                        ]
                    ),
                    ParameterValue(  # Добавляем параметр
                        param_id=NOT_IRIS_SUBSCRIPTION_PARAMETER_ID,
                        value_type=ValueType.STRING,
                        xsl_name='SomeParameter',
                        str_value=[
                            LocalizedString(
                                isoCode='ru',
                                value='some_value_0'
                            )
                        ]
                    ),
                ]
            ).SerializeToString()
        },
        # Добавленная msku с параметром из iris подписки
        {
            'model_id': 2,
            'data': ExportReportModel(
                id=2,
                published_on_market=True,
                export_ts=NEW_DATE_TS,
                parameter_values=[
                    ParameterValue(
                        param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
                        value_type=ValueType.STRING,
                        xsl_name='BarCode',
                        str_value=[
                            LocalizedString(
                                isoCode='ru',
                                value='777000'
                            )
                        ]
                    )
                ]
            ).SerializeToString()
        },
        # Добавился параметр из msku-подписки
        {
            'model_id': 3,
            'data': ExportReportModel(
                id=3,
                export_ts=NEW_DATE_TS,
                parameter_values=[
                    ParameterValue(
                        param_id=NOT_IRIS_SUBSCRIPTION_PARAMETER_ID,
                        value_type=ValueType.BOOLEAN,
                        bool_value=False
                    ),
                    ParameterValue(
                        param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
                        value_type=ValueType.BOOLEAN,
                        bool_value=True
                    ),
                ]
            ).SerializeToString()
        },
        {
            # Удалилась msku с параметами из подписки
            'model_id': 4,
            'data': ExportReportModel(
                id=4,
                export_ts=NEW_DATE_TS
            ).SerializeToString(),
            'deleted_date': '2021-05-25T20:26:08.784+03:00'
        },
        {
            # Параметры не поменялись
            'model_id': 5,
            'data': ExportReportModel(
                id=5,
                vendor_id=111,  # Обновлённое поле
                export_ts=NEW_DATE_TS,
                parameter_values=[
                    ParameterValue(
                        param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
                        value_type=ValueType.STRING,
                        xsl_name='BarCode',
                        str_value=[
                            LocalizedString(
                                isoCode='ru',
                                value='123000',
                            )
                        ]
                    ),
                ]
            ).SerializeToString(),
        },
        # добавился карготипный параметр и для iris
        {
            'model_id': 6,
            'data': ExportReportModel(
                id=6,
                export_ts=NEW_DATE_TS,
                parameter_values=[
                    ParameterValue(
                        param_id=CARGO_TYPE_PARAMETER_ID,
                        bool_value=True,
                        xsl_name=CARGO_TYPE_XSL_NAME
                    ),
                    ParameterValue(
                        param_id=IRIS_SUBSCRIPTION_PARAMETER_ID,
                        bool_value=True
                    ),
                ]
            ).SerializeToString()
        },
    ]


@pytest.fixture(scope='module')
def scanner(
        log_broker_stuff,
        yt_server,
        scanner_resources,
        color,
):
    with make_scanner(yt_server, log_broker_stuff, color, shopsdat_cacher=True, **scanner_resources) as scanner_env:
        yield scanner_env


@pytest.yield_fixture(scope='module')
def output_messages(iris_subscription_internal_topic):
    # YtMboMskuTableReader.BatchSize контролирует сколько msku будет в сообщении от апдейтера.
    # Там стоит число явно больше нашего количество msku, так что всё придёт в одном сообщении.
    return iris_subscription_internal_topic.read(1, wait_timeout=30)


def test_output_messages(scanner, output_messages):
    m = output_messages[0]

    o = DatacampMessage()
    o.ParseFromString(m)
    print('Got message: {}'.format(o))

    assert_that(m, IsSerializedProtobuf(DatacampMessage, {
        'market_skus': {
            'msku': [
                {
                    'id': 1,
                    'mbo_content': {
                        'meta': {
                            'timestamp': {
                                'seconds': ts_to_seconds(NEW_DATE_TS)
                            }
                        },
                        'msku': {
                            'id': 1,
                            'category_id': 101,
                            'parameter_values': [
                                {
                                    'param_id': IRIS_SUBSCRIPTION_PARAMETER_ID,
                                    'str_value': [
                                        {
                                            'isoCode': 'ru',
                                            'value': '123000'
                                        },
                                        {
                                            'isoCode': 'en',
                                            'value': '45'
                                        }
                                    ],
                                    'xsl_name': 'BarCode',
                                    'value_type': ValueType.STRING
                                },
                                {
                                    'param_id': NOT_IRIS_SUBSCRIPTION_PARAMETER_ID,
                                    'str_value': [
                                        {
                                            'isoCode': 'ru',
                                            'value': 'some_value_0'
                                        }
                                    ],
                                    'xsl_name': 'SomeParameter',
                                    'value_type': ValueType.STRING
                                }
                            ],
                            'export_ts': NEW_DATE_TS
                        }
                    }
                },
                {
                    'id': 2,
                    'mbo_content': {
                        'meta': {
                            'timestamp': {
                                'seconds': ts_to_seconds(NEW_DATE_TS)
                            }
                        },
                        'msku': {
                            'id': 2,
                            'parameter_values': [
                                {
                                    'param_id': IRIS_SUBSCRIPTION_PARAMETER_ID,
                                    'str_value': [
                                        {
                                            'isoCode': 'ru',
                                            'value': '777000'
                                        }
                                    ],
                                    'xsl_name': 'BarCode',
                                    'value_type': ValueType.STRING,
                                }
                            ],
                            'export_ts': NEW_DATE_TS,
                            'published_on_market': True
                        }
                    }
                },
                {
                    'id': 3,
                    'mbo_content': {
                        'meta': {
                            'timestamp': {
                                'seconds': ts_to_seconds(NEW_DATE_TS)
                            }
                        },
                        'msku': {
                            'id': 3,
                            'export_ts': NEW_DATE_TS,
                            'parameter_values': [
                                {
                                    'param_id': NOT_IRIS_SUBSCRIPTION_PARAMETER_ID,
                                    'bool_value': False,
                                    'value_type': ValueType.BOOLEAN,
                                },
                                {
                                    'param_id': IRIS_SUBSCRIPTION_PARAMETER_ID,
                                    'bool_value': True,
                                    'value_type': ValueType.BOOLEAN,
                                }
                            ]
                        }
                    }
                },
                {
                    'id': 4,
                    'mbo_content': {
                        'meta': {
                            'timestamp': {
                                'seconds': ts_to_seconds(NEW_DATE_TS)
                            },
                            'removed': True
                        },
                        'msku': {
                            'id': 4,
                            'export_ts': NEW_DATE_TS,
                        }
                    }
                },
                {
                    'id': 6,
                    'mbo_content': {
                        'meta': {
                            'timestamp': {
                                'seconds': ts_to_seconds(NEW_DATE_TS)
                            }
                        },
                        'msku': {
                            'id': 6,
                            'export_ts': NEW_DATE_TS,
                            'parameter_values': [
                                {
                                    'param_id': CARGO_TYPE_PARAMETER_ID,
                                    'bool_value': True,
                                    'xsl_name': CARGO_TYPE_XSL_NAME
                                },
                                {
                                    'param_id': IRIS_SUBSCRIPTION_PARAMETER_ID,
                                    'bool_value': True
                                }
                            ]
                        }
                    },
                    'msku_route_flags': 3  # 1 1 = iris + cargo_type
                },
            ]
        }
    }))
