import os


def is_autocheck():
    """Returns true when run in CI autocheck environment"""
    return os.environ.get('YA_TEST_RUNNER') is not None
