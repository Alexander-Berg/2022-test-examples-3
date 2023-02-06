from market.idx.marketindexer.marketindexer.in_out_checker import input, output, Check, \
    InputYtTableNotFoundException, OutputYtTableNotFoundException, MiC
from yt.wrapper import ypath_join
import pytest
import logging

GENERATION_NAME = '19841017'


class MiConfig(object):
    def __init__(self, yt_proxy=None, yt_tokenpath=None):
        self.yt_proxy = yt_proxy
        self.yt_tokenpath = yt_tokenpath
        self.enable_in_out_checks = True

        self.feature_5_table_0_name = 'table_0'
        self.feature_5_table_1_path = '//home/table_1'


@pytest.fixture(scope='module')
def miconfig(yt_server):
    return MiConfig(yt_proxy=yt_server.get_server())


class BuildMassIndex(object):
    def __init__(self, config, generation_name, yt_server):
        self.config = config
        self.generation_name = generation_name
        self._yt_server = yt_server

    @input(
        Check().table('//home/absolute_table_0')
               .table('//home/absolute_table_1')
    )
    def feature_0(self):
        pass

    @output(
        Check().table('//home/feature_1_out_table')
    )
    def feature_1(self):
        _create_table(self._yt_server, '//home/feature_1_out_table')

    @output(
        Check().table('//home/feature_2_out_table')
    )
    def feature_2(self):
        # Функция не создаёт таблицы, о которой она заявляет. Проверка швырнёт исключение
        pass

    @output(
        Check().table('//home/feature_3_recent_link')
    )
    def feature_3(self):
        _create_table(self._yt_server, '//home/feature_3_out_table')
        _create_link(self._yt_server, '//home/feature_3_out_table', '//home/feature_3_recent_link')

    @output(
        Check().table('//home/feature_3_recent_link')
    )
    def feature_4(self):
        table = '//home/feature_4_out_table'
        _create_table(self._yt_server, table)
        _create_link(self._yt_server, '//home/feature_4_out_table', '//home/feature_4_recent_link')
        # Удаляем таблицу, на которую указывает ссылка
        self._yt_server.get_yt_client().remove(table)

    @output(
        Check().table('//home', MiC('feature_5_table_0_name'))
               .table(MiC('feature_5_table_1_path'))
    )
    def feature_5(self):
        _create_table(self._yt_server, ypath_join('//home', self.config.feature_5_table_0_name))
        _create_table(self._yt_server, self.config.feature_5_table_1_path)
        pass


@pytest.fixture(scope='module')
def mass_index_builder(miconfig, yt_server):
    return BuildMassIndex(miconfig, GENERATION_NAME, yt_server)


# Чистим папку //home в YT перед каждым тестом
@pytest.fixture(scope='function', autouse=True)
def execute_before_any_test(yt_server):
    _yt_cleanup_folder(yt_server, '//home')


# Проверяем, что нет исключений если входные таблицы есть
def test_table_exists_ok(yt_server, mass_index_builder):
    _create_table(yt_server, '//home/absolute_table_0')
    _create_table(yt_server, '//home/absolute_table_1')
    mass_index_builder.feature_0()


# Проверяем, что если функция создаёт выходную таблицу о которой она заявила, то всё ок.
def test_out_table_is_created_ok(yt_server, mass_index_builder):
    mass_index_builder.feature_1()


# Проверяем, что если нет входной таблицы, то вылетит исключение
def test_no_input_exception(yt_server, mass_index_builder):
    # Создаём только одну из нужны таблиц.
    _create_table(yt_server, '//home/absolute_table_0')
    with pytest.raises(InputYtTableNotFoundException):
        mass_index_builder.feature_0()


# Проверяем, что если нет выходной таблицы, то вылетит исключение
def test_no_output_exception(mass_index_builder):
    with pytest.raises(OutputYtTableNotFoundException):
        mass_index_builder.feature_2()


# Фича создаёт link на валидную таблицу. Проверяем, что проверка отрабатывает.
def test_valid_link_ok(mass_index_builder):
    mass_index_builder.feature_3()


# Фича создаёт link на таблицу которой нет. Проверяем, что вылетит исключение.
def test_invalid_link_exception(yt_server, mass_index_builder):
    with pytest.raises(OutputYtTableNotFoundException):
        mass_index_builder.feature_4()


# Проверяем, что нет исключений если выходные таблицы есть. Используем evaluator
def test_table_exists_with_evaluators_ok(yt_server, mass_index_builder):
    mass_index_builder.feature_5()


def _create_table(yt_server, table_path):
    yt_client = yt_server.get_yt_client()

    schema = [
        dict(type='string', name='some_column'),
    ]

    yt_client.create('table', table_path, attributes=dict(
        schema=schema
    ), recursive=True)


def _create_link(yt_server, table_path, link_path):
    yt_server.get_yt_client().link(table_path, link_path)


def _yt_cleanup_folder(yt_server, yt_folder_path):
    to_remove = [table for table in yt_server.get_yt_client().list(yt_folder_path)]
    for child in to_remove:
        abs_path = '{}/{}'.format(yt_folder_path, child)

        logging.info('Removing table {}'.format(abs_path))
        yt_server.get_yt_client().remove(abs_path)
