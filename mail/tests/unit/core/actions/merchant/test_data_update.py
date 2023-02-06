from copy import deepcopy

import pytest

from hamcrest import assert_that, contains, has_properties

from mail.payments.payments.core.actions.merchant.data_update import MerchantDataUpdateAction
from mail.payments.payments.core.entities.enums import CallbackMessageType
from mail.payments.payments.core.entities.merchant import AddressData, OrganizationData, PersonData, PersonType
from mail.payments.payments.core.exceptions import CoreFailError, CoreInteractionFatalError
from mail.payments.payments.interactions.balance.exceptions import BalancePersonNotFound


class TestUpdateMerchantFromPerson:
    def test_updated(self, merchant, person_entity):
        data = deepcopy(merchant.data)
        MerchantDataUpdateAction._update_merchant_from_person(merchant, person_entity)

        data.addresses = [
            AddressData(
                type='legal',
                city=person_entity.legal_address_city,
                country='RUS',
                home=person_entity.legal_address_home,
                street=person_entity.legal_address_street,
                zip=person_entity.legal_address_postcode,
            ),
            AddressData(
                type='post',
                city=person_entity.address_city,
                country='RUS',
                home=person_entity.address_home,
                street=person_entity.address_street,
                zip=person_entity.address_postcode,
            ),
        ]
        data.bank.account = person_entity.account
        data.bank.bik = person_entity.bik
        data.persons = [
            PersonData(
                type=PersonType.CEO,
                name=person_entity.fname,
                email=person_entity.email,
                phone=person_entity.phone,
                surname=person_entity.lname,
                patronymic=person_entity.mname,
                birth_date=merchant.ceo.birth_date,
            ),
            *[
                PersonData(
                    type=p.type,
                    name=p.name,
                    email=p.email,
                    phone=p.phone,
                    surname=p.surname,
                    patronymic=p.patronymic,
                    birth_date=p.birth_date,
                )
                for p in merchant.persons
                if p.type != PersonType.CEO
            ]
        ]
        data.organization = OrganizationData(
            type=merchant.organization.type,
            name=person_entity.name,
            english_name=merchant.organization.english_name,
            full_name=person_entity.longname,
            inn=person_entity.inn,
            kpp=person_entity.kpp,
            ogrn=person_entity.ogrn,
            description=merchant.organization.description,
            site_url=merchant.organization.site_url,
        )
        assert merchant.data == data

    def test_with_post_address(self, merchant, person_entity):
        person_entity.address_city = 'Москва2',
        person_entity.address_home = '17',
        person_entity.address_postcode = '000001',
        person_entity.address_street = 'some street2',

        MerchantDataUpdateAction._update_merchant_from_person(merchant, person_entity)
        assert_that(
            merchant.data,
            has_properties({
                'addresses': contains(*[
                    has_properties({
                        'type': 'legal',
                        'city': person_entity.legal_address_city,
                        'country': 'RUS',
                        'home': person_entity.legal_address_home,
                        'street': person_entity.legal_address_street,
                        'zip': person_entity.legal_address_postcode,
                    }),
                    has_properties({
                        'type': 'post',
                        'city': person_entity.address_city,
                        'country': 'RUS',
                        'home': person_entity.address_home,
                        'street': person_entity.address_street,
                        'zip': person_entity.address_postcode,
                    }),
                ])
            })
        )


class TestMerchantDataUpdateAction:
    @pytest.mark.asyncio
    async def test_merchant_is_none(self):
        with pytest.raises(CoreFailError):
            await MerchantDataUpdateAction(merchant=None).run()

    @pytest.mark.asyncio
    async def test_person_id_is_none(self, merchant, balance_client_mocker):
        merchant.person_id = None
        with balance_client_mocker('get_person') as mock:
            merchant = await MerchantDataUpdateAction(merchant=merchant).run()
            assert merchant.data_loaded and not mock.called

    @pytest.mark.asyncio
    async def test_person_is_none(self, merchant, balance_client_mocker):
        with balance_client_mocker('get_person', result=None):
            with pytest.raises(CoreFailError):
                await MerchantDataUpdateAction(merchant=merchant).run()

    @pytest.mark.asyncio
    async def test_person_not_found(self, merchant, balance_client_mocker):
        with balance_client_mocker('get_person', exc=BalancePersonNotFound):
            with pytest.raises(CoreInteractionFatalError):
                await MerchantDataUpdateAction(merchant=merchant).run()

    @pytest.mark.asyncio
    async def test_person_key_error(self, merchant, balance_client_mocker):
        with balance_client_mocker('get_person', exc=KeyError('some_key')):
            merchant = await MerchantDataUpdateAction(merchant=merchant).run()
            assert merchant.data_loaded

    @pytest.mark.asyncio
    async def test_person_loaded(self, merchant, balance_client_mocker, person_entity):
        with balance_client_mocker('get_person', result=person_entity):
            merchant = await MerchantDataUpdateAction(merchant=merchant).run()
            assert merchant.data_loaded

    @pytest.mark.asyncio
    async def test_sends_notifications(self, merchant, balance_client_mocker, person_entity,
                                       storage, service_merchant, service_client):
        with balance_client_mocker('get_person', result=person_entity):
            await MerchantDataUpdateAction(merchant=merchant).run()

        task = await storage.task.find().__anext__()
        expected_params = dict(tvm_id=service_client.tvm_id,
                               callback_message_type=CallbackMessageType.MERCHANT_REQUISITES_UPDATED.value,
                               callback_url=service_client.api_callback_url,
                               message={'service_merchant_id': service_merchant.service_merchant_id})
        assert task.params == expected_params
