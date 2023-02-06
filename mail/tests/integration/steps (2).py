import retrying

from pytest_bdd import (
    given,
    then,
    when,
    parsers,
)

from hamcrest import (
    assert_that,
    equal_to
)

from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse
from mail.devpack.lib.components.sheltie import Sheltie


@retrying.retry(stop_max_delay=1000)
def expect_ok(request):
    response = request()
    assert_that(response.status_code, equal_to(200), response.text)
    return response


@given('sheltie is started')
def step_sheltie_is_started(context):
    sheltie = get_sheltie_component()
    context.uid = 123
    context.sheltie_api = context.components[sheltie].api(uid=context.uid)
    context.pyremock = context.pyremocks[sheltie]


@given('sheltie response to ping')
def step_sheltie_response_for_ping(context):
    step_we_ping_sheltie(context)
    step_response_is(context, 'pong')


@when('we ping sheltie')
def step_we_ping_sheltie(context):
    context.response = context.sheltie_api.ping(request_id=context.request_id)


@then(parsers.parse('response is "{response_body}"'))
def step_response_is(context, response_body):
    assert_that(
        (context.response.status_code, context.response.text),
        equal_to((200, bodies[response_body])),
    )


@when(parsers.parse('we expect request collie to existing contacts'))
def step_request_collie_to_existing_contacts(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to(r'/v1/users/123/contacts/%5b%5d')
        ),
        response=MockResponse(
            status=200,
            body=bodies['existing_contacts']
        )
    )


@when(parsers.parse('we expect request сollie to create contacts'))
def step_request_collie_to_create_contacts(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('post'),
            path=equal_to('/v1/users/123/contacts')
        ),
        response=MockResponse(
            status=200,
            body=bodies['create_contacts']
        )
    )


@when('we request sheltie to import contacts')
def step_colabook_import(context):
    context.response = context.sheltie_api.colabook_import(
        request_id=context.request_id,
        body=bodies['imported_contacts']
    )
    context.pyremock.assert_expectations()


@when('we request sheltie to export contacts')
def step_colabook_export(context):
    context.response = context.sheltie_api.colabook_export(
        request_id=context.request_id
    )
    context.pyremock.assert_expectations()


@when('we request sheltie to_vcard')
def step_to_vcard(context):
    context.response = context.sheltie_api.to_vcard(
        request_id=context.request_id,
        body=bodies['json']
    )
    context.pyremock.assert_expectations()


@when('we request sheltie from_vcard')
def step_from_vcard(context):
    context.response = context.sheltie_api.from_vcard(
        request_id=context.request_id,
        body=bodies['vcard']
    )
    context.pyremock.assert_expectations()


@then('response is ok')
def step_response_is_ok(context):
    assert_that(context.response.status_code, equal_to(200), context.response.text)


def get_sheltie_component():
    return Sheltie


bodies = {
    'pong': "pong",
    'imported_contacts':  'BEGIN:VCARD\r\n'
                          'VERSION:3.0\r\n'
                          'UID:YAAB-671844354-1\r\n'
                          'EMAIL:name@domen.ru\r\n'
                          'FN:Name\r\n'
                          'N:;Name;;;\r\n'
                          'TEL:8800\r\n'
                          'END:VCARD\r\n'
                          'BEGIN:VCARD\r\n'
                          'VERSION:3.0\r\n'
                          'UID:YAAB-671844354-2\r\n'
                          'EMAIL:vanya@domen.ru\r\n'
                          'FN:Ivan\r\n'
                          'N:;Ivan;;;\r\n'
                          'TEL:8888\r\n'
                          'END:VCARD\r\n',
    'existing_contacts': r'{"contacts":[{"contact_id":1,"list_id":2,"revision":3,"tag_ids":[4,5],"emails":[],"vcard":{"names":[{"prefix":"","first":"Name","middle":"","last":"","suffix":""}],'
                         r'"emails":[{"email":"name@domen.ru"}],"telephone_numbers":[{"telephone_number":"8800"}],"vcard_uids":["YAAB-671844354-1"]}},{"contact_id":2,"list_id": 3,"revision":4,'
                         r'"tag_ids":[5,6],"emails":[],"vcard":{"names":[{"first":"Vasya"}],"emails":[{"email":"vasya@domen.ru"}],"telephone_numbers":[{"telephone_number": "8800"}],'
                         r'"vcard_uids":["YAAB-671844354-3"]}}]}',
    'new_contact': r'[{"vcard":{"names":[{"prefix":"","first":"Ivan","middle":"","last":"","suffix":""}],"emails":[{"email":"vanya@domen.ru"}],'
                   r'"telephone_numbers":[{"telephone_number":"8888"}],"vcard_uids":["YAAB-671844354-2"]}}]',
    'create_contacts': r'{"contact_ids":[2],"revision":1}',
    'import_contacts_response': '{"status":"Ok","rec_cnt":1,"rec_skipped":1}',
    'export_contacts_response': 'BEGIN:VCARD\r\n'
                                'VERSION:3.0\r\n'
                                'UID:YAAB-671844354-1\r\n'
                                'EMAIL:name@domen.ru\r\n'
                                'FN:Name\r\n'
                                'N:;Name;;;\r\n'
                                'TEL:8800\r\n'
                                'END:VCARD\r\n'
                                'BEGIN:VCARD\r\n'
                                'VERSION:3.0\r\n'
                                'UID:YAAB-671844354-3\r\n'
                                'EMAIL:vasya@domen.ru\r\n'
                                'FN:Vasya\r\n'
                                'N:;Vasya;;;\r\n'
                                'TEL:8800\r\n'
                                'END:VCARD\r\n',
    'vcard': 'BEGIN:VCARD\r\n'
             'VERSION:3.0\r\n'
             'UID:YAAB-671844354-1\r\n'
             'BDAY:20190419\r\n'
             'EMAIL:server@domain.ru\r\n'
             'FN:Server\r\n'
             'N:;Server;;;\r\n'
             'TEL:9876543210\r\n'
             'END:VCARD\r\n',
    'json_from_vcard': u'{"events": [{"month": 4, "type": ["birthday"], "day": 19, "year": 2019}], "vcard_uids": ["YAAB-671844354-1"], "names": [{"middle": "", "prefix": "", "last": "", '
                       u'"suffix": "", "first": "Server"}], "telephone_numbers": [{"telephone_number": "9876543210"}], "emails": [{"email": "server@domain.ru"}]}',
    'json': r'{"1": {"names": [{"first": "Eric", "last": "Cartman"}], "emails": [{"email": "eric_cartman@yandex.ru"}], "telephone_numbers": [{"telephone_number": "8800"}]}}',
    'vcard_from_json': u'{"1": "BEGIN:VCARD\\r\\nVERSION:3.0\\r\\nUID:YAAB-123-1\\r\\nEMAIL:eric_cartman@yandex.ru\\r\\nFN:Eric Cartman\\r\\nN:Cartman;Eric;;;\\r\\nTEL:8800\\r\\nEND:VCARD\\r\\n"}'
}
