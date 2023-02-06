import pytest

from mail.payments.payments.core.actions.manager.base import BaseManagerAction
from mail.payments.payments.core.entities.enums import Role
from mail.payments.payments.core.exceptions import ManagerActionNotAllowed


@pytest.fixture
def action_cls():
    class Action(BaseManagerAction):
        require_roles = (Role.ADMIN,)

        async def handle(self):
            pass

    return Action


@pytest.fixture
def params(manager):
    return {'manager_uid': manager.uid}


@pytest.fixture
def action(action_cls, params):
    return action_cls(**params)


@pytest.mark.parametrize(
    ('manager_plain_roles', 'require_roles'),
    [
        [(Role.ADMIN,), (Role.ASSESSOR,)],
        [(Role.ADMIN,), (Role.ADMIN,)],
        [(Role.ASSESSOR,), (Role.ASSESSOR,)],
        [(), ()],
        [(Role.ASSESSOR,), ()]
    ])
@pytest.mark.asyncio
async def test_allowed(action, manager, manager_plain_roles, require_roles, mocker):
    mocker.patch.object(action, 'require_roles', require_roles)
    await action.run()


@pytest.mark.parametrize(
    ('manager_plain_roles', 'require_roles'),
    [
        [(Role.ASSESSOR,), (Role.ADMIN,)],
        [(), (Role.ADMIN,)],
        [(), (Role.ASSESSOR,)],
    ])
@pytest.mark.asyncio
async def test_raises_not_allowed(action, manager, manager_plain_roles, require_roles, mocker):
    mocker.patch.object(action, 'require_roles', require_roles)
    with pytest.raises(ManagerActionNotAllowed):
        await action.run()
