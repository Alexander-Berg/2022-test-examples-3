# -*- coding: utf-8 -*-

import hamcrest
from matchers import has_keys
import mock
import urlparse

from mpfs.core.services.mail_service import MailTVM


def test_mimes_request_standard():
    with open('fixtures/json/wmi/mimes_standard.json') as f:
        response = f.read()

    with mock.patch(
        'mpfs.core.services.mail_service.MailTVM.open_url', return_value=response
    ):
        ms = MailTVM()
        mid_1 = '161848111608628001'
        mid_2 = '161848111608628003'
        result = ms.mimes(uid='0123456789', mids=[mid_1, mid_2])

        hamcrest.assert_that(result, has_keys([mid_1, mid_2]))

        hamcrest.assert_that(result[mid_1], has_keys(['1', '1.1', '1.2']))
        hamcrest.assert_that(result[mid_2], has_keys(['1', '1.1', '1.2']))


def test_mimes_request_winmail():
    with open('fixtures/json/wmi/mimes_winmail.dat.json') as f:
        response = f.read()

    with mock.patch(
        'mpfs.core.services.mail_service.MailTVM.open_url', return_value=response
    ):
        ms = MailTVM()
        mid = '162129586585337910'  # содержится в фикстуре
        result = ms.mimes(uid='0123456789', mids=[mid])

        hamcrest.assert_that(result, has_keys([mid]))
        hamcrest.assert_that(result[mid], has_keys(['1', '1.1', '1.1.1', '1.1.2', '1.1.3']))


def test_get_mime_part_name_if_part_exists():
    with open('fixtures/json/wmi/mimes_standard.json') as f:
        response = f.read()

    with mock.patch(
        'mpfs.core.services.mail_service.MailTVM.open_url', return_value=response
    ):
        ms = MailTVM()
        mid_1 = '161848111608628001'  # содержится в фикстуре
        result = ms.get_mime_part_name(uid='0123456789', mid=mid_1, hid='1.2')
        assert result == 'Ticket.pkpass'


def test_get_mime_part_name_for_winmail_other():
    with open('fixtures/json/wmi/mimes_winmail.dat.json') as f:
        response = f.read()

    with mock.patch(
        'mpfs.core.services.mail_service.MailTVM.open_url', return_value=response
    ):
        ms = MailTVM()
        mid_1 = '162129586585337910'  # содержится в фикстуре
        result = ms.get_mime_part_name(uid='0123456789', mid=mid_1, hid='1.1.2')
        assert result == 'KAYAPINAR BELEDYES.docx'


def test_get_mime_part_name_if_part_not_exists():
    with open('fixtures/json/wmi/mimes_standard.json') as f:
        response = f.read()

    with mock.patch(
        'mpfs.core.services.mail_service.MailTVM.open_url', return_value=response
    ):
        ms = MailTVM()
        mid = '161848111608628001'  # содержится в фикстуре
        result = ms.get_mime_part_name(uid='0123456789', mid=mid, hid='1.100500')
        assert result is None


def test_get_mime_part_name_if_mail_not_exists():
    with mock.patch(
        'mpfs.core.services.mail_service.MailTVM.open_url', return_value='{"mimes":{}}'
    ) as mocked_open_url:
        ms = MailTVM()
        mid = 'does_not_exist'  # не содержится в фикстуре
        uid = '0123456789'
        result = ms.get_mime_part_name(uid=uid, mid=mid, hid='1.100500')
        assert result is None

        # проверяем что корректно сформировали URL для запроса
        args, kwargs = mocked_open_url.call_args
        (url,) = args
        parsed_url = urlparse.urlparse(url)
        parsed_query = urlparse.parse_qs(parsed_url.query)
        assert parsed_query['uid'] == [uid]
        assert parsed_query['mid'] == [mid]
