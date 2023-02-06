# -*- encoding: utf-8 -*-
import pytest

from datetime import date, datetime

from travel.avia.stat_admin.data.models.main import OhmUtm, OhmIncoming, OhmRedirect

from travel.avia.stat_admin.scripts.cohorts_v2.import_utms import is_valid_uid
from travel.avia.stat_admin.scripts.cohorts_v2.calc_cohorts import make_csv_data, make_report_data, read_data


def write_incomings(incoming_data):
    for incoming in incoming_data:
        yandexuid, utm, eventdatetime = incoming
        source, campaign, medium, term = utm

        utm, created = OhmUtm.objects.update_or_create(
            source=source,
            campaign=campaign,
            medium=medium,
            term=term,
        )

        db_incoming = OhmIncoming(
            eventdate=eventdatetime.date(),
            eventdatetime=eventdatetime,
            utm=utm,
            yandexuid=yandexuid,
        )

        db_incoming.save()


def write_redirects(redirect_data):
    for yandexuid, eventdatetime in redirect_data:
        redirect = OhmRedirect(
            eventdate=eventdatetime.date(),
            eventdatetime=eventdatetime,
            yandexuid=yandexuid
        )

        redirect.save()


def test_check_yandexuid():
    uid_data = [
        ["12345678901234567890123456789012", True],
        ["1234567890123456", True],
        ["abc456789012345678901234567890ef", True],
        ["ABC456789012345678901234567890EF", False],
        ["123456789012345678901234567890123", False],
        ["12345678901234567890123456789012;", False],
        ["", False],
    ]

    for uid, check_result in uid_data:
        assert is_valid_uid(uid) == check_result


@pytest.mark.dbuser
def test_use_case_one():
    # https://st.yandex-team.ru/RASPTICKETS-7754 №1 + №2
    yandexuid = "12345678901234567890123456789012"

    utm_one = ["ohm_R1", "campaign", "medium", "term"]
    utm_two = ["ohm_R2", "campaign", "medium", "term"]

    incoming_test_data = [
        [yandexuid, utm_one, datetime(2016, 11, 2, 12, 00)],
        [yandexuid, utm_two, datetime(2016, 11, 6, 12, 00)],
        [yandexuid, utm_two, datetime(2016, 11, 6, 15, 00)],
    ]

    redirect_test_data = [
        [yandexuid, datetime(2016, 11, 2, 12, 10)],
        [yandexuid, datetime(2016, 11, 10, 12, 10)],
        [yandexuid, datetime(2016, 11, 15, 12, 10)],
    ]

    cohorts = 3
    utm_one_cohorts = [1, 0, 0]
    utm_two_cohorts = [1, 1, 0]

    left_date = date(2016, 11, 1)
    right_date = date(2016, 11, 7)

    write_incomings(incoming_test_data)
    write_redirects(redirect_test_data)

    incoming_data, redirects = read_data(left_date, right_date, cohorts)
    utms = make_report_data(incoming_data, redirects, cohorts)
    header, rows = make_csv_data(utms, cohorts)

    expected_rows = [
        utm_one + [sum(utm_one_cohorts)] + utm_one_cohorts,
        utm_two + [sum(utm_two_cohorts)] + utm_two_cohorts,
    ]

    assert sorted(expected_rows) == sorted(rows)


@pytest.mark.dbuser
def test_use_case_two():
    # https://st.yandex-team.ru/RASPTICKETS-7754 №3
    yandexuid = "12345678901234567890123456789012"

    utm_one = ["ohm_R1", "campaign", "medium", "term"]

    incoming_test_data = [
        [yandexuid, utm_one, datetime(2016, 11, 1, 12, 00)],
        [yandexuid, utm_one, datetime(2016, 11, 6, 12, 00)],
    ]

    redirect_test_data = [
        [yandexuid, datetime(2016, 11, 2, 12, 10)],
        [yandexuid, datetime(2016, 11, 2, 12, 15)],

        [yandexuid, datetime(2016, 11, 11, 12, 15)],
        [yandexuid, datetime(2016, 11, 11, 12, 16)],
        [yandexuid, datetime(2016, 11, 11, 12, 17)],
        [yandexuid, datetime(2016, 11, 11, 12, 18)],
        [yandexuid, datetime(2016, 11, 11, 12, 19)],
        [yandexuid, datetime(2016, 11, 11, 12, 20)],
        [yandexuid, datetime(2016, 11, 11, 12, 21)],
    ]

    cohorts = 3
    utm_one_cohorts = [9, 0, 0]

    left_date = date(2016, 11, 1)
    right_date = date(2016, 11, 7)

    write_incomings(incoming_test_data)
    write_redirects(redirect_test_data)

    incoming_data, redirects = read_data(left_date, right_date, cohorts)
    utms = make_report_data(incoming_data, redirects, cohorts)
    header, rows = make_csv_data(utms, cohorts)

    expected_rows = [
        utm_one + [sum(utm_one_cohorts)] + utm_one_cohorts
    ]

    assert rows == expected_rows
