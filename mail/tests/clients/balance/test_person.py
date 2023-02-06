import re
from dataclasses import asdict
from xmlrpc import client as xmlrpc

import pytest

from sendr_interactions.clients.balance.entities import Person

from hamcrest import assert_that, equal_to, has_entries, match_equality, not_none


class TestCreatePerson:
    @pytest.fixture
    def response_data(self, create_person_response):
        return create_person_response

    @pytest.fixture
    async def returned(self, balance_client, person_entity):
        return await balance_client.create_person(uid=333, person=person_entity)

    @pytest.fixture
    def processed_response(self, person_id):
        return ((int(person_id),), None)

    @pytest.fixture
    def expected_request_person_data(self, person_entity):
        person_entity_dict = asdict(person_entity)
        del person_entity_dict['date']
        del person_entity_dict['post_address']
        del person_entity_dict['legal_address']
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
        return person_data

    def test_request_call(self, balance_client, expected_request_person_data, returned, balance_mock):
        assert_that(
            xmlrpc.loads(balance_mock.call_args.kwargs['data']),
            equal_to(
                (
                    (
                        '333',
                        expected_request_person_data,
                    ),
                    'CreatePerson',
                )
            ),
        )

    def test_returns_person_id(self, person_id, returned):
        assert returned == person_id

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, create_person_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=create_person_response)

    @pytest.mark.parametrize(
        'person_entity_field_name, person_data_field_name',
        (
            ('kpp', 'kpp'),
            ('person_id', 'person_id'),
        ),
    )
    class TestFieldNone:
        @pytest.fixture
        def person_entity(self, person_data, person_entity_field_name):
            person_data[person_entity_field_name] = None
            return Person(**person_data)

        def test_request_call_without_field(
            self, balance_client, returned, balance_mock, expected_request_person_data, person_data_field_name
        ):
            expected_request_person_data.pop(person_data_field_name)
            request_person_data = xmlrpc.loads(balance_mock.call_args.kwargs['data'])
            assert_that(
                request_person_data[0][1],
                equal_to(expected_request_person_data)
            )

    class TestWithoutPost:
        @pytest.fixture
        def person_entity(self, person_data):
            person_data.pop('address_city')
            person_data.pop('address_home')
            person_data.pop('address_postcode')
            person_data.pop('address_street')
            return Person(**person_data)

        def test_without_post__request_call(self, balance_client, person_entity, returned, balance_mock):
            request = xmlrpc.loads(balance_mock.call_args.kwargs['data'])
            assert_that(
                request[0][1],
                has_entries({
                    'postcode': person_entity.legal_address_postcode,
                    'postaddress': (
                        f'{person_entity.legal_address_postcode}, '
                        f'{person_entity.legal_address_city}, '
                        f'{person_entity.legal_address_street}, '
                        f'{person_entity.legal_address_home}'
                    ),
                })
            )


class TestGetClientPersons:
    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, client_id, balance_mock):
        await balance_client.get_client_persons(client_id)
        balance_mock.assert_called_once_with(
            data=xmlrpc.dumps(
                (client_id,),
                'GetClientPersons',
            ),
            headers=match_equality(not_none()),
        )

    @pytest.mark.asyncio
    async def test_returns_persons(self, balance_client, client_id, person_entity, person_date):
        person_entity.date = person_date
        assert await balance_client.get_client_persons(client_id) == [person_entity]

    class TestGetClientPersonsEmpty:
        @pytest.mark.asyncio
        async def test_returns_empty_persons(self, balance_client, client_id):
            assert [
                Person(
                    client_id='',
                    person_id=None,

                    account=None,
                    bik=None,
                    fname='',
                    lname='',
                    email='',
                    phone='',

                    name='',
                    longname='',
                    inn='',
                    kpp=None,
                    ogrn=None,

                    legal_address_city='',
                    legal_address_home='',
                    legal_address_postcode='',
                    legal_address_street='',
                )
            ] == await balance_client.get_client_persons(client_id)

        @pytest.fixture(autouse=True)
        def balance_mock(self, aioresponses_mocker, balance_client, get_client_persons_empty_response):
            return aioresponses_mocker.post(
                re.compile(rf'{balance_client.BASE_URL}'), body=get_client_persons_empty_response
            )

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, get_client_persons_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=get_client_persons_response)


class TestGetPerson:
    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, person_id, balance_mock):
        await balance_client.get_person(person_id)
        balance_mock.assert_called_once_with(
            data=xmlrpc.dumps(
                ({"ID": person_id},),
                'GetPerson',
            ),
            headers=match_equality(not_none()),
        )

    @pytest.mark.asyncio
    async def test_found(self, balance_client, person_id, person_entity, person_date):
        person_entity.date = person_date
        assert await balance_client.get_person(person_id) == person_entity

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, get_person_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=get_person_response)
