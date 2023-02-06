import functools

import luigi
import yatest.common

from crypta.lib.python.yt.test_helpers import tests
from crypta.profile.lib.test_helpers import luigi_helpers
from crypta.profile.utils import luigi_utils


def recursive_cleanup(yt_client, target):
    if isinstance(target, (luigi_utils.YtTarget, luigi_utils.YtDailyRewritableTarget)):
        yt_client.remove(target.table, force=True)
    elif isinstance(target, (list, tuple)):
        for elem in target:
            recursive_cleanup(yt_client, elem)
    elif isinstance(target, dict):
        for key, value in target.items():
            recursive_cleanup(yt_client, value)
    else:
        raise TypeError(
            '{} with value {} is not subclass of list, tuple, dict, YtTarget or YtDailyRewritableTarget'.format(
                type(target),
                target,
            )
        )


def io_cleanup(yt, task):
    input = task.input()
    output = task.output()
    yt_client = yt.get_yt_client()

    if input:
        recursive_cleanup(yt_client, input)

    if output:
        recursive_cleanup(yt_client, output)


def check_and_filter_luigi_status(results, dependencies_are_missing):
    ref_status_code = luigi.LuigiStatusCode.MISSING_EXT if dependencies_are_missing else luigi.LuigiStatusCode.SUCCESS
    assert ref_status_code == results[0].status, \
        'Unexpected luigi status code. Expected: {}, got: {}'.format(ref_status_code, results[0].status)
    if not dependencies_are_missing:
        return results[1:]


def run_and_test_task(task, yt, input_tables, output_tables, dependencies_are_missing, data_path=None, need_io_cleanup=True):
    if need_io_cleanup:
        io_cleanup(yt, task)

    results = tests.yt_test_func(
        yt_client=yt.get_yt_client(),
        func=functools.partial(luigi_helpers.run_luigi_task, task=task),
        data_path=data_path or yatest.common.test_source_path('data'),
        input_tables=input_tables,
        output_tables=output_tables,
        return_result=True,
    )

    return check_and_filter_luigi_status(results=results, dependencies_are_missing=dependencies_are_missing)
