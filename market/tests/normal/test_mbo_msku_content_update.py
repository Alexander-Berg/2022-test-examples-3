# coding: utf-8
import pytest
import logging

from hamcrest import assert_that, equal_to
from market.idx.datacamp.controllers.scanner.yatf.test_env import make_scanner
from market.idx.pylibrary.datacamp.utils import wait_until
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    ValueType
)

from market.idx.datacamp.proto.models.MarketSkuMboContent_pb2 import MarketSkuMboContent
from market.idx.datacamp.proto.models.ModelStatus_pb2 import AboHidingReason, ModelStatus
from market.idx.datacamp.proto.errors.ValidationResult_pb2 import AboReason
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampMskuRows
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.yatf.utils.utils import rows_as_table

# Дата того, что лежит в хранилище
OLD_DATE_TS = 1618763231577
# Дата обновления
NEW_DATE_TS = 1618764231000
# Дата обновления для одной из msku - более старая чем то, что лежит в хранилище
VERY_OLD_DATE_TS = 1618734231000


def ts_to_seconds(ts):
    return ts // 1000


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
                    export_ts=OLD_DATE_TS,  # Sun Apr 18 19:27:11 MSK 2021
                    category_id=101,
                    vendor_id=442,
                    current_type='GURU',
                    published_on_market=True,
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
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
        {
            'id': 3,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=3,
                    export_ts=OLD_DATE_TS
                ),
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
        {
            'id': 4,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=4,
                    export_ts=OLD_DATE_TS
                ),
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        },
        {
            'id': 5,
            'mbo_content': MarketSkuMboContent(
                msku=ExportReportModel(
                    id=5,
                    vendor_id=442,
                    export_ts=OLD_DATE_TS
                ),
                meta=create_update_meta(1618763231)
            ).SerializeToString()
        }
    ]


# То, что накатываем
@pytest.fixture(scope='module')
def mbo_msku_table_data():
    return [
        # Обновлённая msku
        {
            'model_id': 1,
            'data': ExportReportModel(
                id=1,
                export_ts=NEW_DATE_TS,  # Sun Apr 18 19:43:51 MSK 2021
                category_id=101,
                # vendor_id=442, Убрали поле
                current_type='GURU',
                published_on_market=True,
                parameter_values=[
                    ParameterValue(
                        param_id=77,
                        value_type=ValueType.STRING,
                        xsl_name='BarCode',
                        str_value=[
                            LocalizedString(
                                isoCode='ru',
                                value='123000',  # Меняем значение параметра
                            ),
                            LocalizedString(
                                isoCode='en',
                                value='45',
                            ),
                        ]
                    ),
                    ParameterValue(  # Добавляем параметр
                        param_id=78,
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
        # Добавленная msku
        {
            'model_id': 2,
            'data': ExportReportModel(
                id=2,
                published_on_market=True,
                export_ts=NEW_DATE_TS,
                parameter_values=[
                    ParameterValue(
                        param_id=77,
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
        {
            # Маркируем msku как удалённую через флаг deleted
            'model_id': 3,
            'data': ExportReportModel(
                id=3,
                export_ts=NEW_DATE_TS,
                deleted=True  # Удаление!
            ).SerializeToString()
        },
        {
            # Маркируем msku как удалённую через строчку deleted_date
            'model_id': 4,
            'data': ExportReportModel(
                id=4,
                export_ts=NEW_DATE_TS
            ).SerializeToString(),
            'deleted_date': '2021-05-25T20:26:08.784+03:00'
        },
        {
            # В обновлении более старые данные!
            'model_id': 5,
            'data': ExportReportModel(
                id=5,
                vendor_id=111,  # Обновлённое поле
                export_ts=VERY_OLD_DATE_TS  # Sun Apr 18 11:23:51 MSK 2021
            ).SerializeToString(),
        }
    ]


@pytest.fixture(scope='module')
def scanner(
        log_broker_stuff,
        yt_server,
        scanner_resources,
        color,
):
    with make_scanner(
        yt_server,
        log_broker_stuff,
        color,
        shopsdat_cacher=True,
        **scanner_resources
    ) as scanner_env:
        yield scanner_env


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
    log.info('datacamp_msku_table:\n{}'.format(
        rows_as_table(
            datacamp_msku_table.data,
            column_to_proto_type_dict=o_table_column_proto,
            column_widths_dict=o_table_column_widths,
            column_alignment_dict=o_table_column_alignment
        )
    ))


def test_update_mbo_content(scanner, datacamp_msku_table):
    # проверяем, что сканнер вычитывает msku таблицу от abo и обновляет таблицу хранилища
    wait_until(lambda: scanner.mskus_processed > 0, timeout=10)
    print_datacamp_msku_table(datacamp_msku_table)

    datacamp_msku_table.load()
    assert_that(len(datacamp_msku_table.data), equal_to(5))
    assert_that(datacamp_msku_table.data, HasDatacampMskuRows([
        {
            'id': 1,
            'status': IsSerializedProtobuf(ModelStatus, {
                'abo_status': {
                    'reason': DTC.FAULTY
                }
            }),  # Проверяем, что обновление не потёрло статус
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': {
                    'timestamp': {
                        'seconds': 1618764231
                    }
                },
                'msku': {
                    'id': 1,
                    'export_ts': NEW_DATE_TS,
                    'category_id': 101,
                    'current_type': 'GURU',
                    'published_on_market': True,
                    'parameter_values': [
                        {
                            'param_id': 77,
                            'value_type': ValueType.STRING,
                            'xsl_name': 'BarCode',
                            'str_value': [
                                {
                                    'isoCode': 'ru',
                                    'value': '123000'
                                },
                                {
                                    'isoCode': 'en',
                                    'value': '45'
                                }
                            ]
                        },
                        {
                            'param_id': 78,
                            'value_type': ValueType.STRING,
                            'xsl_name': 'SomeParameter',
                            'str_value': [
                                {
                                    'isoCode': 'ru',
                                    'value': 'some_value_0'
                                }
                            ]
                        }
                    ]
                }
            })
        },
        {
            'id': 2,
            'status': IsSerializedProtobuf(ModelStatus, {}),  # А вот это беда в коде мержда, где мы делаем mutable_ и потом не ревертим свои изменения
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': {
                    'timestamp': {
                        'seconds': 1618764231
                    }
                },
                'msku': {
                    'id': 2,
                    'published_on_market': True,
                    'export_ts': NEW_DATE_TS,
                    'parameter_values': [
                        {
                            'param_id': 77,
                            'value_type': ValueType.STRING,
                            'xsl_name': 'BarCode',
                            'str_value': [
                                {
                                    'isoCode': 'ru',
                                    'value': '777000'
                                }
                            ]
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
                        'seconds': 1618764231
                    },
                    'removed': True,
                },
                'msku': {
                    'id': 3,
                    'export_ts': NEW_DATE_TS,
                    'deleted': True
                }
            })
        },
        {
            'id': 4,
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
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
            })
        },
        {
            'id': 5,
            'mbo_content': IsSerializedProtobuf(MarketSkuMboContent, {
                'meta': {
                    'timestamp': {
                        'seconds': ts_to_seconds(OLD_DATE_TS)
                    }
                },
                'msku': {
                    'id': 5,
                    'vendor_id': 442,
                    'export_ts': OLD_DATE_TS
                }
            })
        },
    ]))
