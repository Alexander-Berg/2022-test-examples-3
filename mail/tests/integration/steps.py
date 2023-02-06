import json
import jsonschema
import os.path
import random
import retrying
import string
import subprocess
import yatest.common
import time

from pytest_bdd import (
    given,
    then,
    when,
    parsers,
)

from hamcrest import (
    all_of,
    anything,
    assert_that,
    empty,
    equal_to,
    greater_than_or_equal_to,
    greater_than,
    has_entries,
    has_entry,
    has_item,
    has_items,
    is_not,
    only_contains
)

from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse

from mail.collie.devpack.components.collie import (
    Collie,
    CollieDirectorySyncWorker,
    CollieSyncWorker,
)
from mail.collie.devpack.components.colliedb import CollieDb
from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.lib.components.mdb import Mdb

COLLIE_TESTS_TESTING_TVM_APPLICATION_ID = 2001514
COLLIE_TESTING_TVM_APPLICATION_ID = 2001411


@retrying.retry(stop_max_delay=1000)
def expect_ok(request):
    response = request()
    assert_that(response.status_code, equal_to(200), response.text)
    return response


@given('collie is started')
def step_collie_is_started(context):
    context.collie_api = context.components[Collie].api()
    context.pyremock = context.pyremocks[Collie]


@given('collie directory sync worker is started')
def step_collie_directory_sync_worker_is_started(context):
    context.pyremock = context.pyremocks[CollieDirectorySyncWorker]


@given('collie sync is started')
def step_collie_sync_is_started(context):
    context.pyremock = context.pyremocks[CollieSyncWorker]


@given('set ml and staff sync timestamp')
def step_set_ml_and_staf_sync_timestamp(context):
    context.components[CollieDb].query('''
        SELECT code.set_service_sync_timestamp(
            i_service_type := 'ml'::collie.service_type
        )
    ''')
    context.components[CollieDb].query('''
        SELECT code.set_service_sync_timestamp(
            i_service_type := 'staff'::collie.service_type
        )
    ''')


@given('create ml user')
def step_create_ml_user(context):
    context.components[Mdb].query('''
        SELECT * FROM code.create_contacts_user(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_x_request_id := %(x_request_id)s::text
        )
    ''', user_id=1, user_type='connect_organization', x_request_id=context.request_id)


@given('create staff user')
def step_create_staff_user(context):
    context.components[Mdb].query('''
        SELECT * FROM code.create_contacts_user(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_x_request_id := %(x_request_id)s::text
        )
    ''', user_id=2, user_type='connect_organization', x_request_id=context.request_id)


@given('collie response to ping')
def step_collie_response_for_ping(context):
    step_we_ping_collie(context)
    step_response_is(context, 'pong')


@given('TVM2 service tickets for collie')
def step_we_have_tvm2_service_ticket(context):
    context.collie_api.service_ticket = subprocess.check_output([
        get_tvmknife_path(), 'unittest', 'service',
        '-s', str(COLLIE_TESTS_TESTING_TVM_APPLICATION_ID),
        '-d', str(COLLIE_TESTING_TVM_APPLICATION_ID),
    ]).strip()


@given('TVM2 user ticket')
def step_we_have_tvm2_user_ticket(context):
    context.collie_api.user_ticket = subprocess.check_output([
        get_tvmknife_path(), 'unittest', 'user', '-d', str(context.uid),
    ]).strip()


@given('new passport user')
def step_new_passport_user(context):
    response = context.components[FakeBlackbox].register(generate_login())
    assert_that(response.status_code, equal_to(200), response.text)
    assert_that(response.json(), has_entries(uid=anything(), status='ok'), response.text)
    context.uid = int(response.json()['uid'])
    if hasattr(context, 'collie_api'):
        context.collie_api.uid = context.uid


@given('new passport users for staff')
def step_new_passport_users_for_staff(context):
    context.staff = {}
    context.staff["users"] = []
    for _ in range(4):
        response = context.components[FakeBlackbox].register(generate_login())
        assert_that(response.status_code, equal_to(200), response.text)
        assert_that(response.json(), has_entries(uid=anything(), status='ok'), response.text)
        uid = int(response.json()['uid'])
        context.staff["users"].append(uid)


def new_passport_users_for_directory(context):
    context.directory = {}
    context.directory["users"] = []
    for _ in range(2):
        response = context.components[FakeBlackbox].register(generate_login())
        assert_that(response.status_code, equal_to(200), response.text)
        assert_that(response.json(), has_entries(uid=anything(), status='ok'), response.text)
        uid = int(response.json()['uid'])
        context.directory["users"].append(uid)


@given('new passport users for directory')
def step_new_passport_users_for_directory(context):
    new_passport_users_for_directory(context)


@given('new contacts passport user')
def step_new_contacts_passport_user(context):
    step_get_tags(context)


@given('org_id for new organization acquired')
def step_org_id_for_new_organization_acquired(context):
    context.org_id = get_org_id(context)[0][0]


@given('new organization user created')
def step_new_organization_user_created(context):
    context.components[Mdb].query('''
        SELECT code.create_contacts_user(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_x_request_id := %(x_request_id)s::text
        )
    ''', user_id=context.org_id, user_type="connect_organization", x_request_id=context.request_id)


@when('we create organizations users')
def create_organizations_users(context):
    new_passport_users_for_directory(context)
    expect_stat_request_to_cloud_sharpei(context)
    expect_events_request_to_directory(context, "events")
    expect_org_info_request_to_directory(context)
    expect_users_request_to_directory(context)
    expect_departments_request_to_directory(context, "departments")
    expect_groups_request_to_directory(context)


@when('we create contacts connect organization user')
def step_new_contacts_connect_organization_user(context):
    result = create_contacts_user(context, context.org_id, 'connect_organization')
    assert_that(result, equal_to([('success',)]))


@when('we ping collie')
def step_we_ping_collie(context):
    context.response = context.collie_api.ping(request_id=context.request_id)


@when('we request collie to get tags')
def step_get_tags(context):
    context.response = context.collie_api.get_tags(request_id=context.request_id)
    context.pyremock.assert_expectations()


@when('we request collie to create tag')
def step_create_tag(context):
    context.response = context.collie_api.create_tag(name='tag_name', request_id=context.request_id)
    context.pyremock.assert_expectations()
    context.last_create_tag_response = context.response


@when('we request collie to remove last created tag')
def step_remove_tag(context):
    context.response = context.collie_api.remove_tag(
        tag_id=context.last_create_tag_response.json()['tag_id'],
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when(parsers.parse('we request collie to update last created tag name to "{name}"'))
def step_update_tag_name(context, name):
    context.response = context.collie_api.update_tag_name(
        tag_id=context.last_create_tag_response.json()['tag_id'],
        name=name,
        revision=context.last_create_tag_response.json()['revision'],
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request collie to get emails')
def step_get_emails(context):
    context.response = context.collie_api.get_emails(
        request_id=context.request_id,
        tag_ids=[1, 2]
    )
    context.pyremock.assert_expectations()


@when('we request collie to get emails with tag ids')
def step_get_emails_with_tag_ids(context):
    context.response = context.collie_api.get_emails(
        request_id=context.request_id,
        tag_ids=[context.last_create_tag_response.json()['tag_id']]
    )
    context.pyremock.assert_expectations()


def get_default_contacts_list_id(context, uid, type_user):
    result = context.components[Mdb].query('''
        SELECT list_id
          FROM contacts.lists
         WHERE user_id = %(user_id)s::bigint
           AND user_type = %(type_user)s::contacts.user_type
           AND name = 'Personal'::text
           AND type = 'personal'::contacts.list_type
    ''', user_id=uid, type_user=type_user)
    return result[0][0] if result else 0


def prepare_contact(context):
    return [
        dict(
            vcard=dict(
                names=[dict(first='foo', middle='bar', last='baz')]
            )
        )
    ]


def prepare_contact_without_emails(context):
    context.prepared_contacts = prepare_contact(context)


@when('we prepare contact without emails')
def step_prepare_contact_without_emails(context):
    prepare_contact_without_emails(context)


def prepare_contact_with_emails(context):
    prepare_contact_without_emails(context)
    context.prepared_contacts[0]['vcard']['emails'] = [dict(email='foo.baz@yandex.ru'),
                                                       dict(email='foo.bar@yandex.ru')]


@when('we prepare contact with emails')
def step_prepare_contact_with_emails(context):
    prepare_contact_with_emails(context)


@when('we prepare tagged contact with emails')
def step_prepare_contact_with_tag_id_with_emails(context):
    prepare_contact_with_emails(context)
    context.prepared_contacts[0]['tag_ids'] = [context.last_create_tag_response.json()['tag_id']]


def prepare_contacts(context, mode='default'):
    list_id = get_default_contacts_list_id(context, context.uid, 'passport_user')
    contacts = [
        dict(
            list_id=list_id,
            vcard=dict(
                names=[dict(first='John', middle='Peter', last='Doe')],
                emails=[dict(email='john@yandex.ru'), dict(email='doe@yandex.ru')]
            )
        ),
        dict(
            list_id=list_id,
            vcard=dict(
                names=[dict(first='foo', middle='bar', last='baz')],
                emails=[dict(email='foo.baz@yandex.ru', type=['user']), dict(email='foo.bar@yandex.ru')]
            )
        )
    ]
    if mode == 'tag_contact':
        contacts[1]['tag_ids'] = [context.last_create_tag_response.json()['tag_id']]
    if mode == 'tag_contact_with_same_emails':
        contacts.append(dict(
            list_id=list_id,
            tag_ids=[context.last_create_tag_response.json()['tag_id']],
            vcard=dict(
                names=[dict(first='same', middle='email', last='name')],
                emails=[dict(email='same.email@yandex.ru', type=['user']), dict(email='same.email@yandex.ru')]
            )))
    if mode == 'tag_contacts':
        contacts.append(dict(
            list_id=list_id,
            vcard=dict(
                names=[dict(first='First', middle='Middle', last='Last')],
                emails=[dict(email='local0@domain0.ru', type=['user']), dict(email='local1@domain1.ru')]
            )
        ))
        contacts[1]['tag_ids'] = [context.last_create_tag_response.json()['tag_id']]
        contacts[2]['tag_ids'] = [context.last_create_tag_response.json()['tag_id']]
    return contacts


def prepare_organization_contacts(context):
    list_id = get_default_contacts_list_id(context, context.org_id, 'connect_organization')
    vcard = dict(
        names=[dict(first='Hello', middle='Kitty')],
    )
    return '''array_agg(({list_id}, 'vcard_v1', '{vcard}', 'test')::code.new_contact)'''.format(list_id=list_id, vcard=json.dumps(vcard))


def prepare_extended_organization_contacts(context):
    list_id = get_default_contacts_list_id(context, context.org_id, 'connect_organization')
    vcards = [
        dict(names=[dict(first='First0', middle='Middle0')]),
        dict(names=[dict(first='First1', middle='Middle1')]),
        dict(names=[dict(first='First2', middle='Middle2')]),
        dict(names=[dict(first='First3', middle='Middle3')]),
        dict(names=[dict(first='First4', middle='Middle4')])
    ]
    return '''ARRAY[
        ({list_id}, 'vcard_v1', '{vcard0}', 'URI0')::code.new_contact,
        ({list_id}, 'vcard_v1', '{vcard1}', 'URI1')::code.new_contact,
        ({list_id}, 'vcard_v1', '{vcard2}', 'URI2')::code.new_contact,
        ({list_id}, 'vcard_v1', '{vcard3}', 'URI3')::code.new_contact,
        ({list_id}, 'vcard_v1', '{vcard4}', 'URI4')::code.new_contact]'''.format(
        list_id=list_id,
        vcard0=json.dumps(vcards[0]),
        vcard1=json.dumps(vcards[1]),
        vcard2=json.dumps(vcards[2]),
        vcard3=json.dumps(vcards[3]),
        vcard4=json.dumps(vcards[4]))


@when('we prepare contacts')
def step_prepare_contacts(context):
    context.prepared_contacts = prepare_contacts(context)


@when('we prepare contacts without tags')
def step_prepare_contacts_without_tags(context):
    context.prepared_contacts = prepare_contacts(context, mode='without_tags')


@when('we prepare contacts with one contact tagged')
def step_prepare_contacts_with_one_contact_tagged(context):
    context.prepared_contacts = prepare_contacts(context, 'tag_contact')


@when('we prepare contacts with one contact tagged and same emails')
def step_prepare_contacts_with_one_contact_tagged_and_same_emails(context):
    context.prepared_contacts = prepare_contacts(context, 'tag_contact_with_same_emails')


@when('we prepare contacts with multiple contacts tagged')
def step_prepare_contacts_with_tag_ids(context):
    context.prepared_contacts = prepare_contacts(context, 'tag_contacts')


@when('we prepare contacts for organization')
def step_prepare_contacts_for_organization(context):
    context.prepared_contacts = prepare_organization_contacts(context)


@when('we prepare extended contacts for organization')
def step_prepare_extended_contacts_for_organization(context):
    context.prepared_contacts = prepare_extended_organization_contacts(context)


@when('we prepare contacts for passport user')
def step_prepare_contacts_for_passport_user(context):
    step_prepare_contacts(context)


@when('we add directory_entries to created contacts')
def step_add_directory_entries_to_created_contacts(context):
    context.prepared_contacts[0]['vcard']['directory_entries'] = [dict(
        org_id=1, org_name='Name0', entry_id=1, type=['Type0'])]
    context.prepared_contacts[1]['vcard']['directory_entries'] = [dict(
        org_id=2, org_name='Name1', entry_id=2, type=['Type1'])]


@when('we request collie to create contacts')
def step_create_contacts(context):
    context.response = context.collie_api.create_contacts(
        contacts=context.prepared_contacts,
        request_id=context.request_id
    )
    context.pyremock.assert_expectations()
    context.created_contact_ids = context.response.json()['contact_ids']


@when('we request collie to get last created contacts')
def step_get_last_created_contacts(context):
    context.response = context.collie_api.get_contacts(
        contact_ids=context.created_contact_ids,
        request_id=context.request_id
    )
    context.pyremock.assert_expectations()


@when('we request collie to get last created contacts with shared contacts')
def step_get_last_created_contacts_with_shared_contacts(context):
    context.response = context.collie_api.get_contacts(
        contact_ids=context.created_contact_ids,
        request_id=context.request_id,
        mixin='mixin'
    )
    context.pyremock.assert_expectations()


@when('we request collie to get only shared contacts')
def step_get_only_shared_contacts(context):
    context.response = context.collie_api.get_contacts(
        contact_ids=context.created_contact_ids,
        request_id=context.request_id,
        mixin='mixin',
        only_shared=1
    )
    context.pyremock.assert_expectations()


@when('we request collie to get last created contacts with offset and limit')
def step_get_last_created_contacts_with_offset_and_limit(context):
    context.response = context.collie_api.get_contacts(
        contact_ids=context.created_contact_ids,
        request_id=context.request_id,
        offset=1,
        limit=1
    )
    context.pyremock.assert_expectations()


@when('we request collie to get all contacts')
def step_get_all_created_contacts(context):
    context.response = context.collie_api.get_contacts(
        contact_ids=[],
        request_id=context.request_id
    )
    context.pyremock.assert_expectations()


@when('we request collie to remove last created contacts')
def step_remove_last_created_contacts(context):
    context.response = context.collie_api.remove_contacts(
        contact_ids=context.created_contact_ids,
        request_id=context.request_id,
    )


@when('we share organization contacts with user and subscribe user to organization contacts')
def step_share_organization_contacts_with_user_and_subscribe_user_to_organization_contacts(context):
    share_contacts_list(context)
    context.subscribed_list_id = create_user_contacts_list(context)[0]
    subscribe_to_contacts_list(context)


@when('we request collie to get shared lists')
def step_get_shared_lists(context):
    context.response = context.collie_api.get_shared_lists(request_id=context.request_id)
    context.pyremock.assert_expectations()
    if len(context.response.json()['lists']):
        context.subscribed_list_id = context.response.json()['lists'][0]['list_id']


@when('we request collie to get shared contact count from list')
def step_get_shared_contact_count_from_list(context):
    context.response = context.collie_api.get_shared_contacts_count_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id
    )
    context.pyremock.assert_expectations()


@when('we request collie to get shared contact with emails count from list')
def step_get_shared_contact_with_emails_count_from_list(context):
    context.response = context.collie_api.get_shared_contacts_count_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id,
        shared_with_emails=1
    )
    context.pyremock.assert_expectations()


@when('we request collie to get all shared contacts from list')
def step_get_all_shared_contacts_from_list(context):
    context.response = context.collie_api.get_shared_contacts_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id,
        contact_ids=[]
    )
    context.pyremock.assert_expectations()


@when('we request collie to get all shared contacts with emails from list')
def step_get_all_shared_contacts_with_emails_from_list(context):
    context.response = context.collie_api.get_shared_contacts_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id,
        contact_ids=[],
        shared_with_emails=1
    )
    context.pyremock.assert_expectations()


@when('we request collie to get all shared contacts from list with offset and limit')
def step_get_all_shared_contacts_from_list_with_offset_and_limit(context):
    context.response = context.collie_api.get_shared_contacts_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id,
        contact_ids=[],
        offset=1,
        limit=3
    )
    context.pyremock.assert_expectations()


@when('we request collie to get all shared contacts with emails from list with offset and limit')
def step_get_all_shared_contacts_with_emails_from_list_with_offset_and_limit(context):
    context.response = context.collie_api.get_shared_contacts_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id,
        contact_ids=[],
        offset=1,
        limit=2,
        shared_with_emails=1
    )
    context.pyremock.assert_expectations()


@when('we request collie to get all shared contacts from list by IDs with offset and limit')
def step_get_all_shared_contacts_from_list_by_ids_with_offset_and_limit(context):
    context.response = context.collie_api.get_shared_contacts_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id,
        contact_ids=[
            context.last_created_organization_contacts[1],
            context.last_created_organization_contacts[2],
            context.last_created_organization_contacts[3]
        ],
        offset=1,
        limit=2
    )
    context.pyremock.assert_expectations()


@when('we request collie to get all shared contacts with emails from list by IDs with offset and limit')
def step_get_all_shared_contacts_with_emails_from_list_by_ids_with_offset_and_limit(context):
    context.response = context.collie_api.get_shared_contacts_from_list(
        request_id=context.request_id,
        list_id=context.subscribed_list_id,
        contact_ids=[
            context.last_created_organization_contacts[3],
            context.last_created_organization_contacts[4]
        ],
        offset=1,
        limit=1,
        shared_with_emails=1
    )
    context.pyremock.assert_expectations()


@when('we request collie to update last created contact')
def step_update_last_created_contacts(context):
    updated_contacts = list()
    contact = dict(
        list_id=get_default_contacts_list_id(context, context.uid, 'passport_user'),
        vcard=dict(
            names=[
                dict(
                    first='John',
                    middle='Peter',
                    last='Doe',
                )
            ],
            emails=[
                dict(email='john@yandex.ru'),
                dict(email='doe@yandex.ru'),
            ]
        ),
    )
    contact['contact_id'] = context.created_contact_ids[0]
    contact['add_tag_ids'] = [context.last_create_tag_response.json()['tag_id']]
    contact['remove_tag_ids'] = [context.last_create_tag_response.json()['tag_id']]
    updated_contacts.append(contact)
    context.response = context.collie_api.update_contacts(
        updated_contacts=dict(updated_contacts=updated_contacts),
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


def update_last_created_contact(context, mode='default'):
    updated_contacts = [dict(
        contact_id=context.created_contact_ids[0],
        list_id=get_default_contacts_list_id(context, context.uid, 'passport_user'),
        vcard=dict(
            names=[dict(first='First0', middle='Middle0', last='Last0')]
        )
    )]
    if mode == 'tag_contact':
        updated_contacts[0]['vcard']['emails'] = [dict(email='local0@domain0.ru')]
        updated_contacts[0]['add_tag_ids'] = [context.last_create_tag_response.json()['tag_id']]
    if mode == 'untag_contact':
        updated_contacts[0]['vcard']['emails'] = [dict(email='foo.baz@yandex.ru')]
        updated_contacts[0]['remove_tag_ids'] = [context.last_create_tag_response.json()['tag_id']]
    context.response = context.collie_api.update_contacts(
        updated_contacts=dict(updated_contacts=updated_contacts),
        request_id=context.request_id
    )
    context.pyremock.assert_expectations()


@when('we request collie to update and tag last created contact')
def step_update_and_tag_last_created_contact(context):
    update_last_created_contact(context, mode='tag_contact')


@when('we request collie to update and untag last created contact')
def step_update_and_untag_last_created_contact(context):
    update_last_created_contact(context, mode='untag_contact')


@when('we request collie to update contact with tagged emails')
def step_update_contact_with_tagged_emails(context):
    updated_contacts = [dict(
        contact_id=context.created_contact_ids[0],
        list_id=get_default_contacts_list_id(context, context.uid, 'passport_user'),
        vcard=dict(
            names=[dict(first='First1', middle='Middle1', last='Last1')],
            emails=[dict(email='local2@domain2.ru')]
        )
    )]
    context.response = context.collie_api.update_contacts(
        updated_contacts=dict(updated_contacts=updated_contacts),
        request_id=context.request_id
    )
    context.pyremock.assert_expectations()


@when(parsers.parse('we request collie to delete "{uri}" contact'))
def step_carddav_delete(context, uri):
    context.response = context.collie_api.carddav_delete(
        request_id=context.request_id,
        uri=uri
    )
    context.pyremock.assert_expectations()


@when('we request collie to get detailed contact information')
def step_carddav_multiget(context):
    context.response = context.collie_api.carddav_multiget(
        request_id=context.request_id,
        body=bodies['carddav_multiget_request']
    )
    context.pyremock.assert_expectations()


@when('we request collie to get brief contact information')
def step_carddav_propfind(context):
    context.response = context.collie_api.carddav_propfind(request_id=context.request_id)
    context.pyremock.assert_expectations()


@when(parsers.parse('we request carddav_put to create contact with "{uri}"'))
def step_carddav_put_create(context, uri):
    context.response = context.collie_api.carddav_put(
        request_id=context.request_id,
        body=bodies['carddav_put_request'],
        uri=uri,
        etag='%2A'
    )
    context.pyremock.assert_expectations()


@when(parsers.parse('we request carddav_put to update contact with "{uri}" and etag {etag}'))
def step_carddav_put_update(context, uri, etag):
    context.response = context.collie_api.carddav_put(
        request_id=context.request_id,
        body=bodies['carddav_put_request'],
        uri=uri,
        etag=etag
    )
    context.pyremock.assert_expectations()


@when(parsers.parse('we request collie to add directory event'))
def step_add_directory_event(context):
    context.response = context.collie_api.add_directory_event(
        data=dict(org_id=context.uid, event="user_added", revision=42),
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request collie to get contacts count')
def step_get_contacts_count(context):
    context.response = context.collie_api.get_contacts_count(request_id=context.request_id)
    context.pyremock.assert_expectations()


@when('we request collie to get contacts count with shared contacts')
def step_get_contacts_count_with_shared_contacts(context):
    context.response = context.collie_api.get_contacts_count(request_id=context.request_id, mixin='mixin')
    context.pyremock.assert_expectations()


@when('we request collie to search contacts with mixin_group')
def step_search_contacts_with_mixin_group(context):
    context.response = context.collie_api.search_contacts(
        request_id=context.request_id,
        mixin='mixin',
        group='yes'
    )
    context.pyremock.assert_expectations()


@when('we request collie to search contacts with group')
def step_search_contacts_with_group(context):
    context.response = context.collie_api.search_contacts(
        request_id=context.request_id,
        group='yes'
    )
    context.pyremock.assert_expectations()


@when('we request collie to search contacts')
def step_search_contacts(context):
    context.response = context.collie_api.search_contacts(
        request_id=context.request_id
    )
    context.pyremock.assert_expectations()


@when('we request collie to get contacts with tag')
def step_get_contacts_with_tag(context):
    context.response = context.collie_api.get_contacts_with_tag(
        request_id=context.request_id,
        tag_id=int(context.last_create_tag_response.json()['tag_id'])
    )
    context.pyremock.assert_expectations()


@when(parsers.parse('we request collie to get contacts with tag with offset "{offset:d}" and limit "{limit:d}"'))
def step_get_contacts_with_tag_with_offset_and_limit(context, offset, limit):
    context.response = context.collie_api.get_contacts_with_tag(
        request_id=context.request_id,
        tag_id=int(context.last_create_tag_response.json()['tag_id']),
        offset=offset,
        limit=limit
    )
    context.pyremock.assert_expectations()


def prepare_recipients():
    return dict(
        to=['local0@domain0.com', 'ya@ya@ya@ya'],
        cc=['ya@ya@ya@ya', ' "First , Last"  local1@domain1.com', 'local2@domain2.com'],
        bcc=['local3@domain3.com', 'local4@domain4.com', '\'DisplayName\' local5@domain5.com']
    )


def prepare_colabook_feed_addrdb_bulk_body(context):
    return (
        '[{"uid":"' + str(context.uid) + '",' +
        '"to":["local0@domain0.com","ya@ya@ya@ya"],' +
        '"cc":["ya@ya@ya@ya"," \\\"First , Last\\\"  local1@domain1.com","local2@domain2.com"],' +
        '"bcc":["local3@domain3.com","local4@domain4.com","\'DisplayName\' local5@domain5.com"]}]'
    )


def prepare_colabook_feed_addrdb_bulk_body_with_to_field_only(context):
    return (
        '[{"uid":"' + str(context.uid) + '",' +
        '"to":["local0@domain0.com","\\\"First , Last\\\" <local1@domain1.com>","local2@domain2.com",'
        '"local3@domain3.com","local4@domain4.com","\'DisplayName\' <local5@domain5.com>"]}]'
    )


@when('we request collie to add emails')
def step_add_emails(context):
    context.response = context.collie_api.add_emails(
        request_id=context.request_id,
        recipients=prepare_recipients()
    )
    context.pyremock.assert_expectations()
    context.created_contact_ids = context.response.json()['contact_ids']


def prepare_text_recipients():
    return ('local0@domain0.com, "First , Last"  local1@domain1.com,local2@domain2.com,'
            'local3@domain3.com,local4@domain4.com,\'DisplayName\' local5@domain5.com')


@when(parsers.parse('we request collie to add emails via "{source}"'))
def step_add_emails_via(context, source):
    params = dict(request_id=context.request_id, to=prepare_text_recipients())
    if source == 'query string':
        context.response = context.collie_api.colabook_feed_addrdb_get(**params)
    elif source == 'request body':
        context.response = context.collie_api.colabook_feed_addrdb_post(**params)
    context.pyremock.assert_expectations()
    context.created_contact_ids = context.response.json()['contact_ids']


@when(parsers.parse('we expect get profile request to settings with collect_addresses "{value}"'))
def step_expect_get_profile_request_to_settings_with_collect_addresses_value(context, value):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/get_profile'),
            params=has_entries(
                uid=equal_to([str(context.uid)]),
                settings_list=equal_to(['collect_addresses']),
            ),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=200,
            body=json.dumps({
                "settings": {
                    "single_settings": {
                        "collect_addresses": value if value == "on" else str()
                    }
                }
            })
        ),
    )


@when('we request collie to get changes')
def step_get_changes(context):
    context.response = context.collie_api.get_changes(request_id=context.request_id)
    context.pyremock.assert_expectations()
    context.last_get_changes_response = context.response


@when('we request collie to restore to last revision')
def step_restore_last_revision(context):
    context.response = context.collie_api.restore(
        revision=context.last_get_changes_response.json()['changes'][-1]['revision'],
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request collie to restore to first revision')
def step_restore_first_revision(context):
    context.response = context.collie_api.restore(revision=1, request_id=context.request_id)
    context.pyremock.assert_expectations()


@when(parsers.parse('we create "{tag_type}" tag "{tag_name}" for last created contact'))
def step_create_contact_tag_and_tag_contacts_in_db(context, tag_type, tag_name):
    result = context.components[Mdb].query(
        '''
        SELECT tag_id
          FROM code.create_contacts_tag(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_name := %(tag_name)s::text,
            i_type := %(tag_type)s::contacts.tag_type,
            i_x_request_id := %(x_request_id)s::text
        )
        ''',
        user_id=context.uid,
        user_type='passport_user',
        tag_name=tag_name,
        tag_type=tag_type,
        x_request_id=context.request_id)
    tag_id = int(result[0][0])

    create_tag_contacts(
        context,
        tag_id,
    )

    context.tag_id = tag_id


@when('we create new organization')
def step_create_new_organization(context):
    expect_stat_request_to_cloud_sharpei(context)
    expect_events_request_to_directory(context, "events")
    expect_org_info_request_to_directory(context)
    new_passport_users_for_directory(context)
    response_body = read_json(os.path.join(RESPONSES, 'directory/users.json'))
    for i in range(len(response_body['result'])):
        response_body['result'][i]['id'] = context.directory['users'][i]
    step_expect_users_request_to_directory_with_passport_users(context, response_body)
    expect_departments_request_to_directory(context, "departments")
    expect_groups_request_to_directory(context)
    add_event_to_events_queue(context)
    wait_until_events_queue_is_empty(context)
    check_email_in_maildb(context)


def expect_stat_request_to_cloud_sharpei(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v3/stat'),
        ),
        response=MockResponse(
            status=200,
            body=json.dumps({
                '1': dict(
                    name='yc',
                    id=1,
                    databases=[
                        dict(
                            address=dict(
                                host='localhost',
                                port=context.components[CollieDb].port(),
                                dbname='collie_db',
                                dataCenter='local',
                            ),
                            role='master',
                            state=dict(
                                lag=0,
                            ),
                            status='alive',
                        ),
                        dict(
                            address=dict(
                                host='localhost',
                                port=context.components[CollieDb].port() + 1,
                                dbname='collie_db',
                                dataCenter='local',
                            ),
                            role='replica',
                            state=dict(
                                lag=2**31 - 1,
                            ),
                            status='dead',
                        )
                    ],
                )
            })
        ),
        times=None,
    )


@when('we expect stat request to cloud sharpei')
def step_expect_stat_request_to_cloud_sharpei(context):
    expect_stat_request_to_cloud_sharpei(context)


def expect_events_request_to_directory(context, events):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v9/events/'),
            params=has_entries(
                per_page=equal_to(['10']),
                revision__gt=equal_to(['0']),
            ),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=200,
            body=read_json(os.path.join(RESPONSES, 'directory/' + events + '.json')),
        ),
    )


@when(parsers.parse('we expect events request to directory which returns "{events}"'))
def step_expect_events_request_to_directory(context, events):
    expect_events_request_to_directory(context, events)


@when('we expect events request to directory which returns response with deleted organization')
def step_expect_events_request_to_directory_which_returns_response_with_deleted_organization(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v9/events/'),
            params=has_entries(
                per_page=equal_to(['10']),
                revision__gt=equal_to(['0']),
            ),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=404,
            body={"message": "Organization was deleted", "code": "organization_deleted"},
        ),
    )


def expect_org_info_request_to_directory(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v11/organizations/' + str(context.org_id) + '/'),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=200,
            body=read_json(os.path.join(RESPONSES, 'directory/users.json')),
        ),
    )


@when('we expect org info request to directory')
def step_expect_org_info_request_to_directory(context):
    expect_org_info_request_to_directory(context)


def expect_users_request_to_directory(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v9/users/'),
            params=has_entries(
                fields=['id,name,contacts,aliases,birthday,department,position'],
                per_page=['1000'],
            ),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=200,
            body=read_json(os.path.join(RESPONSES, 'directory/users.json')),
        ),
    )


@when('we expect users request to directory')
def step_expect_users_request_to_directory(context):
    expect_users_request_to_directory(context)


def step_expect_users_request_to_directory_with_passport_users(context, request_body):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v9/users/'),
            params=has_entries(
                fields=['id,name,contacts,aliases,birthday,department,position'],
                per_page=['1000'],
            ),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=200,
            body=request_body,
        ),
    )


def expect_departments_request_to_directory(context, departments):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v9/departments/'),
            params=has_entries(
                fields=['id,description,email,parent,name'],
                per_page=['1000'],
            ),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=200,
            body=read_json(os.path.join(RESPONSES, 'directory/' + departments + '.json')),
        ),
    )


@when(parsers.parse('we expect departments request to directory which returns "{departments}"'))
def step_expect_departments_request_to_directory(context, departments):
    expect_departments_request_to_directory(context, departments)


def expect_groups_request_to_directory(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v9/groups/'),
            params=has_entries(
                fields=['id,description,email,name,type'],
                per_page=['1000'],
            ),
            headers=has_entries(**{
                'X-Request-Id': anything(),
            }),
        ),
        response=MockResponse(
            status=200,
            body=read_json(os.path.join(RESPONSES, 'directory/groups.json')),
        ),
    )


@when('we expect groups request to directory')
def step_expect_groups_request_to_directory(context):
    expect_groups_request_to_directory(context)


def add_event_to_events_queue(context):
    context.components[CollieDb].query('''
        SELECT code.add_directory_event(
            i_user_id := %(user_id)s::bigint,
            i_user_type := 'connect_organization'::collie.user_type
        )
    ''', user_id=context.org_id)


@when('we add event to events queue')
def step_add_event_to_events_queue(context):
    add_event_to_events_queue(context)


@when('we add directory event with default params')
def step_add_directory_event_with_default_params(context):
    context.components[CollieDb].query('''
        SELECT code.add_directory_event(
            i_user_id := %(user_id)s::bigint,
            i_user_type := 'connect_organization'::collie.user_type
        )
    ''', user_id=context.org_id)


@then('we check event with default params in queue')
def step_check_event_with_default_params_in_queue(context):
    result = context.components[CollieDb].query('''
        SELECT *
        FROM collie.directory_events
        WHERE user_id = %(user_id)s::bigint
        AND event_type = 'organization_updated'::collie.event_type
        AND event_revision = 0
    ''', user_id=context.org_id)
    assert_that(len(result), equal_to(1))


@when(parsers.parse('we add directory event with "{event_type}" type and revision "{event_revision}"'))
def step_add_directory_event_with_event_type_and_event_revision(context, event_type, event_revision):
    context.components[CollieDb].query('''
        SELECT code.add_directory_event(
            i_user_id := %(user_id)s::bigint,
            i_user_type := 'connect_organization'::collie.user_type,
            i_event_type := %(event_type)s::collie.event_type,
            i_event_revision := %(event_revision)s::bigint
        )
    ''', user_id=context.org_id, event_type=event_type, event_revision=int(event_revision))


@when('we reset directory event revision to null')
def step_reset_directory_event_revision_to_nul(context):
    result = context.components[CollieDb].query('''
        UPDATE collie.directory_events
        SET event_revision = NULL
        WHERE user_id = %(user_id)s::bigint;

        SELECT *
        FROM collie.directory_events
        WHERE user_id = %(user_id)s::bigint
        AND event_revision is NULL
    ''', user_id=context.org_id)
    assert_that(len(result), equal_to(1))


@then(parsers.parse('we check event with "{event_type}" type and revision "{event_revision}" in queue'))
def step_check_event_with_event_type_and_event_revision_in_queue(context, event_type, event_revision):
    result = context.components[CollieDb].query('''
        SELECT *
        FROM collie.directory_events
        WHERE user_id = %(user_id)s::bigint
        AND event_type = %(event_type)s::collie.event_type
        AND event_revision = %(event_revision)s::bigint
    ''', user_id=context.org_id, event_type=event_type, event_revision=int(event_revision))
    assert_that(len(result), equal_to(1))


@when(parsers.parse('we create system tag "{tag_name}" for last created contact'))
def step_get_system_tag_id_and_tag_contacts_in_db(context, tag_name):
    result = context.components[Mdb].query(
        '''
        SELECT tag_id
          FROM contacts.tags
         WHERE user_id = %(user_id)s::bigint
           AND user_type = %(user_type)s::contacts.user_type
           AND type = %(tag_type)s::contacts.tag_type
           AND name = %(tag_name)s::text
        ''',
        user_id=context.uid,
        user_type='passport_user',
        tag_name=tag_name,
        tag_type='system')
    tag_id = int(result[0][0])

    create_tag_contacts(
        context,
        tag_id,
    )

    context.tag_id = tag_id


@when('we expect request to_vcard')
def step_expect_to_vcard_vcard_transform(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('post'),
            path=equal_to('/v1/to_vcard'),
            params=has_entries(
                uid=equal_to([str(context.uid)])
            ),
            headers=has_entries(**{
                'X-Request-Id': [context.request_id],
                'Content-Type': ['application/json']
            })
        ),
        response=MockResponse(
            status=200,
            body=json.dumps({
                'YA-1': '''BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL:doe@yandex.ru\r\nEMAIL:john@yandex.ru\r\n'''
                        '''FN:John Peter Doe\r\nN:Doe;John;Peter;;\r\nEND:VCARD\r\n''',
                'YA-2': '''BEGIN:VCARD\r\nVERSION:3.0\r\nEMAIL:foo.bar@yandex.ru\r\nEMAIL:foo.baz@yandex.ru\r\n'''
                        '''FN:foo bar baz\r\nN:baz;foo;bar;;\r\nEND:VCARD\r\n'''
            })
        )
    )


@when('we expect request from_vcard')
def step_expect_from_vcard_vcard_transform(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('post'),
            path=equal_to('/v1/from_vcard'),
            params=has_entries(
                uid=equal_to([str(context.uid)])
            ),
            headers=has_entries(**{
                'X-Request-Id': [context.request_id],
                'Content-Type': ['application/text']
            }),
            body=equal_to(bodies['carddav_put_request'])
        ),
        response=MockResponse(
            status=200,
            body='''{"emails": [{"email": "server@domain.ru"}], "vcard_uids": ["YAAB-671844354-1"],'''
                 '''"events": [{"month": "04", "day": "19", "year": "2019"}],"telephone_numbers": '''
                 '''[{"telephone_number": "9876543210"}], "names": [{"middle": "", "prefix": "", "last":'''
                 '''"", "suffix":"", "first": "Server"}]}'''
        )
    )


@when('we create contacts for organization')
def step_create_contacts_for_organization(context):
    result = create_contacts(context, context.org_id, 'connect_organization', context.prepared_contacts)
    assert_that(result, [(anything(),)])
    row = json.loads(result[0][0].replace('(', '[').replace(')', ']').replace('{', '[').replace('}', ']').
                     replace('"', ''))
    context.last_created_organization_contacts = row[1]


@when('we create emails for last created contact for organization')
def step_create_emails_for_last_created_contact_for_organization(context):
    new_email = '''array_agg(({contact_id}, 'hello@kitty.cat', '{{}}'::text[], 't')::code.new_contacts_email)'''.format(
        contact_id=context.last_created_organization_contacts[0])
    create_contacts_emails(context, context.org_id, 'connect_organization', new_email)


@when('we create extended emails for last created contacts for organization')
def step_create_extended_emails_for_last_created_contacts_for_organization(context):
    new_emails = '''ARRAY[
        ({contact_id2}, 'local2@domain2.com', '{{}}'::text[], 'Label2')::code.new_contacts_email,
        ({contact_id3}, 'local3@domain3.com', '{{}}'::text[], 'Label3')::code.new_contacts_email,
        ({contact_id4}, 'local4@domain4.com', '{{}}'::text[], 'Label4')::code.new_contacts_email]'''.format(
        contact_id2=context.last_created_organization_contacts[2],
        contact_id3=context.last_created_organization_contacts[3],
        contact_id4=context.last_created_organization_contacts[4])
    create_contacts_emails(context, context.org_id, 'connect_organization', new_emails)


@when('we expect request to ml which returns updated ml contacts')
def step_expect_request_to_ml_which_returns_updated_ml_contacts(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/apiv3/lists/info'),
            params=has_entries(
                fields=['id,name,email'],
            ),
            headers=has_entries(**{
                'X-Request-Id': anything()
            })
        ),
        response=MockResponse(
            status=200,
            body=read_json(os.path.join(RESPONSES, 'ml/updated_ml_contacts.json')),
        ),
        times=None
    )


@when('we expect request to ml which returns new ml contacts')
def step_expect_request_to_ml_which_returns_new_ml_contacts(context):
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/apiv3/lists/info'),
            params=has_entries(
                fields=['id,name,email'],
            ),
            headers=has_entries(**{
                'X-Request-Id': anything()
            })
        ),
        response=MockResponse(
            status=200,
            body=read_json(os.path.join(RESPONSES, 'ml/new_ml_contacts.json')),
        )
    )


@when('we expect request to staff which returns updated staff contacts')
def step_expect_request_to_staff_which_returns_updated_staff_contacts(context):
    response_body = read_json(os.path.join(RESPONSES, 'staff/updated_staff_contacts.json'))
    for i in range(len(response_body['result'])):
        response_body['result'][i]['uid'] = context.staff['users'][response_body['result'][i]['id'] - 1]
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v3/persons'),
            headers=has_entries(**{
                'X-Request-Id': anything()
            })
        ),
        response=MockResponse(
            status=200,
            body=json.dumps(response_body),
        ),
        times=None
    )


@when('we expect request to staff which returns new staff contacts')
def step_expect_request_to_staff_which_returns_new_staff_contacts(context):
    response_body = read_json(os.path.join(RESPONSES, 'staff/new_staff_contacts.json'))
    for i in range(len(response_body['result'])):
        response_body['result'][i]['uid'] = context.staff['users'][response_body['result'][i]['id'] - 1]
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/v3/persons'),
            headers=has_entries(**{
                'X-Request-Id': anything()
            })
        ),
        response=MockResponse(
            status=200,
            body=json.dumps(response_body),
        )
    )


@then(parsers.parse('response is "{response_body}"'))
def step_response_is(context, response_body):
    assert_that(
        (context.response.status_code, context.response.text),
        equal_to((200, response_body)),
    )


@then(parsers.parse('response is ok with body equal to one at "{response_body}"'))
def step_response_is_ok_with_body_equal_to_one_specified(context, response_body):
    assert_that(
        (context.response.status_code, context.response.text),
        equal_to((200, bodies[response_body]))
    )


@then('response is ok')
def step_response_is_ok(context):
    assert_that(context.response.status_code, equal_to(200), context.response.text)


@then(parsers.parse('response is verified by json schema "{json_schema}"'))
def step_response_is_verified_by_json_schema(context, json_schema):
    path = os.path.join(SCHEMAS, json_schema)
    jsonschema.validate(
        context.response.json(),
        read_json(path),
        resolver=jsonschema.RefResolver(
            base_uri='file://%s/' % os.path.dirname(os.path.abspath(path)),
            referrer=json_schema,
        ),
    )


def get_organization_pending_events_count(context, org_id):
    result = context.components[CollieDb].query('''
        SELECT pending_events_count
          FROM collie.directory_events
         WHERE user_id = %(user_id)s::bigint
           AND user_type = 'connect_organization'::collie.user_type
    ''', user_id=org_id)
    return result[0][0] if result else 0


@then(parsers.parse('organization with id "{org_id}" has "{event_count:d}" pending events'))
def step_organization_has_pending_events(context, org_id, event_count):
    assert_that(get_organization_pending_events_count(context, org_id), equal_to(event_count))


@then('response has created tag id')
def step_response_has_created_tag_id(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts',
                                has_item(has_entry('tag_ids',
                                         has_item(context.last_create_tag_response.json()['tag_id'])))))


@then('response has contacts with vcards with created emails')
def step_response_has_contacts_with_vcards_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', has_items(
        has_entry('vcard', has_entry('emails', has_items(
            has_entries({'email': 'john@yandex.ru'}),
            has_entries({'email': 'doe@yandex.ru'})))),
        has_entry('vcard', has_entry('emails', has_items(
            has_entries({'email': 'foo.baz@yandex.ru', 'type': ['user']}),
            has_entries({'email': 'foo.bar@yandex.ru'})))))))


@then('we check revision of unchanged contacts')
def step_we_check_revision_of_unchanged_contacts(context):
    ml_unchanged_contact_id = None
    ml_updated_contact_id = None
    ml_unchanged_contact_revision = None
    ml_updated_contact_revision = None
    for _ in range(10):
        ml_unchanged_contact_id = context.components[CollieDb].query(
            '''
            SELECT contact_id
            FROM collie.ml_id_map
            WHERE ml_id = %(ml_id)s::bigint
            ''', ml_id=4)
        if ml_unchanged_contact_id:
            break
        time.sleep(1)
    assert_that(ml_unchanged_contact_id, is_not(None))
    for _ in range(10):
        ml_updated_contact_id = context.components[CollieDb].query(
            '''
            SELECT contact_id
            FROM collie.ml_id_map
            WHERE ml_id = %(ml_id)s::bigint
            ''', ml_id=1)
        if ml_updated_contact_id:
            break
        time.sleep(1)
    assert_that(ml_updated_contact_id, is_not(None))
    for _ in range(10):
        ml_unchanged_contact_revision = context.components[Mdb].query(
            '''
            SELECT revision
            FROM contacts.contacts
            WHERE contact_id = %(contact_id)s::bigint
            AND user_id = %(ml_user_id)s::bigint
            ''', contact_id=ml_unchanged_contact_id[0], ml_user_id=1)
        ml_updated_contact_revision = context.components[Mdb].query(
            '''
            SELECT revision
            FROM contacts.contacts
            WHERE contact_id = %(contact_id)s::bigint
            AND user_id = %(ml_user_id)s::bigint
            ''', contact_id=ml_updated_contact_id[0], ml_user_id=1)
        if ml_updated_contact_id > ml_unchanged_contact_revision:
            break
        time.sleep(1)
    assert_that(ml_updated_contact_revision[0], greater_than(ml_unchanged_contact_revision[0]))


@then('we check updated contacts')
def step_we_check_updated_contacts(context):
    staff_contact_ids = None
    for _ in range(10):
        staff_contact_ids = context.components[CollieDb].query(
            '''
            SELECT *
            FROM collie.staff_id_map
            ''')
        if staff_contact_ids:
            break
        time.sleep(1)
    assert_that(staff_contact_ids, is_not(None))
    staff_unchanged_contact_id = None
    staff_deleted_contact_id = None
    staff_updated_contact_id = None
    staff_new_contact_id = None
    for contact in staff_contact_ids:
        if contact[0] == 1:
            staff_unchanged_contact_id = contact[1]
        if contact[0] == 2:
            staff_deleted_contact_id = contact[1]
        if contact[0] == 3:
            staff_updated_contact_id = contact[1]
        if contact[0] == 4:
            staff_new_contact_id = contact[1]
    assert_that(staff_unchanged_contact_id, is_not(None))
    assert_that(staff_updated_contact_id, is_not(None))
    assert_that(staff_new_contact_id, is_not(None))
    assert_that(staff_deleted_contact_id, equal_to(None))
    unchanged_contact_middle_name = context.components[Mdb].query(
        '''
        SELECT vcard
        FROM contacts.contacts
        WHERE contact_id = %(contact_id)s::bigint
        AND user_id = %(staff_user_id)s::bigint
            ''', contact_id=staff_unchanged_contact_id, staff_user_id=2)[0][0]['names'][0]['middle']
    assert_that(unchanged_contact_middle_name, equal_to("name1_middle"))
    updated_contact_middle_name = context.components[Mdb].query(
        '''
        SELECT vcard
        FROM contacts.contacts
        WHERE contact_id = %(contact_id)s::bigint
        AND user_id = %(staff_user_id)s::bigint
        ''', contact_id=staff_updated_contact_id, staff_user_id=2)[0][0]['names'][0]['middle']
    assert_that(updated_contact_middle_name, equal_to("name_middle"))
    new_contact_middle_name = context.components[Mdb].query(
        '''
        SELECT vcard
        FROM contacts.contacts
        WHERE contact_id = %(contact_id)s::bigint
        AND user_id = %(staff_user_id)s::bigint
        ''', contact_id=staff_new_contact_id, staff_user_id=2)[0][0]['names'][0]['middle']
    assert_that(new_contact_middle_name, equal_to("name4_middle"))


def strictly_contains(*matchers):
    return all_of(has_items(*matchers), only_contains(*matchers))


@then('response has contact with vcard with created emails')
def step_response_has_contact_with_vcard_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        has_entry('vcard', has_entry('emails', has_items(
            has_entries({'email': 'john@yandex.ru'}),
            has_entries({'email': 'doe@yandex.ru'})))))))


@then('response has contacts with tag with vcards with created emails')
def step_response_has_contacts_with_tag_with_vcards_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        has_entry('vcard', has_entry('emails', has_items(
            has_entries({'email': 'foo.baz@yandex.ru', 'type': ['user']}),
            has_entries({'email': 'foo.bar@yandex.ru'})))),
        has_entry('vcard', has_entry('emails', has_items(
            has_entries({'email': 'local0@domain0.ru', 'type': ['user']}),
            has_entries({'email': 'local1@domain1.ru'})))))))


@then('response has contact with tag with vcard with created emails')
def step_response_has_contact_with_tag_with_vcard_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        has_entry('vcard', has_entry('emails', has_items(
            has_entries({'email': 'foo.baz@yandex.ru', 'type': ['user']}),
            has_entries({'email': 'foo.bar@yandex.ru'})))))))


@then('response has contacts with created emails')
def step_response_has_contacts_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', has_items(
        has_entry('emails', has_items(
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'john@yandex.ru'}),
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'doe@yandex.ru'}))),
        has_entry('emails', has_items(
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'foo.baz@yandex.ru'}),
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'foo.bar@yandex.ru'}))))))


@then('response has contact with created emails')
def step_response_has_contact_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        has_entry('emails', has_items(
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'john@yandex.ru'}),
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'doe@yandex.ru'}))))))


@then('response has contacts with tag with created emails')
def step_response_has_contacts_with_tag_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        has_entry('emails', has_items(
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'foo.baz@yandex.ru'}),
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'foo.bar@yandex.ru'}))),
        has_entry('emails', has_items(
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'local0@domain0.ru'}),
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'local1@domain1.ru'}))))))


@then('response has contact with tag with created emails')
def step_response_has_contact_with_tag_with_created_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        has_entry('emails', has_items(
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'foo.baz@yandex.ru'}),
            has_entries({'id': greater_than_or_equal_to(1), 'value': 'foo.bar@yandex.ru'}))))))


@then('response has contact emails as ungrouped result')
def step_response_has_contact_emails_as_ungrouped_result(context):
    resp = context.response.json()
    tag_id = context.last_create_tag_response.json()['tag_id']
    assert_that(resp, has_entries({'contact': has_items(
        all_of(has_entries({'cid': greater_than_or_equal_to(1),
                            'id': greater_than_or_equal_to(1),
                            'name': has_entries({'first': 'John', 'middle': 'Peter', 'last': 'Doe'}),
                            'email': 'john@yandex.ru',
                            'ya_directory': has_entries({
                                'org_id': 1,
                                'org_name': 'Name0',
                                'type': 'Type0',
                                'id': 1
                            })}),
               is_not(has_entry('tags', anything()))),
        all_of(has_entries({'cid': greater_than_or_equal_to(1),
                            'id': greater_than_or_equal_to(1),
                            'name': has_entries({'first': 'John', 'middle': 'Peter', 'last': 'Doe'}),
                            'email': 'doe@yandex.ru',
                            'ya_directory': has_entries({
                                'org_id': 1,
                                'org_name': 'Name0',
                                'type': 'Type0',
                                'id': 1
                            })}),
               is_not(has_entry('tags', anything()))),
        has_entries({'cid': greater_than_or_equal_to(1),
                     'id': greater_than_or_equal_to(1),
                     'tags': has_item(tag_id),
                     'name': has_entries({'first': 'foo', 'middle': 'bar', 'last': 'baz'}),
                     'email': 'foo.baz@yandex.ru',
                     'ya_directory': has_entries({
                         'org_id': 2,
                         'org_name': 'Name1',
                         'type': 'Type1',
                         'id': 2
                     })}),
        has_entries({'cid': greater_than_or_equal_to(1),
                     'id': greater_than_or_equal_to(1),
                     'tags': has_item(tag_id),
                     'name': has_entries({'first': 'foo', 'middle': 'bar', 'last': 'baz'}),
                     'email': 'foo.bar@yandex.ru',
                     'ya_directory': has_entries({
                         'org_id': 2,
                         'org_name': 'Name1',
                         'type': 'Type1',
                         'id': 2
                     })}),
        has_entries({'cid': greater_than_or_equal_to(1),
                     'id': greater_than_or_equal_to(1),
                     'tags': has_item(tag_id),
                     'name': has_entries({'first': 'First', 'middle': 'Middle', 'last': 'Last'}),
                     'email': 'local0@domain0.ru'}),
        has_entries({'cid': greater_than_or_equal_to(1),
                     'id': greater_than_or_equal_to(1),
                     'tags': has_item(tag_id),
                     'name': has_entries({'first': 'First', 'middle': 'Middle', 'last': 'Last'}),
                     'email': 'local1@domain1.ru'})
        ),
        'count': 6
    }))


@then('response has created contacts as emails')
def step_response_has_created_contacts_as_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', has_items(
        has_entry('vcard', has_entry('emails', has_item(has_entry('email', 'local0@domain0.com')))),
        has_entry('vcard', has_entries({'names': has_item(has_entries({'first': 'First', 'last': 'Last'})),
                                        'emails': has_item(has_entry('email', 'local1@domain1.com'))})),
        has_entry('vcard', has_entry('emails', has_item(has_entry('email', 'local2@domain2.com')))),
        has_entry('vcard', has_entry('emails', has_item(has_entry('email', 'local3@domain3.com')))),
        has_entry('vcard', has_entry('emails', has_item(has_entry('email', 'local4@domain4.com')))),
        has_entry('vcard', has_entries({'names': has_item(has_entry('first', 'DisplayName')),
                                        'emails': has_item(has_entry('email', 'local5@domain5.com'))})))))


@then('response has empty created contacts')
def step_response_has_empty_created_contacts(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contact_ids', empty()))


@then(parsers.parse('response has created tag with contacts count "{count:d}"'))
def step_response_has_tag_with_contacts_count(context, count):
    resp = context.response.json()
    assert_that(resp, has_entry('tags', has_item(has_entries({
        'tag_id': context.tag_id,
        'contacts_count': count,
    }))))


@then('response has emails')
def step_response_has_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry(
        'emails', has_items(
            has_entries({
                'tag_id': 1,
                'count': 2,
                'emails': has_items(
                    has_entries({
                        'contact_id': 4,
                        'email': 'local0@domain0.com'
                    }),
                    has_entries({
                        'email_id': 1,
                        'contact_id': 5,
                        'email': 'local1@domain1.com'
                    })
                )
            }),
            has_entries({
                'tag_id': 2,
                'count': 1,
                'emails': has_item(
                    has_entries({
                        'contact_id': 5,
                        'email': 'local2@domain2.com'
                    })
                )
            })
        )
    ))


@then('response has emails with tag')
def step_response_has_emails_with_tag(context):
    tag_id = context.last_create_tag_response.json()['tag_id']
    contact_ids = context.created_contact_ids

    resp = context.response.json()
    assert_that(resp, has_entry(
        'emails', has_items(
            has_entries({
                'tag_id': tag_id,
                'count': 2,
                'emails': has_items(
                    has_entries({
                        'contact_id': contact_ids[2],
                        'email': 'same.email@yandex.ru'
                    }),
                    has_entries({
                        'contact_id': contact_ids[2],
                        'email': 'same.email@yandex.ru'
                    })
                )
            })
        )
    ))


@then('response has updated contact with tag')
def step_response_has_updated_contact_with_tag(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        has_entries({
            'vcard': has_entries({
                'names': strictly_contains(has_entries({'first': 'First0', 'middle': 'Middle0',
                                                        'last': 'Last0'})),
                'emails': strictly_contains(has_entry('email', 'local0@domain0.ru'))
            }),
            'tag_ids': strictly_contains(context.last_create_tag_response.json()['tag_id']),
            'emails': strictly_contains(has_entries({
                'value': 'local0@domain0.ru',
                'tags': strictly_contains(context.last_create_tag_response.json()['tag_id'])
            }))
        })
    )))


@then('response has no tagged emails')
def step_response_has_no_tagged_emails(context):
    resp = context.response.json()
    assert_that(resp, has_entry('emails', empty()))


@then('response has no tagged contacts')
def step_response_has_no_tagged_contacts(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', empty()))


@then('response has contacts with shared contacts')
def step_response_has_contacts_with_shared_contacts(context):
    resp = context.response.json()
    assert_that(resp, has_entry(
        'contacts', has_items(
            has_entries({
                'contact_id': 2,
                'list_id': 1,
                'revision': 2,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'foo',
                        'middle': 'bar',
                        'last': 'baz'
                    })),
                    'emails': has_items(
                        has_entry('email', 'foo.baz@yandex.ru'),
                        has_entry('email', 'foo.bar@yandex.ru')
                    )
                }),
                'tag_ids': empty()
            }),
            has_entries({
                'contact_id': 1,
                'list_id': 1,
                'revision': 2,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'John',
                        'middle': 'Peter',
                        'last': 'Doe'
                    })),
                    'emails': has_items(
                        has_entry('email', 'john@yandex.ru'),
                        has_entry('email', 'doe@yandex.ru')
                    )
                }),
                'tag_ids': empty()
            }),
            has_entries({
                'contact_id': 1,
                'list_id': 1,
                'revision': 2,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'Hello',
                        'middle': 'Kitty',
                    })),
                }),
                'tag_ids': empty(),
                'uri': 'test'
            })
        )
    ))


@then('response has only shared contacts')
def step_response_has_only_shared_contacts(context):
    resp = context.response.json()
    assert_that(resp, has_entry(
        'contacts', has_items(
            has_entries({
                'contact_id': 1,
                'list_id': 1,
                'revision': 2,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'Hello',
                        'middle': 'Kitty',
                    })),
                }),
                'tag_ids': empty(),
                'uri': 'test'
            })
        )
    ))


@then('response has contacts without shared contacts')
def step_response_has_contacts_without_shared_contacts(context):
    resp = context.response.json()
    assert_that(resp, has_entry(
        'contacts', has_items(
            has_entries({
                'contact_id': 2,
                'list_id': 1,
                'revision': 2,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'foo',
                        'middle': 'bar',
                        'last': 'baz'
                    })),
                    'emails': has_items(
                        has_entry('email', 'foo.baz@yandex.ru'),
                        has_entry('email', 'foo.bar@yandex.ru')
                    )
                }),
                'tag_ids': empty()
            }),
            has_entries({
                'contact_id': 1,
                'list_id': 1,
                'revision': 2,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'John',
                        'middle': 'Peter',
                        'last': 'Doe'
                    })),
                    'emails': has_items(
                        has_entry('email', 'john@yandex.ru'),
                        has_entry('email', 'doe@yandex.ru')
                    )
                }),
                'tag_ids': empty()
            }),
        )
    ))


@then('response has contacts count with shared contacts')
def step_response_as_contacts_count_with_shared_contacts(context):
    resp = context.response.json()
    assert_that(resp, has_entries({
        'total': 3,
        'book': has_items(
            has_entries({
                'count': 2,
                'id': str(context.uid)
            }),
            has_entries({
                'count': 1,
                'id': str(context.org_id)
            }),
        )
    }))


def wait_until_events_queue_is_empty(context):
    result = [1]
    for _ in range(10):
        result = context.components[CollieDb].query('''
            SELECT *
            FROM collie.directory_events
            WHERE user_id = %(user_id)s::bigint
            AND user_type = 'connect_organization'::collie.user_type
            AND pending_events_count > 0
        ''', user_id=context.org_id)
        if not result:
            break
        time.sleep(1)
    assert_that(result, equal_to(list()))


@when('we wait until events queue is empty')
def step_wait_until_events_queue_is_empty(context):
    wait_until_events_queue_is_empty(context)


@then('we delete event from queue')
def step_delete_event_from_queue(context):
    context.components[CollieDb].execute('''
        DELETE
          FROM collie.directory_events
         WHERE user_id = %(user_id)s::bigint;
    ''', user_id=context.org_id)


def check_email_in_maildb(context):
    email = "all@sunday30test.yaconnect.com"
    result = context.components[Mdb].query(
        '''
        SELECT *
          FROM contacts.emails
          WHERE user_id = %(user_id)s::bigint
          AND email = %(email)s::text
        ''',
        user_id=context.org_id,
        email=email)
    assert_that(len(result), equal_to(1))
    # TODO: uncomment and fix it
    # context.pyremock.assert_expectations()


@then('maildb contents is correct')
def step_check_email_in_maildb(context):
    check_email_in_maildb(context)


@then('empty email is not in maildb')
def step_check_deleted_email_in_maildb(context):
    contact_ids = context.components[Mdb].query(
        '''
        SELECT contact_id
          FROM contacts.directory_entities
          WHERE user_id = %(user_id)s::bigint
          AND directory_entity_type = %(entity_type)s::contacts.directory_entity_type
        ''',
        user_id=context.org_id,
        entity_type="department")
    result = context.components[Mdb].query(
        '''
        SELECT *
          FROM contacts.emails
          WHERE user_id = %(user_id)s::bigint
          AND contact_id = %(contact_id)s::bigint
        ''',
        user_id=context.org_id,
        contact_id=contact_ids[0][0])
    assert_that(result, equal_to(list()))
    # TODO: uncomment and fix it
    # context.pyremock.assert_expectations()


@then('orgainzation has been deleted')
def step_check_orgainzation_has_been_deleted(context):
    for i in range(len(context.directory['users'])):
        user_uid = context.directory['users'][i]
        user_org_list = context.components[Mdb].query(
            '''
            SELECT *
              FROM contacts.lists
              WHERE user_id = %(user_id)s::bigint
              AND name = %(list_name)s::text
            ''',
            user_id=user_uid, list_name="org_contacts_" + str(context.org_id))
        assert_that(user_org_list, equal_to(list()))

        user_subscribed_list = context.components[Mdb].query(
            '''
            SELECT *
              FROM contacts.subscribed_lists
              WHERE user_id = %(user_id)s::bigint
            ''',
            user_id=user_uid)
        assert_that(user_subscribed_list, equal_to(list()))

    org_lists = context.components[Mdb].query(
        '''
        SELECT *
            FROM contacts.lists
            WHERE user_id = %(user_id)s::bigint
        ''',
        user_id=context.org_id)
    assert_that(org_lists, equal_to(list()))

    org_shared_lists = context.components[Mdb].query(
        '''
        SELECT *
            FROM contacts.shared_lists
            WHERE user_id = %(user_id)s::bigint
        ''',
        user_id=context.org_id)
    assert_that(org_shared_lists, equal_to(list()))

    org_directory_entities = context.components[Mdb].query(
        '''
        SELECT *
            FROM contacts.directory_entities
            WHERE user_id = %(user_id)s::bigint
        ''',
        user_id=context.org_id)
    assert_that(org_directory_entities, equal_to(list()))

    org_contacts = context.components[Mdb].query(
        '''
        SELECT *
            FROM contacts.contacts
            WHERE user_id = %(user_id)s::bigint
        ''',
        user_id=context.org_id)
    assert_that(org_contacts, equal_to(list()))

    org_emails = context.components[Mdb].query(
        '''
        SELECT *
            FROM contacts.emails
            WHERE user_id = %(user_id)s::bigint
        ''',
        user_id=context.org_id)
    assert_that(org_emails, equal_to(list()))

    org_event = context.components[CollieDb].query('''
        SELECT *
        FROM collie.directory_events
        WHERE user_id = %(user_id)s::bigint
    ''', user_id=context.org_id)
    assert_that(org_event, equal_to(list()))


@then('response has contacts')
def step_response_has_contacts(context):
    resp = context.response.json()
    assert_that(resp, has_entry(
        'contacts', has_items(
            has_entries({
                'contact_id': 39,
                'list_id': 0,
                'revision': 0,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'foo',
                        'middle': 'bar',
                        'last': 'baz'
                    })),
                    'emails': has_items(
                        has_entry('email', 'foo.baz@yandex.ru'),
                        has_entry('email', 'foo.bar@yandex.ru')
                    )
                }),
                'tag_ids': empty()
            }),
            has_entries({
                'contact_id': 38,
                'list_id': 0,
                'revision': 0,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'John',
                        'middle': 'Peter',
                        'last': 'Doe'
                    })),
                    'emails': has_items(
                        has_entry('email', 'john@yandex.ru'),
                        has_entry('email', 'doe@yandex.ru')
                    )
                }),
                'tag_ids': empty()
            }),
            has_entries({
                'contact_id': 37,
                'list_id': 0,
                'revision': 0,
                'vcard': has_entries({
                    'names': has_item(has_entries({
                        'first': 'OvvsUJkx',
                        'middle': 'bFTCbxRb',
                        'last': 'mdDdCune'
                    })),
                    'emails': has_items(
                        has_entry('email', 'wnuFMStu@rDHJy.ehq'),
                        has_entry('email', 'hODoNPSa@FHYev.kqA')
                    )
                }),
                'tag_ids': empty()
            })
        )
    ))


@then('response has contact emails with shared contacts as ungrouped result')
def step_response_has_contact_with_shared_contacts_emails_as_ungrouped_result(context):
    resp = context.response.json()
    assert_that(resp, has_entries({'contact': has_items(
        has_entries({'cid': 1,
                     'id': 2,
                     'name': has_entries({'first': 'John', 'middle': 'Peter', 'last': 'Doe'}),
                     'email': 'doe@yandex.ru'}),
        has_entries({'cid': 1,
                     'id': 1,
                     'name': has_entries({'first': 'John', 'middle': 'Peter', 'last': 'Doe'}),
                     'email': 'john@yandex.ru'}),
        has_entries({'cid': 2,
                     'id': 4,
                     'name': has_entries({'first': 'foo', 'middle': 'bar', 'last': 'baz'}),
                     'email': 'foo.bar@yandex.ru'}),
        has_entries({'cid': 2,
                     'id': 3,
                     'name': has_entries({'first': 'foo', 'middle': 'bar', 'last': 'baz'}),
                     'email': 'foo.baz@yandex.ru'}),
        has_entries({'cid': 1,
                     'id': 1,
                     'name': has_entries({'first': 'Hello', 'middle': 'Kitty'}),
                     'email': 'hello@kitty.cat'})
        ),
        'count': 5
    }))


@then('response has empty lists')
def step_response_has_empty_lists(context):
    resp = context.response.json()
    assert_that(resp, has_entry('lists', empty()))


@then('response has one shared list')
def step_response_has_one_shared_list(context):
    resp = context.response.json()
    assert_that(resp, has_entry('lists', has_items(
        has_entries({
            'list_id': greater_than_or_equal_to(2),
            'name': 'subscribed_list'})
    )))


def get_shared_contact0_matcher():
    return has_entries({
        'contact_id': 1,
        'list_id': 1,
        'revision': 2,
        'vcard': has_entries({
            'names': has_item(has_entries({'first': 'First0', 'middle': 'Middle0'})),
            'emails': empty()
        }),
        'tag_ids': empty(),
        'uri': 'URI0',
        'emails': empty()
    })


def get_shared_contact1_matcher():
    return has_entries({
        'contact_id': 2,
        'list_id': 1,
        'revision': 2,
        'vcard': has_entries({
            'names': has_item(has_entries({'first': 'First1', 'middle': 'Middle1'})),
            'emails': empty()
        }),
        'tag_ids': empty(),
        'uri': 'URI1',
        'emails': empty()
    })


def get_shared_contact2_matcher():
    return has_entries({
        'contact_id': 3,
        'list_id': 1,
        'revision': 2,
        'vcard': has_entries({
            'names': has_item(has_entries({'first': 'First2', 'middle': 'Middle2'})),
            'emails': has_item(has_entries({'email': 'local2@domain2.com', 'label': 'Label2'}))
        }),
        'tag_ids': empty(),
        'uri': 'URI2',
        'emails': has_item(has_entries({'id': 1, 'value': 'local2@domain2.com'}))
    })


def get_shared_contact3_matcher():
    return has_entries({
        'contact_id': 4,
        'list_id': 1,
        'revision': 2,
        'vcard': has_entries({
            'names': has_item(has_entries({'first': 'First3', 'middle': 'Middle3'})),
            'emails': has_item(has_entries({'email': 'local3@domain3.com', 'label': 'Label3'}))
        }),
        'tag_ids': empty(),
        'uri': 'URI3',
        'emails': has_item(has_entries({'id': 2, 'value': 'local3@domain3.com'}))
    })


def get_shared_contact4_matcher():
    return has_entries({
        'contact_id': 5,
        'list_id': 1,
        'revision': 2,
        'vcard': has_entries({
            'names': has_item(has_entries({'first': 'First4', 'middle': 'Middle4'})),
            'emails': has_item(has_entries({'email': 'local4@domain4.com', 'label': 'Label4'}))
        }),
        'tag_ids': empty(),
        'uri': 'URI4',
        'emails': has_item(has_entries({'id': 3, 'value': 'local4@domain4.com'}))
    })


def get_all_shared_contacts_from_list_matchers():
    return (get_shared_contact0_matcher(),
            get_shared_contact1_matcher(),
            get_shared_contact2_matcher(),
            get_shared_contact3_matcher(),
            get_shared_contact4_matcher())


def get_all_shared_contacts_with_emails_from_list_matchers():
    return (get_shared_contact2_matcher(),
            get_shared_contact3_matcher(),
            get_shared_contact4_matcher())


def get_all_shared_contacts_from_list_with_offset_and_limit_matchers():
    return (get_shared_contact1_matcher(),
            get_shared_contact2_matcher(),
            get_shared_contact3_matcher())


def get_all_shared_contacts_with_emails_from_list_with_offset_and_limit_matchers():
    return (get_shared_contact3_matcher(),
            get_shared_contact4_matcher())


def get_all_shared_contacts_from_list_by_ids_with_offset_and_limit_matchers():
    return (get_shared_contact2_matcher(),
            get_shared_contact3_matcher())


def get_all_shared_contacts_with_emails_from_list_by_ids_with_offset_and_limit_matchers():
    return (get_shared_contact4_matcher(),)


@then(parsers.parse('response has shared contact count from list equal to "{count:d}"'))
def step_response_has_shared_contact_with_emails_count_from_list(context, count):
    resp = context.response.json()
    assert_that(resp, has_entry('count', equal_to(count)))


@then('response has all shared contacts from list')
def step_response_has_all_shared_contacts_from_list(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        *get_all_shared_contacts_from_list_matchers())))


@then('response has all shared contacts with emails from list')
def step_response_has_all_shared_contacts_with_emails_from_list(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        *get_all_shared_contacts_with_emails_from_list_matchers())))


@then('response has all shared contacts from list with offset and limit')
def step_response_has_all_shared_contacts_from_list_with_offset_and_limit(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        *get_all_shared_contacts_from_list_with_offset_and_limit_matchers())))


@then('response has all shared contacts with emails from list with offset and limit')
def step_response_has_all_shared_contacts_with_emails_from_list_with_offset_and_limit(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        *get_all_shared_contacts_with_emails_from_list_with_offset_and_limit_matchers())))


@then('response has all shared contacts from list by IDs with offset and limit')
def step_response_has_all_shared_contacts_from_list_by_ids_with_offset_and_limit(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        *get_all_shared_contacts_from_list_by_ids_with_offset_and_limit_matchers())))


@then('response has all shared contacts with emails from list by IDs with offset and limit')
def step_response_has_all_shared_contacts_with_emails_from_list_by_ids_with_offset_and_limit(context):
    resp = context.response.json()
    assert_that(resp, has_entry('contacts', strictly_contains(
        *get_all_shared_contacts_with_emails_from_list_by_ids_with_offset_and_limit_matchers())))


def read_json(path):
    with open(path) as stream:
        return json.load(stream)


def get_tvmknife_path():
    return yatest.common.binary_path('passport/infra/tools/tvmknife/bin/tvmknife')


def generate_login(length=8):
    return ''.join(random.choice(string.letters) for _ in xrange(length)) + '@yandex.ru'


def create_tag_contacts(context, tag_id):
    context.components[Mdb].execute(
        '''
        SELECT code.tag_contacts(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_tag_id := %(tag_id)s::bigint,
            i_contact_ids := %(contact_ids)s::bigint[],
            i_x_request_id := %(x_request_id)s::text
        )
        ''',
        user_id=context.uid,
        user_type='passport_user',
        tag_id=tag_id,
        contact_ids=context.created_contact_ids,
        x_request_id=context.request_id)


def create_contacts_user(context, uid, user_type):
    return context.components[Mdb].query('''
        SELECT * FROM code.create_contacts_user(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_x_request_id := %(x_request_id)s::text
        )
    ''', user_id=uid, user_type=user_type, x_request_id=context.request_id)


def purge_contacts_user(context, uid, user_type):
    return context.components[Mdb].query('''
        SELECT * FROM code.purge_contacts_user(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_force := true
        )
    ''', user_id=uid, user_type=user_type)


def create_contacts(context, uid, user_type, contacts):
    return context.components[Mdb].query('''
        SELECT code.create_contacts(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_contacts := {contacts},
            i_x_request_id := %(x_request_id)s::text
        )
    '''.format(contacts=contacts), user_id=uid, user_type=user_type, x_request_id=context.request_id)


def create_contacts_emails(context, uid, user_type, emails):
    return context.components[Mdb].query('''
        SELECT code.create_contacts_emails(
            i_user_id := %(user_id)s::bigint,
            i_user_type := %(user_type)s::contacts.user_type,
            i_emails := {emails},
            i_x_request_id := %(x_request_id)s::text
        )
    '''.format(emails=emails), user_id=uid, user_type=user_type, x_request_id=context.request_id)


def get_org_id(context):
    return context.components[Mdb].query('''
        SELECT COALESCE(MAX(user_id) + 1 ,100001)
          FROM contacts.users
         WHERE user_type = %(user_type)s::contacts.user_type
    ''', user_type='connect_organization')


def share_contacts_list(context):
    return context.components[Mdb].query(
        '''
        SELECT code.share_contacts_list(
            i_user_id := %(user_id)s::bigint,
            i_user_type := 'connect_organization'::contacts.user_type,
            i_list_id := 1::bigint,
            i_client_user_id := %(client_user_id)s::bigint,
            i_client_user_type := 'passport_user'::contacts.user_type,
            i_x_request_id := %(x_request_id)s::text
        )
        ''',
        user_id=context.org_id,
        client_user_id=context.uid,
        x_request_id=context.request_id)


def create_user_contacts_list(context):
    return context.components[Mdb].query(
        '''
        SELECT list_id FROM code.create_contacts_list(
            i_user_id := %(user_id)s::bigint,
            i_user_type := 'passport_user'::contacts.user_type,
            i_name := 'subscribed_list'::text,
            i_type := 'user'::contacts.list_type,
            i_x_request_id := %(x_request_id)s::text
        )
        ''',
        user_id=context.uid,
        x_request_id=context.request_id)


def subscribe_to_contacts_list(context):
    return context.components[Mdb].query(
        '''
        SELECT code.subscribe_to_contacts_list(
            i_user_id := %(user_id)s::bigint,
            i_user_type := 'passport_user'::contacts.user_type,
            i_list_id := %(list_id)s::bigint,
            i_owner_user_id := %(owner_user_id)s::bigint,
            i_owner_user_type := 'connect_organization'::contacts.user_type,
            i_owner_list_id := 1::bigint,
            i_x_request_id := %(x_request_id)s::text
        )
        ''',
        user_id=context.uid,
        list_id=context.subscribed_list_id,
        owner_user_id=context.org_id,
        x_request_id=context.request_id)


bodies = {
    'carddav_multiget_request':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<request>'
    + '<href>YA-1</href>'
    + '<href>YA-2</href>'
    + '<href>YA-3</href>'
    + '</request>\n',

    'carddav_multiget_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '''<multiget-result found="3">'''
    + '<contact>'
    + '<uri>YA-1</uri>'
    + '<status>200</status>'
    + '<etag>&quot;1-2&quot;</etag>'
    + '<vcard>BEGIN:VCARD&#13;\n'
    + 'VERSION:3.0&#13;\n'
    + 'EMAIL:doe@yandex.ru&#13;\n'
    + 'EMAIL:john@yandex.ru&#13;\n'
    + 'FN:John Peter Doe&#13;\n'
    + 'N:Doe;John;Peter;;&#13;\n'
    + 'END:VCARD&#13;\n'
    + '</vcard>'
    + '</contact>'
    + '<contact>'
    + '<uri>YA-2</uri>'
    + '<status>200</status>'
    + '<etag>&quot;2-2&quot;</etag>'
    + '<vcard>BEGIN:VCARD&#13;\n'
    + 'VERSION:3.0&#13;\n'
    + 'EMAIL:foo.bar@yandex.ru&#13;\n'
    + 'EMAIL:foo.baz@yandex.ru&#13;\n'
    + 'FN:foo bar baz&#13;\n'
    + 'N:baz;foo;bar;;&#13;\n'
    + 'END:VCARD&#13;\n'
    + '</vcard>'
    + '</contact>'
    + '<contact>'
    + '<uri>YA-3</uri>'
    + '<status>404</status>'
    + '</contact>'
    + '</multiget-result>\n',

    'carddav_empty_multiget_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '''<multiget-result found="3">'''
    + '<contact>'
    + '<uri>YA-1</uri>'
    + '<status>404</status>'
    + '</contact>'
    + '<contact>'
    + '<uri>YA-2</uri>'
    + '<status>404</status>'
    + '</contact>'
    + '<contact>'
    + '<uri>YA-3</uri>'
    + '<status>404</status>'
    + '</contact>'
    + '</multiget-result>\n',

    'carddav_propfind_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<response>'
    + '<contact>'
    + '<uri>YA-1</uri>'
    + '<displayname>John Peter Doe</displayname>'
    + '<etag>&quot;1-2&quot;</etag>'
    + '</contact>'
    + '<contact>'
    + '<uri>YA-2</uri>'
    + '<displayname>foo bar baz</displayname>'
    + '<etag>&quot;2-2&quot;</etag>'
    + '</contact>'
    + '</response>\n',

    'carddav_empty_propfind_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<response/>\n',

    'carddav_put_request':
    'BEGIN:VCARD\r\n'
    + 'VERSION:3.0\r\n'
    + 'UID:YAAB-671844354-1\r\n'
    + 'BDAY:2019-04-19\r\n'
    + 'EMAIL:server@domain.ru\r\n'
    + 'FN:Server\r\n'
    + 'N:;Server;;;\r\n'
    + 'TEL:9876543210\r\n'
    + 'END:VCARD\r\n',

    'carddav_put_successful_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<put-response>'
    + '<status>200</status>'
    + '<etag>&quot;%2A&quot;</etag>'
    + '</put-response>\n',

    'carddav_put_unsuccessful_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<put-response>'
    + '<status>200</status>'
    + '<description>etag mismatch, contact etag (&quot;69-64&quot; != &quot;68-63&quot;)</description>'
    + '</put-response>\n',

    'carddav_put_uri_not_found_update_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<put-response>'
    + '<status>404</status>'
    + '<description>uri not found</description>'
    + '</put-response>\n',

    'carddav_put_etag_mismatch_update_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<put-response>'
    + '<status>409</status>'
    + '<description>etag mismatch, contact etag (&quot;1-2&quot; != &quot;3-5&quot;)</description>'
    + '</put-response>\n',

    'carddav_put_successful_create_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<put-response>'
    + '<status>201</status>'
    + '<etag>&quot;1-5&quot;</etag>'
    + '</put-response>\n',

    'carddav_put_successful_update_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<put-response>'
    + '<status>200</status>'
    + '<etag>&quot;1-7&quot;</etag>'
    + '</put-response>\n',

    'carddav_put_alredy_exists_create_response':
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<put-response>'
    + '<status>409</status>'
    + '<description>YA-1 alredy exists</description>'
    + '</put-response>\n',

    'search_contacts_grouped_response':
    '''
    {
        "pager" : {
            "items-count" : 3
        },
        "count" : 3,
        "contact" : [
            {
                "name" : {
                    "first" : "foo",
                    "middle" : "bar",
                    "full" : "foo bar baz",
                    "last" : "baz"
                },
                "cid" : 39,
                "email" : [
                    {
                    "last_usage" : 0,
                    "id" : 2,
                    "tags" : [],
                    "usage_count" : 0,
                    "value" : "foo.baz@yandex.ru"
                    },
                    {
                    "value" : "foo.bar@yandex.ru",
                    "tags" : [],
                    "usage_count" : 0,
                    "last_usage" : 0,
                    "id" : 3
                    }
                ],
                "tag" : [],
                "mcid" : 39
            },
            {
                "name" : {
                    "full" : "John Peter Doe",
                    "first" : "John",
                    "middle" : "Peter",
                    "last" : "Doe"
                },
                "cid" : 38,
                "email" : [
                    {
                    "id" : 2,
                    "last_usage" : 0,
                    "usage_count" : 0,
                    "tags" : [],
                    "value" : "john@yandex.ru"
                    },
                    {
                    "id" : 3,
                    "last_usage" : 0,
                    "tags" : [],
                    "value" : "doe@yandex.ru",
                    "usage_count" : 0
                    }
                ],
                "tag" : [],
                "mcid" : 38
            },
            {
                "email" : [
                    {
                    "value" : "wnuFMStu@rDHJy.ehq",
                    "tags" : [],
                    "usage_count" : 0,
                    "id" : 1,
                    "last_usage" : 0
                    },
                    {
                    "tags" : [],
                    "value" : "hODoNPSa@FHYev.kqA",
                    "usage_count" : 0,
                    "last_usage" : 0,
                    "id" : 2
                    }
                ],
                "mcid" : 37,
                "tag" : [],
                "name" : {
                    "full" : "OvvsUJkx bFTCbxRb mdDdCune",
                    "middle" : "bFTCbxRb",
                    "first" : "OvvsUJkx",
                    "last" : "mdDdCune"
                },
                "cid" : 37
            }
        ]
    }
    ''',

    'search_contacts_ungrouped_response':
    '''
    {
        "count": 2,
        "contact": [
            {
                "cid": 0,
                "id": 0,
                "tags": [0, 1],
                "name": {"first": "First0", "middle": "Middle0", "last": "Last0"},
                "email": "local0@domain0.com",
                "photo_partial_url": "photo0.com",
                "company": "company0",
                "department": "department0",
                "title": "title0",
                "summary": "summary0",
                "ya_directory": {"type": "type0", "id": 0}
            },
            {
                "cid": 1,
                "id": 1,
                "tags": [2, 3],
                "name": {"first": "First1", "middle": "Middle1", "last": "Last1"},
                "email": "local1@domain1.com",
                "photo_partial_url": "photo1.com",
                "company": "company1",
                "department": "department1",
                "title": "title1",
                "summary": "summary1",
                "ya_directory": {"type": "type1", "id": 1}
            }
        ]
    }
    ''',

    'search_contacts_ungrouped_response_with_tags':
    '''
    {
        "count": 7,
        "contact": [
            {
                "cid": 0
            },
            {
                "cid": 1,
                "tags": [1]
            },
            {
                "cid": 2,
                "email": "local0@domain0.com"
            },
            {
                "cid": 3,
                "tags": [0],
                "email": "local0@domain0.com"
            },
            {
                "cid": 4,
                "tags": [1],
                "email": "local0@domain0.com"
            },
            {
                "cid": 5,
                "id": 1,
                "tags": [0, 1],
                "email": "local1@domain1.com"
            },
            {
                "cid": 5,
                "tags": [0, 2],
                "email": "local2@domain2.com"
            }
        ]
    }
    ''',

    'vcard_rfc_to_vcard_json_transform_result':
    '''{"emails": [{"email": "server@domain.ru"}], "vcard_uids": ["YAAB-671844354-1"], "events": [{"month": "04", "day": "19",'''
    '''"year": "2019"}],"telephone_numbers": [{"telephone_number": "9876543210"}], "names": [{"middle": "", "prefix": "", "last":'''
    '''"", "suffix":"", "first": "Server"}]}'''
}

SCHEMAS = yatest.common.source_path('mail/collie/tests/integration/schemas')
RESPONSES = yatest.common.source_path('mail/collie/tests/integration/responses')
