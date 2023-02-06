#!/usr/bin/python
# -*- coding: utf-8 -*-

import pytest
import os
import tempfile

import yatest.common

from market.pylibrary.snappy_protostream import PbsnDataFile
from market.proto.indexer import awaps_pb2

from google.protobuf.text_format import MessageToString


MARKET_AWAPS_MODEL_BIN = yatest.common.binary_path('market/idx/export/awaps/market-awaps-models/bin/market-awaps-models')


def _load_protobuf(file, as_text=True, remove_deprecated=False):
    result = []
    with PbsnDataFile(file, 'AWMD') as stream:
        for obj in stream.reader(awaps_pb2.Model):
            # be aware it's a hack. We don't produce params and aliases anymore.
            if remove_deprecated:
                obj.ClearField("params")
                obj.ClearField("aliases")
            if as_text:
                text = MessageToString(obj)
                result.append(text)
            else:
                result.append(obj)
    return result


@pytest.fixture(scope="module")
def empty_global_vendors_xml():
    fd, file_path = tempfile.mkstemp(suffix="awaps-models-global-vendors")
    try:
        with os.fdopen(fd, "w") as tmp:
            tmp.write("<global-vendors/>")
        yield file_path
    finally:
        os.remove(file_path)


def test_generate_awaps_models(empty_global_vendors_xml):
    """
    Тест для проверки корректности генерации awaps моделей
    по протобуфным выгрузкам models.pb и parameters.pb
    """

    input = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/models.pb")
    parameter = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/parameters.pb")
    ratings = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/ratings.txt")
    mrs = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/mrs.mmap")
    vendors = yatest.common.source_path(empty_global_vendors_xml)
    out_dir = yatest.common.output_path("models")

    args = [
        MARKET_AWAPS_MODEL_BIN,
        '-i', input,
        '-p', parameter,
        '-r', ratings,
        '-m', mrs,
        '-v', vendors,
        '-o', out_dir
    ]

    res = yatest.common.execute(args)

    assert res.exit_code == 0

    expected = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/result.pb")
    # name by category id
    actual = out_dir + "models-13932392"

    expectedModels = _load_protobuf(expected, remove_deprecated=True)
    actualModels = _load_protobuf(actual)

    assert set(expectedModels) == set(actualModels)


def test_generate_buker_awaps_models(empty_global_vendors_xml):
    """
    Тест для проверки корректности генерации awaps модели
    по buker информации hyper_id_data.csv
    """
    input = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/hyper_id_data.csv")
    ratings = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/ratings.txt")
    mrs = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/mrs.mmap")
    vendors = yatest.common.source_path(empty_global_vendors_xml)
    out_dir = yatest.common.output_path("models")

    args = [
        MARKET_AWAPS_MODEL_BIN,
        '-i', input,
        '-r', ratings,
        '-m', mrs,
        '-v', vendors,
        '-o', out_dir
    ]

    res = yatest.common.execute(args)

    assert res.exit_code == 0

    expected = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/result_buker.pb")
    # magic buker category id
    actual = out_dir + "models-90829"

    expectedModels = _load_protobuf(expected, remove_deprecated=True)
    actualModels = _load_protobuf(actual)

    assert set(expectedModels) == set(actualModels)


def test_fake_vendors():
    """
    Проверяется сброс vendor_id в случае, если вендор фейковый.
    """

    input = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/models.pb")
    parameter = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/parameters.pb")
    ratings = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/ratings.txt")
    vendors = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/global.vendors.xml")
    mrs = yatest.common.source_path("market/idx/export/awaps/market-awaps-models/tests/data/mrs.mmap")
    out_dir = yatest.common.output_path("models")

    args = [
        MARKET_AWAPS_MODEL_BIN,
        '-i', input,
        '-p', parameter,
        '-r', ratings,
        '-m', mrs,
        '-v', vendors,
        '-o', out_dir
    ]

    res = yatest.common.execute(args)
    assert res.exit_code == 0

    # name by category id
    actual = out_dir + "models-13932392"

    actualModels = _load_protobuf(actual, as_text=False)
    for model in actualModels:
        assert model.vendor_id == 0
