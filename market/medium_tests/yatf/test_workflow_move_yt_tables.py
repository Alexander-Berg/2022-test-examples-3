# coding: utf-8
from market.idx.marketindexer.marketindexer import workflow
import pytest
import logging
from yt.wrapper.ypath import ypath_join
import market.idx.marketindexer.miconfig as miconfig_module
import market.pylibrary.mindexerlib.config as mindexerlib_config_module


log = logging.getLogger('')

GENERATION = '20210401_1728'

DESTINATION_MOVE_DIR = '//indexer/tables_backups'
FEATURE_OFF_MOVE_DIR = '//feature_off_backups'

# Все таблички, которые есть
YT_TABLES = [
    '//indexer/mi3/main/20210401_1728/some_folder/some_table_0',
    '//indexer/mi3/main/20210401_1728/some_folder/some_table_1',
    '//indexer/mi3/main/20210401_1728/some_table_2',
    '//indexer/mi3/main/20210401_1728/some_table_3',
    '//some_external_table_0',
    '//some_external_table_1',
    DESTINATION_MOVE_DIR + '/20210315_1728/some_dir/some_table'  # Проверяем, что удалится папочка с табличкой!
]

# Пустые директории
YT_EMPTY_DIRS = [
    '//indexer/20210401_1728/some_empty_dir'
]

# Директории с бэкапами
YT_BACKUP_DIRS = [
    '20210320_1728',
    '20210319_1728',  # Будет удалена, так как keep_count = 2
    '20210315_1728',  # Будет удалена, так как keep_count = 2
]

YT_BACKUP_RECENT_LINK_GEN = '20210320_1728'

MOVE_TABLES_INFO = [
    {'path': '//indexer/market/some_nonexistent_table'},
    {'path': '//indexer/mi3/main/{generation}', 'move_to': 'mi3_main'},
    {'path': '//indexer/{generation}/some_empty_dir'},
    {'path': '//some_external_table_0', 'move_to': 'ex_0'},
    {'path': '//some_external_table_1'}
]


class MoveYtTablesSection(object):
    def __init__(self, enabled, destination_yt_dir, source_dirs_info, keep_count):
        self.enabled = enabled
        self.destination_yt_dir = destination_yt_dir
        self.keep_count = keep_count
        self.source_dirs_info = source_dirs_info


class MiConfig(object):
    def __init__(self, yt_proxy=None, enabled_move_yt_tables=True, destination_move_dir=DESTINATION_MOVE_DIR):
        self.yt_proxy = yt_proxy
        self.yt_tokenpath = None
        self.move_yt_tables_on_fail = MoveYtTablesSection(enabled=enabled_move_yt_tables,
                                                          destination_yt_dir=destination_move_dir,
                                                          keep_count=2,  # Оставляем не больше 2x поколений таблиц
                                                          source_dirs_info=MOVE_TABLES_INFO)
        self.yt_pool_batch = ''

    def resolve(self, tmpl, **kwargs):
        subst = dict()
        subst.update(self.__dict__)
        subst.update(kwargs)
        return tmpl.format(**subst)


def _create_table(yt_client, table_path):
    schema = [
        dict(type='string', name='some_column'),
    ]

    yt_client.create('table', table_path, attributes=dict(
        schema=schema
    ), recursive=True)


@pytest.fixture(scope='module')
def miconfig(yt_server):
    return MiConfig(yt_proxy=yt_server.get_server())


@pytest.fixture(scope='module')
def miconfig_feature_off(yt_server):
    return MiConfig(yt_proxy=yt_server.get_server(), enabled_move_yt_tables=False, destination_move_dir=FEATURE_OFF_MOVE_DIR)


@pytest.fixture(scope='module')
def prepare_tables(yt_server):
    yt_client = yt_server.get_yt_client()

    # Пустые директории
    for d in YT_EMPTY_DIRS:
        yt_client.mkdir(d, recursive=True)

    # Директории с бэкапами таблиц, recent линкой
    for d in YT_BACKUP_DIRS:
        yt_client.mkdir(ypath_join(DESTINATION_MOVE_DIR, d), recursive=True)

    yt_client.link(target_path=ypath_join(DESTINATION_MOVE_DIR, YT_BACKUP_RECENT_LINK_GEN),
                   link_path=ypath_join(DESTINATION_MOVE_DIR, 'recent'))

    # Таблички
    for p in YT_TABLES:
        _create_table(yt_client, p)


@pytest.fixture(scope='module')
def prepare_tables_for_feature_off_test(yt_server):
    yt_client = yt_server.get_yt_client()

    # Таблички
    _create_table(yt_client, '//home/some_table_t')


def test_move_tables(yt_server, prepare_tables, miconfig):
    workflow.try_move_yt_tables_on_failure(GENERATION, log, miconfig)

    yt_client = yt_server.get_yt_client()
    # 0. Проверяем, что в папке с бэкапами всего лишь 2 поколения (мы указывали keep_count = 2 в Miconfig),
    # и что recent линка указывает на последнее
    backups_dir = set(yt_client.list(DESTINATION_MOVE_DIR))
    assert backups_dir == {'20210320_1728', '20210401_1728', 'recent'}
    assert yt_client.get(ypath_join(DESTINATION_MOVE_DIR, 'recent', '@path')) == ypath_join(DESTINATION_MOVE_DIR, GENERATION)

    # 1. Проверяем содержимое директории с последними бэкапами таблиц
    assert set(yt_client.list(ypath_join(DESTINATION_MOVE_DIR, GENERATION))) == {'ex_0', 'mi3_main', 'some_empty_dir', 'some_external_table_1'}
    assert set(yt_client.list(ypath_join(DESTINATION_MOVE_DIR, GENERATION, 'mi3_main'))) == {'some_folder', 'some_table_2', 'some_table_3'}

    # 2. Проверяем, что все таблицы перемещены.
    for o in MOVE_TABLES_INFO:
        assert not yt_client.exists(o['path'].format(generation=GENERATION))


def test_feature_off(yt_server, prepare_tables_for_feature_off_test, miconfig_feature_off):
    workflow.try_move_yt_tables_on_failure(GENERATION, log, miconfig_feature_off)

    yt_client = yt_server.get_yt_client()

    # 0. Проверяем, что табличка на месте
    assert yt_client.exists('//home/some_table_t')

    # 1. Проверяем, что нет директории с бэкапами
    assert not yt_client.exists(FEATURE_OFF_MOVE_DIR)


# Пример конфига
# [move_yt_tables_on_fail]
# enabled = true
# destination_yt_dir = //home/market/indexer/stratocaster/failed_generations_tables
# keep_count = 3
# move_paths =
# //home/market/indexer/stratocaster/mi3/main/{generation}(mi3_main)
# //home/market/some_external_table
# //home/market/{generation}/some_table
# //home/market/table(super_table)
#
# Реальный move! НЕ создание ссылки.
# //home/market/indexer/stratocaster/mi3/main/{generation}(mi3_main) -> //home/market/indexer/stratocaster/failed_generations_tables/{generation}/mi3_main
# //home/market/some_external_table -> //home/market/indexer/stratocaster/failed_generations_tables/{generation}/some_external_table
# //home/market/{generation}/some_table -> //home/market/indexer/stratocaster/failed_generations_tables/{generation}/some_table
# //home/market/table(super_table) -> //home/market/indexer/stratocaster/failed_generations_tables/super_table
def test_load_full_config(tmp_path):
    config_content = u'''
[move_yt_tables_on_fail]
enabled = true
destination_yt_dir = //home/market/indexer/stratocaster/failed_generations_tables
keep_count = 3
move_paths =
    //home/market/indexer/stratocaster/mi3/main/{generation}(mi3_main)
    //home/market/some_external_table
    //home/market/{generation}/some_table
    //home/market/table(super_table)
    '''
    f = tmp_path / 'local.ini'
    f.write_text(config_content)

    config = mindexerlib_config_module.Config(str(f))
    c = miconfig_module.MoveYtTablesSection(config, 'move_yt_tables_on_fail')

    assert c.enabled is True
    assert c.destination_yt_dir == '//home/market/indexer/stratocaster/failed_generations_tables'
    assert c.keep_count == 3
    assert c.source_dirs_info == [
        {'path': '//home/market/indexer/stratocaster/mi3/main/{generation}', 'move_to': 'mi3_main'},
        {'path': '//home/market/some_external_table'},
        {'path': '//home/market/{generation}/some_table'},
        {'path': '//home/market/table', 'move_to': 'super_table'},
    ]
