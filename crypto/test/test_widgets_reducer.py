import pytest

from crypta.profile.runners.segments.lib.coded_segments import widgets

VALID_ID = "F2760553-7C49-443F-B65C-AA9FE397262E"
SOURCE = "bro"
ERROR_ID = "Code: 160. DB::Exception: Received from mtmobgiga112-3.metrika.yandex.net:9000. DB::Exception: Query is " \
           "executing too slow: 337936.12848743785 rows/sec., minimum: 1000000: While executing MergeTreeThread. (" \
           "TOO_SLOW) (version 21.10.4.26 (official build))"


@pytest.mark.parametrize("key,rows,result", [
    pytest.param(
        {"id": VALID_ID, "source": SOURCE},
        [
            {'widget': '0', 'id': VALID_ID, 'source': SOURCE},
            {'widget': '1', 'id': VALID_ID, 'source': SOURCE},
            {'widget': '0', 'id': VALID_ID, 'source': SOURCE}
        ],
        [
            {
                'id': VALID_ID,
                'id_type': 'mm_device_id',
                'segment_name': SOURCE + '_widget',
            },
            {
                'id': VALID_ID,
                'id_type': 'mm_device_id',
                'segment_name': SOURCE,
            }
        ],
        id="ValidWithWidget1",
    ),
    pytest.param(
        {"id": VALID_ID, "source": SOURCE},
        [
            {'widget': '0', 'id': VALID_ID, 'source': SOURCE},
            {'widget': '0', 'id': VALID_ID, 'source': SOURCE},
            {'widget': '0', 'id': VALID_ID, 'source': SOURCE}
        ],
        [
            {
                'id': VALID_ID,
                'id_type': 'mm_device_id',
                'segment_name': SOURCE,
            }
        ],
        id="ValidWithWidget0",
    ),
    pytest.param(
        {"id": ERROR_ID},
        [{"id": ERROR_ID}],
        [],
        id="Error",
    ),
])
def test_widgets_reducer(key, rows, result):
    assert result == list(widgets.segment_name_reducer(key, rows))
