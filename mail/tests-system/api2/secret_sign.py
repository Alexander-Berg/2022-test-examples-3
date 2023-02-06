from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import json
import time

def setUp(self):
    global xiva
    xiva = XivaApiV2(host='localhost', port=18083)

def tearDown(self):
    pass

class TestSecretSign:
    def test_get_sign_fails_with_bad_token(self):
        resp = xiva.secret_sign(token = 'L00X', uid = 'uid1', service = 'tst1')
        assert_unauthorized(resp, "bad token")

    def test_get_sign_fails_with_bad_token_multiauth(self):
        resp = xiva.secret_sign(token = 'L001,1234567890123456789X', uid = 'uid1', service = 'tst1,disk-json')
        assert_unauthorized(resp, "disk-json")
        resp = xiva.secret_sign(token = '1234567890123456789X,L001', uid = 'uid1', service = 'disk-json,tst1')
        assert_unauthorized(resp, "disk-json")
        resp = xiva.secret_sign(token = '1234567890123456789X,L00X', uid = 'uid1', service = 'disk-json,tst1')
        assert_unauthorized(resp, "disk-json, tst1")
        resp = xiva.secret_sign(token = 'bass12345X,L001,L12345X', uid = '200', service = 'bass,tst1,autoru')
        assert_unauthorized(resp, "autoru, bass")

    def test_get_sign_fails_with_bad_token_multitopic(self):
        resp = xiva.secret_sign(token = 'L00X', service = 'tst1', uid = 'uid1', topic = 'a,b,c,d,e,f')
        assert_unauthorized(resp, "bad token")

    def test_multiauth_compatibility_with_multiservice_token(self):
        resp = xiva.secret_sign(token = 'L001,12345678901234567890', uid = 'uid1', service = 'tst1,disk-json,tst2')
        assert_ok(resp)

    def test_xtoken_in_header(self):
        resp = xiva.GET(xiva.prepare_url('/v2/secret_sign?', uid = 'uid1', service = 'tst1'),
            {'Authorization': 'Xiva L001'})
        assert_ok(resp)

    def test_xtoken_in_header_multiauth(self):
        resp = xiva.GET(xiva.prepare_url('/v2/secret_sign?', uid = 'uid1', service = 'tst1,disk-json'),
            {'Authorization': 'Xiva L001,12345678901234567890'})
        assert_ok(resp)
        resp = xiva.GET(xiva.prepare_url('/v2/secret_sign?', uid = 'uid1', service = 'disk-json,tst1'),
            {'Authorization': 'Xiva 12345678901234567890,L001'})
        assert_ok(resp)
        resp = xiva.GET(xiva.prepare_url('/v2/secret_sign?', uid = 'uid1', service = 'disk-json,tst1'),
            {'Authorization': 'Xiva L001,12345678901234567890'})
        assert_ok(resp)

    def test_xtoken_in_header_multitopic(self):
        resp = xiva.GET(xiva.prepare_url('/v2/secret_sign?', service = 'disk-json', uid = 'uid1', topic = 't2,t4'),
            {'Authorization': 'Xiva 12345678901234567890'})
        assert_ok(resp)

    def test_content_type(self):
        resp = xiva.secret_sign(**{ 'token' : 'L001', 'uid' : '200', 'service' : 'tst1' })
        assert_ok(resp)
        assert_in('content-type', resp.headers)
        assert_equals(resp.headers['content-type'], 'application/json')

    def test_content_type_multiauth(self):
        resp = xiva.secret_sign(**{ 'token' : 'bass123456,L001,L123456', 'uid' : '200', 'service' : 'bass,tst1,autoru' })
        assert_ok(resp)
        assert_in('content-type', resp.headers)
        assert_equals(resp.headers['content-type'], 'application/json')

    def test_content_type_multitopic(self):
        resp = xiva.secret_sign(token = 'L001', service = 'tst1', uid = 'uid123', topic = 't2,t3,t4')
        assert_ok(resp)
        assert_in('content-type', resp.headers)
        assert_equals(resp.headers['content-type'], 'application/json')

    def test_get_sign_fails_if_forbidden_service(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', service = 'tst')
        assert_unauthorized(resp, "forbidden service")
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', service = 'tst1,tst')
        assert_unauthorized(resp, "forbidden service")
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', service = 'tst,tst1')
        assert_unauthorized(resp, "forbidden service")

    def test_get_sign_returns_sign_and_ts_on_success(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', service = 'tst1')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        assert_in('sign', json_result)
        assert_in('ts', json_result)

    def test_get_sign_returns_sign_and_ts_on_success_multiauth(self):
        resp = xiva.secret_sign(token = 'L123456,L001', uid = 'uid1', service = 'autoru,tst1')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        assert_in('sign', json_result)
        assert_in('ts', json_result)

    def test_get_sign_returns_sign_and_ts_on_success_multitopic(self):
        resp = xiva.secret_sign(token = 'L001', service = 'tst1', uid = 'uid123', topic = 't1,t2,t3,t4')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        assert_in('sign', json_result)
        assert_in('ts', json_result)

    def test_get_sign_returns_valid_sign(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', service = 'tst1')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1', service = 'tst1', sign = json_result['sign'], ts = json_result['ts'] )
        assert_ok(resp)

    def test_get_sign_returns_valid_sign_multiauth(self):
        resp = xiva.secret_sign(token = 'L001,bass123456', uid = 'uid1', service = 'tst1,bass')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001,bass123456', uid = 'uid1', service = 'tst1,bass', sign = json_result['sign'], ts = json_result['ts'] )
        assert_ok(resp)

    def test_get_sign_returns_valid_sign_multitopic(self):
        resp = xiva.secret_sign(token = 'L001', service = 'tst1', uid = 'uid123', topic = 't1,t2,t3,t4,t5')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001', service = 'tst1', uid = 'uid123', topic = 't1,t2,t3,t4,t5', sign = json_result['sign'], ts = json_result['ts'] )
        assert_ok(resp)

    def test_get_sign_validation_fails_with_another_uid_or_service(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', service = 'tst1')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1', service = 'tst2', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid2', service = 'tst1', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1,uid2', service = 'tst1', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1', service = 'tst1,tst2', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')

    def test_get_sign_validation_fails_with_another_uid_or_service_multiauth(self):
        resp = xiva.secret_sign(token = 'L001,L123456', uid = 'uid1', service = 'tst1,autoru')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001,L123456', uid = 'uid1', service = 'tst2,autoru', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001,L123456', uid = 'uid2', service = 'tst1,autoru', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001,L123456', uid = 'uid1,uid2', service = 'tst1,autoru', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1', service = 'tst1', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')

    def test_get_sign_validation_fails_with_another_topics_multitopic(self):
        resp = xiva.secret_sign(token = 'L001', service = 'tst1', uid = 'uid1', topic = 't1,t2,t3,t4,t5')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001', service = 'tst1', uid = 'uid1', topic = 't1,t2,t3,t4', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001', service = 'tst1', uid = 'uid1', topic = 't2,t3,t4,t5', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')
        resp = xiva.verify_secret_sign(token = 'L001', service = 'tst1', uid = 'uid1', topic = 't1,t2,t3,t4,t5,t6', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, 'bad sign')

    def test_validate_expired_ts_sign_fails_with_401(self):
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1', service = 'tst1', sign = "fake", ts="111" )
        assert_unauthorized(resp, 'bad sign')

    def test_fails_on_incorrect_uid(self):
        samples = ['#', 'a#', 'a894%-24', '-', '.', '@', ':', '=', '!', 'adf@.s!']
        for s in samples:
            resp = xiva.secret_sign(token = 'L001', uid = s, service = 'tst1')
            assert_bad_request(resp, 'invalid argument "uid"')

    def test_fails_on_empty_uid(self):
        resp = xiva.secret_sign(token = 'L001', uid = '', service = 'tst1')
        assert_bad_request(resp, 'missing argument "user (uid)"')

    def test_fails_on_empty_uid_multiauth(self):
        resp = xiva.secret_sign(token = 'L123456,L001', uid = '', service = 'autoru,tst1')
        assert_bad_request(resp, 'missing argument "user (uid)"')

    def test_accepts_allowed_characters_in_uid(self):
        samples = ['a', 'a.', 'a9', '0', '0a', '0@a', '0.3.3', 'fdf.sdf.df.']
        for s in samples:
            resp = xiva.secret_sign(token = 'L001', uid = s, service = 'tst1')
            assert_ok(resp)

    def test_accepts_allowed_characters_in_uid_multiauth(self):
        samples = ['a', 'a.', 'a9', '0', '0a', '0@a', '0.3.3', 'fdf.sdf.df.']
        for s in samples:
            resp = xiva.secret_sign(token = 'L001,bass123456', uid = s, service = 'tst1,bass')
            assert_ok(resp)

    def test_accepts_allowed_characters_in_topic(self):
        samples = ['a', 'a.', 'a9', '0', '0a', '0@a', '0.3.3', 'fdf:sdf.df:']
        for s in samples:
            resp = xiva.secret_sign(token = 'L001', topic = s, uid = "test", service = 'tst1')
            assert_ok(resp)

    def test_validate_fails_with_topic_if_generated_without_topic(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', service = 'tst1')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1', topic="mytopic", service = 'tst1', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, "bad sign")

    def test_validate_fails_without_topic_if_generated_with_topic(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', topic="mytopic", service = 'tst1')
        assert_ok(resp)
        json_result = json.loads(resp.body)
        resp = xiva.verify_secret_sign(token = 'L001', uid = 'uid1', service = 'tst1', sign = json_result['sign'], ts = json_result['ts'] )
        assert_unauthorized(resp, "bad sign")

    def test_get_sign_fails_with_topic_and_miltiple_users(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1,uid2', topic="mytopic", service = 'tst1')
        assert_bad_request(resp, "request with topic should contain only one user id")

    def test_get_sign_fails_with_topic_and_multiple_services(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid1', topic="mytopic", service = 'tst1,tst2')
        assert_bad_request(resp, "request with topic should contain only one service")

    def test_get_sign_with_small_ts_gives_sign_min_ts(self):
            utsnow = int(time.time())
            resp = xiva.secret_sign(token = 'L001', uid = 'uid1',
                service = 'tst1', ts = utsnow)
            assert_ok(resp)
            assert_equals(json.loads(resp.body)["ts"], str(utsnow + 2))

    def test_get_sign_accepts_custom_ts(self):
            utsnow = int(time.time())
            resp = xiva.secret_sign(token = 'L001', uid = 'uid1',
                service = 'tst1', ts = utsnow + 200)
            assert_ok(resp)
            assert_equals(json.loads(resp.body)["ts"], str(utsnow + 200))

    def test_get_sign_fails_with_serarator_in_uid(self):
        resp = xiva.secret_sign(token = 'L001', uid = 'uid:1', service = 'tst1')
        assert_bad_request(resp, 'uid can\'t contain ":"')
        resp = xiva.secret_sign(token = 'L123456', uid = 'uid:1', service = 'autoru')
        assert_ok(resp)

    def test_tvm_subscriber_can_subscribe(self):
        resp = xiva.secret_sign(tvm_ticket=xiva.subscriber_tst_ticket,
            uid = '123', service = 'mail,bass')
        assert_ok(resp)

    def test_tvm_subscriber_cannot_subscribe_to_random_service(self):
        resp = xiva.secret_sign(tvm_ticket=xiva.subscriber_tst_ticket,
            uid = '123', service = 'tst1,bass')
        assert_unauthorized(resp, 'forbidden service')

    def test_tvm_publisher_cannot_subscribe(self):
        resp = xiva.secret_sign(tvm_ticket=xiva.publisher_tst_ticket,
            uid = '123', service = 'mail,bass')
        assert_unauthorized(resp, 'forbidden service')
