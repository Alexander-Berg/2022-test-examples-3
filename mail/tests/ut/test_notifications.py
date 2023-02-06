from mail.shiva.stages.api.props.shard.notify_helper import _is_recovery_address, _parse_recovery_addresses, \
    _parse_blackbox, User, blackbox, Metrics, sendr, Templates, _quick_send, _is_valid_default_address, _is_active_bb_user
from mail.shiva.stages.api.settings.blackbox import BlackboxSettings
from mail.shiva.stages.api.settings.sendr import SendrSettings
from mail.shiva.stages.api.roles.shard_worker import TaskStats

import pytest
from unittest.mock import Mock
import asyncio

RECOVERY_ADDRESS = dict(default=True, rpop=False, native=False, unsafe=False, validated=True, silent=False, address='1')
SILENT_ADDRESS = dict(default=False, rpop=False, native=False, unsafe=False, validated=True, silent=True, address='1')

USER = User(uid='1', addresses=['a@ya.ru', 'b@ya.ru'], default_address='a@ya.ru', lang='ru')


def test_is_recovery_address():
    assert _is_recovery_address(RECOVERY_ADDRESS)
    assert not _is_recovery_address(SILENT_ADDRESS)


def test_parse_recovery_addresses():
    assert _parse_recovery_addresses([
        RECOVERY_ADDRESS,
        SILENT_ADDRESS,
    ]) == [RECOVERY_ADDRESS['address']]


def test_parse_blackbox():
    without_addresses = {
        'address-list': [],
        'uid': {
            'value': 'without_addresses'
        },
        'id': 'without_addresses'
    }

    without_uid = {
        'address-list': [SILENT_ADDRESS],
        'uid': {},
        'id': 'without_uid',
    }

    user_without_recovery = {
        'address-list': [
            SILENT_ADDRESS
        ],
        'uid': {
            'value': 'user_without_recovery'
        },
        'id': 'user_without_recovery',
        'dbfields': {
            'subscription.suid.2': '421',
            'userinfo.lang.uid': 'ru',
            'userinfo.firstname.uid': 'Vasya'
        },
        'attributes': {}
    }

    user_with_recovery = {
        'address-list': [
            RECOVERY_ADDRESS
        ],
        'uid': {
            'value': 'user_with_recovery'
        },
        'id': 'user_with_recovery',
        'dbfields': {
            'subscription.suid.2': '422',
            'userinfo.lang.uid': 'en',
            'userinfo.firstname.uid': 'Vasya'
        },
        'attributes': {}
    }

    user_without_sid2 = {
        'address-list': [
            RECOVERY_ADDRESS
        ],
        'uid': {
            'value': 'user_without_sid2'
        },
        'id': 'user_without_sid2',
        'dbfields': {
            'subscription.suid.2': '',
            'userinfo.lang.uid': 'en',
            'userinfo.firstname.uid': 'Gena'
        },
        'attributes': {
            '203': '1'
        }
    }

    user_with_empty_sid14 = {
        'address-list': [
            RECOVERY_ADDRESS
        ],
        'uid': {
            'value': 'user_with_empty_sid14'
        },
        'id': 'user_with_empty_sid14',
        'dbfields': {
            'subscription.suid.2': '123',
            'subscription.suid.14': '',
            'userinfo.lang.uid': 'en',
            'userinfo.firstname.uid': 'Gena'
        },
        'attributes': {
            '203': '1'
        }
    }

    user_with_sid14 = {
        'address-list': [
            RECOVERY_ADDRESS
        ],
        'uid': {
            'value': 'user_with_sid14'
        },
        'id': 'user_with_sid14',
        'dbfields': {
            'subscription.suid.2': '123',
            'subscription.suid.14': '1',
            'userinfo.lang.uid': 'en',
            'userinfo.firstname.uid': 'Gena'
        },
        'attributes': {
            '203': '1'
        }
    }

    user_without_firstname = {
        'address-list': [
            RECOVERY_ADDRESS
        ],
        'uid': {
            'value': 'user_without_firstname'
        },
        'id': 'user_without_firstname',
        'dbfields': {
            'subscription.suid.2': '423',
            'userinfo.lang.uid': 'en',
            'userinfo.firstname.uid': ''
        },
        'attributes': {
            '203': '1'
        }
    }

    frozen_user = {
        'address-list': [
            RECOVERY_ADDRESS
        ],
        'uid': {
            'value': 'user_without_sid2'
        },
        'id': 'user_without_sid2',
        'dbfields': {
            'subscription.suid.2': '424',
            'userinfo.lang.uid': 'en',
            'userinfo.firstname.uid': 'Gena'
        },
        'attributes': {
            '203': '2'
        }
    }

    stats = TaskStats(test_parse_blackbox.__name__)

    result = _parse_blackbox([
        without_addresses,
        without_uid,
        user_with_recovery,
        user_without_recovery,
        user_without_firstname,
        user_without_sid2,
        frozen_user,
        user_with_empty_sid14,
        user_with_sid14,
    ], Metrics(stats))

    assert len(result) == 5
    assert result == [
        User(uid='user_with_recovery', addresses=[RECOVERY_ADDRESS['address']], lang='en', default_address=RECOVERY_ADDRESS['address'], first_name='Vasya', is_direct=False),
        User(uid='user_without_recovery', addresses=[], lang='ru', default_address=None, first_name='Vasya', is_direct=False),
        User(uid='user_without_firstname', addresses=[RECOVERY_ADDRESS['address']], lang='en', default_address=RECOVERY_ADDRESS['address'], first_name=RECOVERY_ADDRESS['address'], is_direct=False),
        User(uid='user_with_empty_sid14', addresses=[RECOVERY_ADDRESS['address']], lang='en', default_address=RECOVERY_ADDRESS['address'], first_name='Gena', is_direct=False),
        User(uid='user_with_sid14', addresses=[RECOVERY_ADDRESS['address']], lang='en', default_address=RECOVERY_ADDRESS['address'], first_name='Gena', is_direct=True),
    ]
    assert (f'{test_parse_blackbox.__name__}_missing_email_ammm', 1) in stats.get()
    assert (f'{test_parse_blackbox.__name__}_missing_sid2_ammm', 1) in stats.get()
    assert (f'{test_parse_blackbox.__name__}_missing_uid_ammm', 1) in stats.get()


def test_templates():
    templates = Templates(default='def', ru='ru', en='en')
    assert templates['ru'] == 'ru'
    assert templates['en'] == 'en'
    assert templates['fr'] == 'def'
    assert templates[''] == 'def'
    assert templates[None] == 'def'


class Response(object):
    async def text(self):
        return self._text()

    @property
    def status(self):
        return self._status()

    def __init__(self):
        self._status = Mock()
        self._status.return_value = 200
        self._text = Mock()
        self._text.return_value = '{"users": []}'

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        return True


class Request(object):
    def __init__(self):
        self.response = Response()

    async def __aenter__(self):
        return self.response

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        return True


def client(exc: Exception = None):
    class C(object):
        def __init__(self, exc: Exception = None):
            self._req = Request()
            self._exc = exc
            self._call_count = 0

        def get(self, *args, **kwargs):
            self._call_count += 1
            if self._exc is not None:
                raise self._exc
            return self._req

        def post(self, *args, **kwargs):
            self._call_count += 1
            if self._exc is not None:
                raise self._exc
            return self._req

    return C(exc)


def mock_parser(user=USER):
    parser = Mock()
    parser.return_value = [user]
    return parser


@pytest.mark.asyncio
async def test_blackbox_should_make_requests():
    stats = TaskStats(test_blackbox_should_make_requests.__name__)
    assert await blackbox(cfg=BlackboxSettings(), uids=[1, 2, 3], client=client(),
                          metrics=Metrics(stats), parser=mock_parser(), bb_tvm_ticket='') == [USER]


@pytest.mark.asyncio
async def test_blackbox_should_retry_on_http_errors():
    cl = client()
    cl._req.response._status.side_effect = [500] * (BlackboxSettings().tries - 1) + [200]

    stats = TaskStats(test_blackbox_should_retry_on_http_errors.__name__)

    assert await blackbox(cfg=BlackboxSettings(), uids=[1, 2, 3], client=cl,
                          metrics=Metrics(stats), parser=mock_parser(), bb_tvm_ticket='') == [USER]
    assert (f'{test_blackbox_should_retry_on_http_errors.__name__}'
            f'_blackbox_error_ammm', 1) in stats.get()


@pytest.mark.asyncio
async def test_blackbox_should_return_empty_array_on_errors():
    cl = client()
    cl._req.response._status.return_value = 500

    stats = TaskStats(test_blackbox_should_return_empty_array_on_errors.__name__)

    assert await blackbox(cfg=BlackboxSettings(), uids=[1, 2, 3], client=cl,
                          metrics=Metrics(stats), parser=mock_parser(), bb_tvm_ticket='') == []
    assert (f'{test_blackbox_should_return_empty_array_on_errors.__name__}'
            f'_blackbox_error_ammm', 4) in stats.get()


@pytest.mark.asyncio
async def test_blackbox_should_retry_on_missing_users_in_response():
    cl = client()
    cl._req.response._text.side_effect = ['', '', '{"users": []}']

    stats = TaskStats(test_blackbox_should_retry_on_missing_users_in_response.__name__)

    assert await blackbox(cfg=BlackboxSettings(), uids=[1, 2, 3], client=cl,
                          metrics=Metrics(stats), parser=mock_parser(), bb_tvm_ticket='') == [USER]

    assert cl._req.response._text.call_count == 3


@pytest.mark.asyncio
async def test_blackbox_should_retry_in_case_of_timeout_exception():
    cl = client(asyncio.TimeoutError())

    stats = TaskStats(test_blackbox_should_retry_in_case_of_timeout_exception.__name__)

    assert await blackbox(cfg=BlackboxSettings(), uids=[1, 2, 3], client=cl,
                          metrics=Metrics(stats), parser=mock_parser(), bb_tvm_ticket='') == []

    assert cl._call_count == 3
    assert (f'{test_blackbox_should_retry_in_case_of_timeout_exception.__name__}'
            f'_blackbox_error_ammm', 4) in stats.get()


@pytest.mark.asyncio
async def test_sendr_should_return_success_users():
    cl = client()
    cl._req.response._text.return_value = '{"result":{"task_id":1}}'
    stats = TaskStats(test_sendr_should_return_success_users.__name__)
    assert await sendr(cfg=SendrSettings(), user=USER, client=cl, metrics=Metrics(stats), templates=Templates())
    assert (f'{test_sendr_should_return_success_users.__name__}'
            f'_successfully_sended_ammm', 1) in stats.get()


@pytest.mark.asyncio
async def test_sendr_should_retry_in_case_of_5xx():
    cl = client()
    cl._req.response._status.return_value = 500

    stats = TaskStats(test_sendr_should_retry_in_case_of_5xx.__name__)

    assert not await sendr(cfg=SendrSettings(), user=USER, client=cl, metrics=Metrics(stats), templates=Templates())
    assert cl._call_count == 3
    assert (f'{test_sendr_should_retry_in_case_of_5xx.__name__}'
            f'_sendr_error_ammm', 4) in stats.get()


@pytest.mark.asyncio
async def test_sendr_should_not_retry_in_case_of_4xx():
    cl = client()
    cl._req.response._status.return_value = 400

    stats = TaskStats(test_sendr_should_not_retry_in_case_of_4xx.__name__)

    assert not await sendr(cfg=SendrSettings(), user=USER, client=cl, metrics=Metrics(stats), templates=Templates())
    assert cl._call_count == 1
    assert (f'{test_sendr_should_not_retry_in_case_of_4xx.__name__}'
            f'_sendr_error_ammm', 1) in stats.get()


@pytest.mark.asyncio
async def test_sendr_should_retry_in_case_of_timeout_exception():
    cl = client(asyncio.TimeoutError())

    stats = TaskStats(test_sendr_should_retry_in_case_of_timeout_exception.__name__)

    assert not await sendr(cfg=SendrSettings(), user=USER, client=cl, metrics=Metrics(stats), templates=Templates())
    assert cl._call_count == 3
    assert (f'{test_sendr_should_retry_in_case_of_timeout_exception.__name__}'
            f'_sendr_exception_ammm', 3) in stats.get()
    assert (f'{test_sendr_should_retry_in_case_of_timeout_exception.__name__}'
            f'_sendr_error_ammm', 1) in stats.get()


@pytest.mark.asyncio
async def test_quick_send_should_return_false_on_bb_error():
    cl = client()
    cl._req.response._status.return_value = 500

    stats = TaskStats(test_quick_send_should_return_false_on_bb_error.__name__)

    assert not await _quick_send(bb_cfg=BlackboxSettings(), sendr_cfg=SendrSettings(), client=cl, bb_tvm_ticket='',
                                 uid=1, templates=Templates(), metrics=Metrics(stats), parser=mock_parser())
    assert cl._call_count == 3
    assert (f'{test_quick_send_should_return_false_on_bb_error.__name__}'
            f'_blackbox_error_ammm', 4) in stats.get()


@pytest.mark.asyncio
async def test_quick_send_should_return_false_on_bb_empty_value():
    cl = client()
    cl._req.response._text.return_value = ''

    stats = TaskStats(test_quick_send_should_return_false_on_bb_empty_value.__name__)

    assert not await _quick_send(bb_cfg=BlackboxSettings(), sendr_cfg=SendrSettings(), client=cl, bb_tvm_ticket='',
                                 uid=1, templates=Templates(), metrics=Metrics(stats), parser=mock_parser())
    assert cl._call_count == 3
    assert (f'{test_quick_send_should_return_false_on_bb_empty_value.__name__}'
            f'_blackbox_error_ammm', 1) in stats.get()


@pytest.mark.asyncio
async def test_quick_send_should_return_false_on_sendr_error():
    cl = client()
    cl._req.response._status.side_effect = [200] + [200] + [500] * (SendrSettings().tries * 2)

    stats = TaskStats(test_quick_send_should_return_false_on_sendr_error.__name__)

    assert not await _quick_send(bb_cfg=BlackboxSettings(), sendr_cfg=SendrSettings(), client=cl, bb_tvm_ticket='',
                                 uid=1, templates=Templates(), metrics=Metrics(stats), parser=mock_parser())
    assert cl._call_count == 5
    assert (f'{test_quick_send_should_return_false_on_sendr_error.__name__}_sendr_error_ammm', 4) in stats.get()


@pytest.mark.asyncio
async def test_quick_send_should_return_true_on_success():
    cl = client()

    stats = TaskStats(test_quick_send_should_return_true_on_success.__name__)

    assert await _quick_send(bb_cfg=BlackboxSettings(), sendr_cfg=SendrSettings(), client=cl, bb_tvm_ticket='',
                             uid=1, templates=Templates(), metrics=Metrics(stats), parser=mock_parser())
    assert cl._call_count == 3
    assert (f'{test_quick_send_should_return_true_on_success.__name__}'
            f'_successfully_sended_with_strange_response_ammm', 1) in stats.get()


@pytest.mark.asyncio
async def test_is_valid_default_address_should_return_false_on_bb_error():
    cl = client()
    cl._req.response._status.return_value = 500

    stats = TaskStats(test_is_valid_default_address_should_return_false_on_bb_error.__name__)

    assert not await _is_valid_default_address(bb_cfg=BlackboxSettings(), user=USER, client=cl, bb_tvm_ticket='',
                                               metrics=Metrics(stats), parser=mock_parser())
    assert cl._call_count == 3
    assert (f'{test_is_valid_default_address_should_return_false_on_bb_error.__name__}'
            f'_blackbox_error_ammm', 4) in stats.get()


@pytest.mark.asyncio
async def test_is_valid_default_address_should_return_false_on_absent_default_address():
    cl = client()
    user = User(uid='1', addresses=['a@ya.ru', 'b@ya.ru'], default_address=None, lang='ru')

    stats = TaskStats(test_is_valid_default_address_should_return_false_on_absent_default_address.__name__)

    assert not await _is_valid_default_address(bb_cfg=BlackboxSettings(), user=user, client=cl, bb_tvm_ticket='',
                                               metrics=Metrics(stats), parser=mock_parser())
    assert cl._call_count == 0


@pytest.mark.asyncio
async def test_is_valid_default_address_should_return_false_on_bad_default_address():
    cl = client()
    user1 = User(uid='1', addresses=['a@ya.ru', 'b@ya.ru'], default_address='a@ya.ru', lang='ru')
    user2 = User(uid='2', addresses=['a@ya.ru', 'b@ya.ru'], default_address='a@ya.ru', lang='ru')

    stats = TaskStats(test_is_valid_default_address_should_return_false_on_bad_default_address.__name__)

    assert not await _is_valid_default_address(bb_cfg=BlackboxSettings(), user=user1, client=cl, bb_tvm_ticket='',
                                               metrics=Metrics(stats), parser=mock_parser(user2))
    assert cl._call_count == 1


@pytest.mark.asyncio
async def test_is_valid_default_address_should_return_true_on_good_default_address():
    cl = client()

    stats = TaskStats(test_is_valid_default_address_should_return_true_on_good_default_address.__name__)

    assert await _is_valid_default_address(bb_cfg=BlackboxSettings(), user=USER, client=cl, bb_tvm_ticket='',
                                               metrics=Metrics(stats), parser=mock_parser(USER))
    assert cl._call_count == 1


@pytest.mark.asyncio
async def test_is_active_bb_user_should_return_false_on_bb_error():
    cl = client()
    cl._req.response._status.return_value = 500

    stats = TaskStats(test_is_active_bb_user_should_return_false_on_bb_error.__name__)

    assert not await _is_active_bb_user(bb_cfg=BlackboxSettings(), uid=1, client=cl, bb_tvm_ticket='',
                                               metrics=Metrics(stats), parser=mock_parser())
    assert cl._call_count == 3
    assert (f'{test_is_active_bb_user_should_return_false_on_bb_error.__name__}'
            f'_blackbox_error_ammm', 4) in stats.get()


@pytest.mark.asyncio
async def test_is_active_bb_user_should_return_false_on_frozen_or_absent_user():
    cl = client()

    stats = TaskStats(test_is_active_bb_user_should_return_false_on_frozen_or_absent_user.__name__)

    parser = Mock()
    parser.return_value = []

    assert not await _is_active_bb_user(bb_cfg=BlackboxSettings(), uid=1, client=cl, bb_tvm_ticket='',
                                               metrics=Metrics(stats), parser=parser)
    assert cl._call_count == 1


@pytest.mark.asyncio
async def test_is_active_bb_user_should_return_true_on_active_bb_user():
    cl = client()

    stats = TaskStats(test_is_active_bb_user_should_return_true_on_active_bb_user.__name__)

    assert await _is_active_bb_user(bb_cfg=BlackboxSettings(), uid=1, client=cl, bb_tvm_ticket='',
                                               metrics=Metrics(stats), parser=mock_parser(USER))
    assert cl._call_count == 1
