# -*- coding: utf-8 -*-
from rtcc.dataprovider.searchconfigs.types.sources import Source


def test_repr_line():
    source = Source("name", "instances", "path", "balancing", "protocol", ["opt1", "opt2"], ["cgi=cgi"],
                    ["login1", "login2"])
    assert source.repr_string() == "name(instances;path;balancing;protocol;opt1,opt2;cgi=cgi;login1,login2)"


def test_repr_oldstyle():
    source = Source("name", "instances", "path", "balancing", "protocol", ["opt1", "opt2"], ["cgi=cgi"],
                    ["login1", "login2"])
    assert source.repr_oldstyle() == "<SearchSource>\n" + \
                                     "# Owners: login1,login2\n" + \
                                     "ServerDescr name\n" + \
                                     "CgiSearchPrefix @@resolve:instances:path:balancing:protocol@@\n" + \
                                     "Options opt1,opt2\n" + \
                                     "ExtraSearchCgi cgi=cgi\n" + \
                                     "</SearchSource>\n"


def test_repr_pretty():
    source = Source("name", "instances", "path", "balancing", "protocol", ["opt1", "opt2"], ["cgi=cgi"], ["login1", "login2"])
    assert source.repr_pretty() == "name\n" + \
                                   "    instances\n" + \
                                   "    path\n" + \
                                   "    balancing\n" + \
                                   "    protocol\n" + \
                                   "    options\n" + \
                                   "        opt1\n" + \
                                   "        opt2\n" + \
                                   "    extrasearchcgi\n" + \
                                   "        cgi=cgi\n" + \
                                   "    owners\n" + \
                                   "        login1\n" + \
                                   "        login2"


def test_parse_line_simple():
    data = ["    name(instances;path;balancing;protocol;opt1,opt2;extra=extra;login1,login2)   "]
    source = Source.read_from_lines(data, "LINE")

    assert source[0][0].name == "name"
    assert source[0][0].instances == "instances"
    assert source[0][0].path == "path"
    assert source[0][0].balancing == "balancing"
    assert source[0][0].protocol == "protocol"
    assert source[0][0].options == ["opt1", "opt2"]
    assert source[0][0].extrasearchcgi == ["extra=extra"]
    assert source[0][0].owners == ["login1", "login2"]


def test_parse_line_empty():
    data = ["  ", "  ", "  ", "  ", ]
    source = Source.read_from_lines(data, "LINE")

    assert len(source[0]) == 0


def test_parse_line_double():
    data = [
        "name(instances;path;balancing;protocol;opt1,opt2;extra=extra;login1,login2) name2(instances;path;balancing;protocol;opt1,opt2;cgi=cgi;login1,login2) \n"]
    source = Source.read_from_lines(data, "LINE")

    assert source[0][0].name == "name"
    assert source[0][0].instances == "instances"
    assert source[0][0].path == "path"
    assert source[0][0].balancing == "balancing"
    assert source[0][0].protocol == "protocol"
    assert source[0][0].options == ["opt1", "opt2"]
    assert source[0][0].extrasearchcgi == ["extra=extra"]
    assert source[0][0].owners == ["login1","login2"]

    assert source[0][1].name == "name2"
    assert source[0][1].instances == "instances"
    assert source[0][1].path == "path"
    assert source[0][1].balancing == "balancing"
    assert source[0][1].protocol == "protocol"
    assert source[0][1].options == ["opt1", "opt2"]
    assert source[0][1].extrasearchcgi == ["cgi=cgi"]
    assert source[0][1].owners == ["login1","login2"]


def test_parse_oldstyle_simple():
    data = "<SearchSource>\n" \
           "# Owners: login1,login2\n" \
           "ServerDescr name\n" \
           "CgiSearchPrefix @@resolve:instances:path:balancing:protocol@@\n" \
           "Options opt1,opt2\n" \
           "</SearchSource>\n"
    data = data.splitlines(False)
    source = Source.read_from_lines(data, "OLDSTYLE")
    assert source[0].name == "name"
    assert source[0].instances == "instances"
    assert source[0].path == "path"
    assert source[0].balancing == "balancing"
    assert source[0].protocol == "protocol"
    assert source[0].options == ["opt1", "opt2"]
    assert source[0].owners == ["login1", "login2"]


def test_parse_oldstyle_duble_sources():
    data = "<SearchSource>\n" \
           "# Owners: login1,login2\n" \
           "ServerDescr name\n" \
           "CgiSearchPrefix @@resolve:instances:path:balancing:protocol@@\n" \
           "Options opt1,opt2\n" \
           "ExtraSearchCgi cgi=cgi\n" \
           "</SearchSource>\n" \
           "<SearchSource>\n" \
           "# Owners: login1,login2\n" \
           "ServerDescr name2\n" \
           "CgiSearchPrefix @@resolve:instances:path:balancing:protocol@@\n" \
           "Options opt1,opt2\n" \
           "ExtraSearchCgi cgi=cgi\n" \
           "</SearchSource>\n"
    data = data.splitlines(False)
    source = Source.read_from_lines(data, "OLDSTYLE")
    assert source[0].name == "name"
    assert source[0].instances == "instances"
    assert source[0].path == "path"
    assert source[0].balancing == "balancing"
    assert source[0].protocol == "protocol"
    assert source[0].options == ["opt1", "opt2"]
    assert source[0].extrasearchcgi == ["cgi=cgi"]
    assert source[0].owners == ["login1","login2"]

    assert source[1].name == "name2"
    assert source[1].instances == "instances"
    assert source[1].path == "path"
    assert source[1].balancing == "balancing"
    assert source[1].protocol == "protocol"
    assert source[1].options == ["opt1", "opt2"]
    assert source[1].extrasearchcgi == ["cgi=cgi"]
    assert source[1].owners == ["login1","login2"]


def test_parse_pretty_two_sources():
    data = "name\n" \
           "    instances\n" \
           "    path\n" \
           "    balancing\n" \
           "    protocol\n" \
           "    options\n" \
           "        opt1\n" \
           "        opt2\n" \
           "    extrasearchcgi\n" \
           "        cgi=cgi\n" \
           "    owners\n" \
           "        login1\n" \
           "        login2\n" \
           "name2\n" \
           "    instances\n" \
           "    path\n" \
           "    balancing\n" \
           "    protocol\n" \
           "    options\n" \
           "        opt1\n" \
           "        opt2\n" \
           "    extrasearchcgi\n" \
           "        cgi=cgi\n" \
           "    owners\n" \
           "        login1\n" \
           "        login2\n"
    data = data.splitlines(False)

    source = Source.read_from_lines(data, "PRETTY")
    assert source[0].name == "name"
    assert source[0].instances == "instances"
    assert source[0].path == "path"
    assert source[0].balancing == "balancing"
    assert source[0].protocol == "protocol"
    assert source[0].options == ["opt1", "opt2"]
    assert source[0].extrasearchcgi == ["cgi=cgi"]
    assert source[0].owners == ["login1", "login2"]

    assert source[1].name == "name2"
    assert source[1].instances == "instances"
    assert source[1].path == "path"
    assert source[1].balancing == "balancing"
    assert source[1].protocol == "protocol"
    assert source[1].options == ["opt1", "opt2"]
    assert source[1].extrasearchcgi == ["cgi=cgi"]
    assert source[1].owners == ["login1", "login2"]
