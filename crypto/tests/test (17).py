from __future__ import print_function

import pytest

from crypta.graph.households.hh_composition.lib import CompositionHH
from crypta.graph.households.hh_composition.lib.query.composition import HouseholdInfo
from crypta.graph.households.hh_composition.lib.query.decoder import HouseholdInfoDecoder
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, execute, read_resource


@load_fixtures(
    ("//home/user_identification/homework/v2/prod/homework_unified_id", "/fixtures/homework_yuid.json"),
    ("//home/crypta/develop/ids_storage/yandexuid/yuid_with_all_info", "/fixtures/yuid_with_all.json"),
    ("//home/crypta/develop/profiles/export/profiles_for_14days", "/fixtures/profiles.json"),
    ("//home/crypta/develop/state/households_new/output/hh_reversed", "/fixtures/hh_reversed.json"),
    ("//home/crypta/develop/state/households_new/bb_output/households_to_bb", "/fixtures/hh_to_bb.json"),
)
@canonize_output
def test_run_hh_composition(local_yt):
    """ Should check is hh composition correct """
    task = CompositionHH()
    execute(task)

    def select_all(table_path):
        return list(local_yt.yt_client.read_table(table_path, format="json"))

    output_tables = (
        "//home/crypta/develop/state/households_new/output/composition",
        "//home/crypta/develop/state/households_new/output/edges",
        "//home/crypta/develop/state/households_new/bb_output/households_to_bb",
        "//home/crypta/develop/state/households_new/bb_output/households_to_bb_old",
        "//home/crypta/develop/state/households_new/bb_output/households_to_bb_diff",
    )
    return {table: sorted(select_all(table)) for table in output_tables}


def make_pytest_parametrize():
    yield "hh_data, hh_info, binary, hhid_32"
    for line in read_resource("/fixtures/composition.json"):
        yield line["data"], line["info"], [line["info_binary"]], line["hhid_32"]


@pytest.mark.parametrize(make_pytest_parametrize().next(), list(make_pytest_parametrize())[1:])
def test_hh_encoder(hh_data, hh_info, binary, hhid_32):
    """ Should check is household encoder work correctly """
    info = HouseholdInfo(hh_data)
    assert info.attrs == hh_info
    assert [info.base64_binary()] == binary


@pytest.mark.parametrize(make_pytest_parametrize().next(), list(make_pytest_parametrize())[1:])
def test_hh_decoder(hh_data, hh_info, binary, hhid_32):
    """ Should check is household decoder work correctly """
    decoder = HouseholdInfoDecoder()
    attrs = decoder.decode_bin(*binary)
    assert attrs.pop("hh_id") == hhid_32
    assert attrs == hh_info


@pytest.mark.parametrize(make_pytest_parametrize().next(), list(make_pytest_parametrize())[1:])
def test_hh_encoder_decoder(hh_data, hh_info, binary, hhid_32):
    """ Should check is household encoder and decoder composition work correctly """
    info = HouseholdInfo(hh_data)
    decoder = HouseholdInfoDecoder()
    attrs = decoder.decode_bin(info.base64_binary())
    assert attrs.pop("hh_id") == hhid_32
    assert info.attrs == attrs
