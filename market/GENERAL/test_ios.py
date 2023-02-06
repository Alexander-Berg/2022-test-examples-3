#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import __classic_import     # noqa

import json
import market.mobile_validator.mt.env as env
from market.pylibrary.lite.matcher import Absent, NotEmpty, Contains


class T(env.MobileValidatorSuite):
    uuid = '86a04fcf-6663-4b27-97c9-9a676e8ebdfd'

    @classmethod
    def prepare(cls):
        super().prepare()
        cls.start_device_check(['good_device_check_token'])
        cls.mobile_validator.config.DeviceCheck.Host = 'localhost:{}'.format(cls.device_check.port)
        cls.mobile_validator.config.DeviceCheck.AliveCondition.RequestCount = 3
        cls.mobile_validator.config.DeviceCheck.AliveCondition.CritThreshold = 1

    def test_ios(self):
        response = self.mobile_validator.request_json('pass_request_without_token?platform=ios&version=1.2.3')
        self.assertFragmentIn(response, {
            'pass': False
        })

        response = self._check_ios_token(uuid=self.uuid, token='good_device_check_token', version='1.2')
        self._assert_response_success(response)
        self._check_monitoring_ok()

        self.common_log.expect(Contains('Invalid DeviceCheck token'), 'ERRR')
        response = self._check_ios_token(uuid=self.uuid, token='bad_device_check', version='1.2')
        self._assert_response_failed(response)
        self._check_monitoring_ok()

        self._set_device_check_reply_with_error(True)
        response = self._check_ios_token(uuid=self.uuid, token='bad_device_check_token', version='1.2')
        self._assert_response_success(response)
        self._check_monitoring_crit()

        response = self.mobile_validator.request_json('pass_request_without_token?platform=ios&version=1.2.3')
        self.assertFragmentIn(response, {
            'pass': True
        })
        self._check_monitoring_crit()
        self._set_device_check_reply_with_error(False)

    def _set_device_check_reply_with_error(self, value):
        self.device_check.request_json('change_settings?device-check-with-error={}'.format(value))

    def _assert_response_success(self, response):
        self.assertFragmentIn(response, {
            'jws_token': NotEmpty(),
            'error': Absent()
        })
        self.assertEqual(response.code, 200)

    def _assert_response_failed(self, response):
        self.assertFragmentIn(response, {
            'jws_token': Absent(),
            'error': NotEmpty()
        })
        self.assertEqual(response.code, 400)

    def _get_monitoring(self):
        return self.mobile_validator.request_text('monitoring', fail_on_error=False)

    def _check_monitoring_ok(self):
        self.assertFragmentIn(self._get_monitoring(), '0;OK')

    def _check_monitoring_crit(self):
        self.common_log.expect(Contains('2;DeviceCheck'), 'ERRR')
        self.assertFragmentIn(self._get_monitoring(), '2;DeviceCheck')

    def _check_ios_token(self, uuid, token, version):
        body = json.dumps({
            'uuid': uuid,
            'token': token,
            'version': version
        })
        return self.mobile_validator.request_json('check_ios_token', body=body, fail_on_error=False)


if __name__ == '__main__':
    env.main()
