# -*- coding: utf-8 -*-
from datetime import datetime

from travel.avia.avia_api.avia.v1.model.subscriber import hashed, UNAPPROVED_SESSION_DECAY_TIME

passsess = 'random_doesnt_really_matter'
passsess_hashed = hashed(passsess)
passsess2 = 'yet_another_random_doesnt_really_matter'
passsess2_hashed = hashed(passsess2)

email_source = 'random_email_source_doesnt_matter'
email_source2 = 'yet_another_email_source'


class TestSubscriber(object):
    def test_create_new(self, subscriber):
        sub = subscriber()
        assert sub.subscriptions == {}
        assert sub.email is not None
        assert sub.api_version == '1.1'
        assert sub.related_sessions == {}
        assert sub.related_passports == {}
        assert sub.id is not None
        assert not sub.subscribed

    def test_add_subscription_no_pending(self, subscriber, qkey):
        sub = subscriber()
        sub.add_subscription(qkey, 'rand_email_source')
        assert sub.subscriptions.keys() == [qkey]
        assert sub.get_approved_subscriptions_list() == [qkey]

    def test_add_subscription_pending_passport(self, subscriber, qkey):
        sub = subscriber()
        sub.add_subscription(qkey, email_source, pending_passport_plain=passsess)
        assert sub.subscriptions.keys() == [qkey]
        assert sub.get_approved_subscriptions_list() == []
        assert sub.subscriptions.values()[0].pending_passport == passsess_hashed

    def test_add_subscription_pending_session(self, subscriber, qkey):
        sub = subscriber()
        sub.add_subscription(qkey, email_source, pending_session_plain=passsess)
        assert sub.subscriptions.keys() == [qkey]
        assert sub.get_approved_subscriptions_list() == []
        assert sub.subscriptions.values()[0].pending_session == passsess_hashed

    def test_add_subscription_pending_passport_approve(self, subscriber, qkey):
        sub = subscriber()
        sub.add_subscription(qkey, email_source, pending_passport_plain=passsess)
        assert sub.subscriptions.keys() == [qkey]
        assert sub.get_approved_subscriptions_list() == []
        for v in sub.subscriptions.values():
            v.approve()
        assert sub.get_approved_subscriptions_list() == [qkey]
        assert sub.subscribed

    def test_double_add_same_subscription_diff_source(self, subscriber, qkey):
        sub = subscriber()
        yet_another_email_source = 'yet_another_email_source'

        sub.add_subscription(qkey, email_source, pending_passport_plain=passsess)
        assert sub.subscriptions.values()[0].source != yet_another_email_source
        sub.add_subscription(qkey, yet_another_email_source, pending_passport_plain=passsess)
        assert sub.subscriptions.values()[0].source == yet_another_email_source

    def test_add_same_subscription_diff_source_and_passport(self, subscriber, qkey):
        sub = subscriber()
        sub.add_subscription(qkey, email_source, pending_passport_plain=passsess)
        assert sub.subscriptions.values()[0].pending_passport == passsess_hashed
        assert sub.subscriptions.values()[0].source != email_source2
        sub.add_subscription(qkey, email_source2, pending_passport_plain=passsess2)
        assert sub.subscriptions.values()[0].pending_passport == passsess2_hashed
        assert sub.subscriptions.values()[0].source == email_source2

    def test_get_approved_subscriptions_list(self, subscriber, qkey_pair):
        sub = subscriber()
        assert sub.get_approved_subscriptions_list() == []
        qkey1, qkey2 = qkey_pair

        sub.add_subscription(qkey1, email_source, pending_passport_plain=passsess)
        assert sub.get_approved_subscriptions_list() == []

        sub.add_subscription(qkey2, email_source, pending_session_plain=passsess2)
        assert sub.get_approved_subscriptions_list() == []

        sub.subscriptions[qkey2].approve()
        assert sub.get_approved_subscriptions_list() == [qkey2]

        sub.subscriptions[qkey1].approve()
        assert sorted(sub.get_approved_subscriptions_list()) == sorted([qkey1, qkey2])

    def test_get_approved_subscriptions_list_old_api(self, subscriber, qkey_pair):
        sub = subscriber()
        sub.api_version = None
        assert sub.get_approved_subscriptions_list() == []
        qkey1, qkey2 = qkey_pair

        sub.add_subscription(qkey1, email_source, pending_passport_plain=passsess)
        assert sub.get_approved_subscriptions_list() == [qkey1]

        sub.add_subscription(qkey2, email_source, pending_session_plain=passsess2)
        assert sorted(sub.get_approved_subscriptions_list()) == sorted([qkey1, qkey2])

        sub.subscriptions[qkey2].approve()
        assert sorted(sub.get_approved_subscriptions_list()) == sorted([qkey1, qkey2])

        sub.subscriptions[qkey1].approve()
        assert sorted(sub.get_approved_subscriptions_list()) == sorted([qkey1, qkey2])

    def test_request_and_approve_relations(self, subscriber):
        sub = subscriber()
        sub.request_relation(passport_plain=passsess)
        sub.request_relation(session_plain=passsess2)
        assert sub.related_passports.keys() == [passsess_hashed]
        assert sub.related_sessions.keys() == [passsess2_hashed]
        assert not sub.related_passports[passsess_hashed].approved
        assert not sub.related_sessions[passsess2_hashed].approved

        sub.approve_relation(passport_hashed=passsess_hashed)
        assert sub.related_passports[passsess_hashed].approved
        assert not sub.related_sessions[passsess2_hashed].approved

        sub.approve_relation(session_hashed=passsess2_hashed)
        assert sub.related_passports[passsess_hashed].approved
        assert sub.related_sessions[passsess2_hashed].approved

    def test_has_too_many_unapproved_sessions(self, subscriber):
        sub = subscriber()
        sub.request_relation(session_plain=passsess)
        assert not sub.has_too_many_unapproved_sessions()
        sub.request_relation(session_plain=passsess2)
        assert not sub.has_too_many_unapproved_sessions()
        sub.request_relation(session_plain=passsess + passsess)
        assert sub.has_too_many_unapproved_sessions()
        sub.related_sessions.values()[0].requested_at = datetime.utcnow() - UNAPPROVED_SESSION_DECAY_TIME
        assert not sub.has_too_many_unapproved_sessions()
        sub.request_relation(session_plain=passsess + passsess2)
        assert sub.has_too_many_unapproved_sessions()
        sub.approve_relation(session_hashed=passsess2_hashed)
        assert not sub.has_too_many_unapproved_sessions()

    def test_is_2_opt_in_progress(self, subscriber):
        sub = subscriber()
        sub.request_relation(passport_plain=passsess)
        sub.request_relation(session_plain=passsess2)
        assert sub.related_passports.keys() == [passsess_hashed]
        assert sub.related_sessions.keys() == [passsess2_hashed]
        assert sub.is_2opt_in_progress(passport_hashed=passsess_hashed)
        assert sub.is_2opt_in_progress(session_hashed=passsess2_hashed)

        sub.approve_relation(passport_hashed=passsess_hashed)
        assert not sub.is_2opt_in_progress(passport_hashed=passsess_hashed)
        assert sub.is_2opt_in_progress(session_hashed=passsess2_hashed)

        sub.approve_relation(session_hashed=passsess2_hashed)
        assert not sub.is_2opt_in_progress(passport_hashed=passsess_hashed)
        assert not sub.is_2opt_in_progress(session_hashed=passsess2_hashed)
