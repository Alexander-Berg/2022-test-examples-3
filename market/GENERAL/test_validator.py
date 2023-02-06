#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import hashlib
import time

try:
    import jwt
except ImportError:
    from contrib.python.PyJWT import jwt

import market.mobile_validator.mt.env as env

from market.mobile_validator.mt.env.jwt import generate_token as generate_jwt_token
from market.mobile_validator.mt.env import safety_net
from market.pylibrary.lite.matcher import Absent, Capture, CaptureTo, Contains, NotEmpty


class T(env.MobileValidatorSuite):
    @classmethod
    def prepare(cls):
        super().prepare()
        cls.mobile_validator.config.SafetyNetMonitoring.AliveCondition.RequestCount = 1000

    def test_validation(self):
        uuid = '86a04fcf-6663-4b27-97c9-9a676e8ebdfd'
        nonce = self._generate_and_check_nonce(uuid=uuid)

        token = safety_net.generate_token(nonce=nonce, certs=self.certs, private_key=self.cert_private_key,
                                          apk_package_name=self.apk_package_name, apk_certificate_digest_sha_256=self.apk_certificate_digest_sha_256)
        response = self.mobile_validator.request_json(
            'check_android_token',
            body='uuid={uuid}&nonce={nonce}&token={token}&version=fake-version'.format(uuid=uuid, nonce=nonce, token=token)
        )
        jwt = Capture()
        self.assertFragmentIn(response, {
            'jws_token': CaptureTo(jwt)
        })

        response = self.mobile_validator.request_json(
            'check_jws_token',
            body='uuid={uuid}&token={jwt}'.format(uuid=uuid, jwt=jwt.value)
        )
        self.assertFragmentIn(response, {
            'status': 'success'
        })

    def test_invalid_android_token(self):
        uuid = '86a04fcf-6663-4b27-97c9-9a676e8ebdfd'
        nonce = self._generate_and_check_nonce(uuid=uuid)

        fake_nonce_secret = 'fake secret'
        timestamp = '1599484826'
        fake_nonce = hashlib.md5(uuid.encode() + fake_nonce_secret.encode() + timestamp.encode()).hexdigest() + ':' + timestamp

        default_apk_params = {'apk_package_name': self.apk_package_name, 'apk_certificate_digest_sha_256': self.apk_certificate_digest_sha_256}
        bad_tokens = (
            safety_net.generate_token(nonce=nonce, certs=[], private_key=self.cert_private_key, **default_apk_params),
            safety_net.generate_token(nonce=nonce, certs=self.certs[1:], private_key=self.cert_private_key, **default_apk_params),
            safety_net.generate_token(nonce=fake_nonce, certs=self.certs, private_key=self.cert_private_key, **default_apk_params),
            safety_net.generate_token(nonce=self._generate_expired_nonce(uuid=uuid), certs=self.certs, private_key=self.cert_private_key, **default_apk_params),
            safety_net.generate_token(nonce=nonce, certs=self.certs, private_key=self.cert_private_key,
                                      apk_package_name='fake package name', apk_certificate_digest_sha_256='fake sha')

        )
        for token in bad_tokens:
            self._check_that_check_android_token_fails(uuid=uuid, nonce=nonce, token=token)

    def test_invalid_jws_token(self):
        uuid = '86a04fcf-6663-4b27-97c9-9a676e8ebdfd'
        jwt = self._generate_expired_jwt(uuid)
        self._check_that_check_jws_token_fails(uuid, jwt)

    def test_emergency_disabling(self):
        uuid = '86a04fcf-6663-4b27-97c9-9a676e8ebdfd'

        self.set_its({'android_verification': {
            'enabled': False
        }})
        self._check_that_check_android_token_passes(uuid=uuid, nonce='fake nonce', token='fake token', version='fake-version')

        self.set_its({'android_verification': {
            'enabled': True,
            'disabled_versions': ['192.104']
        }})
        self._check_that_check_android_token_passes(uuid=uuid, nonce='fake nonce', token='fake token', version='192.104')
        self._check_that_check_android_token_fails(uuid=uuid, nonce='fake nonce', token='fake token', version='fake-version')

        self.set_its({'android_verification': {
            'enabled': True,
            'min_version': '10.20-debug'
        }})
        for ver in ('10.19', '10.19.300', '10-debug', '9'):
            self._check_that_check_android_token_passes(uuid=uuid, nonce='fake nonce', token='fake token', version=ver)
        for ver in ('10.20', '10.20-test', '10.21'):
            self._check_that_check_android_token_fails(uuid=uuid, nonce='fake nonce', token='fake token', version=ver)

        self.set_its({})

    def test_additional_jwt_token_fields(self):
        uuid = '86a04fcf-6663-4b27-97c9-9a676e8ebdfd'
        nonce = self._generate_and_check_nonce(uuid=uuid)

        token = safety_net.generate_token(nonce=nonce, certs=self.certs, private_key=self.cert_private_key,
                                          apk_package_name=self.apk_package_name, apk_certificate_digest_sha_256=self.apk_certificate_digest_sha_256,
                                          cts_profile_match=True, basic_integrity=False, evaluation_type='HARDWARE_BACKED')
        response = self.mobile_validator.request_json(
            'check_android_token',
            body='uuid={uuid}&nonce={nonce}&token={token}&version=fake-version'.format(uuid=uuid, nonce=nonce, token=token)
        )
        jwt_token = Capture()
        self.assertFragmentIn(response, {
            'jws_token': CaptureTo(jwt_token)
        })

        response = self.mobile_validator.request_json(
            'check_jws_token',
            body='uuid={uuid}&token={jwt}'.format(uuid=uuid, jwt=jwt_token.value)
        )
        self.assertFragmentIn(response, {
            'status': 'success'
        })
        self.common_log.expect(Contains(f'basicIntegrity is false, uuid={uuid}'), 'ERRR')

        decoded_jwt = jwt.decode(jwt_token.value, self.jwt_secret, algorithms=['HS256'])
        assert decoded_jwt['ctsProfileMatch']
        assert not decoded_jwt['basicIntegrity']
        assert decoded_jwt['evaluationType'] == 'HARDWARE_BACKED'

    def _generate_and_check_nonce(self, uuid):
        response = self.mobile_validator.request_json('generate_nonce?uuid={uuid}'.format(uuid=uuid))
        nonce = Capture()
        self.assertFragmentIn(response, {
            'nonce': CaptureTo(nonce)
        })
        self._check_nonce(uuid, nonce.value)
        return nonce.value

    def _check_nonce(self, uuid, nonce):
        timestamp_pos = nonce.rfind(':')
        self.assertTrue(timestamp_pos != -1)
        timestamp = nonce[timestamp_pos + 1:]
        self.assertTrue(timestamp.isdigit())

        expected_nonce = hashlib.md5(uuid.encode() + self.nonce_secret.encode() + timestamp.encode()).hexdigest() + ':' + timestamp
        self.assertEqual(nonce, expected_nonce)

    def _generate_expired_nonce(self, uuid):
        current_timestamp = int(time.time())
        timestamp = str(current_timestamp - self.nonce_expiration_time - 1)
        return hashlib.md5(uuid.encode() + self.nonce_secret.encode() + timestamp.encode()).hexdigest() + ':' + timestamp

    def _check_that_check_android_token_passes(self, uuid, nonce, token, version='fake-version'):
        response = self.mobile_validator.request_json(
            'check_android_token',
            body='uuid={uuid}&nonce={nonce}&token={token}&version={version}'.format(uuid=uuid, nonce=nonce, token=token, version=version)
        )
        self.assertFragmentIn(response, {
            'jws_token': NotEmpty(),
            'error': Absent()
        })

    def _check_that_check_android_token_fails(self, uuid, nonce, token, version='fake-version'):
        self.common_log.expect(Contains(''), 'ERRR')
        response = self.mobile_validator.request_json(
            'check_android_token',
            body='uuid={uuid}&nonce={nonce}&token={token}&version={version}'.format(uuid=uuid, nonce=nonce, token=token, version=version),
            fail_on_error=False
        )

        self.assertEqual(response.code, 400)

        error = Capture()
        self.assertFragmentIn(response, {
            'error': CaptureTo(error)
        })
        self.assertTrue(error.value)

        token = Capture()
        self.assertFragmentNotIn(response, {
            'jws_token': CaptureTo(token)
        })

    def _generate_expired_jwt(self, uuid):
        expires_at = int(time.time()) - 1
        return generate_jwt_token(uuid=uuid, expires_at=expires_at, secret=self.jwt_secret)

    def _check_that_check_jws_token_fails(self, uuid, jwt):
        self.common_log.expect(Contains('Invalid jws token'), 'ERRR')
        response = self.mobile_validator.request_json(
            'check_jws_token',
            body='uuid={uuid}&token={jwt}'.format(uuid=uuid, jwt=jwt),
            fail_on_error=False
        )
        self.assertEqual(response.code, 400)
        error = Capture()
        self.assertFragmentIn(response, {
            'status': 'failed',
            'error': CaptureTo(error)
        })
        self.assertTrue(error.value)


if __name__ == '__main__':
    env.main()
