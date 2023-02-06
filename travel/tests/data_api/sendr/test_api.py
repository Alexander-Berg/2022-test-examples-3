# coding: utf-8
import pytest
from hamcrest import assert_that, contains, has_entries

from common.data_api.sendr.api import Attachment, Campaign, ErrorResponse, ParsingError
from common.tester.matchers import has_json
from common.tester.utils.replace_setting import replace_setting


HOST = 'example.com'


@pytest.fixture(scope='module', autouse=True)
def replace_sendr_host():
    with replace_setting('YASENDR_HOST', HOST):
        yield


def test_dump_attachment():
    assert Attachment(
        filename='attachment.txt',
        mime_type='text/plain',
        content='Hello. I am attachment file'
    ).dump() == {
        'filename': 'attachment.txt',
        'mime_type': 'text/plain',
        'data': 'SGVsbG8uIEkgYW0gYXR0YWNobWVudCBmaWxl\n'
    }


class TestCampaign(object):
    def test_send(self, httpretty):
        httpretty.register_uri(httpretty.POST, 'https://{}/api/0/foo/transactional/bar/send'.format(HOST),
                               body='{"result": {"status": "OK"}}')
        Campaign('foo', 'bar', 'baz').send('user@example.org', {'test': 1})
        request = httpretty.last_request

        assert request.headers['Authorization'] == 'Basic YmF6Og=='
        assert_that(request.querystring, has_entries({'to_email': ['user@example.org']}))
        assert_that(request.parsed_body, has_entries('args', contains(has_json({'test': 1}))))

    def test_send_attachments(self, httpretty):
        httpretty.register_uri(httpretty.POST, 'https://{}/api/0/foo/transactional/bar/send'.format(HOST),
                               body='{"result": {"status": "OK"}}')
        Campaign('foo', 'bar', 'baz').send('user@example.org', {'test': 1}, attachments=(
            Attachment('attachment.txt', 'text/plain', 'Hello. I am attachment file'),
            Attachment('attachment.pdf', 'application/pdf', '?'),
        ))
        request = httpretty.last_request

        assert_that(request.parsed_body, has_entries({
            'args': contains(has_json({'test': 1})),
            'attachments': contains(has_json(contains({
                'filename': 'attachment.txt',
                'mime_type': 'text/plain',
                'data': 'SGVsbG8uIEkgYW0gYXR0YWNobWVudCBmaWxl\n'
            }, {
                'filename': 'attachment.pdf',
                'mime_type': 'application/pdf',
                'data': 'Pw==\n'
            })))
        }))

    @pytest.mark.parametrize('body, expected_exception', (
        ('{}', ParsingError),
        ('{"result": {}}', ParsingError),
        ('{"result": {"status": "INVALID"}}', ParsingError),
        ('{"result": {"status": "ERROR"}}', ErrorResponse),
    ))
    def test_send_exceptions(self, httpretty, body, expected_exception):
        httpretty.register_uri(httpretty.POST, 'https://{}/api/0/foo/transactional/bar/send'.format(HOST),
                               body=body)
        campaign = Campaign('foo', 'bar', 'baz')
        with pytest.raises(expected_exception):
            campaign.send('user@example.org', {'test': 1})
