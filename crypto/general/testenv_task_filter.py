import os
import difflib

from crypta.utils.package_build_deps.lib import (
    package,
    testenv_task
)

ARCADIA_PREFIX = '^arcadia'


def _build_arcadia_path(path):
    return os.path.join(ARCADIA_PREFIX, os.path.relpath(path))


def _build_relative_path(path, package_arcadia_path):
    return _build_arcadia_path(os.path.join(os.path.dirname(package_arcadia_path), path))


def make_filter(pkg, package_arcadia_path, commit_path_prefixes):
    task_filter = {'commit_path_prefixes': commit_path_prefixes}

    targets = sorted(set(package.get_targets(pkg)))

    if targets:
        task_filter['targets'] = targets

    debian_dir_path = package.get_debian_dir_path(package_arcadia_path)

    arcadia_paths = [_build_arcadia_path(path) for path in package.get_arcadia_paths(pkg)]
    relative_paths = [_build_relative_path(path, package_arcadia_path) for path in package.get_relative_paths(pkg)]
    observed_paths = sorted(set(
        arcadia_paths +
        relative_paths +
        [os.path.join(ARCADIA_PREFIX, package_arcadia_path)] +
        ([os.path.join(ARCADIA_PREFIX, debian_dir_path)] if debian_dir_path is not None else [])
    ))

    if observed_paths:
        task_filter['observed_paths'] = observed_paths

    return task_filter


def diff_filters(expected_filter, filter):
    expected_filter_str = testenv_task.serialize_task(expected_filter)
    filter_str = testenv_task.serialize_task(filter)

    diff = difflib.ndiff(expected_filter_str.splitlines(1), filter_str.splitlines(1))

    return ''.join(diff)
