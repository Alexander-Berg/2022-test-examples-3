#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import __classic_import     # noqa

import market.mobile_validator.mt.env as env

from market.mobile_validator.mt.env import safety_net
from market.pylibrary.lite.matcher import Capture, CaptureTo, Contains


class T(env.MobileValidatorSuite):
    uuid = '86a04fcf-6663-4b27-97c9-9a676e8ebdfd'
    _good_nonce_value = None
    _good_token_value = None

    @classmethod
    def prepare(cls):
        super().prepare()
        cls.mobile_validator.config.SafetyNetMonitoring.AliveCondition.RequestCount = 5
        cls.mobile_validator.config.SafetyNetMonitoring.AliveCondition.CritThreshold = 3
        cls.mobile_validator.config.SafetyNetMonitoring.AliveCondition.WarnThreshold = 2
        cls.nonce_expiration_time = 600

    def test_safety_net_monitoring(self):
        self._check_monitoring_ok()

        for _ in range(2):
            self._check_pass_request_without_token(False)
        self._check_monitoring_warn()
        self._check_pass_request_without_token(False)

        self._check_pass_request_without_token(True)
        self._check_request_with_bad_token_passes()
        self._check_monitoring_crit()

        for _ in range(3):
            self._make_request_with_good_token()
        self._check_monitoring_warn()
        self._make_request_with_good_token()

        self._check_monitoring_ok()
        for _ in range(2):
            self._check_request_with_bad_token_fails()
        self._check_request_with_bad_token_fails(token='')

        self._check_monitoring_crit()
        self._check_request_with_bad_token_passes(token='')
        self._check_pass_request_without_token(True)

    def _generate_nonce(self, uuid):
        response = self.mobile_validator.request_json('generate_nonce?uuid={uuid}'.format(uuid=uuid))
        nonce = Capture()
        self.assertFragmentIn(response, {
            'nonce': CaptureTo(nonce)
        })
        return nonce.value

    def _check_pass_request_without_token(self, expected_value):
        response = self.mobile_validator.request_json('pass_request_without_token?platform=android&version=some-version')
        self.assertFragmentIn(response, {'pass': expected_value})

    def _check_request_with_bad_token_passes(self, token='fake-token'):
        self.common_log.expect(Contains(''), 'ERRR')
        response = self.mobile_validator.request_json('check_android_token', body='uuid=fake-uid&nonce=fake-nonce&token={}&version=fake-version'.format(token))
        jwt = Capture()
        self.assertFragmentIn(response, {'jws_token': CaptureTo(jwt)})
        self.assertTrue(jwt.value)

    def _check_request_with_bad_token_fails(self, token='fake-token'):
        self.common_log.expect(Contains(''), 'ERRR')
        response = self.mobile_validator.request_json('check_android_token', body='uuid=fake-uid&nonce=fake-nonce&token={}&version=fake-version'.format(token), fail_on_error=False)
        self.assertEqual(response.code, 400)
        error = Capture()
        self.assertFragmentIn(response, {'error': CaptureTo(error)})
        self.assertTrue(error.value)

    @property
    def _good_nonce(self):
        if self._good_nonce_value is None:
            self._good_nonce_value = self._generate_nonce(self.uuid)
        return self._good_nonce_value

    @property
    def _good_token(self):
        if self._good_token_value is None:
            self._good_token_value = safety_net.generate_token(
                nonce=self._good_nonce, certs=self.certs, private_key=self.cert_private_key,
                apk_package_name=self.apk_package_name, apk_certificate_digest_sha_256=self.apk_certificate_digest_sha_256
            )
        return self._good_token_value

    def _make_request_with_good_token(self):
        jwt = Capture()
        response = self.mobile_validator.request_json(
            'check_android_token',
            body='uuid={uuid}&nonce={nonce}&token={token}&version=fake-version'.format(
                uuid=self.uuid, nonce=self._good_nonce, token=self._good_token)
        )
        self.assertFragmentIn(response, {'jws_token': CaptureTo(jwt)})
        self.assertTrue(jwt.value)

    def _get_monitoring(self):
        return self.mobile_validator.request_text('monitoring', fail_on_error=False)

    def _check_monitoring_ok(self):
        self.assertFragmentIn(self._get_monitoring(), '0;OK')

    def _check_monitoring_warn(self):
        self.assertFragmentIn(self._get_monitoring(), '1;SafetyNetMonitoring')

    def _check_monitoring_crit(self):
        self.common_log.expect(Contains('2;SafetyNetMonitoring'), 'ERRR')
        self.assertFragmentIn(self._get_monitoring(), '2;SafetyNetMonitoring')


if __name__ == '__main__':
    env.main()
