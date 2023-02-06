import freezegun
import mock
import os
import pytest
import shutil

import yatest.common as yc

from search.wizard.entitysearch.tools.es_hook_notifier.lib import fs_manipulate


SANDBOX_DIR = 'subdir'


@pytest.fixture
def test_sandbox_dir():
    result = os.path.join(yc.work_path(), SANDBOX_DIR)
    os.mkdir(result)

    yield result

    shutil.rmtree(result)


def make_text_file(dir_path, filename, content_prefix=None):
    with open(os.path.join(dir_path, filename), 'w') as h:
        content = '_'.join(
            (
                content_prefix or os.path.basename(dir_path),
                filename,
                'content\n',
            )
        )
        h.write(content)


@pytest.fixture
def main_resource_dir_path(test_sandbox_dir):
    result_path = os.path.join(test_sandbox_dir, 'main_resource_dir')
    os.mkdir(result_path)

    return result_path


@pytest.fixture
def extra_resource_dir_path(test_sandbox_dir):
    result_path = os.path.join(test_sandbox_dir, 'extra_resource_dir')
    os.mkdir(result_path)

    return result_path


@pytest.fixture
def result_dir(test_sandbox_dir):
    return os.path.join(test_sandbox_dir, 'result_dir')


@pytest.fixture
def result_dir_with_content(result_dir):
    os.mkdir(result_dir)
    make_text_file(result_dir, filename='file_in_result_dir')
    return result_dir


@pytest.fixture
def main_resource_dir(main_resource_dir_path):
    result_path = main_resource_dir_path

    make_text_file(result_path, filename='file_to_override')
    make_text_file(result_path, filename='file_to_keep')

    subdir = os.path.join(result_path, 'subdir')
    os.mkdir(subdir)

    make_text_file(subdir, filename='file_to_override', content_prefix=subdir)
    make_text_file(subdir, filename='file_to_keep', content_prefix=subdir)

    empty_subdir = os.path.join(result_path, 'empty_subdir')
    os.mkdir(empty_subdir)

    return result_path


@pytest.fixture
def extra_resource_dir(extra_resource_dir_path):
    result_path = extra_resource_dir_path

    make_text_file(result_path, filename='file_to_override')
    make_text_file(result_path, filename='file_to_add')

    subdir = os.path.join(result_path, 'subdir')
    os.mkdir(subdir)

    make_text_file(subdir, filename='file_to_override', content_prefix=subdir)

    return result_path


def get_no_links_abs_path(*args):
    return os.readlink(os.path.join(*args))


def assert_expected_content_after_linking(result_dir, extra_resource_dir, main_resource_dir):
    assert get_no_links_abs_path(result_dir, 'file_to_keep') == os.path.join(main_resource_dir, 'file_to_keep')
    assert get_no_links_abs_path(result_dir, 'subdir/file_to_keep') == os.path.join(
        main_resource_dir, 'subdir/file_to_keep'
    )

    assert get_no_links_abs_path(result_dir, 'file_to_override') == os.path.join(extra_resource_dir, 'file_to_override')
    assert get_no_links_abs_path(result_dir, 'subdir/file_to_override') == os.path.join(
        extra_resource_dir, 'subdir/file_to_override'
    )

    assert get_no_links_abs_path(result_dir, 'file_to_add') == os.path.join(extra_resource_dir, 'file_to_add')

    assert os.path.exists(os.path.join(result_dir, 'empty_subdir'))
    assert not os.path.islink(os.path.join(result_dir, 'empty_subdir'))


def test_fs_manipulate_two_link_calls(result_dir, main_resource_dir, extra_resource_dir):
    with freezegun.freeze_time("2020-04-04 05:00:00"):
        fs_manipulate.link_resources_into_one_dir(
            dest_dir=result_dir,
            main_resource_dir=main_resource_dir,
            extra_resources_dirs=[extra_resource_dir],
        )

    first_result_dir = os.readlink(result_dir)
    assert os.path.basename(result_dir) == 'result_dir'
    assert os.path.basename(first_result_dir) == 'result_dir_2020-04-04T08:00:00'

    assert_expected_content_after_linking(result_dir, extra_resource_dir, main_resource_dir)

    # change content and relink later
    make_text_file(extra_resource_dir, 'file_to_add_after_first_link')

    with freezegun.freeze_time("2020-04-04 16:59:59"):
        fs_manipulate.link_resources_into_one_dir(
            dest_dir=result_dir,
            main_resource_dir=main_resource_dir,
            extra_resources_dirs=[extra_resource_dir],
        )

    assert os.path.basename(result_dir) == 'result_dir'
    assert os.path.basename(os.readlink(result_dir)) == 'result_dir_2020-04-04T19:59:59'
    assert not os.path.exists(first_result_dir)

    assert_expected_content_after_linking(result_dir, extra_resource_dir, main_resource_dir)
    assert get_no_links_abs_path(result_dir, 'file_to_add_after_first_link') == os.path.join(
        extra_resource_dir, 'file_to_add_after_first_link'
    )


@pytest.mark.parametrize('extra_dir_folder_name', [None, 'non_existent'])
@freezegun.freeze_time("2020-04-04 05:00:00")
def test_fs_manipulate_ok_only_main_resource(result_dir, main_resource_dir, extra_dir_folder_name):
    extra_dir = extra_dir_folder_name and os.path.join(yc.work_path(), extra_dir_folder_name)

    fs_manipulate.link_resources_into_one_dir(
        dest_dir=result_dir,
        main_resource_dir=main_resource_dir,
        extra_resources_dirs=[extra_dir],
    )

    assert extra_dir is None or not os.path.exists(extra_dir)

    assert os.path.basename(result_dir) == 'result_dir'
    assert os.path.basename(os.readlink(result_dir)) == 'result_dir_2020-04-04T08:00:00'

    assert os.readlink(os.path.join(result_dir, 'file_to_keep')) == os.path.join(main_resource_dir, 'file_to_keep')
    assert os.readlink(os.path.join(result_dir, 'file_to_override')) == os.path.join(
        main_resource_dir, 'file_to_override'
    )

    assert os.readlink(os.path.join(result_dir, 'subdir/file_to_keep')) == os.path.join(
        main_resource_dir, 'subdir/file_to_keep'
    )
    assert os.readlink(os.path.join(result_dir, 'subdir/file_to_override')) == os.path.join(
        main_resource_dir, 'subdir/file_to_override'
    )

    assert not os.path.exists(os.path.join(result_dir, 'file_to_add'))


def test_keep_old_result_dir_content_on_switch_to_missing_dir(result_dir_with_content):
    assert os.path.isfile(os.path.join(result_dir_with_content, 'file_in_result_dir'))

    non_existent_main_resource_dir = os.path.join(yc.work_path(), 'non_existent_main_resource_dir')

    expected_error_match = '^Trying to link non-existent main resource dir: .*non_existent_main_resource_dir$'
    with pytest.raises(ValueError, match=expected_error_match):
        fs_manipulate.link_resources_into_one_dir(
            dest_dir=result_dir_with_content,
            main_resource_dir=non_existent_main_resource_dir,
            extra_resources_dirs=[],
        )

    assert os.path.isfile(os.path.join(result_dir_with_content, 'file_in_result_dir'))


def test_keep_result_dir_valid_on_switch_fail(
    monkeypatch, test_sandbox_dir, result_dir, main_resource_dir, extra_resource_dir
):
    expected_old_ts_dir = 'result_dir_2020-04-04T08:00:00'
    expected_to_be_dropped_new_ts_dir = 'result_dir_2020-04-04T19:59:59'

    def info_fail_on_certain_arg(s):
        if s == 'Switching dst dir to new version: ./subdir/result_dir -> ./subdir/result_dir_2020-04-04T19:59:59':
            assert_expected_content_after_linking(
                os.path.join(test_sandbox_dir, expected_to_be_dropped_new_ts_dir),  # content is prepared in new dir
                extra_resource_dir,
                main_resource_dir,
            )
            # data is linked to old dir
            assert os.readlink(result_dir).endswith(expected_old_ts_dir)

            raise ValueError('Error From Mock!')

    logging_mock = mock.Mock()
    info_mock = mock.Mock(side_effect=info_fail_on_certain_arg)
    logging_mock.info = info_mock
    monkeypatch.setattr(fs_manipulate, 'logging', logging_mock)

    with freezegun.freeze_time("2020-04-04 05:00:00"):
        fs_manipulate.link_resources_into_one_dir(
            dest_dir=result_dir,
            main_resource_dir=main_resource_dir,
            extra_resources_dirs=[extra_resource_dir],
        )

    assert os.path.basename(result_dir) == 'result_dir'
    first_result_dir = os.readlink(result_dir)
    assert first_result_dir.endswith(expected_old_ts_dir)

    assert_expected_content_after_linking(result_dir, extra_resource_dir, main_resource_dir)

    assert logging_mock.info.call_args_list == [
        mock.call('Preparing new dir: ./subdir/result_dir_2020-04-04T08:00:00'),
        mock.call('Linking ./subdir/main_resource_dir into ./subdir/result_dir_2020-04-04T08:00:00'),
        mock.call('Linking ./subdir/extra_resource_dir into ./subdir/result_dir_2020-04-04T08:00:00'),
        mock.call('Current dst dir version: ./subdir/result_dir -> None'),
        mock.call('Switching dst dir to new version: ./subdir/result_dir -> ./subdir/result_dir_2020-04-04T08:00:00'),
    ]

    # change content and relink with fail
    make_text_file(extra_resource_dir, 'file_to_add_after_first_link')

    with freezegun.freeze_time("2020-04-04 16:59:59"):
        with pytest.raises(ValueError, match="^Error From Mock!$"):
            fs_manipulate.link_resources_into_one_dir(
                dest_dir=result_dir,
                main_resource_dir=main_resource_dir,
                extra_resources_dirs=[extra_resource_dir],
            )

    assert os.path.basename(result_dir) == 'result_dir'
    assert os.readlink(result_dir) == first_result_dir

    sandbox_dirs = os.listdir(test_sandbox_dir)

    assert os.path.basename(result_dir) in sandbox_dirs
    assert expected_old_ts_dir in sandbox_dirs
    assert expected_to_be_dropped_new_ts_dir not in sandbox_dirs

    assert_expected_content_after_linking(result_dir, extra_resource_dir, main_resource_dir)
    assert not os.path.exists(os.path.join(result_dir, 'file_to_add_after_first_link'))
