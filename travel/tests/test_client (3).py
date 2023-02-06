import os

import pytest
from betamax import Betamax
from betamax_serializers.pretty_json import PrettyJSONSerializer
from requests import Session
from yatest import common

from travel.rasp.bus.spark_api import models
from travel.rasp.bus.spark_api.client import ENVIRONMENT_TO_HOST, SparkAuthError, SparkClient, SparkConfig
from travel.library.python.rasp_vault.api import get_secret

LOGIN = 'yandex'
PASSWORD = 'so_secret_password'
WRONG_PASSWORD = 'WRONG_PASSWORD'
Betamax.register_serializer(PrettyJSONSerializer)
CASSETE_PATH = 'travel/rasp/bus/spark_api/tests/cassettes'
with Betamax.configure() as config:
    if common.get_param('BETAMAX_MODE') == 'rewrite':
        LOGIN = get_secret('bus-common-production.spark-login')
        PASSWORD = get_secret('bus-common-production.spark-password')
        config.cassette_library_dir = os.path.join(os.environ['ARCADIA_PATH'], CASSETE_PATH)
        config.default_cassette_options['record_mode'] = 'all'
    else:
        config.cassette_library_dir = common.source_path(CASSETE_PATH)
        config.default_cassette_options['record_mode'] = 'none'

    config.default_cassette_options['match_requests_on'].extend([
        'body',
    ])
    config.define_cassette_placeholder(
        '<SPARK_API_PASSWORD>',
        PASSWORD,
    )
    config.define_cassette_placeholder(
        '<SPARK_API_WRONG_PASSWORD>',
        WRONG_PASSWORD,
    )
    config.define_cassette_placeholder(
        '<SPARK_API_LOGIN>',
        LOGIN,
    )


class TestClient:
    def setup_method(self, method):
        self.session = Session()
        del self.session.headers['Accept-Encoding']
        self.spark_config = SparkConfig(
            host=ENVIRONMENT_TO_HOST['production'],
            login=LOGIN,
            password=PASSWORD,
        )

    def test_ok_login(self):
        with Betamax(self.session).use_cassette('ok_login', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session):
                pass

    def test_broken_login(self):
        spark_config = SparkConfig(
            host=ENVIRONMENT_TO_HOST['production'],
            login=LOGIN,
            password=WRONG_PASSWORD,
        )
        with pytest.raises(SparkAuthError):
            with Betamax(self.session).use_cassette('broken_auth', serialize_with='prettyjson'):
                with SparkClient.create(spark_config, self.session):
                    pass

    def test_list_regions(self):
        with Betamax(self.session).use_cassette('list_regions', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                regions = client.list_regions()
                assert len(regions) == 83
                msk = next(r for r in regions if r.code == '45')
                assert msk.name == 'Город Москва столица Российской Федерации город федерального значения'

    def test_find_companies_by_name_in_all_regions(self):
        with Betamax(self.session).use_cassette('find_companies_by_name_in_all_regions', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                companies = client.find_companies_by_name('ЯНДЕКС.АВТОБУСЫ')
                assert len(companies) == 1
                bus_company = companies[0]
                assert bus_company == models.Company(
                    id=11447235,
                    inn='7704402904',
                    ogrn='1177746347591',
                    okpo='14037828',
                    full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.АВТОБУСЫ"',
                    address='г. Москва, ул. Тимура Фрунзе, д. 11 корп. 2 пом. 83125',
                    industry='Деятельность по созданию и использованию баз данных и информационных ресурсов',
                    okopf_name='Общества с ограниченной ответственностью',
                    okopf_code='12300',
                    manager='Снигирёв Андрей Михайлович',
                )

    def test_can_not_find_companies_by_name_in_all_regions(self):
        with Betamax(self.session).use_cassette('can_not_find_companies_by_name_in_all_regions', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                companies = client.find_companies_by_name('МАНГИН И КОМПАНИЯ')
                assert len(companies) == 0

    def test_find_companies_by_name_in_msk(self):
        with Betamax(self.session).use_cassette('find_companies_by_name_in_msk', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                companies = client.find_companies_by_name('ЯНДЕКС.АВТОБУСЫ', ['45'])
                assert len(companies) == 1
                bus_company = companies[0]
                assert bus_company == models.Company(
                    id=11447235,
                    inn='7704402904',
                    ogrn='1177746347591',
                    okpo='14037828',
                    full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.АВТОБУСЫ"',
                    address='г. Москва, ул. Тимура Фрунзе, д. 11 корп. 2 пом. 83125',
                    industry='Деятельность по созданию и использованию баз данных и информационных ресурсов',
                    okopf_name='Общества с ограниченной ответственностью',
                    okopf_code='12300',
                    manager='Снигирёв Андрей Михайлович',
                )

    def test_can_not_find_companies_by_name_in_ekb(self):
        with Betamax(self.session).use_cassette('can_not_find_companies_by_name_in_ekb', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                companies = client.find_companies_by_name('ЯНДЕКС.АВТОБУСЫ', [66])
                assert len(companies) == 0

    def test_find_companies_by_name_and_okopf(self):
        with Betamax(self.session).use_cassette('find_companies_by_name_and_okopf', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                companies = client.find_companies_by_name('Городской Автобус', None, '15200')
                assert len(companies) == 2

    def test_find_entrepreneur_by_name_in_all_regions(self):
        with Betamax(self.session).use_cassette('find_entrepreneur_by_name_in_all_regions', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                entrepreneurs = client.find_entrepreneurs_by_name('Гареев Шамиль Ильдарович')
                assert len(entrepreneurs) == 1
                someone = entrepreneurs[0]
                assert someone == models.Entrepreneur(
                    id=36020898,
                    inn='027506766758',
                    ogrnip='309028028900232',
                    okpo='0167820036',
                    full_name='Гареев Шамиль Ильдарович',
                )

    def test_find_entrepreneur_by_name_in_fixed_region(self):
        with Betamax(self.session).use_cassette('find_entrepreneur_by_name_in_fixed_region', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                entrepreneurs = client.find_entrepreneurs_by_name('Гареев Шамиль Ильдарович', ['80'])
                assert len(entrepreneurs) == 1
                someone = entrepreneurs[0]
                assert someone == models.Entrepreneur(
                    id=36020898,
                    inn='027506766758',
                    ogrnip='309028028900232',
                    okpo='0167820036',
                    full_name='Гареев Шамиль Ильдарович',
                )

    def test_find_entrepreneur_by_short_name(self):
        with Betamax(self.session).use_cassette('find_entrepreneur_by_short_name', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                entrepreneurs = {e.inn for e in client.find_entrepreneurs_by_name('Анчутин С.С.', None)}
                assert entrepreneurs == {'381403701257', '668501330908'}

    def test_can_not_find_entrepreneur_by_name_in_ekb(self):
        with Betamax(self.session).use_cassette('can_not_find_entrepreneur_by_name_in_ekb', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                entrepreneurs = client.find_entrepreneurs_by_name('Гареев Шамиль Ильдарович', ['45'])
                assert len(entrepreneurs) == 0

    def test_can_not_find_entrepreneur_by_name_in_all_regions(self):
        with Betamax(self.session).use_cassette('can_not_find_entrepreneur_by_name_in_all_regions', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                entrepreneurs = client.find_entrepreneurs_by_name('Мангин Александр Андреевич')
                assert len(entrepreneurs) == 0

    def test_get_entrepreneur_report_by_inn(self):
        with Betamax(self.session).use_cassette('get_entrepreneur_report_by_inn', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.get_entrepreneur_report(
                    inn='381403701257',
                    ogrnip=None
                ) == models.EntrepreneurReport(
                    okveds=(
                        models.OkvedItem(
                            code='45.32',
                            name='Торговля розничная автомобильными деталями, узлами и принадлежностями',
                            is_main=True
                        ),
                    ),
                    region=models.Region(code='25', name='Иркутская область')
                )

    def test_get_entrepreneur_report_by_ogrnip(self):
        with Betamax(self.session).use_cassette('get_entrepreneur_report_by_ogrnip', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.get_entrepreneur_report(
                    inn=None,
                    ogrnip='316385000112167'
                ) == models.EntrepreneurReport(
                    okveds=(
                        models.OkvedItem(
                            code='45.32',
                            name='Торговля розничная автомобильными деталями, узлами и принадлежностями',
                            is_main=True
                        ),
                    ),
                    region=models.Region(code='25', name='Иркутская область')
                )

    def test_get_entrepreneur_report_by_ogrnip_and_inn(self):
        with Betamax(self.session).use_cassette('get_entrepreneur_report_by_ogrnip_and_inn', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.get_entrepreneur_report(
                    inn='381403701257',
                    ogrnip='316385000112167'
                ) == models.EntrepreneurReport(
                    okveds=(
                        models.OkvedItem(
                            code='45.32',
                            name='Торговля розничная автомобильными деталями, узлами и принадлежностями',
                            is_main=True
                        ),
                    ),
                    region=models.Region(code='25', name='Иркутская область')
                )

    def test_get_company_report_by_inn(self):
        with Betamax(self.session).use_cassette('get_company_report_by_inn', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.get_company_report(
                    spark_id=None,
                    inn='7704402904',
                    ogrn=None
                ) == models.CompanyReport(
                    legal_address='г. Москва, ул. Тимура Фрунзе, д. 11 корп. 2 пом. 83125',
                    short_name='ООО "ЯНДЕКС.АВТОБУСЫ"',
                    okveds=(
                        models.OkvedItem(
                            code='63.11.1',
                            name='Деятельность по созданию и использованию баз данных и информационных ресурсов',
                            is_main=True
                        ),
                        models.OkvedItem(
                            code='63.11.9',
                            name='Деятельность по предоставлению услуг по размещению информации прочая',
                            is_main=False
                        ),
                        models.OkvedItem(
                            code='63.99',
                            name='Деятельность информационных служб прочая, не включенная в другие группировки',
                            is_main=False
                        ),
                    ),
                    region=models.Region(code='45', name='Москва')
                )

    def test_get_company_report_by_ogrnip(self):
        with Betamax(self.session).use_cassette('get_company_report_by_ogrnip', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.get_company_report(
                    spark_id=None,
                    inn=None,
                    ogrn='1177746347591'
                ) == models.CompanyReport(
                    legal_address='г. Москва, ул. Тимура Фрунзе, д. 11 корп. 2 пом. 83125',
                    short_name='ООО "ЯНДЕКС.АВТОБУСЫ"',
                    okveds=(
                        models.OkvedItem(
                            code='63.11.1',
                            name='Деятельность по созданию и использованию баз данных и информационных ресурсов',
                            is_main=True
                        ),
                        models.OkvedItem(
                            code='63.11.9',
                            name='Деятельность по предоставлению услуг по размещению информации прочая',
                            is_main=False
                        ),
                        models.OkvedItem(
                            code='63.99',
                            name='Деятельность информационных служб прочая, не включенная в другие группировки',
                            is_main=False
                        ),
                    ),
                    region=models.Region(code='45', name='Москва')
                )

    def test_get_company_report_by_ogrnip_and_inn(self):
        with Betamax(self.session).use_cassette('get_company_report_by_ogrnip_and_inn', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.get_company_report(
                    spark_id=None,
                    inn='7704402904',
                    ogrn='1177746347591'
                ) == models.CompanyReport(
                    legal_address='г. Москва, ул. Тимура Фрунзе, д. 11 корп. 2 пом. 83125',
                    short_name='ООО "ЯНДЕКС.АВТОБУСЫ"',
                    okveds=(
                        models.OkvedItem(
                            code='63.11.1',
                            name='Деятельность по созданию и использованию баз данных и информационных ресурсов',
                            is_main=True
                        ),
                        models.OkvedItem(
                            code='63.11.9',
                            name='Деятельность по предоставлению услуг по размещению информации прочая',
                            is_main=False
                        ),
                        models.OkvedItem(
                            code='63.99',
                            name='Деятельность информационных служб прочая, не включенная в другие группировки',
                            is_main=False
                        ),
                    ),
                    region=models.Region(code='45', name='Москва')
                )

    def test_find_entrepreneur_by_code(self):
        with Betamax(self.session).use_cassette('find_entrepreneur_by_code', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.find_entrepreneur_by_code(
                    code='381403701257'
                ) == models.Entrepreneur(
                    full_name='Анчутин Степан Сергеевич',
                    id=43873742,
                    inn='381403701257',
                    ogrnip='316385000112167',
                    okpo='0104212411',
                )

    def test_find_company_by_code(self):
        with Betamax(self.session).use_cassette('find_company_by_code', serialize_with='prettyjson'):
            with SparkClient.create(self.spark_config, self.session) as client:
                assert client.find_company_by_code(
                    code='7704402904'
                ) == models.Company(
                    address='г. Москва, ул. Тимура Фрунзе, д. 11 корп. 2 пом. 83125',
                    full_name='ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.АВТОБУСЫ"',
                    id=11447235,
                    industry='Деятельность по созданию и использованию баз данных и информационных ресурсов',
                    inn='7704402904',
                    manager='Снигирёв Андрей Михайлович',
                    ogrn='1177746347591',
                    okopf_code='12300',
                    okopf_name='Общества с ограниченной ответственностью',
                    okpo='14037828',
                )
