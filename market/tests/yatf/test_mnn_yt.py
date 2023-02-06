# coding=utf-8
import pytest

import market.proto.content.mbo.ExportReportModel_pb2 as ExportReportModel_pb2

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    LocalizedString,
    ParameterValue,
    Relation,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
    Word,
    Option,
    ValueType,
)

import market.proto.feedparser.deprecated.OffersData_pb2
import market.proto.ir.UltraController_pb2

from msku_uploader.yatf.test_env import MskuUploaderTestEnv
from msku_uploader.yatf.resources.mbo_yt import (
    MboAllModelsTable,
    MboModelsTable,
    MboParamsTable,
    MboSkuTable,
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix


ENUM_TYPE = ValueType.Value('ENUM')
BOOLEAN_TYPE = ValueType.Value('BOOLEAN')
NUMERIC_TYPE = ValueType.Value('NUMERIC')
NUMERIC_ENUM_TYPE = ValueType.Value('NUMERIC_ENUM')
STRING_TYPE = ValueType.Value('STRING')


class MboParamCreater(object):
    def get_options(self):
        if self.__value_type in [ENUM_TYPE, NUMERIC_ENUM_TYPE]:
            for option_id, value in enumerate(self.__values):
                yield Option(
                    id=option_id,
                    name=[Word(
                        name=value,
                    )],
                )

    @staticmethod
    def get_bool_param(param_id, xsl_name, value, values):
        return ParameterValue(
            param_id=param_id,
            xsl_name=xsl_name,
            type_id=1,
            value_type=BOOLEAN_TYPE,
            bool_value=value,
        )

    @staticmethod
    def get_enum_param(param_id, xsl_name, value, values):
        return ParameterValue(
            param_id=param_id,
            xsl_name=xsl_name,
            type_id=2,
            value_type=ENUM_TYPE,
            option_id=values.index(value),
        )

    @staticmethod
    def get_numeric_param(param_id, xsl_name, value, values):
        return ParameterValue(
            param_id=param_id,
            xsl_name=xsl_name,
            type_id=3,
            value_type=NUMERIC_TYPE,
            numeric_value=str(value),
        )

    @staticmethod
    def get_numeric_enum_param(param_id, xsl_name, value, values):
        return ParameterValue(
            param_id=param_id,
            xsl_name=xsl_name,
            type_id=4,
            value_type=NUMERIC_ENUM_TYPE,
            option_id=values.index(value),
        )

    @staticmethod
    def get_string_param(param_id, xsl_name, value, values):
        return ParameterValue(
            param_id=param_id,
            xsl_name=xsl_name,
            type_id=5,
            value_type=STRING_TYPE,
            str_value=[LocalizedString(value=str(value))],
        )

    def category(self):
        return Parameter(
            id=self.__param_id,
            xsl_name=self.__xsl_name,
            value_type=self.__value_type,
            option=[item for item in self.get_options()],
        )

    def model(self, value):
        getter_by_type = {
            BOOLEAN_TYPE:      MboParamCreater.get_bool_param,
            ENUM_TYPE:         MboParamCreater.get_enum_param,
            NUMERIC_TYPE:      MboParamCreater.get_numeric_param,
            NUMERIC_ENUM_TYPE: MboParamCreater.get_numeric_enum_param,
            STRING_TYPE:       MboParamCreater.get_string_param,
        }
        return getter_by_type[self.__value_type](self.__param_id, self.__xsl_name, value, self.__values)

    def __init__(self, param_id, xsl_name, value_type, values=[]):
        if not isinstance(values, list):
            values = [values]

        self.__param_id = param_id
        self.__value_type = value_type
        self.__xsl_name = xsl_name
        self.__values = values


MNN_PARAM_ID = 1001
NOT_MNN_PARAM_ID = 1002


@pytest.fixture(scope='module')
def category_source():
    return {
        # Категория с параметром mnn типа enum
        1: {
            'models': {
                11: {
                    'title': 'Первая модель с mnn типа enum',
                    'mnn_value': 'FIRST_ENUM_MNN_VALUE',            # Это значение будет добавлено к поиску
                    'not_mnn_value': 'FIRST_ENUM_NOT_MNN_VALUE',    # А это не будет
                    'skus': {
                        111: 'Первое СКУ с mnn типа enum',
                        112: 'Второе СКУ с mnn типа enum',
                    }
                },
                12: {
                    'title': 'Вторая модель с mnn типа enum',
                    'mnn_value': 'SECOND_ENUM_MNN_VALUE',           # Это значение будет добавлено к поиску
                    'not_mnn_value': 'SECOND_ENUM_NOT_MNN_VALUE',   # А это не будет
                    'skus': {
                        121: 'Третье СКУ с mnn типа enum',
                        122: 'Четвертое СКУ с mnn типа enum',
                    }
                }
            },
            'mnn_params_builder': MboParamCreater(
                param_id=MNN_PARAM_ID,
                xsl_name='mnn',
                value_type=ENUM_TYPE,
                values=['FIRST_ENUM_MNN_VALUE', 'SECOND_ENUM_MNN_VALUE']
            ),
            'not_mnn_params_builder': MboParamCreater(
                param_id=NOT_MNN_PARAM_ID,
                xsl_name='not_mnn',
                value_type=ENUM_TYPE,
                values=['FIRST_ENUM_NOT_MNN_VALUE', 'SECOND_ENUM_NOT_MNN_VALUE']
            ),
        },
        # Категория с параметром mnn типа string
        2: {
            'models': {
                21: {
                    'title': 'Модель с mnn типа string',
                    'mnn_value': 'STRING_MNN_VALUE',            # Это значение будет добавлено к поиску
                    'not_mnn_value': 'STRING_ENUM_NOT_MNN_VALUE',    # А это не будет
                    'skus': {
                        211: 'Первое СКУ с mnn типа string',
                        212: 'Второе СКУ с mnn типа string',
                    }
                },
            },
            'mnn_params_builder': MboParamCreater(
                param_id=MNN_PARAM_ID,
                xsl_name='mnn',
                value_type=STRING_TYPE,
            ),
            'not_mnn_params_builder': MboParamCreater(
                param_id=NOT_MNN_PARAM_ID,
                xsl_name='not_mnn',
                value_type=STRING_TYPE,
            ),
        },
        # Категория с параметром mnn типа boolean. Эти значения не добавляются к поиску
        3: {
            'models': {
                31: {
                    'title': 'Первая модель с mnn типа boolean',
                    'mnn_value': True,
                    'not_mnn_value': True,
                    'skus': {
                        311: 'СКУ с mnn типа boolean',
                    }
                },
                32: {
                    'title': 'Вторая модель с mnn типа boolean',
                    'mnn_value': False,
                    'not_mnn_value': False,
                    'skus': {
                        321: 'СКУ с mnn типа boolean',
                    }
                }
            },
            'mnn_params_builder': MboParamCreater(
                param_id=MNN_PARAM_ID,
                xsl_name='mnn',
                value_type=BOOLEAN_TYPE,
            ),
            'not_mnn_params_builder': MboParamCreater(
                param_id=NOT_MNN_PARAM_ID,
                xsl_name='not_mnn',
                value_type=BOOLEAN_TYPE,
            ),
        },
        # Категория с параметром mnn типа numeric. Эти значения не будут добавлены к поиску
        4: {
            'models': {
                41: {
                    'title': 'Модель с mnn типа numeric',
                    'mnn_value': 4000,
                    'not_mnn_value': 4001,
                    'skus': {
                        411: 'СКУ с mnn типа numeric',
                    }
                },
            },
            'mnn_params_builder': MboParamCreater(
                param_id=MNN_PARAM_ID,
                xsl_name='mnn',
                value_type=NUMERIC_TYPE,
            ),
            'not_mnn_params_builder': MboParamCreater(
                param_id=NOT_MNN_PARAM_ID,
                xsl_name='not_mnn',
                value_type=NUMERIC_TYPE,
            ),
        },
        # Категория без параметра mnn
        5: {
            'models': {
                51: {
                    'title': 'Модель без mnn',
                    'mnn_value': None,
                    'not_mnn_value': None,
                    'skus': {
                        511: 'СКУ без mnn',
                    }
                },
            },
            'mnn_params_builder': None,
            'not_mnn_params_builder': None,
        },
    }


def get_mbo_param_builders(category_prefs):
    result = []

    mnn_param_builder = category_prefs.get('mnn_params_builder')
    if mnn_param_builder:
        result.append(mnn_param_builder)

    not_mnn_param_builder = category_prefs.get('not_mnn_params_builder')
    if not_mnn_param_builder:
        result.append(not_mnn_param_builder)

    return result


def create_category_params(category_prefs):
    return [
        builder.category()
        for builder in get_mbo_param_builders(category_prefs)
    ]


def create_model_params(category_prefs, model_prefs):
    values = [
        model_prefs.get('mnn_value'),
        model_prefs.get('not_mnn_value'),
    ]
    return [
        builder.model(value)
        for builder, value in zip(get_mbo_param_builders(category_prefs), values)
    ]


def mbo_category_protobufs(category_id, category_prefs):
    yield (category_id, Category(
        hid=category_id,
        parameter=create_category_params(category_prefs)
    ))


def mbo_models_protobufs(category_id, category_prefs):
    for model_id, model_prefs in list(category_prefs.get('models', {}).items()):
        yield (category_id, ExportReportModel(
            id=model_id,
            category_id=category_id,
            titles=[LocalizedString(isoCode='ru', value=model_prefs.get('title', ''))],
            parameter_values=create_model_params(category_prefs, model_prefs),
            published_on_blue_market=True,
        ))


def mbo_msku_protobufs(category_id, category_prefs):
    for model_id, model_prefs in list(category_prefs.get('models', {}).items()):
        for sku_id, sku_title in list(model_prefs['skus'].items()):
            yield (category_id, ExportReportModel(
                id=sku_id,
                category_id=category_id,
                vendor_id=966973,
                published_on_blue_market=True,
                current_type='SKU',
                relations=[
                    Relation(
                        id=model_id,
                        category_id=category_id,
                        type=ExportReportModel_pb2.SKU_PARENT_MODEL
                    ),
                ],
                titles=[LocalizedString(isoCode='ru', value=sku_title)],
            ))


@pytest.fixture(scope='module')
def yt_dir():
    return get_yt_prefix()


@pytest.fixture(scope='module')
def mbo_sku_table(
        yt_server,
        yt_dir,
        category_source
):
    sku_pbs = []
    for cat, category_prefs in list(category_source.items()):
        sku_pbs += list(mbo_msku_protobufs(cat, category_prefs))

    return MboSkuTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': category_id,
                'data': sku_pb.SerializeToString(),
            }
            for category_id, sku_pb in sku_pbs
        ]
    )


@pytest.fixture(scope='module')
def mbo_models_table(
    yt_server,
    yt_dir,
    category_source
):
    model_pbs = []
    for cat, category_prefs in list(category_source.items()):
        model_pbs += list(mbo_models_protobufs(cat, category_prefs))

    return MboModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': category_id,
                'data': model_pb.SerializeToString(),
            }
            for category_id, model_pb in model_pbs
        ]
    )


@pytest.fixture(scope='module')
def mbo_all_models_table(
    yt_server,
    yt_dir,
    category_source
):
    model_pbs = []
    sku_pbs = []
    for cat, category_prefs in list(category_source.items()):
        model_pbs += list(mbo_models_protobufs(cat, category_prefs))
        sku_pbs += list(mbo_msku_protobufs(cat, category_prefs))

    return MboAllModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'data': pb.SerializeToString(),
            }
            for _, pb in model_pbs + sku_pbs
        ]
    )


@pytest.fixture(scope='module')
def mbo_params_table(
    yt_server,
    yt_dir,
    category_source
):
    cat_pbs = []
    for cat, category_prefs in list(category_source.items()):
        cat_pbs += list(mbo_category_protobufs(cat, category_prefs))

    return MboParamsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'hid': hid,
                'data': category_pb.SerializeToString()
            }
            for hid, category_pb in cat_pbs
        ]
    )


@pytest.yield_fixture(scope='module')
def workflow(
    yt_server,
    mbo_all_models_table,
    mbo_models_table,
    mbo_params_table,
    mbo_sku_table,
    yt_dir,
):
    resources = {
        'mbo_all_models_table': mbo_all_models_table,
        'mbo_models_table': mbo_models_table,
        'mbo_params_table': mbo_params_table,
        'mbo_sku_table': mbo_sku_table,
    }

    with MskuUploaderTestEnv(yt_input_dir=yt_dir, **resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


@pytest.mark.parametrize('sku, value', [
    # СКУ первой модели типа enum
    (111, ['FIRST_ENUM_MNN_VALUE']),
    (112, ['FIRST_ENUM_MNN_VALUE']),  # Значение добавилось к каждому СКУ модели
    # СКУ второй модели типа enum
    (121, ['SECOND_ENUM_MNN_VALUE']),
    (122, ['SECOND_ENUM_MNN_VALUE']),

    # СКУ строкового значения mnn
    (211, ['STRING_MNN_VALUE']),
    (212, ['STRING_MNN_VALUE']),

    # У остальных СКУ нет дополнительного поискового значения
])
def test_additional_search_text(result_yt_table, sku, value):
    for result in result_yt_table.data:
        fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
        fact_offer.ParseFromString(result["offer"])
        if result['offer_id'] == 'MS{}'.format(sku) and fact_offer.genlog.additional_search_text == value[0]:
            return
    assert False, fact_offer.genlog.additional_search_text


@pytest.mark.parametrize('sku', [
    # boolean - запрещены к добавлению
    311, 321,
    # numeric - запрещены к добавлению
    411,
    # without mnn
    511,
])
def test_not_additional_search_text(result_yt_table, sku):
    for result in result_yt_table.data:
        fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
        fact_offer.ParseFromString(result["offer"])
        if result['offer_id'] == 'MS{}'.format(sku) and not fact_offer.genlog.additional_search_text:
            return
    assert False
