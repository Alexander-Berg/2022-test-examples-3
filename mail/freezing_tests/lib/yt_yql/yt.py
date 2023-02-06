import os

from yt.wrapper import YtClient


def get_yt_cli():
    """
    environment variables used below provided by the yql test framework;

    add "INCLUDE(${ARCADIA_ROOT}/yql/library/test_framework/recipe/recipe.inc)"
    to your tests ya.make in order to use it
    """
    return YtClient(proxy=os.environ["YT_PROXY"])


def create_table(path, yt_cli, schema=None):
    """
    :param path: path of the resulting table
    :param schema: schema of the resulting table
        docs: https://docs.yandex-team.ru/yt/description/storage/static_schema#schema_overview
        example: [
            {"name": "key", "type": "string", "sort_order": "ascending"},
            {"name": "subkey", "type": "string"},
        ]
    :param yt_cli: yt client
    """
    assert not yt_cli.exists(path)
    attributes = {"schema": schema} if schema else {}
    yt_cli.create("table", path=path, recursive=True, attributes=attributes)
    assert yt_cli.exists(path)


def write_table(path, data, yt_cli, append=False):
    """
    :param path: path of the resulting table
    :param data: list of rows
    :param yt_cli: yt client
    :param append: set append attribute
    """
    rows = 0
    if append:
        path = yt_cli.TablePath(path, append=True)
        rows = yt_cli.row_count(path)
    yt_cli.write_table(path, data)
    assert rows + len(data) == yt_cli.row_count(path)
