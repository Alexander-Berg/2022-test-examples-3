"""
    Testing yasm api wrapper
"""

import requests_mock

from search.mon.wabbajack.libs.modlib.modules import juggler


class TestGetChecks:
    def setup_class(self):
        self.warn_resp_json_example = ''.join([
            '{"man1-1036.search.yandex.net": ',
            '{"mount_ssd_check": {"": {"status": ["WARN", 1551117770], "client_version": "", ',
            '"description": [{"status": "WARN", "description": "SSD found: sda,sdb, but /ssd located on md5"}, ',
            '1551117770]}}, "rtc_informal_checks": {"": {"status": ["WARN", 1551117782], "client_version": "", ',
            '"description": [{"status": "WARN", "description": "mount_ssd_check, crontab_mails are in WARN state"}, ',
            '1551117782]}}, "crontab_mails": {"": {"status": ["WARN", 1551117770], "client_version": "", ',
            '"description": [{"status": "WARN", "description": "Cron mails dump are exist"}, 1551117770]}}, ',
            '"yasmagent_check_last_requests": {"": {"status": ["WARN", 1551117770], "client_version": "", ',
            '"description": [{"status": "WARN", ',
            '"description": "Can\'t get response from http://localhost:11003/last_requests/"}, 1551117770]}}, ',
            '"rtc_monitoring_checks": {"": {"status": ["WARN", 1551117782], "client_version": "", ',
            '"description": [{"status": "WARN", "description": "yasmagent_check_last_requests are in WARN state"}, ',
            '1551117782]}}}}'
        ])
        self.crit_resp_json_example = ''.join([
            '{"man1-1036.search.yandex.net": ',
            '{"fs_mount_opts": {"": {"status": ["CRIT", 1551117459], "client_version": "", ',
            '"description": [{"status": "CRIT", "description": "/ has no mandatory opts lazytime"}, ',
            '1551117459]}}}',
            '}'
            ])
        self.response = None
        self.host = 'man1-1036.search.yandex.net'
        self.url = ''.join([
            'http://juggler.search.yandex.net:8998/api-slb/events/raw_events?status={level}&host_name={host}&do=1'
            ])

        with requests_mock.Mocker() as m:
            m.register_uri(
                'GET', self.url.format(level='CRIT', host=self.host), status_code=200, json=self.crit_resp_json_example
            )
            self.test_response_crit = juggler.get_events(host=self.host, level='CRIT')

        with requests_mock.Mocker() as m:
            m.register_uri(
                'GET', self.url.format(level='WARN', host=self.host), status_code=200, json=self.warn_resp_json_example
            )
            self.test_response_warn = juggler.get_events(host=self.host, level='WARN')

    def test_return_type(self):
        assert isinstance(self.test_response_crit, str)
        assert isinstance(self.test_response_warn, str)

    def test_return_value(self):
        assert self.test_response_crit == self.crit_resp_json_example
        assert self.test_response_warn == self.warn_resp_json_example

    def tear_down(self):
        pass
