from datetime import datetime, timedelta, timezone

import pytest

from hamcrest import assert_that, equal_to, has_properties

from mail.ipa.ipa.core.actions.collectors.check import CheckCollectorStatusAction
from mail.ipa.ipa.interactions.yarm import YarmClient
from mail.ipa.ipa.interactions.yarm.entities import YarmCollectorStatus
from mail.ipa.ipa.interactions.yarm.exceptions import YarmNotStartedYetError


def dt_at_minute(minutes):
    assert minutes < 60
    return datetime(1970, 1, 1, 0, minutes, 0, tzinfo=timezone.utc)


@pytest.fixture
def action(collector):
    return CheckCollectorStatusAction(collector)


@pytest.fixture
async def collector(existing_user, storage, create_collector, pop_id):
    return await create_collector(user_id=existing_user.user_id, pop_id=pop_id)


@pytest.fixture(autouse=True)
def mock_datetime(mocker):
    mocker.patch('mail.ipa.ipa.core.actions.collectors.check.utcnow', mocker.Mock(return_value=dt_at_minute(10)))


@pytest.mark.parametrize('db_status, yarm_status, error, expected', (
    (
        {'collected': None, 'total': None, 'errors': None, 'status': 'ok'},
        {'collected': None, 'total': None, 'errors': None},
        'ok',
        False
    ),
    (
        {'collected': None, 'total': None, 'errors': None, 'status': 'ok'},
        {'collected': 10, 'total': None, 'errors': None},
        'ok',
        True
    ),
    (
        {'collected': 20, 'total': 20, 'errors': 20, 'status': 'ok'},
        {'collected': 20, 'total': 20, 'errors': 20},
        'bad',
        True
    ),
))
def test_is_collector_status_changed(action, mocker, db_status, yarm_status, error, expected):
    actual = action._is_collector_status_changed(
        mocker.Mock(**db_status),
        mocker.Mock(**yarm_status),
        error,
    )
    assert_that(actual, equal_to(expected))


@pytest.mark.parametrize('db_collector_raw, yarm_status, expected', (
    pytest.param(
        {'collected': None, 'total': None, 'errors': None, 'modified_at': dt_at_minute(7)},
        {'collected': None, 'total': None, 'errors': None},
        'bad',
        id='no-status_checked-recently'
    ),
    pytest.param(
        {'collected': None, 'total': None, 'errors': None, 'modified_at': dt_at_minute(3)},
        {'collected': None, 'total': None, 'errors': None},
        'bad',
        id='no-status_checked-long-ago'
    ),
    pytest.param(
        {'collected': None, 'total': None, 'errors': None, 'modified_at': dt_at_minute(3)},
        {'collected': 1, 'total': 10, 'errors': 0},
        'ok',
        id='status-appeared_checked-long-ago'
    ),
    pytest.param(
        {'collected': 1, 'total': 10, 'errors': 0, 'modified_at': dt_at_minute(3)},
        {'collected': 2, 'total': 10, 'errors': 0},
        'ok',
        id='status-changed_checked-long-ago'
    ),
    pytest.param(
        {'collected': 1, 'total': 10, 'errors': 0, 'modified_at': dt_at_minute(7)},
        {'collected': 1, 'total': 10, 'errors': 0},
        'ok',
        id='status-not-changed_checked-recently'
    ),
    pytest.param(
        {'collected': 1, 'total': 10, 'errors': 0, 'modified_at': dt_at_minute(3)},
        {'collected': 1, 'total': 10, 'errors': 0},
        'bad',
        id='status-not-changed_checked-long-ago'
    ),
))
class TestGetErrorStatus:
    @pytest.fixture
    def yarm_collector(self):
        return {'error_status': 'bad'}

    @pytest.fixture
    async def collector(self, storage, collector, db_collector_raw):
        for key in db_collector_raw:
            setattr(collector, key, db_collector_raw[key])
        return await storage.collector.save(collector, update_modified_at=False)

    @pytest.fixture(autouse=True)
    def mock_settings(self, ipa_settings, action):
        ipa_settings.TASKQ_COLLECTOR_CHECK_DELAY_MINUTES = 1
        action.__class__.ERROR_FLAP_THRESHOLD_MULT = 4

    def test_get_error_status(self, action, mocker, collector, yarm_status, yarm_collector, expected):
        actual = action._get_error_status(
            collector=collector,
            yarm_collector=mocker.Mock(**yarm_collector),
            yarm_collector_status=mocker.Mock(**yarm_status),
        )
        assert_that(actual, equal_to(expected))


class TestHandle:
    @pytest.fixture(autouse=True)
    def mock_check_status(self, mocker, coromock):
        return mocker.patch.object(CheckCollectorStatusAction, '_check_status', coromock(mocker.Mock()))

    @pytest.fixture(autouse=True)
    def mock_attempt_shutdown(self, mocker, coromock):
        return mocker.patch.object(CheckCollectorStatusAction, '_attempt_shutdown', coromock())

    @pytest.fixture(autouse=True)
    async def returned(self, action):
        return await action.run()

    def test_calls_check_status(self, mock_check_status, collector, existing_user):
        mock_check_status.assert_called_once_with(collector, existing_user)

    @pytest.mark.asyncio
    async def test_calls_attempt_shutdown(self, mock_check_status, mock_attempt_shutdown):
        mock_attempt_shutdown.assert_called_once_with(await mock_check_status())

    @pytest.mark.asyncio
    async def test_returned(self, returned, mock_attempt_shutdown):
        assert_that(returned, equal_to(await mock_attempt_shutdown()))


class TestGetYarmCollector:
    @pytest.fixture
    def mock_yarm_status(self, mocker, coromock):
        return mocker.patch.object(YarmClient, 'status', coromock(mocker.Mock()))

    @pytest.fixture
    def mock_yarm_get_collector(self, mocker, coromock):
        return mocker.patch.object(YarmClient, 'get_collector', coromock(mocker.Mock()))

    @pytest.fixture(autouse=True)
    async def returned(self,
                       action,
                       existing_user,
                       collector,
                       mock_yarm_get_collector,
                       mock_yarm_status):
        return await action._get_yarm_collector(collector, existing_user)

    def test_calls_yarm_get_collector(self, mock_yarm_get_collector, existing_user, collector):
        mock_yarm_get_collector.assert_called_once_with(existing_user.suid, collector.pop_id)

    def test_calls_yarm_status(self, mock_yarm_status, collector):
        mock_yarm_status.assert_called_once_with(collector.pop_id)

    @pytest.mark.asyncio
    async def test_returned(self, returned, mock_yarm_get_collector, mock_yarm_status):
        assert_that(
            returned,
            equal_to(
                (
                    await mock_yarm_get_collector(),
                    await mock_yarm_status(),
                )
            )
        )

    class TestCollectorNotStartedRaised:
        @pytest.fixture(autouse=True)
        def mock_yarm_status(self, mocker, coromock):
            exc = YarmNotStartedYetError(status=200,
                                         code='collector not started yet',
                                         service=None,
                                         method=None)
            return mocker.patch.object(YarmClient, 'status', coromock(exc=exc))

        @pytest.mark.asyncio
        async def test_not_started_yet_raised__returned(self, returned, mock_yarm_get_collector):
            assert_that(
                returned,
                equal_to(
                    (
                        await mock_yarm_get_collector(),
                        YarmCollectorStatus.get_default_status(),
                    )
                )
            )


@pytest.mark.parametrize('changed, modified_at, status, collected, total, errors', (
    (False, dt_at_minute(0), 'previous', 0, 0, 0),
    (True, dt_at_minute(1), 'next', 1, 1, 1),
))
class TestCheckStatus:
    @pytest.fixture(autouse=True)
    def mock_func_now(self, mocker):
        mocker.patch('sqlalchemy.func.now', mocker.Mock(return_value=dt_at_minute(1)))

    @pytest.fixture
    async def collector(self, collector, storage):
        collector.status = 'previous'
        collector.collected = 0
        collector.total = 0
        collector.errors = 0
        collector.modified_at = dt_at_minute(0)
        return await storage.collector.save(collector, update_modified_at=False)

    @pytest.fixture(autouse=True)
    def mock_is_collector_status_changed(self, action, mocker, changed):
        return mocker.patch.object(action, '_is_collector_status_changed', mocker.Mock(return_value=changed))

    @pytest.fixture(autouse=True)
    def mock_get_error_status(self, action, mocker, coromock):
        return mocker.patch.object(action, '_get_error_status', mocker.Mock(return_value='next'))

    @pytest.fixture(autouse=True)
    def mock_get_yarm_collector(self, action, mocker, coromock, collected, total, errors, status):
        return mocker.patch.object(
            action,
            '_get_yarm_collector',
            coromock(
                (
                    mocker.Mock(error_status=status),
                    mocker.Mock(collected=collected, total=total, errors=errors),
                )
            )
        )

    @pytest.fixture(autouse=True)
    async def returned(self,
                       action,
                       storage,
                       collector,
                       existing_user,
                       modified_at,  # dummy dep
                       ):
        async with action.storage_setter():
            return await action._check_status(collector, existing_user)

    @pytest.mark.asyncio
    async def test_collector_in_db(self, storage, collector, collected, total, errors, status, modified_at):
        assert_that(
            await storage.collector.get(collector.collector_id),
            has_properties({
                'status': status,
                'collected': collected,
                'total': total,
                'errors': errors,
                'modified_at': modified_at,
            }),
        )

    def test_calls_get_yarm_collector(self,
                                      mock_get_yarm_collector,
                                      collector,
                                      existing_user,
                                      ):
        mock_get_yarm_collector.assert_called_once_with(
            collector,
            existing_user,
        )

    @pytest.mark.asyncio
    async def test_calls_get_error_status(self,
                                          mock_get_error_status,
                                          collector,
                                          mock_get_yarm_collector,
                                          ):
        yarm_collector, yarm_status = await mock_get_yarm_collector()
        mock_get_error_status.assert_called_once_with(
            collector=collector,
            yarm_collector=yarm_collector,
            yarm_collector_status=yarm_status,
        )

    @pytest.mark.asyncio
    async def test_calls_is_collector_status_changed(self,
                                                     mock_is_collector_status_changed,
                                                     collector,
                                                     mock_get_yarm_collector,
                                                     mock_get_error_status,
                                                     ):
        mock_is_collector_status_changed.assert_called_once_with(
            collector,
            (await mock_get_yarm_collector())[1],
            mock_get_error_status(),
        )


@pytest.mark.parametrize('modified_at, status, expected_enabled', (
    (dt_at_minute(6), 'ok', True),
    (dt_at_minute(6), 'bad', True),
    (dt_at_minute(4), 'ok', True),
    (dt_at_minute(4), 'bad', False),
))
class TestAttemptShutdown:
    @pytest.fixture(autouse=True)
    def mock_shutdown_threshold(self, action):
        action.SHUTDOWN_THRESHOLD = timedelta(minutes=5)

    @pytest.fixture
    async def collector(self, storage, collector, modified_at, status):
        collector.modified_at = modified_at
        collector.status = status
        return await storage.collector.save(collector, update_modified_at=False)

    @pytest.fixture(autouse=True)
    def mock_yarm_enable(self, mocker, coromock):
        mocker.patch.object(YarmClient, 'set_enabled', coromock())

    @pytest.fixture(autouse=True)
    async def returned(self, action, collector, expected_enabled):
        return await action._attempt_shutdown(collector)

    @pytest.mark.asyncio
    async def test_enabled(self, returned, storage, collector, expected_enabled):
        collector = await storage.collector.get(collector.collector_id)
        assert_that(collector.enabled, equal_to(expected_enabled))

    @pytest.mark.asyncio
    async def test_returned(self, returned, storage, collector):
        assert_that(
            returned,
            equal_to(await storage.collector.get(collector.collector_id)),
        )
