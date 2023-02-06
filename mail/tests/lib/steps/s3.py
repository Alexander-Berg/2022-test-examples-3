import ujson as json
from tests_common.pytest_bdd import given, then
from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse, HttpMethod
from pytest_bdd import parsers

from hamcrest import equal_to, has_entries, contains_string, all_of

S3_KEY_SUFFIXES = ['1', '2']


def step_given_s3_will_respond_without_errors_for_list_objects_v2_impl(context, keys):
    user = context.get_user()
    prefix = f'{user.uid}/'.encode('utf-8')
    contents = ''
    for key_suffix in keys:
        contents += f'''
        <Contents>
            <Key>{user.uid}/{key_suffix}</Key>
            <LastModified>2021-08-12T17:50:30.000Z</LastModified>
            <ETag/>
            <Size>434</Size>
            <StorageClass>STANDARD</StorageClass>
        </Contents>'''

    body = f'''<?xml version="1.0" encoding="UTF-8"?>
    <ListBucketResult>
        <Name>mail-archive-test</Name>
        <Prefix>1111</Prefix>
        <KeyCount>2</KeyCount>
        <MaxKeys>1000</MaxKeys>
        <IsTruncated>false</IsTruncated>
        {contents}
    </ListBucketResult>
    '''
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/mail-archive-test'),
        params=has_entries({'list-type': [b'2'], 'prefix': [prefix]}),
    ), response=MockResponse(status=200, body=body))


@given('s3 will respond without errors for list_objects_v2')
def step_given_s3_will_respond_without_errors_for_list_objects_v2(context):
    step_given_s3_will_respond_without_errors_for_list_objects_v2_impl(context, S3_KEY_SUFFIXES)


@given('s3 will respond without errors for list_objects_v2 with keys: "{keys}"')
def step_given_s3_will_respond_without_errors_for_list_objects_v2_with_keys(context, keys):
    step_given_s3_will_respond_without_errors_for_list_objects_v2_impl(context, keys.split(','))


@given(
    's3 will respond without errors for get_object for key "{key}" with {shared:IsShared?}stids: "{stids}"',
    parse_builder=parsers.cfparse)
def step_given_s3_will_respond_without_errors_for_get_object(context, key, shared, stids):
    user = context.get_user()
    messages = [
        {
            'st_id': stid,
            'folder_type': 'inbox',
            'received_date': 1631023745,
            'is_shared': shared,
        } for stid in stids.split(',')]

    body = json.dumps(messages).encode('utf-8')

    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to(f'/mail-archive-test/{user.uid}/{key}'),
    ), response=MockResponse(status=200, body=body))


def step_given_s3_will_respond_without_errors_for_delete_objects_impl(context, keys):
    user = context.get_user()
    deleted = ''
    for key_suffix in keys:
        deleted += f'''
        <Deleted>
            <Key>{user.uid}/{key_suffix}</Key>
        </Deleted>'''
    body = f'''<?xml version="1.0" encoding="UTF-8"?>
    <DeleteResult>
        {deleted}
    </DeleteResult>
    '''
    body_matchers = [contains_string(f'{user.uid}/{key_suffix}') for key_suffix in keys]
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/mail-archive-test'),
        params=has_entries({'delete': [b'']}),
        body=all_of(*body_matchers)
    ), response=MockResponse(status=200, body=body))


@given('s3 will respond without errors for delete_objects')
def step_given_s3_will_respond_without_errors_for_delete_objects(context):
    step_given_s3_will_respond_without_errors_for_delete_objects_impl(context, S3_KEY_SUFFIXES)


@given('s3 will respond without errors for delete_objects for keys: "{keys}"')
def step_given_s3_will_respond_without_errors_for_delete_objects_for_keys(context, keys):
    step_given_s3_will_respond_without_errors_for_delete_objects_impl(context, keys.split(','))


@given('s3 will respond with 500 "{times:d}" times for list_objects_v2')
def step_given_s3_will_respond_with_500_for_list_objects_v2(context, times):
    user = context.get_user()
    prefix = f'{user.uid}/'.encode('utf-8')
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/mail-archive-test'),
        params=has_entries({'list-type': [b'2'], 'prefix': [prefix]}),
    ), response=MockResponse(status=500, body=''), times=times)


@given('s3 will respond with 500 "{times:d}" times for get_object for key "{key}"')
def step_given_s3_will_respond_with_500_for_get_object(context, times, key):
    user = context.get_user()
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to(f'/mail-archive-test/{user.uid}/{key}'),
    ), response=MockResponse(status=500, body=''), times=times)


@given('s3 will respond with 500 "{times:d}" times for delete_objects')
def step_given_s3_will_respond_with_500_for_delete_objects(context, times):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/mail-archive-test'),
        params=has_entries({'delete': [b'']}),
    ), response=MockResponse(status=500, body=''), times=times)


@given('s3 will respond with errors for delete_objects')
def step_given_s3_will_respond_with_errors_for_delete_objects(context):
    body = '''<?xml version="1.0" encoding="UTF-8"?>
    <DeleteResult>
        <Deleted>
            <Key>key1</Key>
        </Deleted>
        <Error>
            <Key>key2</Key>
            <Code>AccessDenied</Code>
            <Message>Access Denied</Message>
        </Error>
    </DeleteResult>
    '''
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/mail-archive-test'),
        params=has_entries({'delete': [b'']}),
    ), response=MockResponse(status=200, body=body))


@then('there are no unexpected requests to s3')
def step_then_there_are_no_unexpected_requests_to_s3(context):
    context.coordinator.pyremock.assert_expectations()
