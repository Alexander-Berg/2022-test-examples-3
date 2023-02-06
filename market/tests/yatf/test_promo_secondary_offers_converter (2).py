#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
from market.tools.promo_secondary_offers_converter.yatf.test_env import PromoSecondaryOffersConvertor
from market.tools.promo_secondary_offers_converter.yatf.resource import SecondaryOffersRawDescription, SecondaryOffersJsonDescription
from market.idx.yatf.matchers.env_matchers import HasExitCode, ContainsErrorMessage


# положительный тест
@pytest.fixture(scope='module')
def good_offers_list():
    return {'result': [
        {
            "feed": 321,
            "offers": [
                "abc",
                "123"
            ]
        },
        {
            "feed": 123,
            "offers": [
                "abc",
                "123"
            ]
        },
    ]}


@pytest.yield_fixture(scope='module')
def local_good_offers_list(good_offers_list):
    resources = {
        'input_json_file': SecondaryOffersJsonDescription(
            data=good_offers_list
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_good_offers_list(local_good_offers_list):
    local_good_offers_list.verify(matchers=[HasExitCode(0), ])


# невалидный json по формату
@pytest.yield_fixture(scope='module')
def local_invalid_json():
    resources = {
        'input_json_file': SecondaryOffersRawDescription(
            data="somenotjsondata"
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_invalid(local_invalid_json):
    local_invalid_json.verify(matchers=[
        HasExitCode(3),
        ContainsErrorMessage(
            err_msg="Failed to parse JSON:",
        )
    ])


# json где нет узла 'result'
@pytest.fixture(scope='module')
def bad_offers_list_format():
    return {
        321 : {
            "feed": 321,
            "offers": [
                "abc",
                "123"
            ]
        },
        322: {
            "feed": 322,
            "offers": [
                "abc",
                "123"
            ]
        },
    }


@pytest.yield_fixture(scope='module')
def local_bad_offers_list_format(bad_offers_list_format):
    resources = {
        'input_json_file': SecondaryOffersJsonDescription(
            data=bad_offers_list_format
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_bad_offers_list_format(local_bad_offers_list_format):
    local_bad_offers_list_format.verify(matchers=[
        HasExitCode(3),
        ContainsErrorMessage(
            err_msg="Failed to parse JSON: 'result' node not found",
        )
    ])


# один из фидов не валидный. some errors
@pytest.fixture(scope='module')
def report_bad_feed_id_some():
    return {'result': [
        {
            "feed": 123,
            "offers": [
                "abc",
                "123"
            ]
        },
        {
            "feed": 1234,
            "offers": [
                "abc",
                "123"
            ]
        },
        {
            "feed": "not_number",
            "offers": [
                "abc",
                "123"
            ]
        },
    ]}


@pytest.yield_fixture(scope='module')
def local_report_bad_feed_id_some(report_bad_feed_id_some):
    resources = {
        'input_json_file': SecondaryOffersJsonDescription(
            data=report_bad_feed_id_some
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_report_bad_feed_id_some(local_report_bad_feed_id_some):
    local_report_bad_feed_id_some.verify(matchers=[
        HasExitCode(1),
        ContainsErrorMessage(
            err_msg="Failed to get feedId as unsigned integer type",
        )
    ])


# много ошибок по фиду - crit
@pytest.fixture(scope='module')
def report_bad_feed_id_crit():
    return {'result': [
        {
            "feed": 123,
            "offers": [
                "abc",
                "123"
            ]
        },
        {
            "feed": "not_number_another",
            "offers": [
                "abc",
                "123"
            ]
        },
        {
            "feed": "not_number",
            "offers": [
                "abc",
                "123"
            ]
        },
    ]}


@pytest.yield_fixture(scope='module')
def local_report_bad_feed_id_crit(report_bad_feed_id_crit):
    resources = {
        'input_json_file': SecondaryOffersJsonDescription(
            data=report_bad_feed_id_crit
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_report_bad_feed_id_crit(local_report_bad_feed_id_crit):
    local_report_bad_feed_id_crit.verify(matchers=[
        HasExitCode(2),
        ContainsErrorMessage(
            err_msg="Failed to get feedId as unsigned integer type",
        )
    ])


# ошибки парсинга имён офферов
@pytest.fixture(scope='module')
def report_bad_offer_id_some():
    return {'result': [
        {
            "feed": 123,
            "offers": [
                "abc",
                "123"
            ]
        },
        {
            "feed": 321,
            "offers": [
                "abc",
                123,
            ]
        },
    ]}


@pytest.yield_fixture(scope='module')
def local_report_bad_offer_id_some(report_bad_offer_id_some):
    resources = {
        'input_json_file': SecondaryOffersJsonDescription(
            data=report_bad_offer_id_some
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_report_bad_offer_id_some(local_report_bad_offer_id_some):
    local_report_bad_offer_id_some.verify(matchers=[
        HasExitCode(1),
        ContainsErrorMessage(
            err_msg="Failed to get offer_id as a string",
        )
    ])


# много ошибок имён офферов
@pytest.fixture(scope='module')
def report_bad_offer_id_crit():
    return {'result': [
        {
            "feed": 123,
            "offers": [
                2222,
                None
            ]
        },
        {
            "feed": 321,
            "offers": [
                "abc",
                123,
            ]
        },
    ]}


@pytest.yield_fixture(scope='module')
def local_report_bad_offer_id_crit(report_bad_offer_id_crit):
    resources = {
        'input_json_file': SecondaryOffersJsonDescription(
            data=report_bad_offer_id_crit
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_report_bad_offer_id_crit(local_report_bad_offer_id_crit):
    local_report_bad_offer_id_crit.verify(matchers=[
        HasExitCode(2),
        ContainsErrorMessage(
            err_msg="Failed to get offer_id as a string",
        )
    ])


# проверка переполнения ui32
@pytest.fixture(scope='module')
def report_bad_feed_id_overflow():
    return {'result': [
        {
            "feed": 4294967300,
            "offers": [
                "2222",
                "1111"
            ]
        },
        {
            "feed": 321,
            "offers": [
                "abc",
                "123",
            ]
        },
    ]}


@pytest.yield_fixture(scope='module')
def local_report_bad_feed_id_overflow(report_bad_feed_id_overflow):
    resources = {
        'input_json_file': SecondaryOffersJsonDescription(
            data=report_bad_feed_id_overflow
        )
    }
    with PromoSecondaryOffersConvertor(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_local_report_bad_feed_id_overflow(local_report_bad_feed_id_overflow):
    local_report_bad_feed_id_overflow.verify(matchers=[
        HasExitCode(2),
        ContainsErrorMessage(
            err_msg="Failed to get feedId as TFeedId: overflow",
        )
    ])
