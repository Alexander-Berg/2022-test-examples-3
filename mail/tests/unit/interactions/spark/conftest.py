from datetime import date

import pytest

from mail.payments.payments.core.entities.enums import MerchantType
from mail.payments.payments.core.entities.merchant import AddressData, OrganizationData
from mail.payments.payments.core.entities.spark import (
    LeaderData, OkvedItem, PhoneData, SparkAddressData, SparkData, SparkOrganizationData
)


@pytest.fixture
def ip_inn():
    return '123456789012'


@pytest.fixture
def ooo_inn():
    return '1234567890'


@pytest.fixture
def auth_success_response():
    return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <AuthmethodResponse xmlns="http://interfax.ru/ifax">
            <AuthmethodResult>True</AuthmethodResult>
        </AuthmethodResponse>
    </soap:Body>
</soap:Envelope>"""


@pytest.fixture
def auth_fail_response():
    return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <AuthmethodResponse xmlns="http://interfax.ru/ifax">
            <AuthmethodResult>False</AuthmethodResult>
        </AuthmethodResponse>
    </soap:Body>
</soap:Envelope>"""


@pytest.fixture
def end_success_response():
    return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <EndResponse xmlns="http://interfax.ru/ifax">
            <EndResult>true</EndResult>
        </EndResponse>
    </soap:Body>
</soap:Envelope>"""


@pytest.fixture
def end_fail_response():
    return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <EndResponse xmlns="http://interfax.ru/ifax">
            <EndResult>false</EndResult>
        </EndResponse>
    </soap:Body>
</soap:Envelope>"""


@pytest.fixture
def entrepreneur_success_response(ip_inn):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <GetEntrepreneurShortReportResponse xmlns="http://interfax.ru/ifax">
            <GetEntrepreneurShortReportResult>True</GetEntrepreneurShortReportResult>
            <xmlData>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Response&gt;
  &lt;Data&gt;
    &lt;Report&gt;
      &lt;SparkID&gt;46126788&lt;/SparkID&gt;
      &lt;Status IsActing="1" Code="24" Text="Действующее" GroupId="1" GroupName="Действующее" Date="2019-05-22"/&gt;
      &lt;DateReg&gt;2019-05-22&lt;/DateReg&gt;
      &lt;FullNameRus&gt;Иванов Иван Иванович&lt;/FullNameRus&gt;
      &lt;INN&gt;{ip_inn}&lt;/INN&gt;
      &lt;OGRNIP&gt;319237500178101&lt;/OGRNIP&gt;
      &lt;OKPO&gt;0160364159&lt;/OKPO&gt;
      &lt;OKATO Code="03406000000" RegionName="Краснодарский край" RegionCode="3"/&gt;
      &lt;OKTMO Code="03608101001"/&gt;
      &lt;OKOPF Code="50102" Name="Индивидуальные предприниматели"/&gt;
      &lt;OKVED2List&gt;
        &lt;OKVED Code="49.41" Name="Деятельность автомобильного грузового транспорта" IsMain="true"/&gt;
      &lt;/OKVED2List&gt;
      &lt;FederalTaxRegistration&gt;
        &lt;RegDate&gt;2019-05-22&lt;/RegDate&gt;
        &lt;RegAuthority&gt;Межрайонная инспекция Федеральной налоговой службы № 16 по Краснодарскому краю&lt;/RegAuthority&gt;
        &lt;RegAuthorityAddress&gt;,350020,,, Краснодар г,, Коммунаров ул, д 235,,&lt;/RegAuthorityAddress&gt;
        &lt;RegAuthorityCode&gt;2375&lt;/RegAuthorityCode&gt;
      &lt;/FederalTaxRegistration&gt;
      &lt;FederalTaxRegistrationCurrent&gt;
        &lt;RegAuthority&gt;Межрайонная инспекция Федеральной налоговой службы № 16 по Краснодарскому краю&lt;/RegAuthority&gt;
        &lt;RegAuthorityAddress&gt;350020, Краснодар г, Коммунаров ул, 235&lt;/RegAuthorityAddress&gt;
        &lt;RegAuthorityCode&gt;2375&lt;/RegAuthorityCode&gt;
      &lt;/FederalTaxRegistrationCurrent&gt;
      &lt;FederalTaxRegistrationPayment&gt;
        &lt;RegDate&gt;2019-05-22&lt;/RegDate&gt;
        &lt;RegAuthority&gt;Межрайонная инспекция Федеральной налоговой службы №9 по Краснодарскому краю&lt;/RegAuthority&gt;
        &lt;RegAuthorityAddress&gt;352630, Краснодарский край, Белореченск г, Ленина ул, 29&lt;/RegAuthorityAddress&gt;
        &lt;RegAuthorityCode&gt;2368&lt;/RegAuthorityCode&gt;
      &lt;/FederalTaxRegistrationPayment&gt;
      &lt;RegistrationInFunds&gt;
        &lt;PensionFund&gt;
          &lt;RegistrationDate&gt;2019-05-24&lt;/RegistrationDate&gt;
          &lt;RegisterNumber&gt;033003029165&lt;/RegisterNumber&gt;
          &lt;RegAuthority&gt;Управление Пенсионного фонда РФ в г.Белореченске Краснодарского края&lt;/RegAuthority&gt;
        &lt;/PensionFund&gt;
      &lt;/RegistrationInFunds&gt;
      &lt;SubmittedStatements&gt;
        &lt;Statement Form="Р21001" SubmissionDate="2019-05-17" AvailabilityDate="2019-05-22" GRN="319237500178101"
            DecisionType="Решение о государственной регистрации"/&gt;
      &lt;/SubmittedStatements&gt;
      &lt;Sex Code="1" Name="мужской"/&gt;
      &lt;Citizenship Code="643" Name="Российская Федерация"/&gt;
      &lt;IncludeInList&gt;
        &lt;ListName Id="1" IsNegative="0"&gt;Реестр субъектов малого и среднего предпринимательства
            &lt;AddInfo&gt;
                &lt;AddField Name="AddDate"&gt;2019-06-10&lt;/AddField&gt;
                &lt;AddField Name="CategoryCode"&gt;1&lt;/AddField&gt;
                &lt;AddField Name="Category"&gt;Микропредприятие&lt;/AddField&gt;
            &lt;/AddInfo&gt;
        &lt;/ListName&gt;
        &lt;ListName Id="94" IsNegative="0"&gt;Перечень лиц, на которых распространяется действие моратория на банкротство&lt;/ListName&gt;
      &lt;/IncludeInList&gt;
    &lt;/Report&gt;
  &lt;/Data&gt;
  &lt;ResultInfo ResultType="True" DateTime="2020-11-05T13:58:26" ExecutionTime="31"/&gt;
&lt;/Response&gt;
            </xmlData>
        </GetEntrepreneurShortReportResponse>
    </soap:Body>
</soap:Envelope>"""  # noqa: E501


@pytest.fixture
def entrepreneur_fail_response():
    return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <GetEntrepreneurShortReportResponse xmlns="http://interfax.ru/ifax">
            <GetEntrepreneurShortReportResult>Authentication error</GetEntrepreneurShortReportResult>
            <xmlData>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Response&gt;
  &lt;ResultInfo ResultType="Authentication error" DateTime="2020-11-18T13:31:30" ExecutionTime="15" /&gt;
&lt;/Response&gt;
            </xmlData>
        </GetEntrepreneurShortReportResponse>
    </soap:Body>
</soap:Envelope>"""


@pytest.fixture
def company_success_response(ooo_inn):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
<soap:Body>
    <GetCompanyShortReportResponse xmlns="http://interfax.ru/ifax">
        <GetCompanyShortReportResult>True</GetCompanyShortReportResult>
        <xmlData>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Response&gt;
  &lt;Data&gt;
    &lt;Report ActualDate="2020-11-05"&gt;
      &lt;SparkID&gt;10605967&lt;/SparkID&gt;
      &lt;CompanyType&gt;1&lt;/CompanyType&gt;
      &lt;Status IsActing="1" Code="24" Type="Действующее" GroupId="1" GroupName="Действующее" Date="2020-11-05"/&gt;
      &lt;EGRPOIncluded&gt;true&lt;/EGRPOIncluded&gt;
      &lt;IsActing&gt;true&lt;/IsActing&gt;
      &lt;DateFirstReg&gt;2016-05-26&lt;/DateFirstReg&gt;
      &lt;ShortNameRus&gt;ООО "ЯНДЕКС.ОФД"&lt;/ShortNameRus&gt;
      &lt;ShortNameEn&gt;OOO "YANDEKS.OFD"&lt;/ShortNameEn&gt;
      &lt;FullNameRus&gt;ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.ОФД"&lt;/FullNameRus&gt;
      &lt;INN&gt;{ooo_inn}&lt;/INN&gt;
      &lt;KPP&gt;770401001&lt;/KPP&gt;
      &lt;OGRN&gt;1167746503231&lt;/OGRN&gt;
      &lt;OKPO&gt;02690517&lt;/OKPO&gt;
      &lt;OKATO Code="45286590000" RegionName="Москва" RegionCode="45"/&gt;
      &lt;OKTMO Code="45383000000"/&gt;
      &lt;OKOPF Code="65" CodeNew="12300" Name="Общества с ограниченной ответственностью"/&gt;
      &lt;OKVED2List&gt;
        &lt;OKVED Code="63.11" Name="Деятельность по обработке данных, предоставление услуг по размещению информации и связанная с этим деятельность" IsMain="true"/&gt;
      &lt;/OKVED2List&gt;
      &lt;CharterCapital&gt;*&lt;/CharterCapital&gt;
      &lt;LeaderList&gt;
        &lt;Leader ActualDate="2019-08-22" FIO="Зорин Максим Александрович" Position="генеральный директор" INN="504225766444"/&gt;
      &lt;/LeaderList&gt;
      &lt;LegalAddresses&gt;
        &lt;Address PostCode="119034" Address="г. Москва, ул. Тимура Фрунзе, д.  11 стр.  44" Region="г. Москва" City="г. Москва" StreetName="ул. Тимура Фрунзе" BuildingType="д." BuildingNumber="11" HousingType="стр." Housing="44" BusinessCenterName="БЦ Палладиум" Longitude="37,589718" Latitude="55,733939" FiasGUID="2577d2c9-1157-4a73-b888-9da4ebf83719" IsHouseFiasGUID="true" FiasCode="770000000000000424500000000" FiasRegion="77" FiasArea="000" FiasCity="000" FiasPlace="000" FiasPlan="0000" FiasStreet="4245" ActualDate="2020-05-15"/&gt;
      &lt;/LegalAddresses&gt;
      &lt;PhoneList&gt;
        &lt;Phone Code="495" Number="7397000" VerificationDate="2017-05-19"/&gt;
      &lt;/PhoneList&gt;
      &lt;IndexOfDueDiligence Index="*" IndexDesc="*"/&gt;
      &lt;AccessibleFinData&gt;
        &lt;Period IDPeriod="555" Name="2019" EndDate="2019-12-31"/&gt;
        &lt;Period IDPeriod="551" Name="2018" EndDate="2018-12-31"/&gt;
        &lt;Period IDPeriod="547" Name="2017" EndDate="2017-12-31"/&gt;
        &lt;Period IDPeriod="543" Name="2016" EndDate="2016-12-31"/&gt;
      &lt;/AccessibleFinData&gt;
      &lt;CompanyWithSameInfo&gt;
        &lt;TelephoneCount PhoneCode="495" PhoneNumber="7397000"&gt;23&lt;/TelephoneCount&gt;
        &lt;PhoneList&gt;
          &lt;PhoneCount Code="495" Number="7397000"&gt;23&lt;/PhoneCount&gt;
        &lt;/PhoneList&gt;
        &lt;AddressCount&gt;5&lt;/AddressCount&gt;
        &lt;AddressWithoutRoomCount&gt;26&lt;/AddressWithoutRoomCount&gt;
        &lt;AddressNotAffiliatedCount&gt;5&lt;/AddressNotAffiliatedCount&gt;
        &lt;AddressFTSCount&gt;1&lt;/AddressFTSCount&gt;
        &lt;ManagerCountInCountry&gt;1&lt;/ManagerCountInCountry&gt;
        &lt;ManagerCountInRegion&gt;1&lt;/ManagerCountInRegion&gt;
        &lt;ManagerInnCount&gt;1&lt;/ManagerInnCount&gt;
      &lt;/CompanyWithSameInfo&gt;
      &lt;CompanyLiquidatedWithSameInfo&gt;
        &lt;AddressCount&gt;11&lt;/AddressCount&gt;
        &lt;AddressWithoutRoomCount&gt;14&lt;/AddressWithoutRoomCount&gt;
      &lt;/CompanyLiquidatedWithSameInfo&gt;
    &lt;/Report&gt;
  &lt;/Data&gt;
  &lt;ResultInfo ResultType="True" DateTime="2020-11-05T14:31:19" ExecutionTime="31"/&gt;
&lt;/Response&gt;
            </xmlData>
        </GetCompanyShortReportResponse>
    </soap:Body>
</soap:Envelope>"""  # noqa: E501


@pytest.fixture
def companies_success_response(ooo_inn):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
<soap:Body>
    <GetCompanyShortReportResponse xmlns="http://interfax.ru/ifax">
        <GetCompanyShortReportResult>True</GetCompanyShortReportResult>
        <xmlData>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Response&gt;
  &lt;Data&gt;
    &lt;Report ActualDate="2020-11-05"&gt;
      &lt;SparkID&gt;10605967&lt;/SparkID&gt;
      &lt;CompanyType&gt;1&lt;/CompanyType&gt;
      &lt;Status IsActing="1" Code="24" Type="Действующее" GroupId="1" GroupName="Действующее" Date="2020-11-05"/&gt;
      &lt;EGRPOIncluded&gt;true&lt;/EGRPOIncluded&gt;
      &lt;IsActing&gt;true&lt;/IsActing&gt;
      &lt;DateFirstReg&gt;2016-05-26&lt;/DateFirstReg&gt;
      &lt;ShortNameRus&gt;ООО "ЯНДЕКС.ОФД"&lt;/ShortNameRus&gt;
      &lt;ShortNameEn&gt;OOO "YANDEKS.OFD"&lt;/ShortNameEn&gt;
      &lt;FullNameRus&gt;ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.ОФД"&lt;/FullNameRus&gt;
      &lt;INN&gt;{ooo_inn}&lt;/INN&gt;
      &lt;KPP&gt;770401001&lt;/KPP&gt;
      &lt;OGRN&gt;1167746503231&lt;/OGRN&gt;
      &lt;OKPO&gt;02690517&lt;/OKPO&gt;
      &lt;OKATO Code="45286590000" RegionName="Москва" RegionCode="45"/&gt;
      &lt;OKTMO Code="45383000000"/&gt;
      &lt;OKOPF Code="65" CodeNew="12300" Name="Общества с ограниченной ответственностью"/&gt;
      &lt;OKVED2List&gt;
        &lt;OKVED Code="63.11" Name="Деятельность по обработке данных, предоставление услуг по размещению информации и связанная с этим деятельность" IsMain="true"/&gt;
      &lt;/OKVED2List&gt;
      &lt;CharterCapital&gt;*&lt;/CharterCapital&gt;
      &lt;LeaderList&gt;
        &lt;Leader ActualDate="2019-08-22" FIO="Зорин Максим Александрович" Position="генеральный директор" INN="504225766444"/&gt;
      &lt;/LeaderList&gt;
      &lt;LegalAddresses&gt;
        &lt;Address PostCode="119034" Address="г. Москва, ул. Тимура Фрунзе, д.  11 стр.  44" Region="г. Москва" City="г. Москва" StreetName="ул. Тимура Фрунзе" BuildingType="д." BuildingNumber="11" HousingType="стр." Housing="44" BusinessCenterName="БЦ Палладиум" Longitude="37,589718" Latitude="55,733939" FiasGUID="2577d2c9-1157-4a73-b888-9da4ebf83719" IsHouseFiasGUID="true" FiasCode="770000000000000424500000000" FiasRegion="77" FiasArea="000" FiasCity="000" FiasPlace="000" FiasPlan="0000" FiasStreet="4245" ActualDate="2020-05-15"/&gt;
      &lt;/LegalAddresses&gt;
      &lt;PhoneList&gt;
        &lt;Phone Code="495" Number="7397000" VerificationDate="2017-05-19"/&gt;
      &lt;/PhoneList&gt;
      &lt;IndexOfDueDiligence Index="*" IndexDesc="*"/&gt;
      &lt;AccessibleFinData&gt;
        &lt;Period IDPeriod="555" Name="2019" EndDate="2019-12-31"/&gt;
        &lt;Period IDPeriod="551" Name="2018" EndDate="2018-12-31"/&gt;
        &lt;Period IDPeriod="547" Name="2017" EndDate="2017-12-31"/&gt;
        &lt;Period IDPeriod="543" Name="2016" EndDate="2016-12-31"/&gt;
      &lt;/AccessibleFinData&gt;
      &lt;CompanyWithSameInfo&gt;
        &lt;TelephoneCount PhoneCode="495" PhoneNumber="7397000"&gt;23&lt;/TelephoneCount&gt;
        &lt;PhoneList&gt;
          &lt;PhoneCount Code="495" Number="7397000"&gt;23&lt;/PhoneCount&gt;
        &lt;/PhoneList&gt;
        &lt;AddressCount&gt;5&lt;/AddressCount&gt;
        &lt;AddressWithoutRoomCount&gt;26&lt;/AddressWithoutRoomCount&gt;
        &lt;AddressNotAffiliatedCount&gt;5&lt;/AddressNotAffiliatedCount&gt;
        &lt;AddressFTSCount&gt;1&lt;/AddressFTSCount&gt;
        &lt;ManagerCountInCountry&gt;1&lt;/ManagerCountInCountry&gt;
        &lt;ManagerCountInRegion&gt;1&lt;/ManagerCountInRegion&gt;
        &lt;ManagerInnCount&gt;1&lt;/ManagerInnCount&gt;
      &lt;/CompanyWithSameInfo&gt;
      &lt;CompanyLiquidatedWithSameInfo&gt;
        &lt;AddressCount&gt;11&lt;/AddressCount&gt;
        &lt;AddressWithoutRoomCount&gt;14&lt;/AddressWithoutRoomCount&gt;
      &lt;/CompanyLiquidatedWithSameInfo&gt;
    &lt;/Report&gt;
    &lt;Report ActualDate="2021-01-21"&gt;
      &lt;ShortNameRus&gt;Вторая компания, которую не должны парсить&lt;/ShortNameRus&gt;
    &lt;/Report&gt;
  &lt;/Data&gt;
  &lt;ResultInfo ResultType="True" DateTime="2020-11-05T14:31:19" ExecutionTime="31"/&gt;
&lt;/Response&gt;
            </xmlData>
        </GetCompanyShortReportResponse>
    </soap:Body>
</soap:Envelope>"""  # noqa: E501


@pytest.fixture
def company_fail_response():
    return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope
    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <soap:Body>
        <GetCompanyShortReportResponse xmlns="http://interfax.ru/ifax">
            <GetCompanyShortReportResult>Authentication error</GetCompanyShortReportResult>
            <xmlData>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Response&gt;
  &lt;ResultInfo ResultType="Authentication error" DateTime="2020-11-18T13:31:30" ExecutionTime="15" /&gt;
&lt;/Response&gt;
            </xmlData>
        </GetCompanyShortReportResponse>
    </soap:Body>
</soap:Envelope>"""


@pytest.fixture
def entrepreneur_spark_data(ip_inn):
    return SparkData(
        spark_id='46126788',
        registration_date=date(2019, 5, 22),
        organization_data=SparkOrganizationData(
            organization=OrganizationData(
                type=MerchantType.IP,
                full_name='Иванов Иван Иванович',
                inn=ip_inn,
                ogrn='319237500178101',
            ),
            actual_date=None,
        ),
        okved_list=[
            OkvedItem(
                main=True,
                code='49.41',
                name='Деятельность автомобильного грузового транспорта',
            ),
        ],
        leaders=[
            LeaderData(
                name='Иванов Иван Иванович',
            ),
        ],
        active=True,
    )


@pytest.fixture
def company_spark_data(ooo_inn):
    return SparkData(
        spark_id='10605967',
        registration_date=date(2016, 5, 26),
        organization_data=SparkOrganizationData(
            OrganizationData(
                type=MerchantType.OOO,
                name='ООО "ЯНДЕКС.ОФД"',
                english_name='OOO YANDEKS.OFD',
                full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.ОФД"',
                inn=ooo_inn,
                kpp='770401001',
                ogrn='1167746503231',
            ),
            actual_date=date(2020, 11, 5),
        ),
        okved_list=[
            OkvedItem(
                main=True,
                code='63.11',
                name='Деятельность по обработке данных, предоставление услуг по размещению информации и связанная с этим деятельность',  # noqa: E501
            ),
        ],
        leaders=[
            LeaderData(
                name='Зорин Максим Александрович',
                position='генеральный директор',
                inn='504225766444',
                actual_date=date(2019, 8, 22),
            ),
        ],
        addresses=[
            SparkAddressData(
                address=AddressData(
                    type='legal',
                    city='Москва',
                    country='RUS',
                    home='11/44',
                    street='Тимура Фрунзе',
                    zip='119034',
                ),
                actual_date=date(2020, 5, 15),
            ),
        ],
        phones=[
            PhoneData(
                code='495',
                number='7397000',
                verification_date=date(2017, 5, 19),
            ),
        ],
        active=True
    )
