import pytest
import logging
from hamcrest import assert_that
from market.idx.pylibrary.mindexer_core.banner.banner import ParallelYqlExecutor, YqlProcessor
from conftest import GENERATION


def assert_table_exists(yt_server, processor):
    assert_that(yt_server.get_yt_client().exists(processor.result_table), 'Feed {} table {} doesn\'t exist'.format(processor.name, processor.result_table))


class StubCopyOffersProcessor(YqlProcessor):
    def __init__(self, name, config, offers_table_path, yql_dependency=None):
        YqlProcessor.__init__(self, name, config, GENERATION, yql_dependency)
        self._offers_table_path = offers_table_path

    @property
    def _yql_statement(self):
        return 'PRAGMA yt.QueryCacheMode = "normal"; PRAGMA yt.AutoMerge = "disabled"; INSERT INTO `{}` SELECT * FROM `{}`'.format(self.result_table, self._offers_table_path)


class NoYqlProcessor(YqlProcessor):
    def __init__(self, name, config, yql_dependency):
        YqlProcessor.__init__(self, name, config, GENERATION, yql_dependency)

    @property
    def _yql_statement(self):
        return None


@pytest.mark.skip(reason='so-so flaky and annoying')
def test_no_exception_all_tables_created(config, offers_table, yql_executor, yt_server):
    log = logging.getLogger()

    def create(name, dependencies=None):
        processor = StubCopyOffersProcessor(name, config, offers_table.table_path, dependencies)
        processor.override_yql_executor(yql_executor)
        return processor

    def create_no_yql(name, dependencies=None):
        processor = NoYqlProcessor(name, config, dependencies)
        processor.override_yql_executor(yql_executor)
        return processor

    # Setup
    c = create('c')
    d = create('d')
    b = create('b', [c, d])
    a = create('a', [b])

    b_copy = create('b')
    f = create('f', [b_copy])

    # Одинаковое имя
    g = create('common_name')
    e = create_no_yql('common_name', [g])

    # Вообще чума
    z0 = create('z0')
    z1 = create('z1')
    y = create('common_y', [z0, z1])
    y1 = create('common_y', [y, z0, z1])

    all_processors = [a, b, b_copy, c, d, f, g, e, z0, z1, y, y1]
    # Run
    processors = [a, f, e, y1]
    executor = ParallelYqlExecutor(config, processors, 2, log)
    executor.run()

    # Check
    for p in all_processors:
        assert_table_exists(yt_server, p)
