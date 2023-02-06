import json
import yatest.common
import pytest
from datetime import date
from getter.yamarec_config_yt import YamarecConfig
from tempfile import NamedTemporaryFile


@pytest.fixture()
def config_path():
    return yatest.common.source_path("market/yamarec/configuration/yamarec.conf")


@pytest.fixture()
def tmp_config_path():
    filepath_in = yatest.common.source_path("market/yamarec/configuration/yamarec.conf")
    with open(filepath_in, "r") as input_file, NamedTemporaryFile(mode="w+") as tmp_file:
        data = input_file.read()
        data = data.replace("yamarec://", "http://yandex.ru/")
        tmp_file.write(data)
        tmp_file.seek(0)
        yield tmp_file.name


def duplication_checking_hook(pairs):
    result = dict()
    for key, value in pairs:
        if key in result:
            raise KeyError("Duplicate key specified: %s" % key)
        result[key] = value
    return result


def test_config_parse(tmp_config_path):
    """Validate config file with parser"""
    parser = YamarecConfig(date(year=2021, month=9, day=1))
    parser.load(tmp_config_path)
    parser.validate()


def test_config(config_path):
    """ Check config syntax """
    with open(config_path, "rt") as cfg_file:
        json.load(cfg_file, object_pairs_hook=duplication_checking_hook)
