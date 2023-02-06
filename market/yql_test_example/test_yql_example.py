#
# Этот файл создан исключительно как пример(ы) написания тестов на YQL!
#
# В данном примере мы заполним таблицу офферов в локальном YT (называем её входной таблицей), запустим над ней YQL,
# который создаст новую таблицу (будем называть её выходной). Далее опишем в коде ожидаемые данные в выходной таблице
# и сравним ожидаемые данные с полученными данными из выходной таблицы.
# В логах теста мы должны увидеть:
# - весь запускаемый YQL
# - ответ YQL c ошибками/warning-ами
# - напечатанную в удобном виде таблицу выходных данных
# - ожидаемые данные
# Так же проверим, что выходная таблица вообще создаётся.

from hamcrest import assert_that
import logging
import pytest
from market.idx.yatf.resources.yql_resource import YtResource, YqlRequestResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.yql_env import YqlRunnerTestEnv
from market.idx.yatf.utils.utils import assert_rows_set_equal


class Offer(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)

    def to_row(self):
        return self.__dict__

    @classmethod
    def table_schema(self):
        # Это схема таблицы. Опишем её руками. Но есть возможность для простых случаев сгенерировать её из
        # протобуфа (см. make_schema из ytutil.py
        schema = [dict(name='ware_md5', type='string'),
                  dict(name="region_id", type='uint64'),
                  dict(name='text', type='string')]
        return schema


OFFERS_DATA = [
    Offer(ware_md5='0', region_id=10, text='Celestron C11-S'),
    Offer(ware_md5='1', region_id=10, text='Celestron C8 EdgeHD'),
    Offer(ware_md5='2', region_id=11, text='Sky-Watcher BK MAK127SP')
]


@pytest.fixture(scope='module')
def offers_data():
    data = []
    for o in OFFERS_DATA:
        data.append(o.to_row())
    return data


@pytest.yield_fixture(scope='module')
def config(yt_server):
    class MiConfig(object):
        def __init__(self, **kwargs):
            self.__dict__.update(kwargs)

    config = MiConfig()

    config.yt_proxy = ''
    config.yt_pool_batch = ''
    config.yt_home_dir = get_yt_prefix()
    config.yt_user = 'test-user'
    config.yt_tokenpath = None
    config.yql_tokenpath = None

    yield config


@pytest.fixture(scope='module')
def offers_table_path(config):
    return '{}/offers/MyOffersTable'.format(config.yt_home_dir)


@pytest.fixture(scope='module')
def output_table_path(config):
    return '{}/OutputTable'.format(config.yt_home_dir)


@pytest.fixture(scope='module')
def offers_table(yt_server, offers_data, offers_table_path):
    table = YtTableResource(yt_server, offers_table_path, offers_data, attributes={'schema': Offer.table_schema()})
    table.dump()
    return table


@pytest.fixture(scope='module')
def yql_runner(yt_server):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
    }
    with YqlRunnerTestEnv(syntax_version=1, **resources) as yql_runner:
        yield yql_runner


class OutputTableRow(object):
    def __init__(self, ware_md5, region_id, text):
        self.ware_md5 = ware_md5
        self.region_id = region_id
        self.text = text

    def toDict(self):
        return self.__dict__


EXPECTED_DATA_SET = [
    OutputTableRow(ware_md5='0', region_id=10, text='Celestron C11-S'),
    OutputTableRow(ware_md5='1', region_id=10, text='Celestron C8 EdgeHD')
]


@pytest.fixture(scope='module')
def expected_data_set():
    return [o.toDict() for o in EXPECTED_DATA_SET]


YQL = '''\
$offers = "{offers_table}";
INSERT INTO `{result_table}` WITH TRUNCATE
SELECT
    o.ware_md5 as ware_md5,
    o.region_id as region_id,
    o.text as text
FROM $offers as o
WHERE region_id = 10
'''


@pytest.fixture(scope='module')
def output_table_data(yt_server,
                      offers_table,  # Для того чтобы таблица офферов была создана до выполнения YQL ниже.
                      offers_table_path,
                      output_table_path,
                      yql_runner):
    log = logging.getLogger('')

    # Выполняем YQL
    query = YQL.format(offers_table=offers_table_path, result_table=output_table_path)
    log.info('Running yql query:\n{}'.format(query))
    yql_result = yql_runner.execute(YqlRequestResource(query))
    log.info('Yql result: {}'.format(yql_result))

    # Проверяем, что таблица есть.
    assert_that(yt_server.get_yt_client().exists(offers_table_path), 'Table {} doesn\'t exist!'.format(offers_table_path))

    # Загружаем данные из таблицы
    table = YtTableResource(yt_server, output_table_path, load=True)
    return table.data


def test_tables_equal(output_table_data, expected_data_set):
    assert_rows_set_equal(output_table_data, expected_data_set)

    # При зпуске через ya make -tt
    # $offers = "//tmp/caramelo/2020/5/26/test_yql_example.py::test_tables_equal/1590498444/offers/MyOffersTable";
    # INSERT INTO `//tmp/caramelo/2020/5/26/test_yql_example.py::test_tables_equal/1590498444/OutputTable` WITH TRUNCATE
    # SELECT
    #     o.ware_md5 as ware_md5,
    #     o.region_id as region_id,
    #     o.text as text
    # FROM $offers as o
    # WHERE region_id = 10
    #
    # 2020-05-26 16:08:30,290 - MainProcess - root - INFO - output_table_data: Yql result:
    # {'errors': [],
    #  'id': '5ecd14a3b07a77fa79eac452',
    #  'issues': [],
    #  'status': 'COMPLETED',
    #  'updatedAt': '2020-05-26T13:08:30.046Z',
    #  'version': 1000000}
    # 2020-05-26 16:08:30,509 - MainProcess - root - INFO - assert_rows_equal: data:
    # +---------------------+----------+-----------+
    # |         text        | ware_md5 | region_id |
    # +---------------------+----------+-----------+
    # |   Celestron C11-S   |    0     |     10    |
    # | Celestron C8 EdgeHD |    1     |     10    |
    # +---------------------+----------+-----------+
    # 2020-05-26 16:08:30,510 - MainProcess - root - INFO - assert_rows_equal: expected:
    # +---------------------+----------+-----------+
    # |         text        | ware_md5 | region_id |
    # +---------------------+----------+-----------+
    # |   Celestron C11-S   |    0     |     10    |
    # | Celestron C8 EdgeHD |    1     |     10    |
    # +---------------------+----------+-----------+
