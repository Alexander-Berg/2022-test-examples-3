import mock
import pytest
import xml.etree.ElementTree

from mail.borador.src.log import _monkey_patch_logger
from mail.borador.src.tasks import launch_and_wait, with_retry
from mail.borador.src.aqua import parse_launch_pack, parse_launch_show, LaunchId, Show, \
    pack_is_running, pack_is_success, parse_restart_pack
from mail.borador.tests.helpers import data_path, add_import_prefix, async_value, async_raise, make_api

_monkey_patch_logger()


def test_validate_aqua_xml():
    for env in ['production', 'testing']:
        from yatest.common import build_path
        with open(build_path('mail/borador/etc/generated/aqua.xml-') + env) as f:
            root = xml.etree.ElementTree.fromstring(f.read())
            for pack in list(root):
                assert pack.get('id') is not None and pack.get('id') != ''
                assert pack.get('alias') is not None and pack.get('alias') != ''
                assert pack.tag == 'pack'
                for prop in list(pack):
                    assert prop.tag == 'property'
                    assert prop.get('name') is not None and prop.get('name') != ''
                    assert prop.get('value') is not None and prop.get('value') != ''


def test_parse_launch_xml():
    with open(data_path('unit', 'launch.xml')) as f:
        launch = f.read()

    assert parse_launch_pack(launch=launch) == LaunchId(id='launch_id')


def test_parse_restart():
    with open(data_path('unit', 'restart.xml')) as f:
        launch = f.read()

    assert parse_restart_pack(launch=launch) == LaunchId(id='launch_id')


def test_parse_launch_show():
    with open(data_path('unit', 'show.xml')) as f:
        launch = f.read()

    assert parse_launch_show(launch=launch) == Show(totalSuites=6, passedSuites=1, launchStatus='reportFailed',
                                                    startTime=1573826498549, stopTime=1573826579339,
                                                    launchId='launch_id')


def test_pack_is_running():
    assert pack_is_running(Show(totalSuites=6, passedSuites=1, launchStatus='', startTime=1, stopTime=0))
    assert not pack_is_running(Show(totalSuites=6, passedSuites=1, launchStatus='', startTime=1, stopTime=1))
    assert not pack_is_running(Show(totalSuites=6, passedSuites=1, launchStatus='', startTime=1, stopTime=-1))


def test_pack_is_success():
    assert pack_is_success(Show(totalSuites=6, passedSuites=6, launchStatus='finished', startTime=1, stopTime=0))
    assert not pack_is_success(Show(totalSuites=6, passedSuites=5, launchStatus='finished', startTime=1, stopTime=1))
    assert not pack_is_success(Show(totalSuites=5, passedSuites=6, launchStatus='finished', startTime=1, stopTime=-1))


@pytest.mark.asyncio
async def test_launch_and_wait_success():
    lid = 'launchid'
    show = Show(totalSuites=6, passedSuites=5, launchStatus='finished', startTime=1, stopTime=1)

    api = make_api(launch=async_value(LaunchId(id=lid)), show=[async_value(show)],
                   wait=async_value(None), restart=async_value(None))

    assert await launch_and_wait(api=api, pack_name='name', failed_only=False) == show

    api.wait_for_next_show.assert_not_called()
    api.restart_launch.assert_not_called()
    api.launch_pack.assert_called_with(pack_name='name')


@pytest.mark.asyncio
async def test_launch_and_wait_show_raises_error():
    lid = 'launchid'
    api = make_api(launch=async_value(LaunchId(id=lid)), show=[async_raise(Exception(''))],
                   wait=async_value(None), restart=async_value(None))

    with pytest.raises(Exception):
        await launch_and_wait(api=api, pack_name='name', failed_only=False)

    api.wait_for_next_show.assert_not_called()
    api.restart_launch.assert_not_called()


@pytest.mark.asyncio
async def test_launch_and_wait_launch_raises_error():
    api = make_api(launch=async_raise(Exception('')), show=[
        async_value(Show(totalSuites=6, passedSuites=5, launchStatus='finished', startTime=1, stopTime=1))
    ], wait=async_value(None), restart=async_value(None))

    with pytest.raises(Exception):
        await launch_and_wait(api=api, pack_name='name', failed_only=False)

    api.wait_for_next_show.assert_not_called()
    api.restart_launch.assert_not_called()


@pytest.mark.asyncio
async def test_launch_and_wait_should_retry_in_case_of_running_task():
    lid = 'launchid'
    result = Show(totalSuites=6, passedSuites=5, launchStatus='bar', startTime=1, stopTime=1)

    api = make_api(launch=async_value(LaunchId(id=lid)), show=[
        async_value(Show(totalSuites=6, passedSuites=5, launchStatus='foo', startTime=1, stopTime=0)),
        async_value(result)
    ], wait=async_value(None), restart=async_value(None))

    assert result == await launch_and_wait(api=api, pack_name='name', failed_only=False)

    api.wait_for_next_show.assert_called_once()
    api.restart_launch.assert_not_called()


@pytest.mark.asyncio
async def test_launch_and_wait_should_pass_failed_only_param():
    lid = 'launchid'
    result = Show(totalSuites=6, passedSuites=5, launchStatus='bar', startTime=1, stopTime=1)

    api = make_api(launch=async_value(LaunchId(id=lid)), show=[
        async_value(Show(totalSuites=6, passedSuites=5, launchStatus='foo', startTime=1, stopTime=0)),
        async_value(result)
    ], wait=async_value(None), restart=async_value(None))

    assert result == await launch_and_wait(api=api, pack_name='name', failed_only=False)

    api.wait_for_next_show.assert_called_once()
    api.restart_launch.assert_not_called()


@pytest.mark.asyncio
async def test__with_retry__should_not_retry_on_success_launch():
    show = Show(totalSuites=6, passedSuites=6, launchStatus='finished', startTime=1, stopTime=1)

    with mock.patch(add_import_prefix('src.tasks.launch_and_wait')) as m:
        m.return_value = show
        assert await with_retry(api=make_api(None, None, None, None), pack_name='name', times=3) == (6, 0, 1)


@pytest.mark.asyncio
async def test__with_retry__should_retry_failed_launches():
    with mock.patch(add_import_prefix('src.tasks.launch_and_wait')) as m:
        show = Show(totalSuites=6, passedSuites=6, launchStatus='finished',
                    startTime=1, stopTime=1, launchId='launch_id')
        api = make_api(None, None, None, None)
        m.side_effect = [
            Show(totalSuites=6, passedSuites=0, launchStatus='finished',
                 startTime=1, stopTime=1, launchId='launch_id'),
            show
        ]
        assert await with_retry(api=api, pack_name='name', times=3) == (6, 0, 2)

        m.assert_has_calls([
            mock.call(api=api, pack_name='name', failed_only=False),
            mock.call(api=api, launch_id='launch_id', failed_only=True)
        ])


@pytest.mark.asyncio
async def test__with_retry__should_retry_failed_launches_not_more_than_specified_times():
    with mock.patch(add_import_prefix('src.tasks.launch_and_wait')) as launch_mock:
        show = Show(totalSuites=6, passedSuites=0, launchStatus='finished',
                    startTime=1, stopTime=1, launchId='launch_id')
        launch_mock.side_effect = [show, show, show]
        api = make_api(None, None, None)

        assert await with_retry(api=api, pack_name='name', times=3) == (6, 6, -1)

        launch_mock.assert_has_calls([
            mock.call(api=api, pack_name='name', failed_only=False),
            mock.call(api=api, launch_id='launch_id', failed_only=True),
            mock.call(api=api, launch_id='launch_id', failed_only=True)
        ])
