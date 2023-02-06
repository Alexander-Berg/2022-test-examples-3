from dataclasses import asdict

import pytest
from sendr_writers.base.pusher import InteractionResponseLog

from hamcrest import all_of, assert_that, has_entries, has_properties, is_

from mail.payments.payments.interactions.balance.entities import Person


class TestCreatePerson:
    @pytest.fixture
    def response_data(self, create_person_response):
        return create_person_response

    @pytest.fixture
    async def returned(self, balance_client, merchant_uid, person_entity):
        return await balance_client.create_person(merchant_uid, person_entity)

    @pytest.fixture
    def processed_response(self, person_id):
        return ((int(person_id),), None)

    def test_request_call(self, balance_client, merchant_uid, person_entity, returned):
        person_entity_dict = asdict(person_entity)
        del person_entity_dict['date']
        person_data = {
            **person_entity_dict,
            'type': 'ur',
            'postcode': person_entity.address_postcode,
            'postaddress': (
                f'{person_entity.address_postcode}, '
                f'{person_entity.address_city}, '
                f'{person_entity.address_street}, '
                f'{person_entity.address_home}'
            ),
            'legaladdress': (
                f'{person_entity.legal_address_postcode}, '
                f'{person_entity.legal_address_city}, '
                f'{person_entity.legal_address_street}, '
                f'{person_entity.legal_address_home}'
            )
        }
        assert balance_client.call_kwargs == {
            'method_name': 'CreatePerson',
            'data': (str(merchant_uid), person_data),
        }

    def test_returns_person_id(self, person_id, returned):
        assert returned == person_id

    def test_create_person_response_logged(self, balance_client, pushers_mock, processed_response, returned):
        assert_that(
            pushers_mock.response_log.push.call_args[0][0],
            all_of(
                is_(InteractionResponseLog),
                has_properties(dict(
                    response=processed_response,
                    request_url=balance_client.call_args[2],
                    request_method=balance_client.call_args[1],
                    request_kwargs=balance_client.call_kwargs,
                    request_id=balance_client.request_id,
                ))
            ),
        )

    class TestKPPNone:
        @pytest.fixture
        def person_entity(self, person_data):
            person_data['kpp'] = None
            return Person(**person_data)

        def test_request_call_without_kpp(self, balance_client, returned):
            assert 'kpp' not in balance_client.call_kwargs['data'][1]

    class TestWithoutPost:
        @pytest.fixture
        def person_entity(self, person_data):
            person_data.pop('address_city')
            person_data.pop('address_home')
            person_data.pop('address_postcode')
            person_data.pop('address_street')
            return Person(**person_data)

        def test_without_post__request_call(self, balance_client, person_entity, returned):
            assert_that(
                balance_client.call_kwargs['data'][1],
                has_entries({
                    'postcode': person_entity.legal_address_postcode,
                    'postaddress': (
                        f'{person_entity.legal_address_postcode}, '
                        f'{person_entity.legal_address_city}, '
                        f'{person_entity.legal_address_street}, '
                        f'{person_entity.legal_address_home}'
                    ),
                    'address_city': '',
                    'address_home': '',
                    'address_postcode': '',
                    'address_street': '',
                })
            )


class TestGetClientPersons:
    @pytest.fixture
    def response_data(self, get_client_persons_response):
        return get_client_persons_response

    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, client_id):
        await balance_client.get_client_persons(client_id)
        assert balance_client.call_kwargs == {
            'method_name': 'GetClientPersons',
            'data': (client_id,),
        }

    @pytest.mark.asyncio
    async def test_returns_persons(self, balance_client, client_id, person_entity, person_date):
        person_entity.date = person_date
        assert await balance_client.get_client_persons(client_id) == [person_entity]

    class TestGetClientPersonsEmpty:
        @pytest.fixture
        def response_data(self, get_client_persons_empty_response):
            return get_client_persons_empty_response

        @pytest.mark.asyncio
        async def test_returns_empty_persons(self, balance_client, client_id):
            assert [
                Person(
                    client_id='',
                    person_id=None,

                    account='',
                    bik='',
                    fname='',
                    lname='',
                    email='',
                    phone='',

                    name='',
                    longname='',
                    inn='',
                    kpp=None,
                    ogrn='',

                    legal_address_city='',
                    legal_address_home='',
                    legal_address_postcode='',
                    legal_address_street='',
                )
            ] == await balance_client.get_client_persons(client_id)


class TestGetPerson:
    @pytest.fixture
    def response_data(self, get_person_response):
        return get_person_response

    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, person_id):
        await balance_client.get_person(person_id)
        assert balance_client.call_kwargs == {
            'method_name': 'GetPerson',
            'data': ({"ID": person_id},),
        }

    @pytest.mark.asyncio
    async def test_found(self, balance_client, person_id, person_entity, person_date):
        person_entity.date = person_date
        assert await balance_client.get_person(person_id) == person_entity
