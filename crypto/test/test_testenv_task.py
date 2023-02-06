import pytest
import yatest

from crypta.utils.package_build_deps.lib import testenv_task


@pytest.mark.parametrize('path,task', [
    ('crypta/utils/package_build_deps/lib/test/data/two_testenv_tasks.yaml', 'DEPLOY_CRYPTA_GRAPHITE_MONITOR'),
    ('crypta/utils/package_build_deps/lib/test/data/single_testenv_task.yaml', None),
    ('crypta/utils/package_build_deps/lib/test/data/template_testenv_task.yaml', 'DEPLOY_CRYPTA_GRAPHITE_MONITOR'),
])
def test_testenv_task(path, task):
    with open(yatest.common.source_path(path), 'r') as f:
        task = testenv_task.parse_task(f.read(), task)
        task_filter = testenv_task.get_filter(task)

        return {
            'task_filter': task_filter,
            'task_filter_yaml': testenv_task.serialize_task(task_filter)
        }


@pytest.mark.xfail(strict=True, reason="Multiple tasks in file but no task type was explicitly specified")
def test_no_explicit_testenv_task():
    with open(yatest.common.source_path('crypta/utils/package_build_deps/lib/test/data/two_testenv_tasks.yaml'), 'r') as f:
        testenv_task.parse_task(f.read())
