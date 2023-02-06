# coding=utf-8
from hamcrest import assert_that
import logging
import pytest

from market.idx.yatf.resources.yql_resource import YtResource, YqlRequestResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.yql_env import YqlRunnerTestEnv
from market.idx.yatf.utils.ytutil import make_schema
from market.idx.yatf.utils.utils import rows_as_table
from market.proto.indexer.yt_offer_pb2 import TOffer as OfferData

from market.idx.pylibrary.mindexer_core.ytupload.ytupload import (
    offers_table_path,
    models_table_path,
    categories_table_path,
    vendors_table_path,
)

from yt.wrapper import ypath_join

GENERATION = '19841017_0000'


class Offer(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def to_row(self):
        return self.__dict__

    @classmethod
    def table_schema(cls):
        # Это схема market.proto.indexer.yt_offer.TOffer
        schema = make_schema(OfferData)
        return schema


COMMON_OFFERS_DATA = [
    Offer(ware_md5='0', model_id=0),
    Offer(ware_md5='1', model_id=0),
    Offer(ware_md5='2', model_id=0),
    Offer(ware_md5='3', model_id=0)
]


@pytest.fixture(scope='module')
def offers_data():
    return [o.to_row() for o in COMMON_OFFERS_DATA]


class Model(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def to_row(self):
        return self.__dict__

    @classmethod
    def table_schema(cls):
        schema = [
            dict(name='id', type='int64'),
            dict(name='parent_id', type='int64'),
            dict(name='category_id', type='int64'),
            dict(name='vendor_id', type='int64'),
            dict(name='current_type', type='string'),
            dict(name='created_date', type='string'),
            dict(name='experiment_flag', type='string'),
            dict(name='group_size', type='int64'),
            dict(name='micro_model_search', type='string'),
            dict(name='published_on_market', type='boolean'),
            dict(name='published_on_blue_market', type='boolean'),
            dict(name='vendor_min_publish_timestamp', type='int64'),
            dict(name='title', type='string'),
            dict(name='picture', type='string'),
            dict(name='pic', type='string'),
            dict(name='aliases', type='string'),
            dict(name='typePrefix', type='string')
        ]
        return schema


COMMON_MODELS_DATA = [
    Model(id=0, title='This is a first model', vendor_min_publish_timestamp=100, picture='http://model_picture.ru'),
    Model(id=1, title='This is a first model', vendor_min_publish_timestamp=100, picture=None),
]


@pytest.fixture(scope='module')
def models_data():
    return [o.to_row() for o in COMMON_MODELS_DATA]


@pytest.fixture(scope='module')
def models_table(yt_server, config, models_data):
    tablepath = models_table_path(config, GENERATION)
    table = YtTableResource(yt_server, tablepath, models_data, attributes={'schema': Model.table_schema()})
    table.dump()
    yt_server.get_yt_client().link(tablepath, models_table_path(config, 'recent'))

    logging.info('\n{}'.format(rows_as_table(table.data)))

    return table


class Category(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def to_row(self):
        return self.__dict__

    @classmethod
    def table_schema(cls):
        schema = [
            dict(name="hyper_id", type="int64"),
            dict(name="nid", type="int64"),
            dict(name="blue_nid", type="int64"),
            dict(name="id", type="int64"),
            dict(name="name", type="string"),
            dict(name="uniq_name", type="string"),
            dict(name="parent", type="int64"),
            dict(name="parents", type="string"),
            dict(name="type", type="string")
        ]
        return schema


COMMON_CATEGORY_DATA = [
    Model(hyper_id=0)
]


@pytest.fixture(scope='module')
def categories_data():
    return [o.to_row() for o in COMMON_CATEGORY_DATA]


@pytest.fixture(scope='module')
def categories_table(yt_server, config, categories_data):
    tablepath = categories_table_path(config, GENERATION)
    table = YtTableResource(yt_server, tablepath, categories_data, attributes={'schema': Category.table_schema()})
    table.dump()
    yt_server.get_yt_client().link(tablepath, categories_table_path(config, 'recent'))

    logging.info('\n{}'.format(rows_as_table(table.data)))

    return table


class Vendor(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def to_row(self):
        return self.__dict__

    @classmethod
    def table_schema(cls):
        schema = [
            dict(name="id", type="int64"),
            dict(name="name", type="string"),
            dict(name="site", type="string"),
            dict(name="picture", type="string"),
            dict(name="description", type="string"),
            dict(name="is-fake-vendor", type="boolean"),
        ]
        return schema


COMMON_VENDOR_DATA = [
    Vendor(id=0, name='vendor1'),
    Vendor(id=242456, name='BORK')
]


@pytest.fixture(scope='module')
def vendors_data():
    return [o.to_row() for o in COMMON_VENDOR_DATA]


@pytest.fixture(scope='module')
def vendors_table(yt_server, config, vendors_data):
    tablepath = vendors_table_path(config, GENERATION)
    table = YtTableResource(yt_server, tablepath, vendors_data, attributes={'schema': Vendor.table_schema()})
    table.dump()
    yt_server.get_yt_client().link(tablepath, vendors_table_path(config, 'recent'))

    logging.info('\n{}'.format(rows_as_table(table.data)))

    return table


class Recipe(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def to_row(self):
        return self.__dict__

    @classmethod
    def table_schema(cls):
        schema = [
            dict(name="recipe_id", type="uint64"),
            dict(name="hid", type="uint64"),
            dict(name="name", type="string"),
            dict(name="header", type="string"),
            dict(name="filters", type="string"),
        ]
        return schema


@pytest.fixture(scope='module')
def recipe_table(yt_server, config, recipe_data):
    tablepath = config.banner_recipe_white_table
    table = YtTableResource(yt_server, tablepath, recipe_data, attributes={'schema': Recipe.table_schema()})
    table.dump()

    logging.info('\n{}'.format(rows_as_table(table.data)))

    return table


# теставая табличка Mstat с парметрами моделей (обрезаная)
class MstatModel(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def to_row(self):
        return self.__dict__

    @classmethod
    def table_schema(cls):
        schema = [
            dict(name="id", type="int64"),
            dict(name="params", type="any"),
#            dict(name="params", type="string"),
#            dict(name="params", type="List"),
            dict(name="category_id", type="int64")
        ]
        return schema

MSTAT_MODEL_PARAM_PRESCRIPTION = [
    {
        'bool_value': True,
        'modification_date': 1541929720313,
        'option_id': 17641892,
        'param_id': 17641892,
        'type_id': 0,
        'user_id': 28027378,
        'value_source': 1,
        'value_type': 1,
        'xsl_name': 'prescription_b'
    }
]

MSTAT_MODEL_PARAM_PRESCRIPTION_NEW = [
    {
        'bool_value': True,
        'modification_date': 1541929720313,
        'option_id': 27272850,
        'param_id': 27272850,
        'type_id': 0,
        'user_id': 28027378,
        'value_source': 1,
        'value_type': 1,
        'xsl_name': 'prescription_b'
    }
]

MSTAT_MODEL_PARAM_EMPTY = []

COMMON_MSTAT_MODEL_DATA = [
#    MstatModel(id=226168380, params=MSTAT_MODEL_PARAM_PRESCRIPTION, category_id=15756919),  # рельная модель и категория
    MstatModel(id=888, params=MSTAT_MODEL_PARAM_PRESCRIPTION, category_id=15),
    MstatModel(id=999, params=MSTAT_MODEL_PARAM_PRESCRIPTION_NEW, category_id=15)
#    MstatModel(id=888, params=MSTAT_MODEL_PARAM_EMPTY, category_id=15)
]


@pytest.fixture(scope='module')
def mstat_model_data():
    return [o.to_row() for o in COMMON_MSTAT_MODEL_DATA]


@pytest.fixture(scope='module')
def mstat_model_table(yt_server, config, mstat_model_data):
    tablepath = config.banner_mstat_models_table
    table = YtTableResource(yt_server, tablepath, mstat_model_data, attributes={'schema': MstatModel.table_schema()})
    table.dump()

    logging.info('\n{}'.format(rows_as_table(table.data)))

    return table


@pytest.fixture(scope='module')
def offers_table(yt_server, config, offers_data):
    tablepath = offers_table_path(config, GENERATION)
    table = YtTableResource(yt_server, tablepath, offers_data, attributes={'schema': Offer.table_schema()})
    table.dump()
    yt_server.get_yt_client().link(tablepath, offers_table_path(config, 'recent'))

    logging.info(u'\n{}\n{}'.format(table.table_path, rows_as_table(table.data)))

    return table


class MiConfig(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def output_yt_path(self, nodepath):
        return ypath_join(get_yt_prefix(), 'out', nodepath)

    def resolve(self, tmpl, **kwargs):
        subst = dict()
        subst.update(self.__dict__)
        subst.update(kwargs)
        return tmpl.format(**subst)


@pytest.yield_fixture(scope='module')
def config(yt_server, tmpdir_factory):
    config = MiConfig()

    config.yt_proxy = yt_server.get_server()
    config.yt_pool_batch = ''
    config.banner_yt_pool = ''
    # Путь до директории индексатора в YT. Например //home/market/production/indexer/gibson/
    config.yt_home_dir = get_yt_prefix()
    config.yt_user = 'test-user'
    config.yt_tokenpath = None
    # директория для файлов фидов. Пока не собираемся в тесте
    config.banner_feeds_dir = str(tmpdir_factory.mktemp('banner_feeds_dir'))
    config.banner_bad_parent_categories = '6091783,90802,13360737,8475840,90829,14334539,16155381,16440100'
    config.banner_bad_vendors = '242456'
    config.yql_tokenpath = None
    config.banner_oversize_google_single_offers = None  # Not used
    config.banner_google_single_offers_max_offers_in_part = None  # Not used
    config.banner_recipe_white_table = '//home/market/production/mbo/recipes/recent/total'
    config.banner_with_pokupki_domain = False
    config.banner_skip_update_recents = False
    config.banner_yt_feed_table_keep_count = 3
    config.banner_bad_title_words = u'anal,sex,секс,гель-смазка'
    config.banner_mstat_models_table = '//home/market/production/mstat/dictionaries/models/latest'
    yield config


class YqlExecutorStub(object):
    def __init__(self, yql_runner):
        self.yql_runner = yql_runner
        self._log = logging.getLogger('YqlExecutorStub')

    def yql_waiting_execute(self, yql_tokenpath, query):
        self._log.info(u'Running yql query:\n{}'.format(query))
        result = self.yql_runner.execute(YqlRequestResource(query))
        self._log.info(u'Yql result: {}'.format(result))

    def yql_start_request(self, yql_tokenpath, query):
        self._log.info(u'Starting yql query:\n{}'.format(query))
        return self.yql_runner.start_execute(YqlRequestResource(query))


@pytest.fixture(scope='module')
def yql_executor(yt_server):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
    }
    with YqlRunnerTestEnv(syntax_version=1, **resources) as yql_runner:
        yield YqlExecutorStub(yql_runner)


def get_output_table_data(yt_server, processor):
    assert_that(yt_server.get_yt_client().exists(processor.result_table), "Feed table {} doesn't exist".format(processor.result_table))

    table = YtTableResource(yt_server, processor.result_table, load=True)
    return table.data


@pytest.fixture(scope='module')
def result_table_data(yt_server, test_processor):
    return get_output_table_data(yt_server, test_processor)
