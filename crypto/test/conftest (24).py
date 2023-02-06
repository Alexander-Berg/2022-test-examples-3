import pytest

subdir_count = 0


@pytest.fixture(scope="function")
def yt_subdir():
    global subdir_count
    subdir_count += 1
    return "{:02d}".format(subdir_count)
