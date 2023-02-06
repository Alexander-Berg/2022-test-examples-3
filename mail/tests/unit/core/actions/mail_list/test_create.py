import pytest

from hamcrest import assert_that, has_properties

from mail.beagle.beagle.core.actions.mail_list.create import CreateMailListAction
from mail.beagle.beagle.core.entities.enums import MailListType
from mail.beagle.beagle.core.entities.mail_list import MailListDescription
from mail.beagle.beagle.core.exceptions import OrganizationNotFoundError


@pytest.mark.asyncio
class TestCreateMailList:
    @pytest.fixture(autouse=True)
    def setup(self, mock_passport, mock_directory, rands, randn):
        mock_passport('/1/bundle/account/register/pdd/', {'status': 'ok', 'uid': randn()})
        mock_directory('/v11/domains/', [{'name': rands(), 'master': True}])

    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def create_user_mock(self, mocker, uid):
        async def dummy_create_user():
            return uid

        mocker.patch.object(
            CreateMailListAction,
            'create_user',
            mocker.Mock(side_effect=dummy_create_user)
        )

    @pytest.fixture
    def username(self, rands):
        return rands()

    @pytest.fixture(params=(True, False))
    def description(self, request, rands):
        if not request.param:
            return None

        return {
            'ru': rands(),
            'en': rands(),
        }

    @pytest.fixture(params=(True, False))
    def pass_uid(self, request):
        return request.param

    @pytest.fixture
    def expected_mail_list_type(self, pass_uid):
        return MailListType.AUTO if pass_uid else MailListType.MANUAL

    @pytest.fixture
    def returned_func(self, username, description, uid, pass_uid):
        async def _inner(org_id):
            return await CreateMailListAction(org_id=org_id,
                                              username=username,
                                              description=description,
                                              uid=uid if pass_uid else None,
                                              ).run()

        return _inner

    async def test_create(self, storage, returned_func, org, uid, username, description, expected_mail_list_type):
        returned = await returned_func(org.org_id)
        mail_list = await storage.mail_list.get(org.org_id, returned.mail_list_id)

        description = description or {}

        assert_that(
            mail_list,
            has_properties({
                'org_id': org.org_id,
                'uid': uid,
                'username': username,
                'description': MailListDescription(**description),
                'mail_list_type': expected_mail_list_type,
            })
        )

    async def test_org_not_found(self, returned_func, randn):
        with pytest.raises(OrganizationNotFoundError):
            await returned_func(randn())

    class TestCreateUser:
        pass  # TODO
