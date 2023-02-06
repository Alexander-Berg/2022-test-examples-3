import pytest

from crypta.dmp.common.upload_to_audience import (
    segment_name,
    upload
)


@pytest.mark.parametrize("value,reference", [
    ("hsdfhkls [123213]", segment_name.AudienceSegmentName(123213, "hsdfhkls")),
    ("hsd[019] f [hk] ls [123214]", segment_name.AudienceSegmentName(123214, "hsd[019] f [hk] ls")),

])
def test_segment_name_deserialize(value, reference):
    res = segment_name.deserialize(value)
    assert reference.title == res.title and reference.aam_segment_id == res.aam_segment_id


@pytest.mark.parametrize("value", [
    ("hsdfhklssd fsdf"),
    ("hsdfhkls [1sdfds23213]"),
    ("hsdfhkls [1sdfds23213] sdafds"),
    ("hsdfhkls [1sdfds23213]1sdafds")
])
def test_segment_name_deserialize_fail(value):
    with pytest.raises(Exception):
        segment_name.deserialize(value)


def test_serialize_segment_name():
    assert "hsd f hk ls [123213]" == segment_name.serialize(segment_name.AudienceSegmentName(123213, "hsd f hk ls"))


@pytest.mark.parametrize("value,reference", [
    ({"en_US": "en_title"}, "en_title"),
    ({"ua_UA": "ua_title"}, "ua_title"),
    ({"tr_TR": "tr_title"}, "tr_title"),
    ({"ru_RU": "ru_title"}, "ru_title"),
    ({"ru_RU": "ru_title", "en_US": "en_title"}, "ru_title"),
    ({"ua_UA": "ua_title", "en_US": "en_title"}, "en_title"),
    ({"tr_TR": "tr_title", "ua_UA": "ua_title", "ru_RU": "ru_title", "en_US": "en_title"}, "ru_title")
])
def test_get_title_positive(value, reference):
    assert reference == upload.get_title(value)


@pytest.mark.parametrize("value", [
    {"": "eng_title"},
    {"snUS": "eng_title"},
    {"sn_US": "eng_title"}
])
def test_get_title_negative(value):
    with pytest.raises(Exception):
        upload.get_title(value)


@pytest.mark.parametrize("grants,acl", [
    pytest.param(["login1", "login2"], ["login1", "login2", "login3"], id="add only"),
    pytest.param(["login1", "login2", "login3"], ["login1", "login2"], id="remove only"),
    pytest.param(["login1", "login2"], ["login1", "login3"], id="add and remove"),
    pytest.param(["login1", "login2"], ["login1", "login2"], id="same"),
])
def test_process_grants(acl, segment_id, mock_audience_server, audience_upload_reducer):
    audience_upload_reducer.process_grants(segment_id, acl)
    assert {segment_id: set(acl)} == mock_audience_server.db
