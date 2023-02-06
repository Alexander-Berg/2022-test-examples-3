import os

from yql.api.v1.client import YqlClient


def get_yql_cli():
    """
    environment variables used below provided by the yql test framework;

    add "INCLUDE(${ARCADIA_ROOT}/yql/library/test_framework/recipe/recipe.inc)"
    to your tests ya.make in order to use it
    """
    return YqlClient(
        server=os.environ["YQL_HOST"],
        port=int(os.environ["YQL_PORT"]),
        db=os.environ["YQL_DB"],
        db_proxy=os.environ["YT_PROXY"],
    )
