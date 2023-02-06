import pytest
import yatest

from crypta.utils.package_build_deps.lib import package


@pytest.mark.parametrize('path', [
    'crypta/utils/package_build_deps/lib/test/data/package.json',
    'crypta/utils/package_build_deps/lib/test/data/empty_package.json',
    'crypta/utils/package_build_deps/lib/test/data/no_deps_package.json'
])
def test_package(path):
    with open(yatest.common.source_path(path), 'r') as f:
        pkg = package.parse_package(f.read())

        return {
            'targets': package.get_targets(pkg),
            'arcadia_paths': package.get_arcadia_paths(pkg),
            'relative_paths': package.get_relative_paths(pkg)
        }
