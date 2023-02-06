import pytest
import yatest.common
import yt.wrapper as yt

from crypta.lab.lib.samples.test import constants
import crypta.lab.lib.samples.samples as descriptor
from crypta.lib.python import (
    time_utils,
)
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


pytest_plugins = [
    'crypta.siberia.bin.common.test_helpers.fixtures',
    'crypta.lib.python.yql.test_helpers.fixtures',
]


def sample_item_schema():
    return schema_utils.yt_schema_from_dict({
        constants.ID_KEY: 'any',
        constants.GROUPING_KEY: 'string',
    })


def test_get_subsample_info(get_subsample_info_task, clean_local_yt, conf, frozen_time):
    return tests.yt_test_func(
        yt_client=clean_local_yt.yt_client,
        func=lambda: test_helpers.execute(get_subsample_info_task),
        data_path=yatest.common.test_source_path('data/get_subsample_info'),
        input_tables=[
            (tables.YsonTable('src_view.yson', yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, constants.SRC_VIEW_ID)), tests.TableIsNotChanged()),

        ],
        output_tables=[
            (
                tables.YsonTable(
                    'subsamples_info.yson',
                    yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples_info'),
                    on_write=tables.OnRead(sort_by=constants.GROUP_NAME),
                ),
                tests.Diff()
            ),
        ],
    )


@pytest.mark.parametrize('src_view', ['src_view_str.yson', 'src_view_int.yson'])
def test_prepare_subsamples(src_view, prepare_subsamples_task, get_subsample_info_task, clean_local_yt, conf, frozen_time):
    return tests.yt_test_func(
        yt_client=clean_local_yt.yt_client,
        func=lambda: test_helpers.execute(prepare_subsamples_task),
        data_path=yatest.common.test_source_path('data/prepare_subsamples'),
        input_tables=[
            (
                tables.YsonTable(
                    src_view,
                    yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, constants.SRC_VIEW_ID),
                    on_write=tables.OnWrite(
                        attributes={
                            get_subsample_info_task._attribute: time_utils.get_current_moscow_datetime().date().isoformat(),
                        },
                    ),
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    'subsamples_info.yson',
                    yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples_info'),
                    on_write=tables.OnWrite(
                        attributes={
                            get_subsample_info_task._attribute: time_utils.get_current_moscow_datetime().date().isoformat(),
                            'schema': descriptor.subsample_info_schema(),
                        },
                        sort_by=constants.GROUP_NAME,
                    )
                ),
                tests.TableIsNotChanged()
            )
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'subsample_group_0.yson', yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples', 'group_0')
                ),
                tests.Diff()
            ),
            (
                tables.YsonTable(
                    'subsample_group_1.yson', yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples', 'group_1')
                ),
                tests.Diff()
            ),
            (
                tables.YsonTable(
                    'subsample_https:--host.ru-page.yson', yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples', 'https:--host.ru-page-a=r-dhdhdhdhgs')
                ),
                tests.Diff()
            )
        ],
    )


@pytest.mark.parametrize(
    'src_view,subsample_group_0,subsample_group_1',
    [
        ('src_view_str.yson', 'subsample_group_0_str.yson', 'subsample_group_1_str.yson'),
        ('src_view_int.yson', 'subsample_group_0_int.yson', 'subsample_group_1_int.yson'),
    ],
)
def test_describe_in_siberia(
    # TODO(unretrofied): use old name of description task (Describe) due to backward copmpatibility, to be deleted after tests
    src_view, subsample_group_0, subsample_group_1, describe_subsamples_task_old, prepare_subsamples_task,
    get_subsample_info_task, clean_local_yt, conf, frozen_time
):
    sample_dir = yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID)
    subsamples_dir = yt.ypath_join(sample_dir, 'subsamples')

    return tests.yt_test_func(
        yt_client=clean_local_yt.yt_client,
        func=lambda: test_helpers.execute(describe_subsamples_task_old),
        data_path=yatest.common.test_source_path('data/describe_in_siberia'),
        input_tables=[
            (
                tables.YsonTable(
                    src_view,
                    yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, constants.SRC_VIEW_ID),
                    on_read=tables.OnWrite(
                        attributes={
                            prepare_subsamples_task._attribute_ready: True,
                            'schema': sample_item_schema(),
                        },
                        sort_by=[constants.GROUPING_KEY],
                    )
                ),
                None
            ),
            (
                tables.YsonTable(
                    'subsamples_info.yson',
                    yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples_info'),
                    on_write=tables.OnWrite(
                        attributes={
                            get_subsample_info_task._attribute: time_utils.get_current_moscow_datetime().date().isoformat(),
                            'schema': descriptor.subsample_info_schema(),
                        },
                        sort_by=[constants.GROUP_NAME]
                    )
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    subsample_group_0, yt.ypath_join(subsamples_dir, 'group_0'),
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    subsample_group_1, yt.ypath_join(subsamples_dir, 'group_1'),
                ),
                tests.TableIsNotChanged()
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    subsample_group_0,
                    yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples', 'group_0')
                ),
                tests.Diff()
            ),
            (
                tables.YsonTable(
                    subsample_group_1,
                    yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, 'subsamples', 'group_1')
                ),
                tests.Diff()
            ),
            (
                tables.YsonTable(
                    'src_view.yson', yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID, constants.SRC_VIEW_ID),
                ),
                tests.Diff()
            ),
        ]
    )


def test_describe_single_sample(api_client_mock, siberia_mock, describe_single_sample_task, clean_local_yt, conf, frozen_time):
    sample_dir = yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID)

    result = tests.yt_test_func(
        yt_client=clean_local_yt.yt_client,
        func=lambda: test_helpers.execute(describe_single_sample_task),
        data_path=yatest.common.test_source_path('data/describe_single_sample'),
        input_tables=[
            (
                tables.YsonTable(
                    'sample.yson',
                    yt.ypath_join(sample_dir, 'identity'),
                ),
                None,
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'sample.yson',
                    yt.ypath_join(sample_dir, 'identity'),
                ),
                tests.Diff(),
            ),
        ],
    )

    user_set_id = api_client_mock.lab.getSample(constants.SAMPLE_ID).result().siberiaUserSetId
    assert user_set_id in siberia_mock.user_set_ids

    return result


def test_create_standard_views(create_standard_views_task, clean_local_yt, conf, frozen_time):
    sample_dir = yt.ypath_join(conf.paths.lab.samples, constants.SAMPLE_ID)

    return tests.yt_test_func(
        yt_client=clean_local_yt.yt_client,
        func=lambda: test_helpers.execute(create_standard_views_task),
        data_path=yatest.common.test_source_path('data/create_standard_views'),
        input_tables=[
            (
                tables.YsonTable(
                    'invalid_src_view_input.yson',
                    yt.ypath_join(sample_dir, 'identity'),
                    on_write=tables.OnWrite(
                        attributes={
                            'schema': sample_item_schema(),
                            create_standard_views_task._attribute: time_utils.get_current_moscow_datetime().date().isoformat()
                        },
                        sort_by=[constants.GROUPING_KEY]
                    )
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    'subsamples_info.yson',
                    yt.ypath_join(sample_dir, 'subsamples_info'),
                    on_write=tables.OnWrite(
                        attributes={create_standard_views_task._attribute: time_utils.get_current_moscow_datetime().date().isoformat()},
                        sort_by=[constants.GROUP_NAME]
                    ),
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    'vertices_no_multi_profile.yson',
                    conf.paths.graph.vertices_no_multi_profile,
                    on_write=tables.OnWrite(sort_by=['id', 'id_type'])
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    'vertices_no_multi_profile.yson',
                    conf.paths.graph.vertices_by_crypta_id,
                    on_write=tables.OnWrite(sort_by=['cryptaId'])
                ),
                tests.TableIsNotChanged()
            )

        ],
        output_tables=[
            (
                tables.YsonTable(
                    'invalid_src_view_output.yson', yt.ypath_join(sample_dir, constants.INVALID_VIEW_ID)
                ),
                tests.Diff()
            ),
            (
                tables.YsonTable(
                    'invalid_yandexuid_view_output.yson', yt.ypath_join(sample_dir, constants.YANDEXUID_VIEW_ID)
                ),
                tests.Diff()
            ),
        ]
    )
