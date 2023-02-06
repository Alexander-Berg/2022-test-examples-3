from search.geo.tools.personal_pois.lib.reducers import (
    UpdateColumnReducer,
    UpdateJoinColumnReducer,
    AddColumnReducer,
    SaasReducer,
    MergeKeysAndTagReducer,
    CoordinatesIndexerReducer,
    CoordinatesIndexerGrouper
)

from search.geo.tools.personal_pois.lib.ut.reducers_rows import (
    update_column_reducer_rows,
    update_join_column_reducer_rows,
    add_column_reducer_with_default_rows,
    add_column_reducer_without_default_rows,
    saas_reducer_rows,
    merge_keys_and_tag_reducer_rows,
    coordinates_indexer_reducer_for_oid_rows,
    coordinates_indexer_reducer_for_puid_rows,
    coordinates_indexer_grouper_rows,
)

from search.geo.tools.personal_pois.lib.reducers_helpers import get_rendered_zooms


def reducer_test(reducer, rows):
    for row in rows:
        result = list(reducer(row['input']['key'], row['input']['rows']))
        assert result == row['output']


def test_update_column_reducer():
    reducer_test(UpdateColumnReducer('info'), update_column_reducer_rows)


def test_update_join_column_reducer():
    reducer_test(UpdateJoinColumnReducer('info'), update_join_column_reducer_rows)


def test_add_column_reducer_with_default():
    reducer_test(
        AddColumnReducer('value', 'key'),
        add_column_reducer_with_default_rows
    )


def test_add_column_reducer_without_default():
    reducer_test(
        AddColumnReducer('value'),
        add_column_reducer_without_default_rows
    )


def test_saas_reducer():
    reducer_test(SaasReducer(0), saas_reducer_rows)


def test_merge_keys_and_tag_reducer():
    reducer_test(MergeKeysAndTagReducer('tag'), merge_keys_and_tag_reducer_rows)


def test_coordinates_indexer_reducer_for_oid():
    reducer_test(
        CoordinatesIndexerReducer(10, additional_info=get_rendered_zooms),
        coordinates_indexer_reducer_for_oid_rows
    )


def test_coordinates_indexer_reducer_for_puid():
    reducer_test(
        CoordinatesIndexerReducer(10, main_id='puid', main_is_near=False),
        coordinates_indexer_reducer_for_puid_rows
    )


def test_coordinates_indexer_grouper():
    reducer_test(
        CoordinatesIndexerGrouper(),
        coordinates_indexer_grouper_rows
    )
