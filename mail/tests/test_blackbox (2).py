# coding: utf-8
from contextlib import contextmanager
from io import BytesIO as SIO

from ora2pg import blackbox

import pytest
import mock


class TestBlackBoxRequest(object):
    URLOPEN_PATH = 'ora2pg.blackbox.urlopen'

    @contextmanager
    def urlopen_mock(self, response_str):
        with mock.patch(self.URLOPEN_PATH, autospec=True) as uo_mock:
            uo_mock.return_value = SIO(response_str)
            yield uo_mock

    def test_get_user_info(self):
        with self.urlopen_mock(b'''
            <doc>
                <uid hosted="0">1000</uid>
                <login>wizard</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>6000</karma_status>
                <dbfield id="subscription.suid.2">3000</dbfield>
                <address-list>
                    <address validated="1" default="1" rpop="0" silent="0" unsafe="0" native="1" born-date="2010-07-05 11:01:00">wizard@yandex-team.ru</address>
                </address-list>
            </doc>
        '''):
            bbr = blackbox.BlackBoxRequest('test://blackbox')
            info = bbr.by_suid(3000)
            assert info == blackbox.BBInfo(
                uid=1000,
                suid=3000,
                login='wizard',
                default_email='wizard@yandex-team.ru',
            )

    def test_raise_NonExistentUserError(self):
        with self.urlopen_mock(b'''
            <doc>
                <uid hosted="0"/>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
            </doc>
        '''):
            bbr = blackbox.BlackBoxRequest('test://blackbox')
            with pytest.raises(blackbox.NonExistentUserError):
                bbr.by_login('deleted.user')

    def test_raise_NotAMailUser(self):
        with self.urlopen_mock(b'''
            <doc>
                <uid hosted="0">100500</uid>
                <login>user.without.mail.sid</login>
                <have_password>1</have_password>
                <have_hint>0</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="hosts.db_id.-"></dbfield>
                <dbfield id="subscription.suid.2"></dbfield>
            </doc>
        '''):
            bbr = blackbox.BlackBoxRequest('test://blackbox')
            with pytest.raises(blackbox.NotAMailUserError):
                bbr.by_login('user.without.mail.sid')

    def test_raise_BlackboxResponseError_on_error_in_response(self):
        with self.urlopen_mock(b'''
            <user id="3000151429">
              <exception id="10">DB_EXCEPTION</exception>
              <error>Fatal BlackBox error: dbpool exception in sezam dbfields fetch</error>
            </user>
        '''):
            bbr = blackbox.BlackBoxRequest('test://blackbox')
            with pytest.raises(blackbox.BlackboxResponseError):
                bbr.by_login('unhappy-user')

    def test_raise_BlackboxResponseError_on_bad_xml(self):
        with self.urlopen_mock(b'''
            doc:
                uid: 100500
                login: ...
        '''):
            bbr = blackbox.BlackBoxRequest('test://blackbox')
            with pytest.raises(blackbox.BlackboxResponseError):
                bbr.by_uid(100500)

    def test_raise_BlackboxCallError_on_IOError(self):
        with mock.patch(
            self.URLOPEN_PATH,
            autospec=True
        ) as urlopen_mock:
            urlopen_mock.side_effect = IOError
            bbr = blackbox.BlackBoxRequest('test://blackbox')
            with pytest.raises(blackbox.BlackboxCallError):
                bbr.by_login('unhappy.user')


@contextmanager
def patch_http_request(response):
    with mock.patch('ora2pg.blackbox.http.request', autospec=True) as mocked:
        mocked.return_value.__enter__.return_value = SIO(response)
        yield mocked
