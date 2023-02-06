import pytest
import market.lilucrm.tools.tracker.gantt as gantt
from market.lilucrm.tools.tracker.issue_graph import IssueGraph, Issue


def test_no_items_for_empty_graph():
    graph = IssueGraph()
    items = gantt._prepare_dataframe(graph)
    assert len(items) == 0


def test_items_single_root_graph():
    graph = IssueGraph(
        issues=[
            Issue("A"),
            Issue("B"),
            Issue("C"),
            Issue("D")
        ],
        links={
            "A": ["B", "C"],
            "B": ["D"],
            "C": ["D"]
        }
    )

    items = gantt._prepare_dataframe(graph)
    assert len(items) == 4

    assert_item(("A", 0, 1), items[0])
    assert_item(("B", 1, 2), items[1])
    assert_item(("C", 1, 2), items[2])
    assert_item(("D", 2, 3), items[3])


def test_items_multiroot_graph():
    graph = IssueGraph(
        issues=[
            Issue("A"),
            Issue("B"),
            Issue("C"),
            Issue("D"),
            Issue("E"),
            Issue("F"),
            Issue("G"),
            Issue("H")
        ],
        links={
            "A": ["B", "F"],
            "B": ["C", "D"],
            "C": ["E"],
            "D": ["E"],
            "G": ["F"],
            "F": ["E"]
        }
    )

    items = gantt._prepare_dataframe(graph)
    assert len(items) == 8

    assert items[0]["key"] == "A"
    assert items[1]["key"] == "B"
    assert items[2]["key"] == "C"
    assert items[3]["key"] == "D"
    assert items[4]["key"] == "G"
    assert items[5]["key"] == "F"
    assert items[6]["key"] == "E"
    assert items[7]["key"] == "H"


def test_take_1_sp_as_default():
    """Если оценка тикета не указана в трекере, при построении диаграммы принимается
    дефолтная оценка в 1sp"""
    graph = IssueGraph(
        issues=[
            Issue("A", sp=None),
            Issue("B", sp=3)
        ],
        links={
            "A": ["B"]
        }
    )

    items = gantt._prepare_dataframe(graph)
    assert len(items) == 2

    assert_item(("A", 0, 1), items[0])
    assert_item(("B", 1, 4), items[1])


def test_raise_exception_if_cycle_is_delected():
    graph = IssueGraph(
        issues=[
            Issue("A"),
            Issue("B"),
            Issue("C"),
            Issue("D")
        ],
        links={
            "A": ["B"],
            "B": ["C"],
            "C": ["D"],
            "D": ["B"]
        }
    )

    with pytest.raises(gantt.CycleDetected):
        gantt._prepare_dataframe(graph)


def assert_item(expected, actual):
    assert actual["key"] == expected[0]
    assert actual["start"] == expected[1]
    assert actual["end"] == expected[2]
