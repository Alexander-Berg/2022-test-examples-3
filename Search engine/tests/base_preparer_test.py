import collections
import pytest

from urllib.parse import urlparse, parse_qs

from base_parsers import SerpParser  # noqa
from base_parsers import INIT_TIMESTAMP  # noqa
from base_parsers import FetchMethod  # noqa
from test_utils import read_json_from_fixture_file


@pytest.mark.parametrize("url,additional_cgis,resulting_url", [
    ("https://domain.com/search/?text=hello&foo=1", "foo=2&bar=1",
     "https://domain.com/search/?text=hello&foo=1&bar=1&foo=2"),
    ("https://domain.com/search/?text=hello&foo=1", "&foo=2&bar=1",
     "https://domain.com/search/?text=hello&foo=1&bar=1&foo=2"),
    ("https://domain.com/search?text=hello&foo=1", "foo=2&bar=1",
     "https://domain.com/search?text=hello&foo=1&bar=1&foo=2"),
    ("https://domain.com/search?text=hello&foo=1", "foo=1&foo=1",
     "https://domain.com/search?text=hello&foo=1&foo=1&foo=1"),
    ("https://domain.com/search?text=hello&foo=1", "&bar",
     "https://domain.com/search?text=hello&foo=1&bar"),
    ("https://domain.com/search?text=hello&foo=1", "bar",
     "https://domain.com/search?text=hello&foo=1&bar"),
    ("https://domain.com/search", "&foo=1",
     "https://domain.com/search?foo=1"),
    ("https://domain.com/search", "foo=1",
     "https://domain.com/search?foo=1"),
    ("https://domain.com/search/", "&foo=1",
     "https://domain.com/search/?foo=1"),
    ("https://domain.com/search/", "foo=1",
     "https://domain.com/search/?foo=1")

])
def test_append_strategy(url, additional_cgis, resulting_url):
    augumented_url = SerpParser._add_cgis_from_string(url, additional_cgis)
    augmented = urlparse(augumented_url)
    expected = urlparse(resulting_url)
    assert augmented.scheme == expected.scheme
    assert augmented.netloc == expected.netloc
    assert augmented.params == expected.params
    assert augmented.path == expected.path
    assert parse_qs(augmented.query) == parse_qs(expected.query)
    assert augmented.fragment == expected.fragment


@pytest.mark.parametrize("cgi_string,cgi_pairs", [
    ("foo=1&bar=1&foo=2",
     [("foo", "1"), ("bar", "1"), ("foo", "2")]),
    ("&foo=1&bar=1&foo=2",
     [("foo", "1"), ("bar", "1"), ("foo", "2")]),
    ("text=89086479109&lr=63&no-tests=1&exp_flags=baobab%3Dtree&sbh=1&foo=1&bar=1&foo=2",
     [("text", "89086479109"), ("lr", "63"), ("no-tests", "1"), ("exp_flags", "baobab=tree"), ("sbh", "1"), ("foo", "1"), ("bar", "1"), ("foo", "2")]),
    ("&foo=1&bar=",
     [("foo", "1"), ("bar", "")]),
    ("&foo=1&bar=1=1",  # is that valid?
     [("foo", "1"), ("bar", "1=1")]),
    ("&foo=1&bar",
     [("foo", "1"), ("bar", None)]),
])
def test_parse_qs(cgi_string, cgi_pairs):
    assert SerpParser._parse_qs(cgi_string) == cgi_pairs


@pytest.mark.parametrize("cgi_param,cgi_pair", [
    ("foo=1", ("foo", "1")),
    ("bar=1=2", ("bar", "1=2"))
])
def test_parse_qp(cgi_param, cgi_pair):
    assert SerpParser._parse_qp(cgi_param) == cgi_pair


@pytest.mark.parametrize("node,entity,result", [
    ({}, "anything", False),
    ({"allowed-to-edit": ["something"]}, "something", True),
    ({"allowed-to-edit": ["something"], "other-node": ["thing"]}, "thing", False),
])
def test_is_allowed_to_edit(node, entity, result):
    assert SerpParser._is_allowed_to_edit(node, entity) == result


@pytest.mark.parametrize("multimap,lst", [
    ({}, []),
    ({"foo": ["bar", "code", False], "bar": ["code"], "empty": []}, [("foo", "bar"), ("foo", "code"), ("foo", False), ("bar", "code")])
])
def test_convert_multimap_to_tuple_list(multimap, lst):
    assert SerpParser._convert_multimap_to_tuple_list(multimap) == lst


@pytest.mark.parametrize("node,result", [
    ({}, []),
    ({"per-set-parameters": {}}, []),
    ({"per-set-parameters": {"some-additional": {"name": [1, "value"]}}}, [("name", "1"), ("name", "value")])
])
def test_get_additional(node, result):
    assert SerpParser._get_additional(node, "some-additional") == result


@pytest.mark.parametrize("node,result", [
    ({}, {}),
    ({"restriction": {}}, {}),
    ({"restriction": {"some-node": ["some-value", True]}}, {"some-node": ["some-value", "True"]})
])
def test_get_cgi_restriction(node, result):
    assert SerpParser._get_cgi_restriction(node, "restriction") == result


@pytest.mark.parametrize("additional_parameters,result", [
    ({}, False),
    ({"ignoreProfileConflicts": True}, True),
    ({"ignoreProfileConflicts": False}, False)
])
def test_get_ignore_profile_conflicts(additional_parameters, result):
    assert SerpParser._get_ignore_profile_conflicts(additional_parameters) == result


@pytest.mark.parametrize("additional_parameters,result", [
    ({}, None),
    ({"profile": {"allowed-to-edit": []}}, {"allowed-to-edit": []})
])
def test_get_profile(additional_parameters, result):
    assert SerpParser._get_profile(additional_parameters) == result


@pytest.mark.parametrize("collection,result", [
    ([], True),
    (["some-value", "another-value"], True),
    (["another-value", "one-more-value"], False),
    ({}, True),
    ({"some-value": "some-data", "another-value": 1}, True),
    ({"another-value": False}, False)
])
def test_smart_contains(collection, result):
    assert SerpParser._smart_contains(collection, "some-value") == result


@pytest.mark.parametrize("all_cgi_parameters,allowed_cgi,not_allowed_cgi,expected_raised_value_error", [
    ([], {}, {}, False),
    ([("some-name", "some-value")], {}, {}, False),
    ([("some-name", "some-value")], {"other-name": []}, {}, True),
    ([("some-name", "some-value")], {"some-name": ["other-value"]}, {}, True),
    ([("some-name", "some-value")], {"some-name": []}, {}, False),
    ([("some-name", "some-value")], {"some-name": ["some-value"]}, {}, False),
    ([("some-name", "some-value")], {}, {"some-name": []}, True),
    ([("some-name", "some-value")], {}, {"some-name": ["another-value"]}, False),
    ([("some-name", "some-value")], {}, {"some-name": ["some-value"]}, True)
])
def test_check_cgi(all_cgi_parameters, allowed_cgi, not_allowed_cgi, expected_raised_value_error):
    raised_value_error = False
    try:
        SerpParser._check_cgi(all_cgi_parameters, allowed_cgi, not_allowed_cgi)
    except ValueError:
        raised_value_error = True

    assert expected_raised_value_error == raised_value_error, \
        "allowed_cgi: {}, not_allowed_cgi: {}, all_cgi_parameters: {}" \
            .format(allowed_cgi, not_allowed_cgi, all_cgi_parameters)


@pytest.mark.parametrize("tuples,new_tuples", [
    ([], {}),
    ([("some-name", "value"), ("some-name", "some-value"), ("name", "value"), ("name", "val")],
     {"some-name": ["some-value", "value"], "name": ["val", "value"]})
])
def test_get_canonical(tuples, new_tuples):
    assert SerpParser._get_canonical(tuples) == new_tuples


@pytest.mark.parametrize("entities_to_check,entities_etalon,result", [
    ([], [], True),
    ([("some-name", "value"), ("some-name", "some-value"), ("name", "value"), ("name", "val")],
     [("name", "val"), ("some-name", "some-value"), ("some-name", "value"), ("name", "value")], True),
    ([("some-name", "some-value"), ("name", "value"), ("name", "val")],
     [("name", "val"), ("some-name", "some-value"), ("some-name", "value"), ("name", "value")], False)
])
def test_check_entities_equals(entities_to_check, entities_etalon, result):
    assert SerpParser._check_entities_equals(entities_to_check, entities_etalon) == result


@pytest.mark.parametrize("allowed_cgi,name,value,result", [
    ({}, "cgi-name", "cgi-value", True),
    ({"cgi-name": []}, "cgi-name", "cgi-value", True),
    ({"cgi-name": ["cgi-value"]}, "cgi-name", "cgi-value", True),
    ({"other-name": ["cgi-value"]}, "cgi-name", "cgi-value", False),
    ({"cgi-name": ["other-value"]}, "cgi-name", "cgi-value", False)
])
def test_is_allowed_cgi(allowed_cgi, name, value, result):
    assert SerpParser._is_allowed_cgi(allowed_cgi, name, value) == result


@pytest.mark.parametrize("not_allowed_cgi,name,value,result", [
    ({}, "cgi-name", "cgi-value", False),
    ({"cgi-name": []}, "cgi-name", "cgi-value", True),
    ({"cgi-name": ["cgi-value"]}, "cgi-name", "cgi-value", True),
    ({"other-name": ["cgi-value"]}, "cgi-name", "cgi-value", False),
    ({"cgi-name": ["other-value"]}, "cgi-name", "cgi-value", False)
])
def test_is_not_allowed_cgi(not_allowed_cgi, name, value, result):
    assert SerpParser._is_not_allowed_cgi(not_allowed_cgi, name, value) == result


@pytest.mark.parametrize("all_cgi_parameters,allowed_cgi,not_allowed_cgi,expected_all_cgi_parameters", [
    ([], {}, {}, []),
    ([("name", "value")], {}, {}, [("name", "value")]),
    ([("name", "value")], {"name": []}, {}, [("name", "value")]),
    ([("name", "value")], {"other-name": []}, {}, []),
    ([("name", "value")], {"name": ["value"]}, {}, [("name", "value")]),
    ([("name", "value")], {"name": ["other-value"]}, {}, []),
    ([("name", "value")], {"name": []}, {"other-name": []}, [("name", "value")]),
    ([("name", "value")], {"other-name": []}, {"other-name": []}, []),
    ([("name", "value")], {"name": ["value"]}, {"other-name": []}, [("name", "value")]),
    ([("name", "value")], {"name": ["other-value"]}, {"other-name": []}, []),
    ([("name", "value")], {"name": []}, {"other-name": ["value"]}, [("name", "value")]),
    ([("name", "value")], {"other-name": []}, {"other-name": ["value"]}, []),
    ([("name", "value")], {"name": ["value"]}, {"other-name": ["value"]}, [("name", "value")]),
    ([("name", "value")], {"name": ["other-value"]}, {"other-name": ["value"]}, []),
    ([("name", "value")], {"name": []}, {"name": []}, []),
    ([("name", "value")], {"other-name": []}, {"name": []}, []),
    ([("name", "value")], {"name": ["value"]}, {"name": []}, []),
    ([("name", "value")], {"name": ["other-value"]}, {"name": []}, []),
    ([("name", "value")], {"name": []}, {"name": ["value"]}, []),
    ([("name", "value")], {"other-name": []}, {"name": ["value"]}, []),
    ([("name", "value")], {"name": ["value"]}, {"name": ["value"]}, []),
    ([("name", "value")], {"name": ["other-value"]}, {"name": ["value"]}, [])
])
def test_sanitize_cgi(all_cgi_parameters, allowed_cgi, not_allowed_cgi, expected_all_cgi_parameters):
    assert SerpParser._sanitize_cgi(all_cgi_parameters, allowed_cgi, not_allowed_cgi) == expected_all_cgi_parameters


@pytest.mark.parametrize("lst,separator,result", [
    ([], ":", []),
    ([("name", "value")], "=", ["name=value"]),
    ([("name", "value"), ("other-name", "other-value")], ": ", ["name: value", "other-name: other-value"])
])
def test_collect(lst, separator, result):
    assert SerpParser._collect(lst, separator) == result


@pytest.mark.parametrize("map,result", [
    ({}, {}),
    ({"name": 1, "other-name": False, "another-name": "some-value"},
     {"name": "1", "other-name": "False", "another-name": "some-value"}),
    ({"name": None, None: "value"}, {"name": None, None: "value"})
])
def test_stringify_map(map, result):
    assert SerpParser._stringify_map(map) == result


@pytest.mark.parametrize("multimap,result", [
    ({}, {}),
    ({"name": []}, {"name": []}),
    ({"name": [1, False, "some-value"]},
     {"name": ["1", "False", "some-value"]}),
    ({1: []}, {"1": []}),
    ({None: [], "some": [None]}, {None: [], "some": [None]})
])
def test_stringify_multimap(multimap, result):
    assert SerpParser._stringify_multimap(multimap) == result


@pytest.mark.parametrize("lst,result", [
    ([], []),
    ([("name", 1), ("other-name", "value"), (False, 2)], [("name", "1"), ("other-name", "value"), ("False", "2")]),
    ([(None, "value"), ("name", None)], [(None, "value"), ("name", None)])
])
def test_stringify_list_of_tuples(lst, result):
    assert SerpParser._stringify_list_of_tuples(lst) == result


def _make_test_profile(is_allowed_to_edit, allowed_cgi={}, not_allowed_cgi={}):
    allowed_to_edit = ["additional-cgi"] if is_allowed_to_edit else []
    per_set_parameters = {"additional-cgi": {"name": ["value"], "other-name": ["other-value"]}}
    return {"allowed-to-edit": allowed_to_edit, "allowed-cgi": allowed_cgi, "not-allowed-cgi": not_allowed_cgi,
            "per-set-parameters": per_set_parameters, "profile": {"id": "profile-id"}}


# This helper type should improve readability, nothing else.
TMEWP_Parameters = collections.namedtuple(
    "TMEWP_Parameters",
    ["profile", "ignore_profile_conflicts", "entities", "result"]
)


@pytest.mark.parametrize("profile,ignore_profile_conflicts,entities,result", [
    TMEWP_Parameters(None, False, [], []),
    TMEWP_Parameters(None, True, [], []),
    TMEWP_Parameters(None, False, [("name", "value")], [("name", "value")]),
    TMEWP_Parameters(None, True, [("name", "value")], [("name", "value")]),
    TMEWP_Parameters(
        _make_test_profile(is_allowed_to_edit=True),
        ignore_profile_conflicts=False,
        entities=[("name", "value"), ("other-name", "other-value")],
        result=[("name", "value"), ("other-name", "other-value"), ("name", "value"), ("other-name", "other-value")]
    ),
    TMEWP_Parameters(
        _make_test_profile(is_allowed_to_edit=False), False,
        entities=[("name", "value"), ("other-name", "other-value")],
        result=[("name", "value"), ("other-name", "other-value")]
    ),
    TMEWP_Parameters(
        _make_test_profile(True), False,
        entities=[("another-name", "another-value")],
        result=[("another-name", "another-value"), ("name", "value"), ("other-name", "other-value")]
    ),
    TMEWP_Parameters(
        _make_test_profile(False), False,
        entities=[("another-name", "another-value")],
        result=None
    ),
    TMEWP_Parameters(
        _make_test_profile(True), True,
        entities=[("name", "value"), ("other-name", "other-value")],
        result=[("name", "value"), ("other-name", "other-value"), ("name", "value"), ("other-name", "other-value")]
    ),
    TMEWP_Parameters(
        _make_test_profile(False), True,
        entities=[("name", "value"), ("other-name", "other-value")],
        result=[("name", "value"), ("other-name", "other-value"), ("name", "value"), ("other-name", "other-value")]
    ),
    TMEWP_Parameters(
        _make_test_profile(True), True,
        entities=[("another-name", "another-value")],
        result=[("another-name", "another-value"), ("name", "value"), ("other-name", "other-value")]
    ),
    TMEWP_Parameters(
        _make_test_profile(False), True,
        entities=[("another-name", "another-value")],
        result=[("another-name", "another-value"), ("name", "value"), ("other-name", "other-value")]
    )
])
def test_merge_entities_with_profile(profile, ignore_profile_conflicts, entities, result):
    try:
        actual_result = \
            SerpParser._merge_entities_with_profile(profile, ignore_profile_conflicts, entities, "additional-cgi")
        assert result is not None, 'Expected ValueError but it was not thrown'
        assert result == actual_result
    except ValueError as e:
        assert result is None, 'ValueError exception is not expected: {}'.format(e)


TMCWP_Parameters = collections.namedtuple(
    "TMCWP_Parameters",
    ["profile", "ignore_profile_conflicts", "all_cgi_parameters", "preparer_cgi", "result"]
)


@pytest.mark.parametrize("profile,ignore_profile_conflicts,all_cgi_parameters,preparer_cgi,result", [
    (None, False, [], [], []),
    (None, True, [], [], []),
    TMCWP_Parameters(
        None, False,
        all_cgi_parameters=[("name", "value")],
        preparer_cgi=[("other-name", "other-value")],
        result=[("name", "value"), ("other-name", "other-value")]
    ),
    TMCWP_Parameters(
        None,
        True,
        [("name", "value")],
        [("other-name", "other-value")],
        [("name", "value"), ("other-name", "other-value")],
    ),
    TMCWP_Parameters(
        _make_test_profile(False),
        False,
        all_cgi_parameters=[("name", "value"), ("other-name", "other-value")],
        preparer_cgi=[("prepaper", "is-not-restricted")],
        result=[
            ("flag", "restriction_profile=profile-id"),
            ("prepaper", "is-not-restricted"),
            ("name", "value"),
            ("other-name", "other-value"),
        ],
    ),
    TMCWP_Parameters(_make_test_profile(False), False, [("name", "value")], [], None),
    TMCWP_Parameters(
        _make_test_profile(False),
        True,
        [("name", "value")],
        [("prepaper", "is-not-restricted")],
        [
            ("flag", "restriction_profile=profile-id"),
            ("prepaper", "is-not-restricted"),
            ("name", "value"),
            ("other-name", "other-value"),
        ],
    ),
    TMCWP_Parameters(
        _make_test_profile(False),
        True,
        all_cgi_parameters=[("name", "value")],
        preparer_cgi=[("prepaper", "is-not-restricted")],
        result=[
            ("flag", "restriction_profile=profile-id"),
            ("prepaper", "is-not-restricted"),
            ("name", "value"),
            ("other-name", "other-value"),
        ],
    ),
    TMCWP_Parameters(
        _make_test_profile(True, {"name": ["some-value"]}),
        False,
        all_cgi_parameters=[("name", "value")],
        preparer_cgi=[],
        result=None,
    ),
    TMCWP_Parameters(
        _make_test_profile(True, {}, {"other-name": ["other-value"]}),
        False,
        [("other-name", "other-value")],
        [],
        None,
    ),
    TMCWP_Parameters(
        _make_test_profile(True, allowed_cgi={"name": ["some-value"]}),
        True,
        all_cgi_parameters=[("name", "value"), ("name", "some-value")],
        preparer_cgi=[("prepaper", "is-not-restricted")],
        result=[
            ("flag", "restriction_profile=profile-id"),
            ("prepaper", "is-not-restricted"),
            ("name", "value"),
            ("other-name", "other-value"),
            ("name", "some-value"),
        ],
    ),
    TMCWP_Parameters(
        _make_test_profile(True, {}, {"other-name": ["other-value"]}),
        True,
        all_cgi_parameters=[("other-name", "other-value"), ("name", "value")],
        preparer_cgi=[("prepaper", "is-not-restricted")],
        result=[
            ("flag", "restriction_profile=profile-id"),
            ("prepaper", "is-not-restricted"),
            ("name", "value"),
            ("other-name", "other-value"),
            ("name", "value"),
        ],
    )
])
def test_merge_cgi_with_profile(profile, ignore_profile_conflicts, all_cgi_parameters, preparer_cgi, result):
    try:
        actual_result = SerpParser._merge_cgi_with_profile(profile, ignore_profile_conflicts, all_cgi_parameters,
                                                           preparer_cgi, "additional-cgi")
        assert result is not None, 'Expected ValueError but it was not thrown'
        assert result == actual_result
    except ValueError as e:
        assert result is None, 'ValueError exception is not expected: {}'.format(e)


class SampleCommonPreparer(SerpParser):
    URL_TEMPLATE = 'https://{host}'

    def _prepare_headers_multimap(self, basket_query, host, additional_parameters):
        return basket_query["headers"]

    def _prepare_cookies_multimap(self, basket_query, host, additional_parameters):
        return basket_query["cookies"]

    def _prepare_cgi(self, basket_query, host, additional_parameters, user_cgi_parameters):
        return basket_query["cgi"]

    def _prepare_userdata(self, basket_query, host, additional_parameters, soy_curl):
        return basket_query["userdata"]

    def _prepare_postdata(self, basket_query, host, additional_parameters):
        return basket_query["postdata"]


def test_prepare():
    preparer = SampleCommonPreparer()
    pseudo_basket_headers = {"header-1": ["header-value-1"]}
    pseudo_basket_cookies = {"cookie-1": ["cookie-value-1"]}
    pseudo_basket_cgi = [("cgi-1", "value-1")]
    pseudo_basket_userdata = {"some-data": "value"}
    additional_cgi_string = "cgi-2=cgi-value-2&cgi-5=cgi-value-5"
    profile_allowed_cgi = {"cgi-1": [], "cgi-2": [], "timestamp-cgi": [], "cgi-5": []}
    profile_not_allowed_cgi = {"cgi-2": []}
    profile_allowed_to_edit = ["additional-cgi", "additional-headers", "additional-cookies"]
    profile_additional_cgi = {"cgi-3": ["value-3"], "cgi-4": ["cgi-value-4"]}
    profile_additional_headers = {"header-2": ["header-value-2"]}
    profile_additional_cookies = {"cookie-2": ["cookie-value-2"]}

    pseudo_basket_query = {"headers": pseudo_basket_headers, "cookies": pseudo_basket_cookies, "cgi": pseudo_basket_cgi,
                           "userdata": pseudo_basket_userdata}
    per_set_parameters = {"additional-cgi": profile_additional_cgi, "additional-headers": profile_additional_headers,
                          "additional-cookies": profile_additional_cookies}
    profile = {"allowed-to-edit": profile_allowed_to_edit, "allowed-cgi": profile_allowed_cgi,
               "not-allowed-cgi": profile_not_allowed_cgi, "per-set-parameters": per_set_parameters, "profile": {"id": "dummy"}}
    additional_parameters = {"cgi": additional_cgi_string, "timestampCgi": "timestamp-cgi",
                             "profile": profile, "ignoreProfileConflicts": True}

    actual_object = preparer.prepare(362, pseudo_basket_query, "example.com", additional_parameters)

    expected_uri = \
        "https://example.com?flag=restriction_profile%3Ddummy&cgi-1=value-1&cgi-3=value-3&cgi-4=cgi-value-4&cgi-5=cgi-value-5&timestamp-cgi={}"\
            .format(INIT_TIMESTAMP)
    expected_object = {
        "id": "362",
        "method": "GET",
        "uri": expected_uri,
        "headers": ["header-1: header-value-1", "header-2: header-value-2"],
        "cookies": ["cookie-1=cookie-value-1", "cookie-2=cookie-value-2"],
        "userdata": pseudo_basket_userdata,
    }

    assert actual_object == expected_object


def test_prepare_post():
    preparer = SampleCommonPreparer()
    pseudo_basket_headers = {"header-1": ["header-value-1"]}
    pseudo_basket_cookies = {"cookie-1": ["cookie-value-1"]}
    pseudo_basket_cgi = [("cgi-1", "value-1")]
    pseudo_basket_userdata = {"some-data": "value"}
    pseudo_basket_postdata = {"some-postdata": "value"}
    additional_cgi_string = "cgi-2=cgi-value-2&cgi-5=cgi-value-5"
    profile_allowed_cgi = {"cgi-1": [], "cgi-2": [], "timestamp-cgi": [], "cgi-5": []}
    profile_not_allowed_cgi = {"cgi-2": []}
    profile_allowed_to_edit = ["additional-cgi", "additional-headers", "additional-cookies"]
    profile_additional_cgi = {"cgi-3": ["value-3"], "cgi-4": ["cgi-value-4"]}
    profile_additional_headers = {"header-2": ["header-value-2"]}
    profile_additional_cookies = {"cookie-2": ["cookie-value-2"]}

    pseudo_basket_query = {"headers": pseudo_basket_headers, "cookies": pseudo_basket_cookies, "cgi": pseudo_basket_cgi,
                           "userdata": pseudo_basket_userdata, "postdata": pseudo_basket_postdata}
    per_set_parameters = {"additional-cgi": profile_additional_cgi, "additional-headers": profile_additional_headers,
                          "additional-cookies": profile_additional_cookies}
    profile = {"allowed-to-edit": profile_allowed_to_edit, "allowed-cgi": profile_allowed_cgi,
               "not-allowed-cgi": profile_not_allowed_cgi, "per-set-parameters": per_set_parameters, "profile": {"id": "dummy"}}
    additional_parameters = {"cgi": additional_cgi_string, "timestampCgi": "timestamp-cgi",
                             "profile": profile, "ignoreProfileConflicts": True}

    preparer.FETCH_METHOD = FetchMethod.POST

    actual_object = preparer.prepare(362, pseudo_basket_query, "example.com", additional_parameters)

    expected_uri = \
        "https://example.com?flag=restriction_profile%3Ddummy&cgi-1=value-1&cgi-3=value-3&cgi-4=cgi-value-4&cgi-5=cgi-value-5&timestamp-cgi={}"\
            .format(INIT_TIMESTAMP)
    expected_object = {
        "id": "362",
        "method": FetchMethod.POST,
        "uri": expected_uri,
        "headers": ["header-1: header-value-1", "header-2: header-value-2"],
        "cookies": ["cookie-1=cookie-value-1", "cookie-2=cookie-value-2"],
        "userdata": pseudo_basket_userdata,
        "postdata": pseudo_basket_postdata,
    }

    assert actual_object == expected_object


def test_prepare_with_headers_cookies_override():
    preparer = SampleCommonPreparer()
    pseudo_basket_headers = {"header-1": ["header-value-1"], "header-2": ["header-value-2"], "header-3": ["header-value-3"]}
    pseudo_basket_cookies = {"cookie-1": ["cookie-value-1"], "cookie-2": ["cookie-value-2"], "cookie-3": ["cookie-value-3"]}
    pseudo_basket_per_query_headers = {"header-3": ["header-value-3-per-query"]}
    pseudo_basket_per_query_cookies = {"cookie-3": ["cookie-value-3-per-query"]}
    pseudo_basket_per_query_parameters = {"additional-headers": pseudo_basket_per_query_headers,
                                          "additional-cookies": pseudo_basket_per_query_cookies}

    ssr_per_set_headers = {"header-2": ["header-value-2-per-set"], "header-3": ["header-value-3-per-set"]}
    ssr_per_set_cookies = {"cookie-2": ["cookie-value-2-per-set"], "cookie-3": ["cookie-value-3-per-set"]}

    pseudo_basket_query = {"headers": pseudo_basket_headers, "cookies": pseudo_basket_cookies,
                           "per-query-parameters": pseudo_basket_per_query_parameters, "cgi": [], "userdata": {}}
    ssr = {"per-set-parameters": {"additional-headers": ssr_per_set_headers,
                                  "additional-cookies": ssr_per_set_cookies}}
    profile = {"profile": {"id": "dummy"}}
    additional_parameters = {"profile": profile, "ssr": ssr, "ignoreProfileConflicts": True}

    actual_object = preparer.prepare(362, pseudo_basket_query, "example.com", additional_parameters)

    expected_uri = "https://example.com?flag=restriction_profile%3Ddummy"
    expected_object = {
        "id": "362",
        "method": "GET",
        "uri": expected_uri,
        "headers": ["header-1: header-value-1", "header-2: header-value-2-per-set", "header-3: header-value-3-per-query"],
        "cookies": ["cookie-1=cookie-value-1", "cookie-2=cookie-value-2-per-set", "cookie-3=cookie-value-3-per-query"],
        "userdata": {}
    }

    assert actual_object == expected_object


class SampleTimestampCgiPreparer(SerpParser):

    URL_TEMPLATE = 'https://{host}/test'


def test_timestamp_cgi():
    preparer = SampleTimestampCgiPreparer()
    assert preparer._prepare_url({}, 'ya.ru', {'timestampCgi': 'pron'}) == 'https://ya.ru/test?pron=%d' % INIT_TIMESTAMP


@pytest.mark.parametrize("country, tld", [
    ("DE", "com"),
    ("RU", "ru"),
])
def test_tld_mapping(country, tld):
    tlds = SerpParser().get_tlds()
    assert tlds.get(country) == tld


def test_per_query_cgi_monitoring():
    basket_query = read_json_from_fixture_file("base_preparer_data", "monitoring_query.json")
    check_per_query_cgi(
        basket_query,
        expected_per_query_cgi=[("meow", "meow"), ("foo", None)],
        expected_url="https://tushkan.poisk.ru/search/?meow=meow&foo"
    )


def test_per_query_cgi_soy_workflow():
    basket_query = read_json_from_fixture_file("base_preparer_data", "soy_workflow_query.json")
    check_per_query_cgi(
        basket_query,
        # 1,
        expected_per_query_cgi=[("foo", ""), ("meow", "meow"), ("meow", "gav")],
        expected_url="https://tushkan.poisk.ru/search/?foo&meow=meow&meow=gav"
    )


def check_per_query_cgi(basket_query, expected_per_query_cgi, expected_url):
    parser = SerpParser()
    parser.URL_TEMPLATE = 'https://{host}/search/'
    assert parser._prepare_per_query_cgi(basket_query, "tushkan.poisk.ru") == expected_per_query_cgi
    assert parser._prepare_url(basket_query, "tushkan.poisk.ru") == expected_url
