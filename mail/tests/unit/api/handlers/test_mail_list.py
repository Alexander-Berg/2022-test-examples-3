import pytest

from sendr_utils import utcnow

from mail.beagle.beagle.core.actions.mail_list.create import CreateMailListAction
from mail.beagle.beagle.core.actions.mail_list.delete import DeleteMailListAction
from mail.beagle.beagle.core.actions.mail_list.get import GetMailListAction
from mail.beagle.beagle.core.actions.mail_list.get_list import GetListListMailAction
from mail.beagle.beagle.core.actions.mail_list.update import UpdateMailListAction
from mail.beagle.beagle.core.entities.enums import MailListType


class TestMailListListHandler:
    @pytest.fixture
    def org_id(self, randn):
        return randn()

    class TestGet:
        @pytest.fixture
        def action(self, mock_action):
            return mock_action(GetListListMailAction)

        @pytest.fixture
        async def response(self, app, org_id):
            return await app.get(f'/api/v1/mail_list/{org_id}')

        def test_params(self, action, response, org_id):
            action.assert_called_with(org_id=org_id)

        class TestQueryParams:
            @pytest.fixture
            def params(self, rands):
                return {'name_query': rands()}

            @pytest.fixture
            async def response(self, app, org_id, params):
                return await app.get(f'/api/v1/mail_list/{org_id}', params=params)

            def test_get_by_name_query(self, action, response, org_id, params):
                action.assert_called_with(org_id=org_id, **params)

    class TestPost:
        @pytest.fixture
        def action(self, mock_action):
            return mock_action(CreateMailListAction)

        @pytest.fixture
        def data(self, randn, rands):
            return {
                'username': rands(),
                'mail_list_id': randn(),
                'mail_list_type': MailListType.AUTO.value,
                'uid': randn(),
                'description': {
                    'ru': rands(),
                    'en': rands(),
                    rands(): rands(),
                },
                'created': utcnow().isoformat(),
                'updated': utcnow().isoformat()
            }

        @pytest.fixture
        async def response(self, app, org_id, data):
            return await app.post(f'/api/v1/mail_list/{org_id}', json=data)

        def test_params(self, action, response, org_id, data):
            action.assert_called_with(
                org_id=org_id,
                username=data['username'],
                generate_cache=True,
                description={'ru': data['description']['ru'], 'en': data['description']['en']}
            )


class TestMailListHandler:
    @pytest.fixture
    def org_id(self, randn):
        return randn()

    @pytest.fixture
    def mail_list_id(self, randn):
        return randn()

    class TestGet:
        @pytest.fixture
        def action(self, mock_action):
            return mock_action(GetMailListAction)

        @pytest.fixture
        async def response(self, app, org_id, mail_list_id):
            return await app.get(f'/api/v1/mail_list/{org_id}/{mail_list_id}')

        def test_params(self, action, response, org_id, mail_list_id):
            action.assert_called_with(org_id=org_id, mail_list_id=mail_list_id)

    class TestPut:
        @pytest.fixture
        def action(self, mock_action):
            return mock_action(UpdateMailListAction)

        @pytest.fixture
        def data(self, rands):
            return {
                'description': {
                    'ru': rands(),
                    'en': rands(),
                    rands(): rands(),
                },
            }

        @pytest.fixture
        async def response(self, app, org_id, mail_list_id, data):
            return await app.put(f'/api/v1/mail_list/{org_id}/{mail_list_id}', json=data)

        def test_params(self, action, response, org_id, mail_list_id, data):
            action.assert_called_with(
                org_id=org_id,
                mail_list_id=mail_list_id,
                description={'ru': data['description']['ru'], 'en': data['description']['en']}
            )

    class TestDelete:
        @pytest.fixture
        def action(self, mock_action):
            return mock_action(DeleteMailListAction)

        @pytest.fixture
        async def response(self, app, org_id, mail_list_id):
            return await app.delete(f'/api/v1/mail_list/{org_id}/{mail_list_id}')

        def test_params(self, action, response, org_id, mail_list_id):
            action.assert_called_with(org_id=org_id, mail_list_id=mail_list_id)
