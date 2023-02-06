#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import __classic_import     # noqa

import time
import market.mobile_validator.mt.env as env
from market.mobile_validator.mt.env import mock_server
from market.pylibrary.lite.matcher import Capture, CaptureTo, Contains


class T(env.MobileValidatorSuite):
    @classmethod
    def prepare(cls):
        super().prepare()

        cls.mock_safety_net = mock_server.SafetyNet()
        cls.mock_safety_net.start()

        cls.mobile_validator.config.SafetyNetPinger.AliveCondition.RequestCount = 3
        cls.mobile_validator.config.SafetyNetPinger.AliveCondition.CritThreshold = 1
        cls.mobile_validator.config.SafetyNetPinger.PingInterval = 50
        cls.mobile_validator.config.SafetyNetPinger.PingUrls.append('localhost:{}'.format(cls.mock_safety_net.port))

        cls.mobile_validator.config.SafetyNetMonitoring.AliveCondition.RequestCount = 1000000000
        cls.mobile_validator.config.SafetyNetMonitoring.AliveCondition.CritThreshold = 1000000000

    def test_pinger(self):
        self._check_monitoring_ok()
        self.assertFalse(self.passes_request_without_token())

        self.mock_safety_net.respond = False
        while not self.passes_request_without_token():
            time.sleep(0.1)
        self._check_monitoring_crit()

        self.mock_safety_net.respond = True
        while self.passes_request_without_token():
            time.sleep(0.1)
        self._check_monitoring_ok()

    def passes_request_without_token(self):
        response = self.mobile_validator.request_json('pass_request_without_token?platform=android&version=some-version')
        pass_value = Capture()
        self.assertFragmentIn(response, {'pass': CaptureTo(pass_value)})
        return pass_value.value

    def _get_monitoring(self):
        return self.mobile_validator.request_text('monitoring', fail_on_error=False)

    def _check_monitoring_ok(self):
        self.assertFragmentIn(self._get_monitoring(), '0;OK')

    def _check_monitoring_crit(self):
        self.common_log.expect(Contains('2;SafetyNetPinger'), 'ERRR')
        self.assertFragmentIn(self._get_monitoring(), '2;SafetyNetPinger')


if __name__ == '__main__':
    env.main()
