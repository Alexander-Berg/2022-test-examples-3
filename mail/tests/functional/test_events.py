import asyncio

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains, has_properties

from mail.ipa.ipa.core.actions.import_.json import CreateImportFromJSONAction
from mail.ipa.ipa.core.actions.import_.stop import CreateStopImportAction
from mail.ipa.ipa.core.entities.enums import EventType


@pytest.fixture
def general_import_params():
    return {
        'server': 'server.test',
        'port': 993,
        'imap': 1,
        'ssl': 0,
        'mark_archive_read': 1,
        'delete_msgs': 0,
    }


@pytest.fixture
def create_request_params(admin_uid, user_ip, general_import_params):
    return {
        'admin_uid': admin_uid,
        'user_ip': user_ip,
        **general_import_params,
    }


@pytest.fixture
def request_json(rands):
    return {
        'users': [
            {
                'login': rands(),
                'password': rands(),
                'src_login': rands(),
            }
            for _ in range(3)
        ],
    }


@pytest.mark.asyncio
async def test_race(mocker,
                    app,
                    create_request_params,
                    request_json,
                    org_id,
                    organization,
                    storage,
                    hold,
                    enforce_action_lock_timeout):
    handle_stop = hold(CreateStopImportAction, '_handle')
    enforce_action_lock_timeout(CreateImportFromJSONAction, milliseconds=1)

    stop = app.post(f'/import/{org_id}/stop/')
    create = app.post(f'/import/{org_id}/', params=create_request_params, json=request_json)

    stop = asyncio.create_task(stop)
    await handle_stop.wait_entered()

    await asyncio.create_task(create)

    handle_stop.release()

    await stop

    events = await alist(storage.event.find(org_id))
    assert_that(
        events,
        contains(has_properties({'event_type': EventType.STOP}))
    )
