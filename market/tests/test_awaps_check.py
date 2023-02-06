# coding: utf-8

from datetime import datetime, timedelta

import market.idx.pylibrary.mindexer_core.awaps.awaps as awaps


GENERATION = '20180119_2000'
NOW = datetime.strptime(GENERATION, '%Y%m%d_%H%M') + timedelta(hours=5)
BAD_THRESHOLD = timedelta(hours=4)
GOOD_THRESHOLD = timedelta(hours=6)


def fetch_ok(config):
    return [GENERATION]


def fetch_exc(config):
    raise Exception('Oh oh')


def patch_fetch(monkeypatch, fetch_func):
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.awaps.awaps._fetch_check_export_generations',
        fetch_func
    )


def patch_now(monkeypatch):
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.awaps.awaps._get_check_export_now',
        lambda: NOW
    )


def test_ok(monkeypatch):
    patch_fetch(monkeypatch, fetch_ok)
    patch_now(monkeypatch)
    result = awaps.do_check_export(
        None,
        GOOD_THRESHOLD,
        GOOD_THRESHOLD,
    )

    assert '0;OK' == result


def test_exc(monkeypatch):
    patch_fetch(monkeypatch, fetch_exc)
    patch_now(monkeypatch)
    result = awaps.do_check_export(
        None,
        GOOD_THRESHOLD,
        GOOD_THRESHOLD,
    )

    assert result.startswith('2;')
    assert awaps.CHECK_PROMPT_EXC in result


def test_warn(monkeypatch):
    patch_fetch(monkeypatch, fetch_ok)
    patch_now(monkeypatch)
    result = awaps.do_check_export(
        None,
        BAD_THRESHOLD,
        GOOD_THRESHOLD,
    )

    assert result.startswith('1;')
    assert awaps.CHECK_PROMPT_OLD_GEN in result


def test_error(monkeypatch):
    patch_fetch(monkeypatch, fetch_ok)
    patch_now(monkeypatch)
    result = awaps.do_check_export(
        None,
        BAD_THRESHOLD,
        BAD_THRESHOLD,
    )

    assert result.startswith('2;')
    assert awaps.CHECK_PROMPT_OLD_GEN in result
