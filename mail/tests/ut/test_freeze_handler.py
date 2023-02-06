import pytest

from mail.shiva.stages.api.props.util.freeze_handler import FreezeUserParams
from mail.shiva.stages.api.settings.freeze_settings import FreezeSettings, ALLOWED_UIDS
from mail.python.theatre.app.env import EnvType

test_data = [
    (ALLOWED_UIDS[0], EnvType.Prod, True),
    ('abcdefghjklmn', EnvType.Prod, False),
    (ALLOWED_UIDS[0], EnvType.Corp, False),
    ('abcdefghjklmn', EnvType.Corp, False),
    (ALLOWED_UIDS[0], EnvType.Test, True),
    ('abcdefghjklmn', EnvType.Test, True),
]


@pytest.mark.parametrize('uid,env,expected', test_data)
def test_check_uid_allowed(uid, env, expected):
    params = FreezeUserParams(uid=uid, freeze_settings=FreezeSettings().configure(env))
    assert params.check_uid_allowed() == expected
