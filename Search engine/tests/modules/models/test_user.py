import pytest

from bot.modules.models import User


@pytest.fixture(scope='function')
@pytest.mark.asyncio
async def prepare_dismissed(get_context):
    data = get_context.data
    async with await data.connect() as conn:
        dismissed = await User.create(
            conn,
            login='dismissed_user',
            tg_id=12345,
            is_dismissed=True
        )
    try:
        yield dismissed
    finally:
        async with await data.connect() as conn:
            conn.execute('DELETE from users where login in (%s);', (dismissed.login,))


@pytest.mark.asyncio
async def test_ensure_dismissed(get_context, prepare_dismissed):
    person = {
        'login': 'dismissed_user',
        'official': {
            'is_dismissed': False
        }
    }
    async with await get_context.data.connect() as conn:
        ensured = await User.ensure(conn, person)
    assert ensured is not None
    assert ensured.tg_id == 12345
    assert ensured.is_dismissed == False
