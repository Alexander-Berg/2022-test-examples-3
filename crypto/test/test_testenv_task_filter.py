import pytest
import yatest

from crypta.utils.package_build_deps.lib import (
    package,
    testenv_task_filter
)


@pytest.mark.parametrize('path', [
    'crypta/utils/package_build_deps/lib/test/data/package.json',
    'crypta/utils/package_build_deps/lib/test/data/empty_package.json',
    'crypta/utils/package_build_deps/lib/test/data/no_deps_package.json'
])
def test_testenv_task_filter(path):
    with open(yatest.common.source_path(path), 'r') as f:
        return testenv_task_filter.make_filter(
            package.parse_package(f.read()),
            path,
            ['/trunk/arcadia/crypta/']
        )
