import pytest
import xmltodict

from mail.payments.payments.interactions.spark.exceptions import SparkAuthError, SparkGetInfoError, SparkSessionEndError
from mail.payments.payments.interactions.spark.response import SparkResponse


def remove_field_from_response_data(xml, body_item, path):
    path_items = path.split('.')
    parsed = xmltodict.parse(xml, dict_constructor=dict)
    parsed_response = parsed['soap:Envelope']['soap:Body'][body_item]
    parsed_response_data = xmltodict.parse(parsed_response['xmlData'], dict_constructor=dict)
    data = parsed_response_data['Response']['Data']['Report']
    for path_item in path_items[:-1]:
        data = data[path_item]
    data.pop(path_items[-1])
    parsed_response['xmlData'] = xmltodict.unparse(parsed_response_data)
    return xmltodict.unparse(parsed)


class TestSparkResponse:
    def test_auth_success(self, auth_success_response):
        SparkResponse.check_auth_response(auth_success_response)

    def test_auth_fail(self, auth_fail_response):
        with pytest.raises(SparkAuthError):
            SparkResponse.check_auth_response(auth_fail_response)

    def test_end_success(self, end_success_response):
        SparkResponse.check_end_response(end_success_response)

    def test_end_fail(self, end_fail_response):
        with pytest.raises(SparkSessionEndError):
            SparkResponse.check_end_response(end_fail_response)

    def test_entrepreneur_success_response(self, entrepreneur_success_response, entrepreneur_spark_data):
        actual_spark_data = SparkResponse.parse_entrepreneur_response(entrepreneur_success_response)
        assert actual_spark_data == entrepreneur_spark_data

    def test_entrepreneur_fail_response(self, entrepreneur_fail_response):
        with pytest.raises(SparkGetInfoError) as e:
            SparkResponse.parse_entrepreneur_response(entrepreneur_fail_response)
            assert e.message == 'Authentication error'

    def test_single_company_success_response(self, company_success_response, company_spark_data):
        actual_spark_data = SparkResponse.parse_company_response(company_success_response)
        assert actual_spark_data == company_spark_data

    def test_several_companies_success_response(self, companies_success_response, company_spark_data):
        actual_spark_data = SparkResponse.parse_company_response(companies_success_response)
        assert actual_spark_data == company_spark_data

    def test_company_fail_response(self, company_fail_response):
        with pytest.raises(SparkGetInfoError) as e:
            SparkResponse.parse_company_response(company_fail_response)
            assert e.message == 'Authentication error'

    class TestMissingFieldAddress:
        @pytest.mark.parametrize('keys,attr,value', (
            pytest.param(['@City'], 'city', '', id='city'),
            pytest.param(['@PostCode'], 'zip', '', id='zip'),
            pytest.param(['@BuildingType', '@BuildingNumber', '@HousingType', '@Housing'], 'home', None, id='home'),
        ))
        def test_no_address(self, company_success_response, company_spark_data, keys, attr, value):
            response = company_success_response
            for key in keys:
                response = remove_field_from_response_data(response,
                                                           'GetCompanyShortReportResponse',
                                                           f'LegalAddresses.Address.{key}')
            setattr(company_spark_data.addresses[0].address, attr, value)
            assert SparkResponse.parse_company_response(response) == company_spark_data

        def test_no_address_street(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response,
                                                       'GetCompanyShortReportResponse',
                                                       'LegalAddresses.Address.@StreetName')
            expected = company_spark_data.addresses[0].address
            expected.street = expected.home
            expected.home = ''
            assert SparkResponse.parse_company_response(response) == company_spark_data

        def test_no_addresses_items(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response,
                                                       'GetCompanyShortReportResponse',
                                                       'LegalAddresses.Address')
            company_spark_data.addresses = None
            assert SparkResponse.parse_company_response(response) == company_spark_data

        def test_empty_address(self, company_success_response, company_spark_data):
            response = company_success_response
            for key in ['@BuildingType',
                        '@BuildingNumber',
                        '@HousingType',
                        '@Housing',
                        '@City',
                        '@PostCode',
                        '@StreetName']:
                response = remove_field_from_response_data(response,
                                                           'GetCompanyShortReportResponse',
                                                           f'LegalAddresses.Address.{key}')
            company_spark_data.addresses = None
            assert SparkResponse.parse_company_response(response) == company_spark_data

    class TestMissingFieldCompany:
        body_item = 'GetCompanyShortReportResponse'

        def test_missing_field_company__no_reg_date(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response, self.body_item, 'DateFirstReg')
            company_spark_data.registration_date = None
            actual_spark_data = SparkResponse.parse_company_response(response)
            assert actual_spark_data == company_spark_data

        def test_missing_field_company__no_fullname(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response, self.body_item, 'FullNameRus')
            company_spark_data.organization_data.organization.full_name = None
            actual_spark_data = SparkResponse.parse_company_response(response)
            assert actual_spark_data == company_spark_data

        def test_missing_field_company__no_kpp(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response, self.body_item, 'KPP')
            company_spark_data.organization_data.organization.kpp = None
            actual_spark_data = SparkResponse.parse_company_response(response)
            assert actual_spark_data == company_spark_data

        def test_missing_field_company__no_inn(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response, self.body_item, 'INN')
            company_spark_data.organization_data.organization.inn = None
            actual_spark_data = SparkResponse.parse_company_response(response)
            assert actual_spark_data == company_spark_data

        def test_missing_field_company__no_ogrn(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response, self.body_item, 'OGRN')
            company_spark_data.organization_data.organization.ogrn = None
            actual_spark_data = SparkResponse.parse_company_response(response)
            assert actual_spark_data == company_spark_data

        def test_missing_field_company__no_okved_list(self, company_success_response, company_spark_data):
            response = remove_field_from_response_data(company_success_response, self.body_item, 'OKVED2List')
            company_spark_data.okved_list = None
            actual_spark_data = SparkResponse.parse_company_response(response)
            assert actual_spark_data == company_spark_data

    class TestMissingFieldEntrepreneur:
        body_item = 'GetEntrepreneurShortReportResponse'

        def test_missing_field_entrepreneur__no_fullname(self, entrepreneur_success_response, entrepreneur_spark_data):
            response = remove_field_from_response_data(entrepreneur_success_response, self.body_item, 'FullNameRus')
            entrepreneur_spark_data.organization_data.organization.full_name = None
            entrepreneur_spark_data.leaders = None
            actual_spark_data = SparkResponse.parse_entrepreneur_response(response)
            assert actual_spark_data == entrepreneur_spark_data

        def test_missing_field_entrepreneur__no_inn(self, entrepreneur_success_response, entrepreneur_spark_data):
            response = remove_field_from_response_data(entrepreneur_success_response, self.body_item, 'INN')
            entrepreneur_spark_data.organization_data.organization.inn = None
            actual_spark_data = SparkResponse.parse_entrepreneur_response(response)
            assert actual_spark_data == entrepreneur_spark_data

        def test_missing_field_entrepreneur__no_ogrn(self, entrepreneur_success_response, entrepreneur_spark_data):
            response = remove_field_from_response_data(entrepreneur_success_response, self.body_item, 'OGRNIP')
            entrepreneur_spark_data.organization_data.organization.ogrn = None
            actual_spark_data = SparkResponse.parse_entrepreneur_response(response)
            assert actual_spark_data == entrepreneur_spark_data

        def test_missing_field_entrepreneur__no_okved_list(self, entrepreneur_success_response,
                                                           entrepreneur_spark_data):
            response = remove_field_from_response_data(entrepreneur_success_response, self.body_item, 'OKVED2List')
            entrepreneur_spark_data.okved_list = None
            actual_spark_data = SparkResponse.parse_entrepreneur_response(response)
            assert actual_spark_data == entrepreneur_spark_data

    class TestFormatHomeForAddress:
        def test_format_home_for_address__all_components(self):
            assert '11/22с33' == SparkResponse._format_home_for_address({
                '@BuildingNumber': '11',
                '@Housing': '22',
                '@Block': '33',
            })

        def test_format_home_for_address__without_block(self):
            assert '11/22' == SparkResponse._format_home_for_address({
                '@BuildingNumber': '11',
                '@Housing': '22',
            })

        def test_format_home_for_address__without_housing(self):
            assert '11/33' == SparkResponse._format_home_for_address({
                '@BuildingNumber': '11',
                '@Block': '33',
            })

        def test_format_home_for_address__without_housing_and_block(self):
            assert '11' == SparkResponse._format_home_for_address({'@BuildingNumber': '11'})
