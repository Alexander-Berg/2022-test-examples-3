import pytest


@pytest.mark.asyncio
async def test_user_created(user_entity, user):
    assert user_entity == user


@pytest.mark.asyncio
async def test_get_user(user, storage):
    assert user == await storage.user.get(uid=user.uid)
