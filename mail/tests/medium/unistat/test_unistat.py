from hamcrest import assert_that, all_of, is_, not_, anything

from mail.borador.devpack.components.borador import Borador
from mail.borador.devpack.components.unistat import Unistat
from mail.borador.tests.helpers import aqua_will_response, waitable_file, Waitable, data_path
from mail.borador.tests.matchers import success_launch, wait_with, first_with_action, value_of_field, missing_field, \
    all_suites_are_passed, failed_launch, success_unistat_response
from library.python.testing.pyremock.lib.pyremock import MockHttpServer


def read_file(path: str) -> str:
    with open(data_path('integration', path)) as f:
        return f.read()


def test_ping(unistat: Unistat):
    assert_that(unistat.healthcheck().status_code, is_(200))


def test_unistat(borador: Borador, aqua_mock: MockHttpServer, unistat: Unistat, x_request_id: str):
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

    x_request_id = x_request_id + '_success'
    aqua_will_response(aqua_mock=aqua_mock, responses=[
        ('launch pack --tag borador_missing sendbernar', read_file('launch.xml')),
        ('show launch launch_id', read_file('show_failed.xml')),
        ('restart --failedOnly launch_id', read_file('restart.xml')),
        ('show launch launch_id', read_file('show_success.xml')),
    ])
    assert_that(borador.start_pack(pack_name='sendbernar', x_request_id=x_request_id), success_launch())
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

    x_request_id = x_request_id + '_failed'
    assert_that(borador.start_pack(pack_name='name', x_request_id=x_request_id), failed_launch(500))
    assert_that(
        waitable_file(borador, x_request_id),
        wait_with(
            first_with_action(
                'finished',
                value_of_field('error', is_(anything()))
            )
        )
    )

    assert_that(borador.request(args=''), failed_launch(400))

    assert_that(
        Waitable(callable=lambda: unistat.unistat(),
                 info=lambda x: f'unistat response: status={x.status_code} text={x.text}'),
        wait_with(
            success_unistat_response(
                all_of(
                    value_of_field('akita_total_ammm', is_(6)),
                    value_of_field('akita_passed_ammm', is_(1)),
                    value_of_field('akita_failed_ammm', is_(5)),
                    value_of_field('akita_times_ammm', is_(-1)),

                    value_of_field('sendbernar_total_ammm', is_(6)),
                    value_of_field('sendbernar_passed_ammm', is_(6)),
                    value_of_field('sendbernar_failed_ammm', is_(0)),
                    value_of_field('sendbernar_times_ammm', is_(2)),

                    value_of_field('unistat_errors_ammm', is_(0)),

                    value_of_field('launch_200_ammm', is_(2)),
                    value_of_field('launch_400_ammm', is_(1)),
                    value_of_field('launch_500_ammm', is_(1)),

                    value_of_field('akita_running_ammm', is_(0)),
                    value_of_field('sendbernar_running_ammm', is_(0)),
                )
            )
        )
    )


def test_borador_init(borador: Borador, unistat: Unistat):
    borador.stop()
    borador.start()
    borador.stop()
    borador.start()

    assert_that(
        Waitable(callable=lambda: unistat.unistat(),
                 info=lambda x: f'unistat response: status={x.status_code} text={x.text}'),
        wait_with(
            success_unistat_response(
                all_of(
                    value_of_field('borador_init_ammm', is_(3)),
                )
            )
        )
    )


def test_running_packs(borador: Borador, aqua_mock: MockHttpServer, unistat: Unistat, x_request_id: str):
    aqua_will_response(aqua_mock=aqua_mock, responses=[
        ('launch pack --tag borador_missing akita', read_file('launch.xml')),
        ('show launch launch_id', read_file('show_failed.xml')),
        ('restart --failedOnly launch_id', read_file('restart.xml')),
        ('show launch launch_id', read_file('show_failed.xml')),
        ('restart --failedOnly launch_id', read_file('restart.xml')),
        ('show launch launch_id', read_file('show_success.xml')),
    ])
    assert_that(borador.start_pack(pack_name='akita', x_request_id=x_request_id), success_launch())

    assert_that(
        Waitable(callable=lambda: unistat.unistat(),
                 info=lambda x: f'unistat response: status={x.status_code} text={x.text}'),
        wait_with(
            success_unistat_response(
                all_of(
                    value_of_field('akita_running_ammm', is_(1)),
                )
            )
        )
    )

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
