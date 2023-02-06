from random import randint

import pytest

from mail.payments.payments.core.actions.base.db import BaseDBAction


class ExistingAction(BaseDBAction):
    # some random name
    action_name = ''.join(str(randint(0, 9)) for _ in range(10))

    async def handle(self):
        pass


def test_action_name_collision():
    """Cannot create another BaseAction inheritor with the same name"""
    with pytest.raises(RuntimeError):
        type(
            'SomeAction',
            (BaseDBAction,),
            {
                'action_name': ExistingAction.action_name,
            }
        )
