import logging

from mail.borador.devpack.components.borador import Borador
from mail.borador.tests.helpers import data_path, get_log_lines, waitable_file, aqua_will_response
from mail.borador.tests.matchers import wait_with, success_launch, all_suites_are_passed, \
    first_with_action, value_of_field, missing_field, failed_launch

from hamcrest import is_, assert_that, all_of, not_, contains_string, anything
from library.python.testing.pyremock.lib.pyremock import MockHttpServer


logger = logging.getLogger('tests')


def read_file(path: str) -> str:
    with open(data_path('integration', path)) as f:
        return f.read()


def test_ping(borador: Borador):
    assert_that(borador.healthcheck().status_code, is_(200))


def test_all_suites_are_passed(borador: Borador, aqua_mock: MockHttpServer, x_request_id: str):
    aqua_will_response(aqua_mock=aqua_mock, responses=[
        ('launch pack --tag borador_missing akita', read_file('launch.xml')),
        ('show launch launch_id', read_file('show_success.xml'))
    ])

    assert_that(borador.start_pack(pack_name='akita', x_request_id=x_request_id), success_launch())

    assert_that(
        waitable_file(borador, x_request_id),
        wait_with(
            first_with_action(
                'finished',
                all_of(
                    value_of_field('action', is_('finished')),
                    missing_field('error'),
                    all_suites_are_passed()
                )
            )
        )
    )

    aqua_mock.assert_expectations()


def test_borador_will_restart_failed_suites(borador: Borador, aqua_mock: MockHttpServer, x_request_id: str):
    aqua_will_response(aqua_mock=aqua_mock, responses=[
        ('launch pack --tag borador_missing akita', read_file('launch.xml')),
        ('show launch launch_id', read_file('show_failed.xml')),
        ('restart --failedOnly launch_id', read_file('restart.xml')),
        ('show launch launch_id', read_file('show_success.xml'))
    ])

    assert_that(borador.start_pack(pack_name='akita', x_request_id=x_request_id), success_launch())

    assert_that(
        waitable_file(borador, x_request_id),
        wait_with(
            first_with_action(
                'finished',
                all_of(
                    value_of_field('action', is_('finished')),
                    missing_field('error'),
                    all_suites_are_passed()
                )
            )
        )
    )

    aqua_mock.assert_expectations()


def test_not_all_suites_are_passed(borador: Borador, aqua_mock: MockHttpServer, x_request_id: str):
    aqua_will_response(aqua_mock=aqua_mock, responses=[
        ('launch pack --tag borador_missing akita', read_file('launch.xml')),
        ('show launch launch_id', read_file('show_failed.xml')),
        ('restart --failedOnly launch_id', read_file('restart.xml')),
        ('show launch launch_id', read_file('show_failed.xml')),
        ('restart --failedOnly launch_id', read_file('restart.xml')),
        ('show launch launch_id', read_file('show_failed.xml')),
    ])

    assert_that(borador.start_pack(pack_name='akita', x_request_id=x_request_id), success_launch())

    assert_that(
        waitable_file(borador, x_request_id),
        wait_with(
            first_with_action(
                'finished',
                all_of(
                    value_of_field('action', is_('finished')),
                    missing_field('error'),
                    not_(
                        all_suites_are_passed()
                    )
                )
            )
        )
    )

    aqua_mock.assert_expectations()


def test_launch_returns_200_but_is_failed(borador: Borador, aqua_mock: MockHttpServer, x_request_id: str):
    aqua_will_response(aqua_mock=aqua_mock, responses=[
        ('launch pack --tag borador_missing akita', read_file('launch.xml')),
        ('show launch launch_id', 'wrong xml'),
    ])

    assert_that(borador.start_pack(pack_name='akita', x_request_id=x_request_id), success_launch())

    assert_that(
        waitable_file(borador, x_request_id),
        wait_with(
            first_with_action(
                'finished',
                all_of(
                    value_of_field('error', is_(anything())),
                )
            )
        )
    )

    aqua_mock.assert_expectations()


def test_access_log(borador: Borador, x_request_id: str):
    assert_that(borador.start_pack(pack_name='akita', x_request_id=x_request_id), failed_launch(code=500))

    assert_that(
        get_log_lines(path=borador.tskv_log(), req_id=x_request_id),
        first_with_action(
            'access',
            all_of(
                value_of_field('status', is_('500')),
                value_of_field('url', contains_string('/launch')),
            )
        )
    )

    assert_that(
        waitable_file(borador, x_request_id),
        wait_with(
            first_with_action(
                'finished',
                value_of_field('error', is_(anything())),
            )
        )
    )
