import pytest

from yamarec_metarouter.pattern import Pattern


def test_pattern_initiializes_correctly():
    Pattern("(?:/model/(?P<model_id>\d+)|/product/(?P<model_id>\d+))")
    with pytest.raises(Exception):
        Pattern("/(?P<model_id>\d+)xxxx:)")


def test_pattern_matches_correctly():
    model = Pattern("(?:/model/(?P<model_id>\d+)/?|/product/(?P<model_id>\d+)/supertab)$")
    assert model.match("/model/123")
    assert model.match("/model/123/")
    assert model.match("/product/123/supertab")
    assert not model.match("/model/a1")
    assert not model.match("/product/123")
    assert not model.match("/product/123/supertab?p=1")


def test_pattern_extracts_correctly():
    model = Pattern("(?:/model/(?P<model_id>\d+)/?|/product/(?P<model_id>\d+)/(?P<tab>\w+))$")
    assert model.extract("/model/123") == {"model_id": "123"}
    assert model.extract("/model/123/") == {"model_id": "123"}
    assert model.extract("/product/123/supertab") == {"model_id": "123", "tab": "supertab"}
    assert model.extract("/model/a1") is None
    assert model.extract("/product/123") is None
    assert model.extract("/product/123/supertab?p=1") is None
