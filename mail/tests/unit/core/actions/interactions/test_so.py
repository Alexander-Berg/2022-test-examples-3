import pytest

from mail.payments.payments.core.actions.interactions.so import SenderLetterIsSpamAction
from mail.payments.payments.interactions.so import SoClient
from mail.payments.payments.utils.helpers import md5_hex


class TestFlatContext:
    @pytest.fixture(params=(True, False))
    def hash_keys(self, request):
        return request.param

    @pytest.fixture(params=('', '{'))
    def key_prefix(self, request):
        return request.param

    @pytest.fixture(params=('', '}'))
    def key_suffix(self, request):
        return request.param

    @pytest.fixture
    def key(self, hash_keys):
        def _inner(value):
            return md5_hex(value) if hash_keys else value

        return _inner

    @pytest.fixture
    def key_for_context(self, hash_keys, key_prefix, key_suffix):
        def _inner(value):
            return f'{key_prefix}{md5_hex(value) if hash_keys else value}{key_suffix}'

        return _inner

    @pytest.fixture
    def context(self):
        return {
            "a": "b",
            "c": "d",
            "e": {
                "h": 1,
                "j": "k",
                "l": {
                    "m": "n",
                    "o": ["p", "q", "r"]
                }
            },
            "s": ["t", "u"],
            "w": "x",
            "y": {"z": "aa"},
            "ab": [
                {"ac": "ad"},
                {"ac": "ae"},
                {"ac": "af"},
                {"ac": "ag"}
            ]
        }

    @pytest.fixture
    def fields_context(self, key, key_for_context):
        return {
            "a": key_for_context("a"),
            "c": key_for_context("c"),
            "e": {
                "h": key_for_context("e.h"),
                "j": key_for_context("e.j"),
                "l": {
                    "m": key_for_context("e.l.m"),
                    "o": [
                        key_for_context("e.l.o.0"),
                        key_for_context("e.l.o.1"),
                        key_for_context("e.l.o.2")
                    ]
                }
            },
            "s": [
                key_for_context("s.0"),
                key_for_context("s.1")
            ],
            "w": key_for_context("w"),
            "y": {
                "z": key_for_context("y.z")
            },
            "ab": [
                {
                    "ac": key_for_context("ab.0.ac")
                },
                {
                    "ac": key_for_context("ab.1.ac")
                },
                {
                    "ac": key_for_context("ab.2.ac")
                },
                {
                    "ac": key_for_context("ab.3.ac")
                }
            ]
        }

    @pytest.fixture
    def fields(self, key):
        return {
            key("a"): "b",
            key("c"): "d",
            key("e.h"): 1,
            key("e.j"): "k",
            key("e.l.m"): "n",
            key("e.l.o.0"): "p",
            key("e.l.o.1"): "q",
            key("e.l.o.2"): "r",
            key("s.0"): "t",
            key("s.1"): "u",
            key("w"): "x",
            key("y.z"): "aa",
            key("ab.0.ac"): "ad",
            key("ab.1.ac"): "ae",
            key("ab.2.ac"): "af",
            key("ab.3.ac"): "ag"
        }

    @pytest.fixture
    def returned(self, context, hash_keys, key_prefix, key_suffix):
        return SenderLetterIsSpamAction._flat_context(context,
                                                      hash_keys=hash_keys,
                                                      key_prefix=key_prefix,
                                                      key_suffix=key_suffix)

    def test_flat(self, returned, fields_context, fields):
        returned_fields_context, returned_fields = returned
        assert all((
            fields_context == returned_fields_context,
            fields == returned_fields,
        ))


class TestSenderLetterIsSpam:
    @pytest.fixture(params=(True, False))
    def verdict(self, request):
        return request.param

    @pytest.fixture
    def to_email(self, randmail):
        return randmail()

    @pytest.fixture
    def fields(self, rands):
        return dict((rands(), rands()) for _ in range(10))

    @pytest.fixture
    def user_ip(self, rands):
        return rands()

    @pytest.fixture
    def from_email(self, randmail):
        return randmail()

    @pytest.fixture
    def mailing_id(self, rands):
        return rands()

    @pytest.fixture
    def form_id(self, mailing_id):
        return mailing_id

    @pytest.fixture
    def from_uid(self, randn):
        return randn()

    @pytest.fixture
    def request_id(self, rands):
        return rands()

    @pytest.fixture
    def letter_id(self, randn):
        return f'{randn()}'

    @pytest.fixture(params=(1, 5))
    def count(self, request):
        return request.param

    @pytest.fixture
    def body_template(self, rands):
        return rands()

    @pytest.fixture
    def body(self, body_template):
        return body_template

    @pytest.fixture
    def campaign(self, randn, mailing_id, letter_id, count):
        return {
            'letters': [{"code": "A", "id": letter_id} for _ in range(count)],
            "id": f"{randn()}",
            "submitted_by": "noname",
            "title": "test",
            "slug": mailing_id
        }

    @pytest.fixture(autouse=True)
    def so_mock(self, so_client_mocker, verdict):
        with so_client_mocker('post', result={'check': {'spam': verdict}}, multiple_calls=True) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def sender_campaign_detail_mock(self, sender_client_mocker, campaign):
        with sender_client_mocker('campaign_detail', result=campaign, multiple_calls=True) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def sender_render_transactional_letter_mock(self, sender_client_mocker, body):
        with sender_client_mocker('render_transactional_letter', result=body, multiple_calls=True) as mock:
            yield mock

    @pytest.fixture
    def form_fields(self, fields):
        _, result = SenderLetterIsSpamAction._flat_context(fields, hash_keys=True)
        return result

    @pytest.fixture
    def params(self, mailing_id, to_email, fields, user_ip, from_email, from_uid):
        return dict(
            mailing_id=mailing_id,
            to_email=to_email,
            render_context=fields,
            user_ip=user_ip,
            from_email=from_email,
            from_uid=from_uid
        )

    @pytest.fixture
    async def returned(self, params):
        return await SenderLetterIsSpamAction(**params).run()

    def test_call_post(self,
                       payments_settings,
                       so_mock,
                       request_id,
                       form_id,
                       body_template,
                       user_ip,
                       body,
                       form_fields,
                       from_email,
                       to_email,
                       from_uid,
                       returned,
                       ):
        so_mock.assert_called_with(
            interaction_method='check_json',
            url=f'{SoClient.BASE_URL}/check-json',
            params={
                "service": payments_settings.SO_SERVICE,
                "form_id": form_id,
                "id": request_id,
                "format": "json"
            },
            json={
                "client_ip": user_ip,
                "client_uid": from_uid,
                "client_email": from_email,
                "capture_type": None,
                "form_type": "user",
                "form_author": f"{from_uid}",
                "form_recipients": [to_email],
                "form_fields": dict(
                    (key, {"type": "string", "filled_by": "user", "value": str(value)})
                    for key, value in form_fields.items()
                ),
                "body": body,
                "body_template": body_template,
            }
        )

    def test_returned(self, returned, verdict):
        assert returned == verdict
