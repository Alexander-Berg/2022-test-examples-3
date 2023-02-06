import json
import urlparse
import uuid

import pytest
import responses

from yamarec1.statbox.exceptions import StatboxClientError
from yamarec1.statbox.exceptions import StatboxServerError


def test_client_creates_report(statbox_client, report, settings):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/config")
        statbox_client.create(report, settings, "IamFakeReport")
        assert len(r.calls) == 1
        request = r.calls[0].request
        body = dict(urlparse.parse_qsl(request.body))
        assert body["cube_config"] == settings
        assert body["name"] == statbox_client.report_prefix + report
        assert body["title"] == "IamFakeReport"
        assert request.headers["StatRobotUser"] == "admin"
        assert request.headers["StatRobotPassword"] == "admin"


def test_client_raises_exception_if_report_creation_failed(statbox_client, report, settings):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/config", status=500)
        with pytest.raises(StatboxClientError):
            statbox_client.create(report, settings, "IamFakeReport")
        assert len(r.calls) == 1


def test_client_raises_exception_if_connection_failed(statbox_client, report, settings):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/config", body=StatboxServerError())
        with pytest.raises(StatboxServerError):
            statbox_client.create(report, settings, "IamFakeReport")
        assert len(r.calls) == 1


def test_report_exists(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(r.GET, statbox_client.url + "/_api/report/config")
        assert statbox_client.report_exists(report)


def test_report_does_not_exist(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(r.GET, statbox_client.url + "/_api/report/config", status=500)
        assert not statbox_client.report_exists(report)


def test_client_deletes_report(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/delete_report")
        statbox_client.delete(report)
        assert len(r.calls) == 1
        request = r.calls[0].request
        assert urlparse.parse_qs(request.body)["name"][0] == statbox_client.report_prefix + report
        assert request.headers["StatRobotUser"] == "admin"
        assert request.headers["StatRobotPassword"] == "admin"


def test_client_raises_exception_if_report_deletion_failed(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/delete_report", status=500)
        with pytest.raises(StatboxClientError):
            statbox_client.delete(report)
        assert len(r.calls) == 1


def test_client_truncates_report(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/truncate")
        statbox_client.truncate(report, "d")
        assert len(r.calls) == 1
        request = r.calls[0].request
        body = dict(urlparse.parse_qsl(request.body))
        assert body["name"] == statbox_client.report_prefix + report
        assert body["scale"] == "d"
        assert request.headers["StatRobotUser"] == "admin"
        assert request.headers["StatRobotPassword"] == "admin"


def test_client_raises_exception_if_report_truncation_failed(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/truncate", status=500)
        with pytest.raises(StatboxClientError):
            statbox_client.truncate(report, 'd')
        assert len(r.calls) == 1


def test_client_uploads_data(monkeypatch, statbox_client, report):
    request_id = 42
    monkeypatch.setattr(uuid, "uuid4", lambda: request_id)
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/data")
        status_url = statbox_client.url + "/_v3/meta_storage/add_data_status/%d" % request_id
        r.add(r.GET, status_url, body='{"status":"inprogress"}')
        r.add(r.GET, status_url, body='{"status":"success"}')
        report_data = [{"fielddate": "2016-10-10", "one": "one hello", "two": 42, "three": "three hello"}]
        statbox_client.upload(report, report_data, 'd')
        assert len(r.calls) == 3
        request = r.calls[0].request
        body = dict(urlparse.parse_qsl(request.body))
        all_values = json.loads(body["json_data"])
        assert body["name"] == statbox_client.report_prefix + report
        assert all_values["values"] == report_data
        assert body["uuid"] == "42"
        assert request.headers["StatRobotUser"] == "admin"
        assert request.headers["StatRobotPassword"] == "admin"


def test_client_raises_exception_if_data_uploading_failed(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client.url + "/_api/report/data", status=500)
        report_data = [{"fielddate": "2016-10-10", "one": "one hello", "two": 42, "three": "three hello"}]
        with pytest.raises(StatboxClientError):
            statbox_client.upload(report, report_data, 'd')
        assert len(r.calls) == 1


def test_data_exists_for_day(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(
            r.GET,
            statbox_client.url + '/_api/statreport/json/%s' % statbox_client.report_prefix + report,
            body='{"values": ["fake values"]}'
        )
        assert statbox_client.data_exists(report, "2018-01-01", 'd')
        assert len(r.calls) == 1
        request = r.calls[0].request
        params = dict(urlparse.parse_qsl(urlparse.urlsplit(request.url).query))
        assert params["date_min"] == "2018-01-01"
        assert params["date_max"] == "2018-01-01"
        assert params["scale"] == "d"
        assert params["_raw_data"] == "1"
        assert params["_fill_missing_dates"] == "0"
        assert request.headers["StatRobotUser"] == "admin"
        assert request.headers["StatRobotPassword"] == "admin"


def test_data_does_not_exist_for_day(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(
            r.GET,
            statbox_client.url + '/_api/statreport/json/%s' % statbox_client.report_prefix + report,
            body='{"values": []}'
        )
        assert not statbox_client.data_exists(report, "2018-01-01", 'd')
        assert len(r.calls) == 1


def test_data_exists_for_week(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(
            r.GET,
            statbox_client.url + '/_api/statreport/json/%s' % statbox_client.report_prefix + report,
            body='{"values": ["fake values"]}'
        )
        assert statbox_client.data_exists(report, "2018-01-01", 'w')
        assert len(r.calls) == 1
        request = r.calls[0].request
        params = dict(urlparse.parse_qsl(urlparse.urlsplit(request.url).query))
        assert params["date_min"] == "2018-01-01"
        assert params["date_max"] == "2018-01-01"
        assert params["scale"] == "w"
        assert params["_raw_data"] == "1"
        assert params["_fill_missing_dates"] == "0"
        assert request.headers["StatRobotUser"] == "admin"
        assert request.headers["StatRobotPassword"] == "admin"


def test_data_exists_for_month_by_dimensions(statbox_client, report):
    with responses.RequestsMock() as r:
        r.add(
            r.GET,
            statbox_client.url + '/_api/statreport/json/%s' % statbox_client.report_prefix + report,
            body='{"values": ["fake values"]}'
        )
        assert statbox_client.data_exists(report, "2018-01-01", 'm', {'place': 'Desktop'})
        assert len(r.calls) == 1
        request = r.calls[0].request
        params = dict(urlparse.parse_qsl(urlparse.urlsplit(request.url).query))
        assert params["date_min"] == "2018-01-01"
        assert params["date_max"] == "2018-01-01"
        assert params["scale"] == "m"
        assert params["_raw_data"] == "1"
        assert params["_fill_missing_dates"] == "0"
        assert params["place"] == "Desktop"
        assert request.headers["StatRobotUser"] == "admin"
        assert request.headers["StatRobotPassword"] == "admin"


def test_client_creates_report_with_token(statbox_client_with_token, report, settings):
    with responses.RequestsMock() as r:
        r.add(r.POST, statbox_client_with_token.url + "/_api/report/config")
        statbox_client_with_token.create(report, settings, "IamFakeReport")
        assert len(r.calls) == 1
        request = r.calls[0].request
        body = dict(urlparse.parse_qsl(request.body))
        assert body["cube_config"] == settings
        assert body["name"] == statbox_client_with_token.report_prefix + report
        assert body["title"] == "IamFakeReport"
        assert request.headers["Authorization"] == "OAuth XXXX-token"
