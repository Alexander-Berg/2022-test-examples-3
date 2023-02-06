import datetime
import os

import pytest
import yatest.common

from crypta.s2s.services.conversions_processor.lib.parsers.crm_api_csv_parser import CrmApiCsvParser
from crypta.s2s.services.conversions_processor.lib.parsers.crm_api_row_parser import CrmApiRowParser
from crypta.s2s.services.conversions_processor.lib.parsers.date_time_parser import DateTimeParser
from crypta.s2s.services.conversions_processor.lib.parsers.fields import Fields
from crypta.s2s.services.conversions_processor.lib.parsers.order_statuses import OrderStatuses
from crypta.s2s.services.conversions_processor.lib.parsers.parsing_error import ParsingError


@pytest.mark.parametrize("date_string,expected_result", [
    pytest.param("21.04.2020", datetime.datetime(year=2020, month=4, day=21, hour=23, minute=59, second=59)),
    pytest.param("21.04.20", datetime.datetime(year=2020, month=4, day=21, hour=23, minute=59, second=59)),
    pytest.param("2020-04-21", datetime.datetime(year=2020, month=4, day=21, hour=23, minute=59, second=59)),

    pytest.param("21.04.2020 11:59:21", datetime.datetime(year=2020, month=4, day=21, hour=11, minute=59, second=21)),
    pytest.param("21.04.2020 11:59", datetime.datetime(year=2020, month=4, day=21, hour=11, minute=59)),
    pytest.param("21.04.20 11:59:21", datetime.datetime(year=2020, month=4, day=21, hour=11, minute=59, second=21)),
    pytest.param("21.04.20 11:59", datetime.datetime(year=2020, month=4, day=21, hour=11, minute=59)),
    pytest.param("2020-04-21 11:59:21", datetime.datetime(year=2020, month=4, day=21, hour=11, minute=59, second=21)),
    pytest.param("2020-04-21 11:59", datetime.datetime(year=2020, month=4, day=21, hour=11, minute=59)),

    pytest.param("1970-01-01 00:00:00", datetime.datetime(year=1970, month=1, day=1)),
])
def test_date_time_parser_positive(date_string, expected_result):
    assert expected_result == DateTimeParser.parse(date_string)


@pytest.mark.parametrize("date_string,expected_error_msg", [
    pytest.param("", "Invalid date string ''"),
    pytest.param("xxx", "Invalid date string 'xxx'"),
    pytest.param("21-04-2020", "Invalid date string '21-04-2020'"),
    pytest.param("21-04-2020 11:59:21", "Invalid date string '21-04-2020 11:59:21'"),

    pytest.param("1969-12-31 23:59:59", "Date '1969-12-31T23:59:59' is less than 1970-01-01"),
])
def test_date_time_parser_negative(date_string, expected_error_msg):
    with pytest.raises(ParsingError, match=expected_error_msg):
        DateTimeParser.parse(date_string)


@pytest.mark.parametrize("row,expected_result", [
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.id_: "11",
            Fields.client_uniq_id: "111",
            Fields.client_ids: "1,4,7",
            Fields.emails: "",
            Fields.phones: "",
            Fields.emails_md5: "",
            Fields.phones_md5: "",
            Fields.order_status: OrderStatuses.in_progress,
            Fields.revenue: "11.00",
            Fields.cost: "44.44",
        },
        {
            Fields.create_date_time: "2020-01-01 23:59:59",
            Fields.id_: "11",
            Fields.client_uniq_id: "111",
            Fields.client_ids: "1,4,7",
            Fields.emails: "",
            Fields.phones: "",
            Fields.emails_md5: "",
            Fields.phones_md5: "",
            Fields.order_status: OrderStatuses.in_progress,
            Fields.revenue: "11.0",
            Fields.cost: "44.44",
        },
        id="valid-client-ids-only",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.id_: "11",
            Fields.client_uniq_id: "111",
            Fields.client_ids: "1,4,7",
            Fields.emails: "xxx@local.local,x.x.x@gmail.com",
            Fields.phones: "+79999999999,81234567890",
            Fields.emails_md5: "3c56ff8fef0f6c65b36b2d25720fe276,8D2B135BA9BD88FBFBD8ECECB31196D1",
            Fields.phones_md5: "C3281A42C6EF3CBAF3508F9738381816,5a893224859c93b3be4c1be4389dc58f",
            Fields.order_status: OrderStatuses.in_progress,
            Fields.revenue: "11.00",
            Fields.cost: "44.44",
        },
        {
            Fields.create_date_time: "2020-01-01 23:59:59",
            Fields.id_: "11",
            Fields.client_uniq_id: "111",
            Fields.client_ids: "1,4,7",
            Fields.emails: "xxx@local.local,xxx@gmail.com",
            Fields.phones: "79999999999,71234567890",
            Fields.emails_md5: "3c56ff8fef0f6c65b36b2d25720fe276,8d2b135ba9bd88fbfbd8ececb31196d1",
            Fields.phones_md5: "c3281a42c6ef3cbaf3508f9738381816,5a893224859c93b3be4c1be4389dc58f",
            Fields.order_status: OrderStatuses.in_progress,
            Fields.revenue: "11.0",
            Fields.cost: "44.44",
        },
        id="valid-full",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.id_: "11",
            Fields.client_uniq_id: "111",
            Fields.client_ids: "1,4,7",
            Fields.revenue: "11.00",
            Fields.cost: "44.44",
        },
        {
            Fields.create_date_time: "2020-01-01 23:59:59",
            Fields.id_: "11",
            Fields.client_uniq_id: "111",
            Fields.client_ids: "1,4,7",
            Fields.emails: "",
            Fields.phones: "",
            Fields.emails_md5: "",
            Fields.phones_md5: "",
            Fields.order_status: OrderStatuses.paid,
            Fields.revenue: "11.0",
            Fields.cost: "44.44",
        },
        id="valid-missing-order-status",
    ),
])
def test_crm_api_row_parser_positive(row, expected_result):
    min_create_date_time = datetime.datetime(year=2020, month=1, day=1, hour=23, minute=59, second=59)
    assert expected_result == CrmApiRowParser(min_create_date_time).parse(row)


@pytest.mark.parametrize("row,expected_error_msg", [
    pytest.param({}, "Missing column 'create_date_time'", id="missing-create-date-time"),
    pytest.param(
        {
            Fields.create_date_time: "",
        },
        "Empty value of column 'create_date_time'",
        id="empty-create-date-time",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01 23:59:58",
        },
        "Row is too old",
        id="old-row",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
        },
        "Missing field with identifiers",
        id="missing-identifiers",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.emails: "xxx,yyy,zzz",
        },
        "Invalid 'email' identifier 'xxx'",
        id="invalid-emails",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.phones: "xxx,yyy,zzz",
        },
        "Invalid 'phone' identifier 'xxx'",
        id="invalid-phones",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.emails_md5: "xxx,yyy,zzz",
        },
        "Invalid 'email_md5' identifier 'xxx'",
        id="invalid-emails-md5",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.phones_md5: "xxx,yyy,zzz",
        },
        "Invalid 'phone_md5' identifier 'xxx'",
        id="invalid-phones-md5",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.emails: "xxx@local.local",
            Fields.revenue: "xxx",
        },
        "Invalid 'revenue' value 'xxx'",
        id="invalid-revenue",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.emails: "xxx@local.local",
            Fields.cost: "xxx",
        },
        "Invalid 'cost' value 'xxx'",
        id="invalid-cost",
    ),
    pytest.param(
        {
            Fields.create_date_time: "2020-01-01",
            Fields.emails: "xxx@local.local",
            Fields.order_status: "xxx",
        },
        "Invalid 'order_status' value 'xxx'",
        id="invalid-order-status",
    ),
])
def test_crm_api_row_parser_negative(row, expected_error_msg):
    min_create_date_time = datetime.datetime(year=2020, month=1, day=1, hour=23, minute=59, second=59)
    with pytest.raises(ParsingError, match=expected_error_msg):
        CrmApiRowParser(min_create_date_time).parse(row)


def test_crm_api_csv_parser_valid():
    source_path = yatest.common.test_source_path("data/valid.csv")
    destination_path = yatest.common.test_output_path("valid.csv")
    errors_path = yatest.common.test_output_path("errors.csv")
    order_status_to_revenue = {
        OrderStatuses.in_progress: 10,
        OrderStatuses.paid: 100,
    }
    min_create_date_time = datetime.datetime(year=2020, month=1, day=1, hour=23, minute=59, second=59)
    crm_api_csv_parser = CrmApiCsvParser(order_status_to_revenue, min_create_date_time)
    assert crm_api_csv_parser.parse(source_path, destination_path, errors_path)
    return {
        "destination": yatest.common.canonical_file(destination_path, local=True),
        "errors": yatest.common.canonical_file(errors_path, local=True),
    }


@pytest.mark.parametrize("filename", [
    "invalid.csv",
    "missing_create_date_time_fieldname.csv",
    "missing_id_fieldnames.csv",
    "unknown_fieldname.csv",
])
def test_crm_api_csv_parser_invalid(filename):
    source_path = yatest.common.test_source_path(os.path.join("data", filename))
    destination_path = yatest.common.test_output_path("valid.csv")
    errors_path = yatest.common.test_output_path("errors.csv")
    order_status_to_revenue = {
        OrderStatuses.in_progress: 10,
        OrderStatuses.paid: 100,
    }
    min_create_date_time = datetime.datetime(year=2020, month=1, day=1, hour=23, minute=59, second=59)
    crm_api_csv_parser = CrmApiCsvParser(order_status_to_revenue, min_create_date_time)
    assert not crm_api_csv_parser.parse(source_path, destination_path, errors_path)
    return {
        "destination": yatest.common.canonical_file(destination_path, local=True),
        "errors": yatest.common.canonical_file(errors_path, local=True),
    }
