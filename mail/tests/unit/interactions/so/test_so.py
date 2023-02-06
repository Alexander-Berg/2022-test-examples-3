import pytest


class TestFormIsSpam:
    @pytest.fixture
    def request_id(self, rands):
        return rands()

    @pytest.fixture
    def mailing_id(self, rands):
        return rands()

    @pytest.fixture
    def form_id(self, mailing_id):
        return mailing_id

    @pytest.fixture
    def user_ip(self, rands):
        return rands()

    @pytest.fixture
    def to_email(self, randmail):
        return randmail()

    @pytest.fixture
    def from_email(self, randmail):
        return randmail()

    @pytest.fixture
    def from_uid(self, randn):
        return randn()

    @pytest.fixture
    def body_template(self, rands):
        return rands()

    @pytest.fixture
    def body(self, body_template):
        return body_template

    @pytest.fixture
    def fields(self, rands):
        return dict((rands(), rands()) for _ in range(10))

    @pytest.fixture
    def form_fields(self, fields):
        return fields

    @pytest.fixture(params=(True, False))
    def verdict(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def response_json(self, verdict):
        return {'check': {'spam': verdict}}

    def test_call_post(self, payments_settings, so_client, request_id, form_id, body_template, user_ip, body,
                       form_fields, from_email, to_email, from_uid):
        so_client.post.assert_called_with(
            interaction_method='check_json',
            url=f'{so_client.BASE_URL}/check-json',
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

    @pytest.fixture(autouse=True)
    async def returned(self, mocker, from_email, so_client, request_id, form_id, user_ip, to_email, fields, body,
                       body_template,
                       from_uid):
        mocker.spy(so_client, 'post')
        return await so_client.form_is_spam(request_id=request_id,
                                            form_id=form_id,
                                            user_ip=user_ip,
                                            to_email=to_email,
                                            from_email=from_email,
                                            from_uid=from_uid,
                                            fields=fields,
                                            body=body,
                                            body_template=body_template)

    def test_returned(self, returned, verdict):
        assert returned == verdict
