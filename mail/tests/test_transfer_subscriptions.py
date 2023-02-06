import pytest
import mock
from collections import namedtuple
from pymdb.types.db_enums.subscription_state import SubscriptionState
from ora2pg.transfer_subscriptions import (
    wait_for_normal_working_subs,
    wait_for_migrate_finish_subs,
    BadSubscriptionError,
    CantWait
)


RETRY_PROGRESSION = (0.1, 0.2)

SubscriptionTestInfo = namedtuple('SubscriptionTestInfo', ('subscription_id', 'state'))


def patch_get_sf_subs():
    return mock.patch(
        'ora2pg.transfer_subscriptions.get_shared_folder_subscriptions',
        autospec=True)


class Test_wait_for_working_subs(object):

    @pytest.mark.parametrize('state', [
        SubscriptionState.sync,
        SubscriptionState.terminated,
        SubscriptionState.migrate,
        SubscriptionState.migrate_finished,
    ])
    def test_for_ready_subs(self, state):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.return_value = [
                SubscriptionTestInfo('id1', state),
            ]

            wait_for_normal_working_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_called_once_with('connection', 'uid1')

    @pytest.mark.parametrize('state', [
        SubscriptionState.init_fail,
        SubscriptionState.sync_fail,
        SubscriptionState.clear_fail,
        SubscriptionState.migrate_fail,
    ])
    def test_for_bad_subs(self, state):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.return_value = [
                SubscriptionTestInfo('id1', state)
            ]
            with pytest.raises(BadSubscriptionError):
                wait_for_normal_working_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_called_once_with('connection', 'uid1')

    @pytest.mark.parametrize(('state1', 'state2'), [
        (SubscriptionState.new, SubscriptionState.sync),
        (SubscriptionState.init, SubscriptionState.sync),
        (SubscriptionState.discontinued, SubscriptionState.terminated),
        (SubscriptionState.clear, SubscriptionState.terminated),
    ])
    def test_for_not_ready_subs_retry_ready_subs(self, state1, state2):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.side_effect = [
                [SubscriptionTestInfo('id1', state1)],
                [SubscriptionTestInfo('id1', state2)]
            ]

            wait_for_normal_working_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_has_calls([
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
            ])
            assert get_sf_subs.call_count == 2

    @pytest.mark.parametrize(('state1', 'state2'), [
        (SubscriptionState.new, SubscriptionState.init_fail),
        (SubscriptionState.init, SubscriptionState.init_fail),
        (SubscriptionState.discontinued, SubscriptionState.clear_fail),
        (SubscriptionState.clear, SubscriptionState.clear_fail),
    ])
    def test_for_not_ready_subs_retry_bad_subs(self, state1, state2):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.side_effect = [
                [SubscriptionTestInfo('id1', state1)],
                [SubscriptionTestInfo('id1', state2)]
            ]

            with pytest.raises(BadSubscriptionError):
                wait_for_normal_working_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_has_calls([
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
            ])
            assert get_sf_subs.call_count == 2

    @pytest.mark.parametrize('state', [
        SubscriptionState.new,
        SubscriptionState.init,
        SubscriptionState.discontinued,
        SubscriptionState.clear,
    ])
    def test_raise_CantWait_if_no_more_retries(self, state):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.side_effect = [
                [SubscriptionTestInfo('id1', state)],
                [SubscriptionTestInfo('id1', state)],
                [SubscriptionTestInfo('id1', state)]
            ]

            with pytest.raises(CantWait):
                wait_for_normal_working_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_has_calls([
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
            ])
            assert get_sf_subs.call_count == 3


class Test_wait_for_migrate_finish_subs(object):

    @pytest.mark.parametrize('state', [
        SubscriptionState.terminated,
        SubscriptionState.migrate_finished,
    ])
    def test_for_ready_subs(self, state):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.return_value = [
                SubscriptionTestInfo('id1', state),
            ]

            wait_for_migrate_finish_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_called_once_with('connection', 'uid1')

    @pytest.mark.parametrize('state', [
        SubscriptionState.new,
        SubscriptionState.init,
        SubscriptionState.sync,
        SubscriptionState.discontinued,
        SubscriptionState.clear,
        SubscriptionState.init_fail,
        SubscriptionState.sync_fail,
        SubscriptionState.clear_fail,
        SubscriptionState.migrate_fail,
    ])
    def test_for_bad_subs(self, state):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.return_value = [
                SubscriptionTestInfo('id1', state)
            ]
            with pytest.raises(BadSubscriptionError):
                wait_for_migrate_finish_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_called_once_with('connection', 'uid1')

    def test_for_not_ready_subs_retry_ready_subs(self):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.side_effect = [
                [SubscriptionTestInfo('id1', SubscriptionState.migrate)],
                [SubscriptionTestInfo('id1', SubscriptionState.migrate_finished)]
            ]

            wait_for_migrate_finish_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_has_calls([
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
            ])
            assert get_sf_subs.call_count == 2

    def test_for_not_ready_subs_retry_bad_subs(self):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.side_effect = [
                [SubscriptionTestInfo('id1', SubscriptionState.migrate)],
                [SubscriptionTestInfo('id1', SubscriptionState.migrate_fail)]
            ]

            with pytest.raises(BadSubscriptionError):
                wait_for_migrate_finish_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_has_calls([
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
            ])
            assert get_sf_subs.call_count == 2

    def test_raise_CantWait_if_no_more_retries(self):
        with patch_get_sf_subs() as get_sf_subs:
            get_sf_subs.side_effect = [
                [SubscriptionTestInfo('id1', SubscriptionState.migrate)],
                [SubscriptionTestInfo('id1', SubscriptionState.migrate)],
                [SubscriptionTestInfo('id1', SubscriptionState.migrate)]
            ]

            with pytest.raises(CantWait):
                wait_for_migrate_finish_subs('connection', 'uid1', RETRY_PROGRESSION)
            get_sf_subs.assert_has_calls([
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
                mock.call('connection', 'uid1'),
            ])
            assert get_sf_subs.call_count == 3
