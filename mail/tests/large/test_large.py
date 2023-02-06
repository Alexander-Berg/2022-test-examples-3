from mail.borador.devpack.components.borador import BoradorTesting
from hamcrest import assert_that, is_


def test_ping(borador_testing: BoradorTesting):
    assert_that(borador_testing.healthcheck().status_code, is_(200))
