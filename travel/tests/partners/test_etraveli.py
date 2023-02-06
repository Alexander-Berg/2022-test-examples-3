import hashlib
from urllib import unquote_plus
from urlparse import parse_qsl, urlparse

import pytest

from travel.avia.ticket_daemon.tests.partners.helper import get_query
from travel.avia.ticket_daemon.ticket_daemon.partners.etraveli import ETravelIQueryBuilder, process_order_data


def get_dict_params_from_url(url):
    return {
        key: unquote_plus(value)
        for key, value in parse_qsl(urlparse(url)[4])
    }


def urls_have_same_parameters(first, second):
    return get_dict_params_from_url(first) == get_dict_params_from_url(second)


def test_query_builder_round_trip():
    builder = ETravelIQueryBuilder(base_url='https://host/', partner='partner')
    query = get_query(passengers={'adults': 2, 'children': 1, 'infants': 1})
    actual = builder.build(query)
    assert query.id == '170115-030000-000.ticket.plane.c213_c54_2017-01-21_2017-01-24_economy_2_1_1_ru.ru'
    id_md5 = hashlib.md5(query.id).hexdigest()
    assert urls_have_same_parameters(
        actual,
        'https://host/?brand=partner&country=RU&format=json&travellers=a1,a2,c1:10,i1:2&searchId={}&bounds=MOWSVX2017-01-21%2CSVXMOW2017-01-24&cabin=Y'.format(id_md5),
    )


def test_query_builder_one_way():
    builder = ETravelIQueryBuilder(base_url='https://host/', partner='partner')
    query = get_query(passengers={'adults': 2, 'children': 1, 'infants': 1}, date_backward=None)
    actual = builder.build(query)
    assert query.id == '170115-030000-000.ticket.plane.c213_c54_2017-01-21_None_economy_2_1_1_ru.ru'
    id_md5 = hashlib.md5(query.id).hexdigest()
    assert urls_have_same_parameters(
        actual,
        'https://host/?brand=partner&country=RU&format=json&travellers=a1,a2,c1:10,i1:2&searchId={}&bounds=MOWSVX2017-01-21&cabin=Y'.format(id_md5),
    )


class EtraveliBookTester:
    def __init__(self):
        self.qid = get_query(service='ticket').id

    def process_data(self, order_data):
        order_data = order_data.copy()
        order_data['qid'] = self.qid
        return process_order_data(order_data)

    def get_expected_url(self, url):
        return '{url}&ext-src=desktop'.format(url=url)


@pytest.mark.dbuser
def test_book__with_m_url():
    order_data = {
        'url': 'https://url.kz/?key1=value1',
        'm_url': 'https://m.url.kz/?key2=value2',
    }

    tester = EtraveliBookTester()
    result = tester.process_data(order_data)

    expect_url = tester.get_expected_url(order_data['url'])
    assert urls_have_same_parameters(expect_url, result['url'])

    expect_url = tester.get_expected_url(order_data['m_url'])
    assert urls_have_same_parameters(expect_url, result['m_url'])


@pytest.mark.dbuser
def test_book__without_m_url():
    order_data = {
        'url': 'https://url.kz/?key1=value1',
    }
    tester = EtraveliBookTester()
    result = tester.process_data(order_data)

    expect_url = tester.get_expected_url(order_data['url'])
    assert urls_have_same_parameters(expect_url, result)
