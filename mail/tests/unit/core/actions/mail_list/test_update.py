from dataclasses import asdict

import pytest

from hamcrest import assert_that, has_entries

from mail.beagle.beagle.core.actions.mail_list.update import UpdateMailListAction
from mail.beagle.beagle.core.entities.mail_list import MailListDescription
from mail.beagle.beagle.core.exceptions import MailListNotFoundError


@pytest.mark.asyncio
class TestUpdateMailList:
    @pytest.fixture(params=(True, False))
    def description(self, request, rands):
        if not request.param:
            return None

        return {
            'ru': rands(),
            'en': rands(),
        }

    @pytest.fixture
    def returned_func(self, org, description):
        async def _inner(mail_list_id):
            return await UpdateMailListAction(
                org_id=org.org_id,
                mail_list_id=mail_list_id,
                description=description,
            ).run()

        return _inner

    async def test_update(self, mail_list, storage, returned_func, org, description):
        returned = await returned_func(mail_list.mail_list_id)
        mail_list = await storage.mail_list.get(org.org_id, returned.mail_list_id)

        description = description or {}

        assert_that(
            asdict(mail_list),
            has_entries({
                'description': asdict(MailListDescription(**description)),
            })
        )

    async def test_org_not_found(self, returned_func, randn):
        with pytest.raises(MailListNotFoundError):
            await returned_func(randn())
