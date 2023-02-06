# -*- coding: utf-8 -*-
import commands
import tempfile

import os
import re

from rtcc.dataprovider.searchconfigs import types
from rtcc.controllers.searchconfig import config, data
from rtcc.controllers.searchconfig import element
from rtcc.controllers.searchconfig.factory import ElFactory


def get_parser_name(type_name):
    return ElFactory(type_name).parser_name


def get_ctl_name(type_name):
    return ElFactory(type_name).ctl_type


def test_parsers(prototypes_dir, test_name):
    parser_name = get_parser_name(test_name)
    module = getattr(types, parser_name)
    main = getattr(module, "main")
    prototype_filename = os.path.join(prototypes_dir, "{test_name}_rkub.txt".format(test_name=test_name))
    lines = [x.rstrip() for x in open(prototype_filename).readlines()]
    lines = [line.rstrip() for line in lines]
    lines = main(lines, "OLDSTYLE", "LINE")
    lines = main(lines, "LINE", "PRETTY")
    lines = main(lines, "PRETTY", "OLDSTYLE")

    with tempfile.NamedTemporaryFile() as tmp_file:
        tmp_file.write('\n'.join(lines) + '\n')
        tmp_file.flush()
        status, output = commands.getstatusoutput(
                "diff -wbu {src_file} {result_file}".format(src_file=prototype_filename, result_file=tmp_file.name))
        assert status == 0, output


def test_ctl_elem(test_name, data_dir, prototypes_dir):
    if get_ctl_name(test_name) != "elem":
        return

    all_known_file_path = os.path.join(data_dir, "search/element", test_name, "allknown.dat")
    elem = ""
    if os.path.exists(all_known_file_path):
        elem = open(all_known_file_path).readline().rstrip()
    with open(os.path.join(prototypes_dir, "empty.txt")) as empty_file:
        lines = [x.rstrip() for x in empty_file.readlines()]
        lines = config.main(lines, command="ADD")
        lines = element.main(lines, l="MSK", command="ADD", eltype=test_name, el=elem, data=[elem])
        lines = config.main(lines, l="MSK", p="WEB", c="RKUB", s="PRODUCTION")
        assert re.search("^{elem}".format(elem=elem), lines[0].split()[1])


def test_ctl_data(test_name, prototypes_dir):
    if get_ctl_name(test_name) != "data":
        return

    parser_name = get_parser_name(test_name)
    if parser_name == "sectionblockdata":
        input_data = "{test_name}(blah blah)".format(test_name=test_name)
    elif parser_name == "namedblockdata":
        input_data = "{test_name}(blah blah)".format(test_name=test_name)
    else:
        input_data = "{test_name} blah".format(test_name=test_name)

    lines = open(os.path.join(prototypes_dir, "empty.txt")).readlines()

    lines = config.main(lines, command='ADD')
    lines = data.main(lines, l="MSK", command="ADD", eltype=test_name, data=[input_data])
    lines = config.main(lines, l="MSK", p="WEB", c="RKUB", s="PRODUCTION")
    assert re.search(test_name, lines[0])
