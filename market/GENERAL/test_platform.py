import os


def is_yatest():
    return os.environ.get('YA_TEST_RUNNER', None) is not None


def is_bin_exec():
    return __name__.startswith('__tests__')


def is_classic():
    return not (is_yatest() or is_bin_exec())
