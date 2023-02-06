import contextlib

import pytest

import mapreduce.yt.python.yt_stuff as vanilla


def get_yt_config():
    return vanilla.YtConfig(
        node_count=8,
        wait_tablet_cell_initialization=True,
        node_config={
            "tablet_node": {
                "resource_limits": {
                    "tablet_dynamic_memory": 100 * 1024 * 1024,
                    "tablet_static_memory": 100 * 1024 * 1024,
                }
            }
        }
    )


@contextlib.contextmanager
def get_yt_stuff(yt_config):
    yt = vanilla.YtStuff(yt_config)
    yt.start_local_yt()
    try:
        yield yt
    finally:
        yt.stop_local_yt()


@pytest.fixture
def yt_config():
    return get_yt_config()


@pytest.fixture(scope="function")
def yt_stuff(yt_config):
    with get_yt_stuff(yt_config) as yt_stuff:
        yield yt_stuff


@pytest.fixture(scope="module")
def module_yt_stuff():
    with get_yt_stuff(get_yt_config()) as yt_stuff:
        yield yt_stuff
