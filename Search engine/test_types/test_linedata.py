# -*- coding: utf-8 -*-
from rtcc.dataprovider.searchconfigs.types.linedata import LineData


def test_repr_line_empty():
    linedata = LineData()
    assert linedata.repr_string() == ""


def test_repr_line_simple():
    linedata = LineData(options=["opt1", "opt2"])
    assert linedata.repr_string() == "opt1,opt2"


def test_repr_pretty_empty():
    linedata = LineData()
    assert linedata.repr_pretty() == ""


def test_repr_pretty_simple():
    linedata = LineData(options=["opt1", "opt2"])
    assert linedata.repr_pretty() == "opt1\n" + \
                                     "opt2"


def test_repr_oldstyle_empty():
    linedata = LineData()
    assert linedata.repr_oldstyle() == ""


def test_repr_oldstyle_simple():
    linedata = LineData(options=["opt1", "opt2"])
    assert linedata.repr_oldstyle() == "opt1,opt2"


def test_parse_line_empty():
    data = [""]
    linedata = LineData.read_from_lines(data, "LINE")
    assert linedata[0][0].options == [""]


def test_parse_line_simple():
    data = ["opt1,opt2"]
    linedata = LineData.read_from_lines(data, "LINE")
    assert linedata[0][0].options == ["opt1", "opt2"]


def test_parse_pretty_empty():
    data = [""]
    linedata = LineData.read_from_lines(data, "PRETTY")
    assert linedata[0].options == [""]


def test_parse_pretty_simple():
    data = ["opt1",
            "opt2"]
    linedata = LineData.read_from_lines(data, "PRETTY")
    assert linedata[0].options == ["opt1", "opt2"]


def test_parse_oldstyle_empty():
    data = [""]
    linedata = LineData.read_from_lines(data, "OLDSTYLE")
    assert linedata[0][0].options == [""]


def test_parse_oldstyle_simple():
    data = ["opt1,opt2"]
    linedata = LineData.read_from_lines(data, "OLDSTYLE")
    assert linedata[0][0].options == ["opt1", "opt2"]
