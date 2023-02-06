import pytest
from mock import Mock

from helpers import (
    mk_source_user,
    auto_patch,
    test_config,
)
from ora2pg.clone_user.clone_user_data import override_stids, override_state, get_source_fetcher
from ora2pg.clone_user.source_user_fetcher import (
    PGSourceFetcher,
)
from ora2pg.clone_user.stids_copier import STIDsCopier, StidWithMime
from ora2pg.pg_types import PgUser
from pymdb.types import User

clone_data_patch = auto_patch('ora2pg.clone_user.clone_user_data')


@pytest.mark.parametrize(('original_state', 'overridden_state'), [
    ('active', 'special'),
    ('inactive', 'special'),
    ('notified', 'special'),
    ('frozen', 'special'),
    ('archived', 'archived'),
    ('deleted', 'deleted'),
    ('special', 'special'),
])
def test_override_state(original_state, overridden_state):
    user_data = PgUser()
    user_data.user = User(
        state=original_state,
        here_since=None, is_here=None, data_version=None, is_deleted=None, purge_date=None,
        can_read_tabs=None, last_state_update=None, notifies_count=0,
    )
    override_state(user_data)
    assert user_data.user.state == overridden_state


def test_override_stids():
    def copier_side_effect(st_id):
        return StidWithMime(st_id + ':copy', None, None)
    copier_mock = Mock(autospec=STIDsCopier)
    copier_mock.copy.side_effect = copier_side_effect

    mails = []
    for st_id in 'abc':
        mails.append(Mock())
        mails[-1].coords.st_id = st_id

    user_data = PgUser()
    user_data.mails = iter(mails)

    override_stids(
        copier_mock,
        user_data)

    new_st_ids = [m.coords.st_id for m in user_data.mails]

    assert new_st_ids == ['a:copy', 'b:copy', 'c:copy']


def test_override_stids_for_old_messages():
    def copier_side_effect(st_id):
        return StidWithMime(st_id + ':copy', None, 'new_mime_' + st_id)
    copier_mock = Mock(autospec=STIDsCopier)
    copier_mock.copy.side_effect = copier_side_effect

    mails = []
    for st_id in 'abc':
        mails.append(Mock())
        mails[-1].coords.st_id = st_id
        mails[-1].mime = 'old_mime_' + st_id

    user_data = PgUser()
    user_data.mails = iter(mails)

    override_stids(
        copier_mock,
        user_data)

    new_mimes = [m.mime for m in user_data.mails]

    assert new_mimes == ['new_mime_a', 'new_mime_b', 'new_mime_c']


def test_override_stids_for_new_messages():
    def copier_side_effect(st_id):
        return StidWithMime(st_id + ':copy', None, None)
    copier_mock = Mock(autospec=STIDsCopier)
    copier_mock.copy.side_effect = copier_side_effect

    mails = []
    for st_id in 'abc':
        mails.append(Mock())
        mails[-1].coords.st_id = st_id
        mails[-1].mime = 'old_mime_' + st_id

    user_data = PgUser()
    user_data.mails = iter(mails)

    override_stids(
        copier_mock,
        user_data)

    new_mimes = [m.mime for m in user_data.mails]

    assert new_mimes == ['old_mime_a', 'old_mime_b', 'old_mime_c']


@pytest.mark.parametrize(('db', 'Fetcher'), [
    ('pg', PGSourceFetcher),
])
def test_get_source_fetcher(db, Fetcher):
    with \
        clone_data_patch('get_shard_info'), \
        clone_data_patch('get_pg_dsn_by_shard_id') \
    :
        fetcher = get_source_fetcher(
            mk_source_user(db),
            test_config,
            min_received_date=None,
            max_received_date=None,
        )
        assert isinstance(fetcher, Fetcher)
