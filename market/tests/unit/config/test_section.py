import os.path

import pytest

from yamarec1.config import Config
from yamarec1.config import ConfigExpression
from yamarec1.config import ConfigSection
from yamarec1.config.exceptions import ConfigEvaluationError


def test_section_computes_into_valid_config():
    entries = {"x": ConfigExpression("a"), "y": ConfigExpression("x + 1")}
    section = ConfigSection(None, entries)
    assert section.meta is None
    assert section.entries == entries
    config = section.compute("", "test", {"a": 1})
    assert config.x == 1
    assert config.y == 2


def test_section_meta_provides_basic_specials():
    z = ConfigSection(None, {"up2": ConfigExpression("__up__(2).__key__")})
    y = ConfigSection(None, {"z": z, "up1": ConfigExpression("__up__.__key__")})
    x = ConfigSection(None, {"y": y, "up0": ConfigExpression("__key__")})
    config = Config("", {"x": x}, {})
    assert config.x.y.z.up2 == "x"
    assert config.x.y.up1 == "x"
    assert config.x.up0 == "x"


def test_extended_section_meta_copies_proto_entries():
    p = ConfigSection(None, {"x": ConfigExpression("__up__.x + 1")})
    q = ConfigSection(ConfigExpression("__extend__(p)"), {})
    config = Config("", {"x": ConfigExpression("0"), "p": p, "q": q}, {})
    assert config.x == 0
    assert config.p.x == 1
    assert config.q.x == 1  # not 2!


def test_extended_section_meta_preserves_scope():
    p = ConfigSection(None, {"x": ConfigExpression("1"), "y": ConfigExpression("x + 1")})
    q = ConfigSection(ConfigExpression("__extend__(p)"), {"x": ConfigExpression("2")})
    config = Config("", {"p": p, "q": q}, {})
    assert config.p.x == 1
    assert config.p.y == 2
    assert config.q.x == 2
    assert config.q.y == 3


def test_extended_section_meta_operates_recursively():
    x = ConfigSection(None, {"v": ConfigExpression("1"), "w": ConfigExpression("v - 1")})
    p = ConfigSection(None, {"x": x, "y": ConfigExpression("x.w + 2")})
    xx = ConfigSection(None, {"v": ConfigExpression("2")})
    q = ConfigSection(ConfigExpression("__extend__(p)"), {"x": xx})
    config = Config("", {"p": p, "q": q}, {})
    assert config.p.x.v == 1
    assert config.p.x.w == 0
    assert config.p.y == 2
    assert config.q.x.v == 2
    assert config.q.x.w == 1
    assert config.q.y == 3


def test_extended_section_meta_fails_to_extend_expression():
    p = ConfigExpression("0")
    q = ConfigSection(ConfigExpression("__extend__(p)"), {"x": ConfigExpression("1")})
    config = Config("", {"p": p, "q": q}, {})
    with pytest.raises(ConfigEvaluationError):
        config.q


def test_loaded_section_meta_allows_no_additional_entries():
    s = ConfigSection(ConfigExpression("__load__('')"), {"x": ConfigExpression("1")})
    config = Config("", {"s": s}, {})
    with pytest.raises(ConfigEvaluationError):
        config.s


def test_loaded_section_meta_raises_no_errors_when_not_accessed():
    s = ConfigSection(ConfigExpression("__load__('')"), {})
    a = ConfigSection(None, {"s": s})
    b = ConfigSection(ConfigExpression("__extend__(a)"), {})
    config = Config("", {"a": a, "b": b}, {})
    config.b


def test_loaded_section_meta_raises_proper_error_when_loading_fails():
    s = ConfigSection(ConfigExpression("__load__('')"), {})
    config = Config("", {"s": s}, {})
    with pytest.raises(ConfigEvaluationError):
        config.s


def test_loaded_section_meta_loads_entries_correctly(tmpdir):
    path = os.path.join(str(tmpdir), "test.config")
    with open(path, "w") as stream:
        stream.write("x = 1\ny = 2")
    s = ConfigSection(ConfigExpression("__load__(path)"), {})
    config = Config("", {"s": s, "z": ConfigExpression("s.x + s.y")}, {"path": path})
    assert config.z == 3
    assert config.s.y == 2
    assert config.s.x == 1
