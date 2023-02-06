from dataclasses import asdict

import pytest
from marshmallow import ValidationError

from mail.payments.payments.api.schemas.merchant import (
    AddressSchema, BankSchema, DumpMerchantSchema, InnFieldValidators, MerchantSuggestRequestSchema, OrganizationSchema
)
from mail.payments.payments.core.entities.enums import MerchantType
from mail.payments.payments.tests.utils import random_string
from mail.payments.payments.utils.const import MAX_LENGTH_ENGLISH_NAME


class TestAddressSchema:
    @pytest.mark.parametrize('zip', ['1234', '1234567', 'ac1234', ''])
    def test_incorrect_zip_in_address(self, zip):
        json = {'city': 'Москва', 'country': 'RUS', 'home': '81', 'street': 'Заслонова', 'zip': zip}
        with pytest.raises(ValidationError):
            AddressSchema().load(json)

    @pytest.mark.parametrize('country', ['RU', ''])
    def test_incorrect_country_in_address(self, country):
        json = {'city': 'Москва', 'country': country, 'home': '81', 'street': 'Заслонова', 'zip': '123456'}
        with pytest.raises(ValidationError):
            AddressSchema().load(json)

    def test_incorrect_city_in_address(self):
        json = {'city': '', 'country': 'RUS', 'home': '81', 'street': 'Заслонова', 'zip': '123456'}
        with pytest.raises(ValidationError):
            AddressSchema().load(json)


class TestOrganizationSchema:
    @pytest.mark.parametrize('name', ['', random_string(OrganizationSchema.MAX_LENGTH_NAME + 1)])
    def test_invalid_name_in_organization_schema(self, name):
        json = {'type': MerchantType.OOO, 'name': name, 'english_name': 'english_name', 'full_name': 'full_name',
                'inn': '1234', 'ogrn': '1234567890123'}
        with pytest.raises(ValidationError):
            OrganizationSchema().load(json)

    @pytest.mark.parametrize('english_name', ['', random_string(MAX_LENGTH_ENGLISH_NAME + 1),
                                              'кириллица', ':?;;*'])
    def test_invalid_english_name_in_organization_schema(self, english_name):
        json = {'type': MerchantType.OOO, 'name': 'ssss', 'english_name': english_name, 'full_name': 'full_name',
                'inn': '1234', 'ogrn': '1234567890123'}
        with pytest.raises(ValidationError):
            OrganizationSchema().load(json)

    @pytest.mark.parametrize('inn', ['', random_string(InnFieldValidators.MAX_LENGTH + 1), 'latin23',
                                     '123456789876   '])
    def test_invalid_inn_name_in_organization_schema(self, inn):
        json = {'type': MerchantType.OOO, 'name': 'ssss', 'english_name': 'somename', 'full_name': 'full_name',
                'inn': inn, 'ogrn': '1234567890123'}
        with pytest.raises(ValidationError):
            OrganizationSchema().load(json)

    def test_invalid_site_url_in_organization_schema(self):
        json = {'type': MerchantType.OOO, 'name': 'ssss', 'english_name': 'somename', 'full_name': 'full_name',
                'inn': '12345', 'ogrn': '1234567890123', 'siteUrl': 'http://woo. ka'}
        with pytest.raises(ValidationError):
            OrganizationSchema().load(json)

    @pytest.mark.parametrize('ogrn', ['', '123456789123 '])
    def test_invalid_ogrn_in_organization_schema(self, ogrn):
        json = {'type': MerchantType.OOO, 'name': 'ssss', 'english_name': 'somename', 'full_name': 'full_name',
                'inn': '123456789876', 'ogrn': ogrn}
        with pytest.raises(ValidationError):
            OrganizationSchema().load(json)

    def test_invalid_type_in_organization_schema(self):
        json = {'type': random_string(5), 'name': 'ssss', 'english_name': 'somename', 'full_name': 'full_name',
                'inn': '1234', 'ogrn': '1234567890123'}
        with pytest.raises(ValidationError):
            OrganizationSchema().load(json)


class TestBankSchema:
    @pytest.mark.parametrize('bik', ['', '12345678 ', 's12345678'])
    def test_invalid_bik_in_bank_schema(self, bik):
        json = {'account': 'account', 'bik': bik, 'correspondent_account': '12345678901234567890', 'name': 'name'}
        with pytest.raises(ValidationError):
            BankSchema().load(json)

    @pytest.mark.parametrize('correspondent_account', ['', random_string(21), random_string(19),
                                                       '1234567890123456789 '])
    def test_invalid_corr_account_in_bank_schema(self, correspondent_account):
        json = {'account': 'account', 'bik': '1234565789', 'correspondent_account': correspondent_account,
                'name': 'name'}
        with pytest.raises(ValidationError):
            BankSchema().load(json)


class TestMerchantSchema:
    class TestPreregisterDataField:
        def test_no_preregistration(self, merchant):
            assert merchant.preregistration is None
            schema = DumpMerchantSchema()
            merchant_dumped = schema.dump(merchant).data
            assert 'preregister_data' not in merchant_dumped

        def test_no_preregister_data(self, merchant, merchant_preregistration):
            merchant.preregistration = merchant_preregistration
            merchant.preregistration.data.preregister_data = None
            schema = DumpMerchantSchema()
            merchant_dumped = schema.dump(merchant).data
            merchant_dumped['preregister_data'] is None

        def test_preregister_data(self, merchant, merchant_preregistration):
            merchant.preregistration = merchant_preregistration
            schema = DumpMerchantSchema()
            merchant_dumped = schema.dump(merchant).data
            merchant_dumped['preregister_data'] == asdict(merchant_preregistration.data.preregister_data)


class TestMerchantSuggestRequestSchema:
    @pytest.mark.parametrize('query,expected', (
        pytest.param('simple text', 'simple text', id='text'),
        pytest.param('digits 111 2345', 'digits 111 2345', id='digits'),
        pytest.param(' trim  spaces    ', 'trim spaces', id='trim'),
        pytest.param('a!"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~b', 'a b', id='punctuation'),
        pytest.param('  .ИП "Иваныч" #1', 'ИП Иваныч 1', id='complex'),
        pytest.param('  .', '', id='empty'),
    ))
    def test_merchant_suggest_request_query(self, query, expected):
        result = MerchantSuggestRequestSchema().load({'query': query})
        assert expected == result.data['query']

    def test_merchant_suggest_request_empty(self):
        with pytest.raises(ValidationError):
            MerchantSuggestRequestSchema().load({})
