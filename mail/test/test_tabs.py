from collections import namedtuple
from pytest import mark

from ora2pg.tools.tabs import (
    DEFAULT_TAB,
    make_tabs_list,
    make_lid_tab_map,
    LidTabMap,
)

Label = namedtuple('Label', [
    'lid', 'name', 'type'
])


@mark.parametrize(('tabs_mapping', 'expected'), [
    (None, []),
    ([], [DEFAULT_TAB]),
    ([{'type': DEFAULT_TAB, 'so_type': '42'}], [DEFAULT_TAB]),
    ([{'type': 'tab', 'so_type': '42'}], [DEFAULT_TAB, 'tab']),
    (
        [{'type': 'tab', 'so_type': '42'}, {'type': 'other', 'so_type': '100'}, {'type': 'tab', 'so_type': '66'}],
        [DEFAULT_TAB, 'tab', 'other']
    ),
])
def test_for_make_tabs_list_returns_full_uniq_list(tabs_mapping, expected):
    result = make_tabs_list(tabs_mapping)
    assert result == expected


@mark.parametrize(('labels', 'tabs_mapping', 'expected'), [
    ([], None, LidTabMap(None)),
    ([], [], LidTabMap([])),
    ([], [{'type': 'tab', 'so_type': '42'}], LidTabMap([])),
    ([Label('13', '100', 'type')], [{'type': 'tab', 'so_type': '42'}], LidTabMap([])),
    ([Label('13', '42', 'user')], [{'type': 'tab', 'so_type': '42'}], LidTabMap([])),
    ([Label('13', '42', 'type')], [{'type': 'tab', 'so_type': '42'}], LidTabMap([('13', 'tab')])),
    (
        [Label('11', '66', 'type'), Label('13', '42', 'type'), Label('42', '100', 'type')],
        [{'type': 'tab', 'so_type': '42'}, {'type': 'other', 'so_type': '100'}, {'type': 'tab', 'so_type': '66'}],
        LidTabMap([('13', 'tab'), ('42', 'other'), ('11', 'tab')])
    ),
])
def test_for_make_lid_tab_map_returns_correct_rules(labels, tabs_mapping, expected):
    result = make_lid_tab_map(labels, tabs_mapping)
    assert result == expected


@mark.parametrize(('lid_tab_map', 'msg_lids', 'expected'), [
    (LidTabMap(None), [], None),
    (LidTabMap([]), [], DEFAULT_TAB),
    (LidTabMap([]), ['1', '2'], DEFAULT_TAB),
    (LidTabMap([('1', 'tab')]), ['1', '2'], 'tab'),
    (LidTabMap([('2', 'tab'), ('1', 'other')]), ['1', '2'], 'tab'),
])
def test_for_get_tab_returns_correct_tab(lid_tab_map, msg_lids, expected):
    result = lid_tab_map.get_tab(msg_lids)
    assert result == expected
