import pytest

from hamcrest import assert_that, contains_inanyorder, has_properties

from mail.beagle.beagle.core.actions.mail_list_responsible.get import GetMailListResponsibleAction
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


class TestGetMailListResponsible(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return GetMailListResponsibleAction

    @pytest.fixture
    def params(self, org, mail_list):
        return {
            'org_id': org.org_id,
            'mail_list_id': mail_list.mail_list_id
        }

    @pytest.mark.asyncio
    async def test_returned(self, mail_list_responsibles, returned):
        assert_that(
            returned,
            contains_inanyorder(*[
                has_properties({
                    'uid': responsible.uid,
                    'mail_list_id': responsible.mail_list_id,
                    'org_id': responsible.org_id,
                    'created': responsible.created,
                    'updated': responsible.updated
                })
                for responsible in mail_list_responsibles
            ])
        )
