# -*- coding: utf-8 -*-
import pytest

from rtcc.controllers.searchconfig.factory import ElFactory
from rtcc.dataprovider.searchconfigs.types.collectionlist import Collection
from rtcc.dataprovider.searchconfigs.types.kvoptions import KVOption
from rtcc.dataprovider.searchconfigs.types.linedata import LineData
from rtcc.dataprovider.searchconfigs.types.sources import Source

TYPES = {
    "sources_list": {
        "construct": Source.construct,
        "write_to_lines": Source.write_to_lines,
        "parser_name": "sources",
        "ctl_type": "elem",
    },
    "collection_list": {
        "construct": Collection.construct,
        "write_to_lines": Collection.write_to_lines,
        "parser_name": "collectionlist",
        "ctl_type": "elem",
    },
    "collection_options_list": {
        "construct": KVOption.construct,
        "write_to_lines": KVOption.write_to_lines,
        "parser_name": "kvoptions",
        "ctl_type": "data",
    },
    "server_options_list": {
        "construct": KVOption.construct,
        "write_to_lines": KVOption.write_to_lines,
        "parser_name": "kvoptions",
        "ctl_type": "data",
    },
    "scatter_options_list": {
        "construct": KVOption.construct,
        "write_to_lines": KVOption.write_to_lines,
        "parser_name": "kvoptions",
        "ctl_type": "data",
    },
    "wizard_options_list": {
        "construct": KVOption.construct,
        "write_to_lines": KVOption.write_to_lines,
        "parser_name": "kvoptions",
        "ctl_type": "data",
    },
    "scheme": {
        "construct": LineData.construct,
        "write_to_lines": LineData.write_to_lines,
        "parser_name": "linedata",
        "ctl_type": "data",
    },
}


@pytest.mark.parametrize("datatype", TYPES.keys())
def test_types_parser_name(datatype):
    elf = ElFactory(datatype)
    assert elf.parser_name == TYPES[datatype]["parser_name"]


@pytest.mark.parametrize("datatype", TYPES.keys())
def test_types_ctl_type(datatype):
    elf = ElFactory(datatype)
    assert elf.ctl_type == TYPES[datatype]["ctl_type"]
