import six

from crypta.lib.python.audience import id_converter


if six.PY3:
    long = int


def test_segment_id_to_goal_id():
    assert long('2000000001') == id_converter.segment_id_to_goal_id(1)
    assert long('2000001024') == id_converter.segment_id_to_goal_id(1024)
    assert long('2499000000') == id_converter.segment_id_to_goal_id(499000000)
