# coding: utf-8

import datetime

from freezegun import freeze_time
import mock

from ora2pg.pg_put import StoreUser
from ora2pg.tests.common import mocked_conn

FAKE_NOW = datetime.datetime.now()
UID = 42
DATA_VERSION = 3


@freeze_time(FAKE_NOW)
@mock.patch('ora2pg.pg_put.exec_simple_insert')
def test_base_init_uses_date_version(mocked_exec):
    cur = mock.Mock()
    user = StoreUser(conn=mocked_conn(cur), uid=UID)

    user.base_init(
        data_version=DATA_VERSION,
        can_read_tabs=True,
        is_deleted=False,
        state='active',
        last_state_update=FAKE_NOW,
        notifies_count=1,
    )

    mocked_exec.assert_called()
    mocked_exec.call_args_list[0] == mock.call(
        cur, "mail.users",
        uid=UID,
        is_here=True,
        here_since=FAKE_NOW,
        data_version=DATA_VERSION,
        can_read_tabs=True,
        is_deleted=False,
        state='active',
        last_state_update=FAKE_NOW,
        notifies_count=1,
    )
