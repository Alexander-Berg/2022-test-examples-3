import json
import xml.etree.ElementTree as ET
from mail.devpack.lib.components.fakebb import FakeBlackbox


XML = '<xml></xml>'
XML2 = '<xml2></xml2>'


def check_registered(response):
    assert response.status_code == 200
    response_dict = json.loads(response.text)
    assert response_dict['status'] == 'ok'

    return response_dict['uid'] if 'uid' in response_dict else None


def check_response_status_and_test(resp, text):
    assert resp.status_code == 200
    assert resp.text == text


def test_fbb(coordinator):
    fbb = coordinator.components[FakeBlackbox]
    response = fbb.register('test-fbb@yandex.ru')
    uid = check_registered(response)

    userinfo_response = fbb.userinfo('test-fbb@yandex.ru')
    assert userinfo_response.status_code == 200

    e = ET.fromstring(userinfo_response.text)
    assert e.find('uid').text == str(uid)
    assert e.find('login').text == 'test-fbb'


def test_should_return_xml_from_userinfo_if_specified(coordinator):
    fbb = coordinator.components[FakeBlackbox]

    response = fbb.register('test-userinfo_xml@yandex.ru', userinfo_response=XML)
    check_registered(response)

    userinfo_response = fbb.userinfo('test-userinfo_xml@yandex.ru')
    check_response_status_and_test(userinfo_response, XML)


def test_should_return_json_from_userinfo_if_specified(coordinator):
    fbb = coordinator.components[FakeBlackbox]
    JS = '{}'

    response = fbb.register('test-userinfo_json@yandex.ru', userinfo_response=JS)
    check_registered(response)

    userinfo_response = fbb.userinfo('test-userinfo_json@yandex.ru')
    check_response_status_and_test(userinfo_response, JS)


def test_should_return_xml_from_sessionid(coordinator):
    fbb = coordinator.components[FakeBlackbox]

    response = fbb.save_sessionid('test-sessionid_xml@yandex.ru', XML)
    check_registered(response)

    sessionid_response = fbb.sessionid('test-sessionid_xml@yandex.ru')
    check_response_status_and_test(sessionid_response, XML)
