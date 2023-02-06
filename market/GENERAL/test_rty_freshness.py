import pytest
import mock
import time

import rty_freshness
import test_common


RESPONSE_TEMPLATE = '''<?xml version="1.0" encoding="utf-8"?><admin-action>
<report>2022.1.92.0</report>
<revision>9101411</revision>
<host>sas6-1718-837-sas-market-prod--552-17050.gencfg-c.yandex.net</host>
<market-indexer-version>2022.1.979.0</market-indexer-version>
<mbo-stuff>20220202_0457</mbo-stuff>
<index-generation>20220203_1135</index-generation>
<regional_delivery_fb></regional_delivery_fb>
<num-offers>378326290</num-offers>
<num-blue-offers>28272406</num-blue-offers>
<report-status>{report_status}</report-status>
<report-lockdown></report-lockdown>
<report-safe-mode>0</report-safe-mode>
<dssm>0.8645999</dssm>
<formulas>0.9098602</formulas>
<color>white</color>
<report-cpu-usage>1.07499</report-cpu-usage>
<report-cpu-limit>23.37378471</report-cpu-limit>
<report-stats>
    <last-rty-document-freshness>{freshness_ts}</last-rty-document-freshness><dynamic-data>
    <cpashopfilter timestamp="2022-02-03T11:15:12.000000Z">#33458225</cpashopfilter>
    <cpcshopfilter timestamp="2022-02-03T11:15:12.000000Z">#33458225</cpcshopfilter>
    <offerfilter timestamp="2022-02-03T11:15:12.000000Z">#33458225</offerfilter>
    </dynamic-data><rty-backup><index ts="1643885917" time="Thu, 03 Feb 2022 13:58:37 MSK"/>
    <backup_in_progress>False</backup_in_progress>
    <was_restore>False</was_restore></rty-backup>
    <dynamic-rollback><qpromos_filter timestamp="2022-02-03T08:35:00.000000Z"></qpromos_filter></dynamic-rollback>
</report-stats>
'''


def assert_ok(capture):
    assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-rty-freshness;0;Ok\n'
    assert capture.get_stderr() == ''


@pytest.mark.usefixtures('indigo_cluster')
def test_ignore_indigo_clusters():
    with test_common.OutputCapture() as capture:
        rty_freshness.main()
        assert_ok(capture)


def test_cannot_reach_report():
    with mock.patch('rty_freshness.requst_versions', side_effect=Exception):
        with test_common.OutputCapture() as capture:
            rty_freshness.main()
            assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-null;0;Ok\n'
            assert capture.get_stderr() == ''


def test_ignore_closed_clusters():
    with mock.patch(
        'rty_freshness.requst_versions',
        return_value=RESPONSE_TEMPLATE.format(report_status='CLOSED_CONSISTENT_MANUAL_OPENING', freshness_ts=0),
    ):
        with test_common.OutputCapture() as capture:
            rty_freshness.main()
            assert_ok(capture)


def test_ok_when_open_and_fresh():
    ts = int(time.time() - 10)
    with mock.patch(
        'rty_freshness.requst_versions',
        return_value=RESPONSE_TEMPLATE.format(report_status='OPENED_CONSISTENT', freshness_ts=ts),
    ):
        with test_common.OutputCapture() as capture:
            rty_freshness.main()
            assert_ok(capture)


def test_crit_when_open_with_lag():
    ts = int(time.time() - 400)
    with mock.patch(
        'rty_freshness.requst_versions',
        return_value=RESPONSE_TEMPLATE.format(report_status='OPENED_CONSISTENT', freshness_ts=ts),
    ):
        with test_common.OutputCapture() as capture:
            rty_freshness.main()
            assert 'PASSIVE-CHECK:market-report-rty-freshness;2;Лаг чтения:' in capture.get_stdout()
            assert capture.get_stderr() == ''
