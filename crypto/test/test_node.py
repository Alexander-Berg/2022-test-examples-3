import pytest

import yt.wrapper as yt

from crypta.spine.pushers.common import mocks


PATH = "foo/bar"
CREATION_TIME = "2021-06-02T00:00:00.000000Z"
TABLE_TYPE = "table"
MAP_NODE_TYPE = "map_node"
CUSTOM_ATTR_1 = "custom_attr_1"
CUSTOM_ATTR_2 = "custom_attr_2"
USER_ATTRIBUTES = {
    CUSTOM_ATTR_1: "custom_value_1",
    CUSTOM_ATTR_2: "custom_value_2",
}


@pytest.mark.parametrize("type", [TABLE_TYPE, MAP_NODE_TYPE])
def test_node_construction(type):
    node = mocks.Node(PATH, CREATION_TIME, type=type, attributes=USER_ATTRIBUTES)

    assert PATH == node.path
    assert PATH == node.get("path")

    assert CREATION_TIME == node.creation_time
    assert CREATION_TIME == node.get("creation_time")

    assert type == node.type
    assert type == node.get("type")

    assert USER_ATTRIBUTES[CUSTOM_ATTR_1] == node.custom_attr_1
    assert USER_ATTRIBUTES[CUSTOM_ATTR_1] == node.get("custom_attr_1")

    assert USER_ATTRIBUTES[CUSTOM_ATTR_2] == node.custom_attr_2
    assert USER_ATTRIBUTES[CUSTOM_ATTR_2] == node.get("custom_attr_2")


def test_default_type():
    node = mocks.Node(PATH, CREATION_TIME)
    assert TABLE_TYPE == node.type


def test_builtin_attributes_not_overwritten():
    attributes = dict(USER_ATTRIBUTES)
    attributes["path"] = PATH + "/baz"
    attributes["creation_time"] = "2000-01-01T00:00:00.000000Z"
    attributes["type"] = "link"
    attributes["children"] = {"a_table": mocks.Node("a/b/c", CREATION_TIME)}

    map_node = mocks.Node(PATH, CREATION_TIME, type=MAP_NODE_TYPE, attributes=attributes)

    assert PATH == map_node.path
    assert CREATION_TIME == map_node.creation_time
    assert MAP_NODE_TYPE == map_node.type
    assert {} == map_node.children
    assert USER_ATTRIBUTES[CUSTOM_ATTR_1] == map_node.custom_attr_1
    assert USER_ATTRIBUTES[CUSTOM_ATTR_2] == map_node.custom_attr_2


def test_add_children():
    node = mocks.Node(PATH, CREATION_TIME, type=MAP_NODE_TYPE)

    child = mocks.Node(PATH + "/baz", CREATION_TIME)
    node.add(child)

    assert {"baz": child} == node.children


def test_to_ypath():
    node = mocks.Node(PATH, CREATION_TIME)
    mock_yt_client = mocks.MockYtClient([])

    assert yt.YPath(PATH, attributes={"creation_time": CREATION_TIME}, client=mock_yt_client) == node.to_ypath(client=mock_yt_client)
