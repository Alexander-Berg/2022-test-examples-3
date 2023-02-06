# flake8: noqa

from yql_api import yql_api
from mongo_runner import mongo
from yt_runner import yt
from yql_utils import tmpdir_module

from fixtures import local_yt_and_yql, yt_client
from crypta.graph.fuzzy.lib.tasks.extract import ExtractBsWatchLog, yesterday

import luigi


def test_it_works(local_yt_and_yql, yt_client):
    task = ExtractBsWatchLog(date=yesterday())
    schema = [dict(name="clientip", type="string"), dict(name="uniqid", type="string")]
    yt_client.create("table", task.source, recursive=True, attributes=dict(schema=schema))
    assert luigi.build([task], local_scheduler=True)
