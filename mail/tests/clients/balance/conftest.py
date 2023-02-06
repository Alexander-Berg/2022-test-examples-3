from datetime import datetime

import pytest
import pytz
from aiohttp import TCPConnector

from sendr_interactions.clients.balance import AbstractBalanceClient
from sendr_interactions.clients.balance.entities import Client, Person


class BalanceClient(AbstractBalanceClient):
    REQUEST_RETRY_TIMEOUTS = ()
    DEBUG = False
    BASE_URL = 'https://balance.test/path'
    CONNECTOR = TCPConnector()


@pytest.fixture
async def balance_client(create_interaction_client):

    client = create_interaction_client(BalanceClient)
    yield client
    await client.close()


@pytest.fixture
def client_id():
    return '1'


@pytest.fixture
def client_data(client_id):
    return {
        'client_id': client_id,
        'name': 'test-cilent-name',
        'email': 'test-cilent-email',
        'phone': 'test-cilent-phone',
    }


@pytest.fixture
def client_entity(client_data):
    return Client(**client_data)


@pytest.fixture
def person_id():
    return '2'


@pytest.fixture
def person_data(client_id, person_id):
    return {
        'client_id': client_id,
        'person_id': person_id,

        'account': 'test-account',
        'bik': 'test-bik',
        'fname': 'test-fname',
        'lname': 'test-lname',
        'mname': 'test-mname',
        'email': 'test-email',
        'phone': 'test-phone',

        'name': 'test-name',
        'longname': 'test-longname',
        'inn': 'test-inn',
        'kpp': 'test-kpp',
        'ogrn': 'test-ogrn',

        'legal_address_city': 'test-legal_address_city',
        'legal_address_home': 'test-legal_address_home',
        'legal_address_postcode': 'test-legal_address_postcode',
        'legal_address_street': 'test-legal_address_street',

        'address_city': 'test-address_city',
        'address_home': 'test-address_home',
        'address_postcode': 'test-address_postcode',
        'address_street': 'test-address_street',
    }


@pytest.fixture
def person_date():
    return datetime(2020, 11, 18, 19, 31, 59).replace(tzinfo=pytz.timezone('Europe/Moscow'))


@pytest.fixture
def person_date_str(person_date):
    return person_date.strftime('%Y-%m-%d %H:%M:%S')


@pytest.fixture
def person_entity(person_data):
    return Person(**person_data)


@pytest.fixture
def response_code():
    return 0


@pytest.fixture
def create_client_response(client_id):
    return f'''
        <methodResponse>
        <params>
        <param>
        <value><int>0</int></value>
        </param>
        <param>
        <value><string>SUCCESS</string></value>
        </param>
        <param>
        <value><int>{client_id}</int></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def find_client_response(response_code, client_entity):
    client = '' if client_entity.client_id is None else f'''
        <value><struct>
        <member>
            <name>CLIENT_ID</name>
            <value><int>{client_entity.client_id}</int></value>
        </member>
        <member>
            <name>NAME</name>
            <value><string>{client_entity.name}</string></value>
        </member>
        <member>
            <name>EMAIL</name>
            <value><string>{client_entity.email}</string></value>
        </member>
        <member>
            <name>PHONE</name>
            <value><string>{client_entity.phone}</string></value>
        </member>
        </struct></value>
    '''
    return f'''
        <methodResponse>
        <params>
        <param>
        <value><int>{response_code}</int></value>
        </param>
        <param>
        <value><int>0</int></value>
        </param>
        <param>
        <value><array><data>{client}</data></array></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def user_client_association_response(response_code):
    return f'''
        <methodResponse>
        <params>
        <param>
        <value><int>{response_code}</int></value>
        </param>
        <param>
        <value><string>Some message</string></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def create_person_response(person_id):
    return f'''
        <?xml version='1.0'?>
        <methodResponse>
        <params>
        <param>
        <value><int>{person_id}</int></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def get_client_persons_response(person_data, person_date_str):
    return f'''
        <?xml version='1.0'?>
        <methodResponse>
        <params>
        <param>
        <value><array><data>
        <value><struct>
        <member>
        <name>ACCOUNT</name>
        <value><string>{person_data['account']}</string></value>
        </member>
        <member>
        <name>FNAME</name>
        <value><string>{person_data['fname']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_CITY</name>
        <value><string>{person_data['legal_address_city']}</string></value>
        </member>
        <member>
        <name>ADDRESS_CITY</name>
        <value><string>{person_data['address_city']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_STREET</name>
        <value><string>{person_data['legal_address_street']}</string></value>
        </member>
        <member>
        <name>ADDRESS_STREET</name>
        <value><string>{person_data['address_street']}</string></value>
        </member>
        <member>
        <name>OGRN</name>
        <value><string>{person_data['ogrn']}</string></value>
        </member>
        <member>
        <name>POSTCODE</name>
        <value><string>123456</string></value>
        </member>
        <member>
        <name>CLIENT_ID</name>
        <value><string>{person_data['client_id']}</string></value>
        </member>
        <member>
        <name>DT</name>
        <value><string>{person_date_str}</string></value>
        </member>
        <member>
        <name>EMAIL</name>
        <value><string>{person_data['email']}</string></value>
        </member>
        <member>
        <name>SIGNER_PERSON_NAME</name>
        <value><string>Grishkin Maxim Somepatronymic</string></value>
        </member>
        <member>
        <name>BIK</name>
        <value><string>{person_data['bik']}</string></value>
        </member>
        <member>
        <name>MNAME</name>
        <value><string>{person_data['mname']}</string></value>
        </member>
        <member>
        <name>ID</name>
        <value><string>{person_data['person_id']}</string></value>
        </member>
        <member>
        <name>PHONE</name>
        <value><string>{person_data['phone']}</string></value>
        </member>
        <member>
        <name>LONGNAME</name>
        <value><string>{person_data['longname']}</string></value>
        </member>
        <member>
        <name>KPP</name>
        <value><string>{person_data['kpp']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_HOME</name>
        <value><string>{person_data['legal_address_home']}</string></value>
        </member>
        <member>
        <name>ADDRESS_HOME</name>
        <value><string>{person_data['address_home']}</string></value>
        </member>
        <member>
        <name>LNAME</name>
        <value><string>{person_data['lname']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_POSTCODE</name>
        <value><string>{person_data['legal_address_postcode']}</string></value>
        </member>
        <member>
        <name>ADDRESS_POSTCODE</name>
        <value><string>{person_data['address_postcode']}</string></value>
        </member>
        <member>
        <name>POSTADDRESS</name>
        <value><string>Lva Tolstogo</string></value>
        </member>
        <member>
        <name>NAME</name>
        <value><string>{person_data['name']}</string></value>
        </member>
        <member>
        <name>INN</name>
        <value><string>{person_data['inn']}</string></value>
        </member>
        <member>
        <name>LEGALADDRESS</name>
        <value><string>Lva Tolstogo 123</string></value>
        </member>
        <member>
        <name>TYPE</name>
        <value><string>ur</string></value>
        </member>
        </struct></value>
        </data></array></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def get_client_persons_empty_response():
    return '''
        <?xml version='1.0'?>
        <methodResponse>
        <params>
        <param>
        <value><array><data>
        <value><struct>
        </struct></value>
        </data></array></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def get_person_response(person_data, person_date_str):
    return f'''
        <?xml version='1.0'?>
        <methodResponse>
        <params>
        <param>
        <value><array><data>
        <value><struct>
        <member>
        <name>ACCOUNT</name>
        <value><string>{person_data['account']}</string></value>
        </member>
        <member>
        <name>FNAME</name>
        <value><string>{person_data['fname']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_CITY</name>
        <value><string>{person_data['legal_address_city']}</string></value>
        </member>
        <member>
        <name>ADDRESS_CITY</name>
        <value><string>{person_data['address_city']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_STREET</name>
        <value><string>{person_data['legal_address_street']}</string></value>
        </member>
        <member>
        <name>ADDRESS_STREET</name>
        <value><string>{person_data['address_street']}</string></value>
        </member>
        <member>
        <name>OGRN</name>
        <value><string>{person_data['ogrn']}</string></value>
        </member>
        <member>
        <name>POSTCODE</name>
        <value><string>123456</string></value>
        </member>
        <member>
        <name>CLIENT_ID</name>
        <value><string>{person_data['client_id']}</string></value>
        </member>
        <member>
        <name>DT</name>
        <value><string>{person_date_str}</string></value>
        </member>
        <member>
        <name>EMAIL</name>
        <value><string>{person_data['email']}</string></value>
        </member>
        <member>
        <name>SIGNER_PERSON_NAME</name>
        <value><string>Grishkin Maxim Somepatronymic</string></value>
        </member>
        <member>
        <name>BIK</name>
        <value><string>{person_data['bik']}</string></value>
        </member>
        <member>
        <name>MNAME</name>
        <value><string>{person_data['mname']}</string></value>
        </member>
        <member>
        <name>ID</name>
        <value><string>{person_data['person_id']}</string></value>
        </member>
        <member>
        <name>PHONE</name>
        <value><string>{person_data['phone']}</string></value>
        </member>
        <member>
        <name>LONGNAME</name>
        <value><string>{person_data['longname']}</string></value>
        </member>
        <member>
        <name>KPP</name>
        <value><string>{person_data['kpp']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_HOME</name>
        <value><string>{person_data['legal_address_home']}</string></value>
        </member>
        <member>
        <name>ADDRESS_HOME</name>
        <value><string>{person_data['address_home']}</string></value>
        </member>
        <member>
        <name>LNAME</name>
        <value><string>{person_data['lname']}</string></value>
        </member>
        <member>
        <name>LEGAL_ADDRESS_POSTCODE</name>
        <value><string>{person_data['legal_address_postcode']}</string></value>
        </member>
        <member>
        <name>ADDRESS_POSTCODE</name>
        <value><string>{person_data['address_postcode']}</string></value>
        </member>
        <member>
        <name>POSTADDRESS</name>
        <value><string>Lva Tolstogo</string></value>
        </member>
        <member>
        <name>NAME</name>
        <value><string>{person_data['name']}</string></value>
        </member>
        <member>
        <name>INN</name>
        <value><string>{person_data['inn']}</string></value>
        </member>
        <member>
        <name>LEGALADDRESS</name>
        <value><string>Lva Tolstogo 123</string></value>
        </member>
        <member>
        <name>TYPE</name>
        <value><string>ur</string></value>
        </member>
        </struct></value>
        </data></array></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def create_offer_response(contract_id, external_id):
    return f'''
        <?xml version='1.0'?>
        <methodResponse>
        <params>
        <param>
        <value><struct>
        <member>
        <name>EXTERNAL_ID</name>
        <value><string>{external_id}</string></value>
        </member>
        <member>
        <name>ID</name>
        <value><int>{contract_id}</int></value>
        </member>
        </struct></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def create_collateral_response(contract_id):
    return f'''
        <?xml version='1.0'?>
        <methodResponse>
        <params>
        <param>
        <value><struct>
        <member>
        <name>CONTRACT_EXTERNAL_ID</name>
        <value><string>12345/67</string></value>
        </member>
        <member>
        <name>COLLATERAL_NUM</name>
        <value><string>01</string></value>
        </member>
        <member>
        <name>CONTRACT_ID</name>
        <value><int>{contract_id}</int></value>
        </member>
        </struct></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()


@pytest.fixture
def get_client_contracts_response(person_id, contract_id, external_id):
    return f'''
        <?xml version='1.0'?>
        <methodResponse>
        <params>
        <param>
        <value><array><data>
        <value><struct>
        <member>
        <name>CURRENCY</name>
        <value><string>RUR</string></value>
        </member>
        <member>
        <name>IS_SUSPENDED</name>
        <value><int>0</int></value>
        </member>
        <member>
        <name>MANAGER_CODE</name>
        <value><int>12345</int></value>
        </member>
        <member>
        <name>IS_ACTIVE</name>
        <value><int>1</int></value>
        </member>
        <member>
        <name>IS_SIGNED</name>
        <value><int>1</int></value>
        </member>
        <member>
        <name>CONTRACT_TYPE</name>
        <value><int>9</int></value>
        </member>
        <member>
        <name>PERSON_ID</name>
        <value><int>{person_id}</int></value>
        </member>
        <member>
        <name>IS_FAXED</name>
        <value><int>0</int></value>
        </member>
        <member>
        <name>SERVICES</name>
        <value><array><data>
        <value><int>625</int></value>
        </data></array></value>
        </member>
        <member>
        <name>PAYMENT_TYPE</name>
        <value><int>3</int></value>
        </member>
        <member>
        <name>PARTNER_COMMISSION_PCT2</name>
        <value><string>2.15</string></value>
        </member>
        <member>
        <name>IS_CANCELLED</name>
        <value><int>0</int></value>
        </member>
        <member>
        <name>IS_DEACTIVATED</name>
        <value><int>0</int></value>
        </member>
        <member>
        <name>DT</name>
        <value><dateTime.iso8601>20190402T00:00:00</dateTime.iso8601></value>
        </member>
        <member>
        <name>EXTERNAL_ID</name>
        <value><string>{external_id}</string></value>
        </member>
        <member>
        <name>ID</name>
        <value><int>{contract_id}</int></value>
        </member>
        </struct></value>
        </data></array></value>
        </param>
        </params>
        </methodResponse>
    '''.strip()
