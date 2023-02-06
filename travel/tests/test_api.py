# -*- coding: utf-8 -*-
from contextlib import contextmanager

import mock
import pytest
import requests
from freezegun import freeze_time

import travel.avia.avia_api.avia.v1.email_dispenser.api as api
from travel.avia.avia_api.ant.exceptions import ValidationError
from travel.avia.avia_api.avia.v1.model.filters import Filter
from travel.avia.avia_api.avia.v1.model.subscriber import hashed
from travel.avia.avia_api.tests.conftest import email1, email2, TEST_DATE

passsess = 'random_doesnt_really_matter'
passsess_hashed = hashed(passsess)
passsess2 = 'yet_another_random_doesnt_really_matter'
passsess2_hashed = hashed(passsess2)

email_source = 'random_email_source_doesnt_matter'
email_source2 = 'yet_another_email_source'


def mock_passport_email(email):
    return mock.patch(
        'travel.avia.avia_api.avia.v1.email_dispenser.api.get_email_by_uid',
        new=lambda *args, **kwargs: email,
    )


def mock_passport_emails(email):
    return mock.patch(
        'travel.avia.avia_api.avia.v1.email_dispenser.api.get_emails_by_uid',
        new=lambda *args, **kwargs: [email],
    )


def mock_db_sub_this(sub):
    @contextmanager
    def mocked_db_sub(create=True, **kwargs):
        if 'email' in kwargs:
            sub.email = kwargs.pop('email')
        yield sub

    return mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.db_subscriber', new=mocked_db_sub)


def mock_db_sub(subsciber):
    @contextmanager
    def mocked_db_sub(create=True, **kwargs):
        sub = subsciber(**kwargs)
        yield sub

    return mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.db_subscriber', new=mocked_db_sub)


def mock_subscribe():
    def mocked_subscribe(sub, qkey, source, pending_passport_plain=None, pending_session_plain=None, date_range=1,
                         filter_=None, **kwargs):
        sub.add_subscription(
            qkey,
            source,
            pending_passport_plain=pending_passport_plain,
            pending_session_plain=pending_session_plain,
            date_range=date_range,
            filter_=filter_,
        )

    return mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.subscribe', new=mocked_subscribe)


class TestEmailDispenserApi(object):
    def test_normalize_single_email_str(self):
        mail = '  JunkASDfg3@MSifawf.sadjf '
        normal = 'junkasdfg3@msifawf.sadjf'
        assert api.normalize_email(mail) == normal

    def test_normalize_single_email_list(self):
        mail = '  JunkASDfg3@MSifawf.sadjf '
        normal = 'junkasdfg3@msifawf.sadjf'
        lst = [mail, mail, mail]
        nlist = [normal, normal, normal]
        assert map(api.normalize_email, lst) == nlist

    def test_select_optin_type_no_input(self, subscriber):
        sub = subscriber()
        assert api._select_optin_type(sub, None, None) is None

    def test_select_optin_type_passport(self, subscriber):
        sub = subscriber(email=api.normalize_email(email1))
        with mock_passport_email(email1):
            with mock_passport_emails(email1):
                assert api._select_optin_type(sub, passsess, None) == api.OptinType.single

        with mock_passport_email(email2):
            with mock_passport_emails(email2):
                assert api._select_optin_type(sub, passsess, None) == api.OptinType.single
                sub.request_relation(passport_plain=passsess)
                assert api._select_optin_type(sub, passsess, None) == api.OptinType.pending
                sub.approve_relation(passport_hashed=passsess_hashed)
                assert api._select_optin_type(sub, passsess, None) == api.OptinType.single

    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    def test_select_optin_type_session(self, subscriber):
        sub = subscriber(email=api.normalize_email(email1))
        assert api._select_optin_type(sub, None, passsess2) == api.OptinType.single
        sub.request_relation(session_plain=passsess2)
        assert api._select_optin_type(sub, None, passsess2) == api.OptinType.pending
        sub.approve_relation(session_hashed=passsess2_hashed)
        assert api._select_optin_type(sub, None, passsess2) == api.OptinType.single

    @mock_passport_email(email2)
    @mock_passport_emails(email2)
    def test_select_optin_type_passport_priority(self, subscriber):
        sub = subscriber(email=api.normalize_email(email1))
        assert api._select_optin_type(sub, passsess, passsess2) == api.OptinType.single
        sub.request_relation(session_plain=passsess2)
        assert api._select_optin_type(sub, passsess, passsess2) == api.OptinType.single
        sub.request_relation(passport_plain=passsess)
        assert api._select_optin_type(sub, passsess, passsess2) == api.OptinType.pending
        sub.approve_relation(session_hashed=passsess2_hashed)
        assert api._select_optin_type(sub, passsess, passsess2) == api.OptinType.pending
        sub.approve_relation(passport_hashed=passsess_hashed)
        assert api._select_optin_type(sub, passsess, None) == api.OptinType.single

    def test_subscribe_handler_fails_if_no_passport_and_session(self, subscriber, qkey):
        sub = subscriber()
        with mock_db_sub(subscriber):
            with pytest.raises(ValidationError):
                api._email_subscribe_handler(
                    passport_plain=None,
                    session_plain=None,
                    email=sub.email,
                    qid=None,
                    qkey=qkey,
                    email_source='VALERA',
                )

    def test_subscribe_handler_fails_if_no_email_and_passport(self, subscriber, qkey):
        with pytest.raises(ValidationError):
            api._email_subscribe_handler(
                passport_plain=None,
                session_plain=passsess2,
                email=None,
                qid=None,
                qkey=qkey,
                email_source='VALERA',
            )

    def test_subscribe_handler_fails_on_invalid_qkey(self, qkey):
        with pytest.raises(ValidationError):
            api._email_subscribe_handler(
                passport_plain=None,
                session_plain=passsess2,
                email=None,
                qid=None,
                qkey='badQkeyWithoutUnderScores',
                email_source='VALERA',
            )
        with pytest.raises(ValidationError):
            api._email_subscribe_handler(
                passport_plain=None,
                session_plain=passsess2,
                email=None,
                qid=None,
                qkey=qkey + '.ru',
                email_source='VALERA',
            )

        with pytest.raises(ValidationError):
            api._email_subscribe_handler(
                passport_plain=None,
                session_plain=passsess2,
                email=None,
                qid=None,
                qkey=qkey + '$',
                email_source='VALERA',
            )

    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    def test_sub_handler_normalizes_emails(self, subscriber, qkey):
        sub = subscriber()
        with mock_db_sub(subscriber):
            email = sub.email
            api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=passsess2,
                email=email1,
                qid=None,
                qkey=qkey,
                email_source='VALERA',
            )
            assert sub.email == email.lower()

    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    def test_sub_handler_gets_email_from_passport(self, subscriber, qkey):
        sub = subscriber(email=email2)
        with mock_db_sub_this(sub):
            api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=passsess2,
                email=None,
                qid=None,
                qkey=qkey,
                email_source='VALERA',
            )
            assert sub.email == str(email1).lower()

    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    def test_subscribe_handler_valid_passport_no_session_no_email(self, subscriber, qkey):
        sub = subscriber()
        with mock_db_sub_this(sub):
            assert sub.related_sessions.keys() == []
            assert sub.related_passports.keys() == []
            result = api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=None,
                email=None,
                qid=None,
                qkey=qkey,
                email_source='VALERA',
            )
            assert not result['pending']
            assert not result['required_double_opt_in']
            assert result['id'] == sub.id
            assert sub.related_passports.keys() == [passsess_hashed]
            assert sub.related_sessions.keys() == []
            assert sub.get_approved_subscriptions_list() == [qkey]
            assert sub.email == api.normalize_email(email1)
            assert sub.subscriptions.keys() == [qkey]
            assert sub.subscribed

    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    def test_subscribe_handler_valid_passport_and_session(self, subscriber, qkey):
        sub = subscriber()
        with mock_db_sub_this(sub):
            assert sub.related_sessions.keys() == []
            assert sub.related_passports.keys() == []
            result = api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=passsess2,
                email=None,
                qid=None,
                qkey=qkey,
                email_source='VALERA',
            )
            assert not result['pending']
            assert not result['required_double_opt_in']
            assert result['id'] == sub.id
            assert sub.related_passports.keys() == [passsess_hashed]
            assert sub.related_sessions.keys() == [passsess2_hashed]
            assert sub.get_approved_subscriptions_list() == [qkey]
            assert sub.email == api.normalize_email(email1)
            assert sub.subscriptions.keys() == [qkey]
            assert sub.subscribed

    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    @freeze_time(TEST_DATE)
    def test_subscribe_handler_with_date_range(self, subscriber, qkey):
        sub = subscriber()
        with mock_db_sub_this(sub):
            result = api._email_subscribe_handler(
                passport_plain=passsess,
                email=email1,
                qkey=qkey,
                email_source='VALERA',
                date_range=2,
            )
            assert not result['pending']
            assert not result['required_double_opt_in']
            assert result['id'] == sub.id
            assert sub.related_passports.keys() == [passsess_hashed]
            assert sub.get_approved_subscriptions_list() == [qkey]
            assert sub.email == api.normalize_email(email1)
            assert sub.subscriptions.keys() == [qkey]
            assert sub.subscriptions[qkey].date_range == 2
            assert sub.get_approved_subscriptions_list() == [qkey]
            assert len(sub.expand_approved_subscriptions()) == 2
            assert sub.subscribed

    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    def test_subscribe_handler_with_filters(self, subscriber, qkey):
        sub = subscriber()
        with mock_db_sub_this(sub):
            result = api._email_subscribe_handler(
                passport_plain=passsess,
                email=email1,
                qkey=qkey,
                email_source='VALERA',
                filter_='{"airlines":[2], "filter_url_postfix":"#bg=1"}',
            )
            assert not result['pending']
            assert not result['required_double_opt_in']
            assert result['id'] == sub.id
            assert sub.related_passports.keys() == [passsess_hashed]
            assert sub.get_approved_subscriptions_list() == [qkey]
            assert sub.email == api.normalize_email(email1)
            assert sub.subscriptions.keys() == [qkey]
            assert sub.get_approved_subscriptions_list() == [qkey]
            assert sub.subscriptions[qkey].applied_filters
            assert len(sub.subscriptions[qkey].applied_filters) == 1
            assert sub.subscriptions[qkey].applied_filters[0] == Filter(
                airlines=[2],
                frontend_filter_postfix="#bg=1",
            )
            assert sub.subscribed

    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.get_readable_flight_info', new=lambda *a: {})
    def test_2optin_returns_true_if_sent_successfully(self, subscriber, qkey):
        class MockResponse(requests.Response):
            def __init__(self, ok):
                super(MockResponse, self).__init__()
                if ok:
                    self.status_code = 200
                else:
                    self.status_code = 400
                    self._content = 'Not a real error'

        def raise_exception(*args, **kwargs):
            raise Exception

        with mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api._double_opt_in.send',
                        new=lambda *a, **kwa: MockResponse(True)):
            sub = subscriber()
            with mock_db_sub_this(sub):
                assert api.send_2opt_in(sub, qkey, passsess, None)
                assert api.send_2opt_in(sub, qkey, None, passsess)

        with mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api._double_opt_in.send', new=raise_exception):
            sub = subscriber()
            with mock_db_sub_this(sub):
                assert not api.send_2opt_in(sub, qkey, passsess, None)
                assert not api.send_2opt_in(sub, qkey, None, passsess)


class TestUserflow(object):
    @mock_passport_email(email1)
    @mock_passport_emails(email1)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    def test_old_style_passport_only_subscription(self, subscriber, qkey_pair):
        sub = subscriber()
        with mock_db_sub_this(sub):
            qkey1, qkey2 = qkey_pair
            assert not sub.subscribed
            api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=None,
                email=None,
                qid=None,
                qkey=qkey1,
                email_source='VALERA',
            )
            assert sub.subscribed
            assert len(sub.get_approved_subscriptions_list()) == 1
            api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=None,
                email=None,
                qid=None,
                qkey=qkey2,
                email_source='VALERA',
            )
            assert sub.subscribed
            assert len(sub.get_approved_subscriptions_list()) == 2

    @mock_passport_email(email2)
    @mock_passport_emails(email2)
    @mock_subscribe()
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_1opt_in', new=lambda *args, **kwargs: True)
    @mock.patch('travel.avia.avia_api.avia.v1.email_dispenser.api.send_2opt_in', new=lambda *args, **kwargs: True)
    def test_user_subscribes_with_passport_to_arbitrary_email(self, subscriber, qkey_pair):
        sub = subscriber()
        with mock_db_sub_this(sub):
            qkey1, qkey2 = qkey_pair
            assert not sub.subscribed
            # subscribing to one qkey
            api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=None,
                email=email1,
                qid=None,
                qkey=qkey1,
                email_source='VALERA',
            )

            assert sub.subscribed
            assert len(sub.get_approved_subscriptions_list()) == 1
            assert len(sub.subscriptions) == 1
            # subscribing to another qkey
            api._email_subscribe_handler(
                passport_plain=passsess,
                session_plain=None,
                email=email1,
                qid=None,
                qkey=qkey2,
                email_source='VALERA',
            )

            assert sub.subscribed
            assert len(sub.get_approved_subscriptions_list()) == 2
            assert len(sub.subscriptions) == 2

            # confirming wrong passport is noop
            api._double_opt_confirm_handler(
                _id=sub.id,
                passport_hashed=passsess2_hashed
            )

            assert sub.subscribed
            assert len(sub.get_approved_subscriptions_list()) == 2
            assert len(sub.subscriptions) == 2

            # confirming session (not even passport) is noop
            api._double_opt_confirm_handler(
                _id=sub.id,
                session_hashed=passsess_hashed
            )

            assert sub.subscribed
            assert len(sub.get_approved_subscriptions_list()) == 2
            assert len(sub.subscriptions) == 2

            # confirming right passport is also noop
            api._double_opt_confirm_handler(
                _id=sub.id,
                passport_hashed=passsess_hashed
            )

            assert sub.subscribed
            assert len(sub.get_approved_subscriptions_list()) == 2
            assert len(sub.subscriptions) == 2
