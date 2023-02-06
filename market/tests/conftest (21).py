import os
import pytest
from yatest.common import work_path
from market.robotics.cv.library.py.synthgen import config as synthgen_config
from market.robotics.cv.library.py.synthgen.config import common

LOCAL_DEBUG = False


@pytest.fixture
def base_dir() -> str:
    if LOCAL_DEBUG:
        return "/Users/ilinvalery/arcadia/market/robotics/cv/library/py/synthgen/"
    return work_path()


@pytest.fixture
def config(base_dir) -> common.Config:
    config_filepath = os.path.join(base_dir, "config_example.yaml")
    reader = synthgen_config.SynthConfigReader(config_filepath)
    return reader.read()
