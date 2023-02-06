from functools import wraps
import os

from library.python.find_root import detect_root

import yatest.common
from yatest.common.runtime import _join_path, _get_ya_plugin_instance  # noqa


def get_root():
    root = detect_root(os.getcwd()) or detect_root(os.environ.get('Y_PYTHON_SOURCE_ROOT'))
    if not root:
        raise RuntimeError("Can't find acradia root, set cwd inside arcadia or specify Y_PYTHON_SOURCE_ROOT")
    return root


def raw_source_path(path=None):
    arc_root = get_root()
    return _join_path(arc_root, path)


def raw_binary_path(path=None):
    arc_root = get_root()
    if os.name == "nt":
        if not path.endswith(".exe"):
            path += ".exe"
    for binary_path in [os.path.join(arc_root, "bin", path), os.path.join(arc_root, path)]:
        if os.path.exists(binary_path):
            return binary_path
    raise RuntimeError(
        "Cannot find binary '{binary}': make sure it was added in the DEPENDS section".format(binary=path)
    )


def patch_yatest_common():
    def fallback_on_no_yamake(func, fallback):
        @wraps(func)
        def impl(*args, **kwargs):
            try:
                _get_ya_plugin_instance()
            except (AttributeError, NotImplementedError):
                print('No yatest runtime present, falling back {0} to {1}'.format(func, fallback))
                return fallback(*args, **kwargs)
            return func(*args, **kwargs)
        return impl

    yatest.common.source_path = fallback_on_no_yamake(yatest.common.source_path, raw_source_path)
    yatest.common.binary_path = fallback_on_no_yamake(yatest.common.binary_path, raw_binary_path)


patch_yatest_common()
