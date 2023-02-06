import pytest

from hamcrest import assert_that, contains, equal_to, has_entries

from mail.beagle.beagle.interactions.passport import UnknownPassportError
from mail.beagle.beagle.interactions.passport.exceptions import UserAlreadyExistsError
from mail.beagle.beagle.utils.helpers import without_none


@pytest.mark.asyncio
class TestAccountRegisterPDD:
    @pytest.fixture
    def login(self, rands):
        return rands()

    @pytest.fixture
    def domain(self, rands):
        return rands()

    @pytest.fixture
    def status(self):
        return 'ok'

    @pytest.fixture
    def errors(self):
        return None

    @pytest.fixture
    def response(self, randn, status, errors):
        return without_none({
            'status': status,
            'uid': randn(),
            'errors': errors
        })

    @pytest.fixture
    def returned_func(self, domain, login, response, clients, mock_passport):
        async def _inner():
            mock_passport('/1/bundle/account/register/pdd/', response)
            return await clients.passport.account_register_pdd(login, domain)

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_params(self, returned, login, domain, last_passport_request, response):
        request = last_passport_request()

        assert_that(
            (await request.post(), returned),
            contains(
                has_entries({'login': login, 'domain': domain, 'no_password': 'true', 'is_maillist': 'true'}),
                equal_to(response['uid'])
            )
        )

    @pytest.mark.parametrize(
        'status,errors', (
            ('error', None),
            ('error2', None),
            ('error', ['1', '2']),
            ('error', ['login.notavailable', 'login.notavailable2']),
        )
    )
    async def test_error(self, returned_func):
        with pytest.raises(UnknownPassportError):
            await returned_func()

    @pytest.mark.parametrize('status,errors', (('error', ['login.notavailable']),))
    async def test_user_already_exist_serror(self, returned_func):
        with pytest.raises(UserAlreadyExistsError):
            await returned_func()
