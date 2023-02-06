# coding: utf-8

import json
import yaml

from hamcrest import (
    anything,
    assert_that,
    empty,
    equal_to,
    greater_than_or_equal_to,
    has_entries,
    has_entry,
    has_item,
    has_length,
    has_properties,
    is_,
    not_,
    only_contains
)

from pymdb.operations import (
    CreateContactsUser,
    DeleteContactsUser,
    CreateContactsList,
    DeleteContactsList,
    UpdateContactsList,
    CreateContactsTag,
    DeleteContactsTag,
    UpdateContactsTag,
    CreateContacts,
    DeleteContacts,
    UpdateContacts,
    TagContacts,
    UntagContacts,
    UntagContactsCompletely,
    ShareContactsList,
    RevokeContactsList,
    SubscribeToContactsList,
    RevokeSubscribedContactsList,
    RestoreContacts,
    CreateContactsEmails,
    DeleteContactsEmails,
    UpdateContactsEmails,
    TagContactsEmails,
    UntagContactsEmails,
    UntagContactsEmailsCompletely,
    AddContactsDirectoryEvent,
    CompleteSyncContactsDirectoryEvent,
)

from pymdb.tools import mark_contacts_user_as_moved_from_here

from pymdb.types import (
    NewContact,
    UpdatedContact,
    NewContactsEmail,
    UpdatedContactsEmail,
)

from tests_common.pytest_bdd import given, when, then


@given('new contacts "{user_type:ContactsUserType}" user')
def step_new_contacts_user(context, user_type):
    context.user_id = context.uid
    context.user_type = user_type
    contacts_user_exists(context)
    contacts_user_has_serials(context)
    context.contact_ids = {
        '$nonexistent_contact_id': 0,
    }
    context.contacts_tag_ids = {
        '$nonexistent_tag_id': 0,
        '$invited_tag_id': context.qs.contacts_tag_by_type_and_name(
            user_id=context.user_id,
            user_type=context.user_type,
            tag_type='system',
            tag_name='Invited',
        )[0].tag_id,
    }
    context.contacts_list_ids = {
        '$nonexistent_list_id': 0,
        '$personal_list_id': context.qs.contacts_list_by_type_and_name(
            user_id=context.user_id,
            user_type=context.user_type,
            list_type='personal',
            list_name='Personal',
        )[0].list_id,
    }
    context.email_ids = {
        '$nonexistent_email_id': 0,
    }
    context.revisions = dict()
    context.event_ids = dict()


def new_contacts_tag(context, tag_type, tag_name, tag_id):
    create_contacts_tag(context=context, tag_type=tag_type, tag_name=tag_name)
    context.contacts_tag_ids[tag_id] = context.last_operation_result[0]['tag_id']
    contacts_user_has_serials(context)


@given('new contacts "{tag_type}" tag "{tag_name}" as "{tag_id}"')
def step_new_contacts_tag(context, tag_type, tag_name, tag_id):
    new_contacts_tag(context, tag_type, tag_name, tag_id)


@given('new contacts "{tag_type}" tags')
def step_new_contacts_tags(context, tag_type):
    for tag_id in (v['tag_id'] for v in context.table):
        new_contacts_tag(context=context, tag_type=tag_type, tag_name='Tag'+tag_id, tag_id=tag_id)


@given('new contacts "{tag_id}" tag revision as "{tag_revision}"')
def step_new_contacts_tag_revision(context, tag_id, tag_revision):
    context.revisions[tag_revision] = context.qs.contacts_tag_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        tag_id=context.contacts_tag_ids[tag_id],
    )[0].revision


@given('new contacts "{list_type}" list "{list_name}" as "{list_id}"')
def step_new_contacts_list(context, list_type, list_name, list_id):
    create_contacts_list(context=context, list_type=list_type, list_name=list_name)
    context.contacts_list_ids[list_id] = context.last_operation_result[0]['list_id']
    contacts_user_has_serials(context)


@given('new contacts "{list_id}" list revision as "{list_revision}"')
def step_new_contacts_list_revision(context, list_id, list_revision):
    context.revisions[list_revision] = context.qs.contacts_list_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        list_id=context.contacts_list_ids[list_id],
    )[0].revision


@given('new contacts')
def step_new_contacts(context):
    for n, row in enumerate(context.table):
        row.setdefault('list_id', '$personal_list_id')
        row.setdefault('first_name', str(n))
        row.setdefault('last_name', str(n))
    create_contacts(context=context)
    contacts_user_has_serials(context)


@given('new contacts emails')
def step_new_contacts_emails(context):
    for n, row in enumerate(context.table):
        row.setdefault('email', str(n) + '@yandex.ru')
        row.setdefault('type', 'null')
        row.setdefault('label', None)
    create_contacts_emails(context=context)
    contacts_user_has_serials(context)


@given('new contact "{contact_id}" revision as "{revision}"')
def step_new_contact_revision(context, contact_id, revision):
    context.revisions[revision] = context.qs.contact_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        contact_id=context.contact_ids[contact_id],
    )[0].revision


@given('new contacts email "{email_id}" revision as "{revision}"')
def step_new_contacts_email_revision(context, email_id, revision):
    context.revisions[revision] = context.qs.contacts_email_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        email_id=context.email_ids[email_id],
    )[0].revision


@given('new contacts list "{list_id}" is shared to "{client_user_id}" with type "{client_user_type:ContactsUserType}"')
def step_new_contacts_list_is_shared_to(context, list_id, client_user_id, client_user_type):
    share_contacts_list_to_user(
        context=context,
        list_id=list_id,
        client_user_id=client_user_id,
        client_user_type=client_user_type,
    )
    contacts_user_has_serials(context)


@given('new "{list_id}" is subscribed to user "{owner_user_id}" with type "{owner_user_type:ContactsUserType}" contacts list "{owner_list_id}"')
def step_new_contacts_list_is_subscribed_to(context, list_id, owner_user_id, owner_user_type, owner_list_id):
    subscribe_to_contacts_list(
        context=context,
        list_id=list_id,
        owner_user_id=owner_user_id,
        owner_user_type=owner_user_type,
        owner_list_id=owner_list_id
    )
    contacts_user_has_serials(context)


@given('current revision as "{revision}"')
def step_current_revision_is(context, revision):
    context.revisions[revision] = context.qs.contacts_serials(
        user_id=context.user_id,
        user_type=context.user_type,
    ).next_revision - 1


def tagged_contacts_by(context, tag_id):
    tag_contacts_by(context=context, tag_id=tag_id)
    contacts_user_has_serials(context)


@given('tagged contacts by "{tag_id}"')
def step_tagged_contacts_by(context, tag_id):
    tagged_contacts_by(context, tag_id)


@given('tagged contacts by "{tag_ids:IdRange}" tags')
def step_tag_contacts_by_multiple_tags(context, tag_ids):
    for tag_id in tag_ids:
        tagged_contacts_by(context=context, tag_id=tag_id)


def tagged_contacts_emails_by(context, tag_id):
    tag_contacts_emails_by(context=context, tag_id=tag_id)
    contacts_user_has_serials(context)


@given('tagged contacts emails by "{tag_id}"')
def step_tagged_contacts_emails_by(context, tag_id):
    tagged_contacts_emails_by(context, tag_id)


@given('tagged contacts emails by "{tag_ids:IdRange}" tags')
def step_tagged_contacts_emails_by_multiple_tags(context, tag_ids):
    for tag_id in tag_ids:
        tagged_contacts_emails_by(context=context, tag_id=tag_id)


@given('pending directory event with id "{event_id}"')
def step_pending_directory_event(context, event_id):
    context.event_ids[event_id] = context.qs.contacts_user(
        user_id=context.user_id,
        user_type=context.user_type,
    ).directory_last_event_id + 1
    add_contacts_directory_event_with_id(context=context, event_id=context.event_ids[event_id])


@when('we create contacts "{user_type:ContactsUserType}" user')
def step_create_contacts_user(context, user_type):
    context.last_operation_result = context.make_operation(
        CreateContactsUser,
        user_id=context.uid,
        user_type=user_type,
    )(x_request_id='tests').commit().result
    context.user_id = context.uid
    context.user_type = user_type


@when('we delete contacts user')
def step_delete_contacts_user(context):
    context.make_operation(
        DeleteContactsUser,
        user_id=context.user_id,
        user_type=context.user_type,
    )(x_request_id='tests').commit()


@when('we mark contacts user as moved from here')
def step_mark_contacts_user_as_moved_from_here(context):
    mark_contacts_user_as_moved_from_here(conn=context.conn, uid=context.user_id, is_deleted=False)


def create_contacts_list(context, list_name, list_type):
    context.last_operation_result = context.make_operation(
        CreateContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_name=list_name,
        list_type=list_type,
        x_request_id='tests',
    ).commit().result


@when('we create contacts "{list_type}" list "{list_name}"')
def step_create_contacts_list(context, list_name, list_type):
    create_contacts_list(context, list_name, list_type)


@when('we create contacts list "{list_id}" with type "{list_type}" and name "{list_name}"')
def step_create_contacts_list_with_type_and_name(context, list_id, list_name, list_type):
    context.last_operation_result = context.make_operation(
        CreateContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_name=list_name,
        list_type=list_type,
        x_request_id='tests',
    ).commit().result
    context.contacts_list_ids[list_id] = context.last_operation_result[0]['list_id']


@when('we try create contacts "{list_type}" list "{list_name}" as "{op_id}"')
def step_try_create_contacts_list(context, list_name, list_type, op_id):
    context.operations[op_id] = context.make_async_operation(
        CreateContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_name=list_name,
        list_type=list_type,
        x_request_id='tests',
    )


@when('we delete contacts list "{list_id}"')
def step_delete_contacts_list(context, list_id):
    context.last_operation_result = context.make_operation(
        DeleteContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        x_request_id='tests',
        list_id=context.contacts_list_ids[list_id],
    ).commit().result


@when('we try delete contacts list "{list_id}" as "{op_id}"')
def step_try_delete_contacts_list(context, list_id, op_id):
    context.operations[op_id] = context.make_async_operation(
        DeleteContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        x_request_id='tests',
    )


@when('we update contacts list "{list_id}" name to "{list_name}" without revision')
def step_update_contacts_list(context, list_id, list_name):
    context.last_operation_result = context.make_operation(
        UpdateContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        list_name=list_name,
        x_request_id='tests',
    ).commit().result


@when('we update contacts list "{list_id}" name to "{list_name}" with "{revision}"')
def step_update_contacts_list_with_revision(context, list_id, list_name, revision):
    context.last_operation_result = context.make_operation(
        UpdateContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        list_name=list_name,
        x_request_id='tests',
        revision=context.revisions[revision],
    ).commit().result


@when('we try update contacts list "{list_id:OpID}" name to "{list_name:w}" as "{op_id}"')
def step_try_update_contacts_list(context, list_id, list_name, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        list_name=list_name,
        x_request_id='tests',
    )


@when('we try update contacts list "{list_id:OpID}" name to "{list_name:w}" with "{revision:OpID}" as "{op_id:OpID}"')
def step_try_update_contacts_list_with_revision(context, list_id, list_name, revision, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        list_name=list_name,
        x_request_id='tests',
        revision=context.revisions[revision],
    )


def create_contacts_tag(context, tag_name, tag_type):
    context.last_operation_result = context.make_operation(
        CreateContactsTag,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_name=tag_name,
        tag_type=tag_type,
        x_request_id='tests',
    ).commit().result


@when('we create contacts "{tag_type}" tag "{tag_name}"')
def step_create_contacts_tag(context, tag_name, tag_type):
    create_contacts_tag(context, tag_name, tag_type)


@when('we try create contacts "{tag_type}" tag "{tag_name}" as "{op_id}"')
def step_try_create_contacts_tag(context, tag_name, tag_type, op_id):
    context.operations[op_id] = context.make_async_operation(
        CreateContactsTag,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_name=tag_name,
        tag_type=tag_type,
        x_request_id='tests',
    )


@when('we delete contacts tag "{tag_id}"')
def step_delete_contacts_tag(context, tag_id):
    context.last_operation_result = context.make_operation(
        DeleteContactsTag,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        x_request_id='tests'
    ).commit().result


@when('we try delete contacts tag "{tag_id}" as "{op_id}"')
def step_try_delete_contacts_tag(context, tag_id, op_id):
    context.operations[op_id] = context.make_async_operation(
        DeleteContactsTag, user_id=context.user_id, user_type=context.user_type)(
        tag_id=context.contacts_tag_ids[tag_id], x_request_id='tests')


@when('we update contacts tag "{tag_id}" name to "{tag_name}" without revision')
def step_update_contacts_tag(context, tag_id, tag_name):
    context.last_operation_result = context.make_operation(
        UpdateContactsTag,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        tag_name=tag_name,
        x_request_id='tests',
    ).commit().result


@when('we update contacts tag "{tag_id}" name to "{tag_name}" with "{revision}"')
def step_update_contacts_tag_with_revision(context, tag_id, tag_name, revision):
    context.last_operation_result = context.make_operation(
        UpdateContactsTag,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        tag_name=tag_name,
        x_request_id='tests',
        revision=context.revisions[revision],
    ).commit().result


@when('we try update contacts tag "{tag_id:OpID}" name to "{tag_name:w}" as "{op_id:OpID}"')
def step_try_update_contacts_tag(context, tag_id, tag_name, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateContactsTag, user_id=context.user_id, user_type=context.user_type)(
        tag_id=context.contacts_tag_ids[tag_id], tag_name=tag_name, x_request_id='tests')


@when('we try update contacts tag "{tag_id:OpID}" name to "{tag_name:w}" with "{revision:OpID}" as "{op_id:OpID}"')
def step_try_update_contacts_tag_with_revision(context, tag_id, tag_name, revision, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateContactsTag, user_id=context.user_id, user_type=context.user_type)(
        tag_id=context.contacts_tag_ids[tag_id], tag_name=tag_name, x_request_id='tests',
        revision=context.revisions[revision])


def create_contacts(context):
    context.last_operation_result = context.make_operation(
        CreateContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        new_contacts=[NewContact(
            list_id=context.contacts_list_ids[v.get('list_id', '$personal_list_id')],
            format='vcard_v1',
            vcard=make_vcard(v['first_name'], v['last_name']),
            uri=v.get('uri'),
        ) for v in context.table],
        x_request_id='tests',
    ).commit().result
    contact_ids = (v['contact_id'] for v in context.table)
    for contact_id, value in zip(contact_ids, context.last_operation_result[0]['contact_ids']):
        context.contact_ids[contact_id] = value


@when('we create contacts')
def step_create_contacts(context):
    create_contacts(context)


@when('we delete contacts')
def step_delete_contacts(context):
    context.last_operation_result = context.make_operation(
        DeleteContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        contact_ids=[context.contact_ids[v['contact_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we update contacts without revision')
def step_update_contacts_without_revision(context):
    context.last_operation_result = context.make_operation(
        UpdateContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        contacts=[UpdatedContact(
            contact_id=context.contact_ids[v['contact_id']],
            list_id=context.contacts_list_ids[v['list_id']] if 'list_id' in v else None,
            format=v.get('format'),
            vcard=(make_vcard(v.get('first_name'), v.get('last_name'))
                   if 'first_name' and 'last_name' in v else None),
            uri=v.get('uri'),
        ) for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we update contacts with "{revision}"')
def step_update_contacts_with_revision(context, revision):
    context.last_operation_result = context.make_operation(
        UpdateContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        contacts=[UpdatedContact(
            contact_id=context.contact_ids[v['contact_id']],
            list_id=context.contacts_list_ids[v['list_id']] if 'list_id' in v else None,
            format=v.get('format'),
            vcard=(make_vcard(v.get('first_name'), v.get('last_name'))
                   if 'first_name' and 'last_name' in v else None),
            uri=v.get('uri'),
        ) for v in context.table],
        revision=context.revisions[revision],
        x_request_id='tests',
    ).commit().result


@when('we try update contacts with "{revision}" as "{op_id}"')
def step_try_update_contacts_with_revision(context, revision, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        contacts=[UpdatedContact(
            contact_id=context.contact_ids[v['contact_id']],
            list_id=context.contacts_list_ids[v['list_id']] if 'list_id' in v else None,
            format=v.get('format'),
            vcard=(make_vcard(v.get('first_name'), v.get('last_name'))
                   if 'first_name' and 'last_name' in v else None),
            uri=v.get('uri'),
        ) for v in context.table],
        revision=context.revisions[revision],
        x_request_id='tests',
    )


def tag_contacts_by(context, tag_id):
    context.last_operation_result = context.make_operation(
        TagContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        contact_ids=[context.contact_ids[v['contact_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we tag contacts by "{tag_id}"')
def step_tag_contacts_by(context, tag_id):
    tag_contacts_by(context, tag_id)


@when('we untag contacts by "{tag_id}"')
def step_untag_contacts_by(context, tag_id):
    context.last_operation_result = context.make_operation(
        UntagContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        contact_ids=[context.contact_ids[v['contact_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we untag contacts completely')
def step_untag_contacts_completely(context):
    context.last_operation_result = context.make_operation(
        UntagContactsCompletely,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        contact_ids=[context.contact_ids[v['contact_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we try untag contacts by "{tag_id}" as "{op_id}"')
def step_try_untag_contacts_by(context, tag_id, op_id):
    context.operations[op_id] = context.make_async_operation(
        UntagContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        contact_ids=[context.contact_ids[v['contact_id']] for v in context.table],
        x_request_id='tests',
    )


def share_contacts_list_to_user(context, list_id, client_user_id, client_user_type):
    context.last_operation_result = context.make_operation(
        ShareContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        client_user_id=client_user_id,
        client_user_type=client_user_type,
        x_request_id='tests',
    ).commit().result


@when('we share contacts list "{list_id}" to user "{client_user_id}" with type "{client_user_type:ContactsUserType}"')
def step_share_contacts_list_to_user(context, list_id, client_user_id, client_user_type):
    share_contacts_list_to_user(context, list_id, client_user_id, client_user_type)


@when('we revoke contacts list "{list_id}" from user "{client_user_id}" with type "{client_user_type:ContactsUserType}"')
def step_revoke_contacts_list_from_user(context, list_id, client_user_id, client_user_type):
    context.last_operation_result = context.make_operation(
        RevokeContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        client_user_id=client_user_id,
        client_user_type=client_user_type,
        x_request_id='tests',
    ).commit().result


def subscribe_to_contacts_list(context, list_id, owner_user_id, owner_user_type, owner_list_id):
    context.last_operation_result = context.make_operation(
        SubscribeToContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        owner_user_id=owner_user_id,
        owner_user_type=owner_user_type,
        owner_list_id=owner_list_id,
        x_request_id='tests'
    ).commit().result


@when('we subscribe "{list_id}" to user "{owner_user_id}" with type "{owner_user_type:ContactsUserType}" contacts list "{owner_list_id}"')
def step_subscribe_to_contacts_list(context, list_id, owner_user_id, owner_user_type, owner_list_id):
    subscribe_to_contacts_list(context, list_id, owner_user_id, owner_user_type, owner_list_id)


@when('we revoke subscribed "{list_id}" to user "{owner_user_id}" with type "{owner_user_type:ContactsUserType}" contacts list "{owner_list_id}"')
def step_revoke_subscribed_contacts_list(context, list_id, owner_user_id, owner_user_type, owner_list_id):
    context.last_operation_result = context.make_operation(
        RevokeSubscribedContactsList,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        list_id=context.contacts_list_ids[list_id],
        owner_user_id=owner_user_id,
        owner_user_type=owner_user_type,
        owner_list_id=owner_list_id,
        x_request_id='tests'
    ).commit().result


@when('we restore contacts to "{revision}"')
def step_restore_contacts_to(context, revision):
    context.last_operation_result = context.make_operation(
        RestoreContacts,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        revision=context.revisions[revision],
        x_request_id='tests',
    ).commit().result


@when('current revision as "{revision}"')
def step_current_revision_is_2(context, revision):
    context.revisions[revision] = context.qs.contacts_serials(
        user_id=context.user_id,
        user_type=context.user_type,
    ).next_revision - 1


def create_contacts_emails(context):
    context.last_operation_result = context.make_operation(
        CreateContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        new_emails=[NewContactsEmail(
            contact_id=context.contact_ids[v['contact_id']],
            email=v.get('email'),
            type=yaml.safe_load(v['type']) if 'type' in v else None,
            label=v.get('label'),
        ) for v in context.table],
        x_request_id='tests',
    ).commit().result
    email_ids = (v['email_id'] for v in context.table)
    for email_id, value in zip(email_ids, context.last_operation_result[0]['email_ids']):
        context.email_ids[email_id] = value


@when('we create contacts emails')
def step_create_contacts_emails(context):
    create_contacts_emails(context)


@when('we delete contacts emails')
def step_delete_contacts_emails(context):
    context.last_operation_result = context.make_operation(
        DeleteContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        email_ids=[context.email_ids[v['email_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we update contacts emails without revision')
def step_update_contacts_emails_without_revision(context):
    context.last_operation_result = context.make_operation(
        UpdateContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        emails=[UpdatedContactsEmail(
            email_id=context.email_ids[v['email_id']],
            contact_id=context.contact_ids[v['contact_id']] if 'contact_id' in v else None,
            email=v.get('email'),
            type=yaml.load(v['type']) if 'type' in v else None,
            label=v.get('label'),
        ) for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we update contacts emails with "{revision}"')
def step_update_contacts_emails_with_revision(context, revision):
    context.last_operation_result = context.make_operation(
        UpdateContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        emails=[UpdatedContactsEmail(
            email_id=context.email_ids[v['email_id']],
            contact_id=context.contact_ids[v['contact_id']] if 'contact_id' in v else None,
            email=v.get('email'),
            type=yaml.safe_load(v['type']) if 'type' in v else None,
            label=v.get('label'),
        ) for v in context.table],
        revision=context.revisions[revision],
        x_request_id='tests',
    ).commit().result


@when('we try update contacts emails with "{revision}" as "{op_id}"')
def step_try_update_contacts_emails_with_revision(context, revision, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        emails=[UpdatedContactsEmail(
            email_id=context.email_ids[v['email_id']],
            contact_id=context.contact_ids[v['contact_id']] if 'contact_id' in v else None,
            email=v.get('email'),
            type=yaml.load(v['type']) if 'type' in v else None,
            label=v.get('label'),
        ) for v in context.table],
        revision=context.revisions[revision],
        x_request_id='tests',
    )


def tag_contacts_emails_by(context, tag_id):
    context.last_operation_result = context.make_operation(
        TagContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        email_ids=[context.email_ids[v['email_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we tag contacts emails by "{tag_id}"')
def step_tag_contacts_emails_by(context, tag_id):
    tag_contacts_emails_by(context, tag_id)


@when('we try tag contacts emails by "{tag_id}" as "{op_id}"')
def step_try_tag_contacts_emails_by(context, tag_id, op_id):
    context.operations[op_id] = context.make_async_operation(
        TagContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        email_ids=[context.email_ids[v['email_id']] for v in context.table],
        x_request_id='tests',
    )


@when('we untag contacts emails by "{tag_id}"')
def step_untag_contacts_emails_by(context, tag_id):
    context.last_operation_result = context.make_operation(
        UntagContactsEmails,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        tag_id=context.contacts_tag_ids[tag_id],
        email_ids=[context.email_ids[v['email_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


@when('we untag contacts emails completely')
def step_untag_contacts_emails_completely(context):
    context.last_operation_result = context.make_operation(
        UntagContactsEmailsCompletely,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        email_ids=[context.email_ids[v['email_id']] for v in context.table],
        x_request_id='tests',
    ).commit().result


def add_contacts_directory_event_with_id(context, event_id):
    context.last_operation_result = context.make_operation(
        AddContactsDirectoryEvent,
        user_id=context.user_id,
        user_type=context.user_type,
    )(event_id=event_id).commit().result


@when('we add contacts directory event with id "{event_id}"')
def step_add_contacts_directory_event_with_id(context, event_id):
    add_contacts_directory_event_with_id(context, event_id)


@when('we add contacts directory event with id "${event_id}"')
def step_add_contacts_directory_event_with_id_by_var(context, event_id):
    context.last_operation_result = context.make_operation(
        AddContactsDirectoryEvent,
        user_id=context.user_id,
        user_type=context.user_type,
    )(event_id=context.event_ids['$' + event_id]).commit().result


@when('we complete sync contacts directory event at revision "{revision}" with id "{event_id}"')
def step_complete_sync_contacts_diretory_event_at_revision(context, revision, event_id):
    context.last_operation_result = context.make_operation(
        CompleteSyncContactsDirectoryEvent,
        user_id=context.user_id,
        user_type=context.user_type,
    )(
        synced_revision=revision,
        synced_event_id=context.event_ids[event_id]
    ).commit().result


@then('operation result is "{result}"')
def step_operation_result_is(context, result):
    assert_that(context.last_operation_result, has_item(has_entry(anything(), result)))


def contacts_user_exists(context):
    context.contacts_user = context.qs.contacts_user(
        user_id=context.user_id,
        user_type=context.user_type,
    )


@then('contacts user exists')
def step_contacts_user_exists(context):
    contacts_user_exists(context)


@then('contacts user is here')
def step_contacts_user_is_here(context):
    assert_that(context.contacts_user.is_here, is_(True))


@then('contacts user is not here')
def step_contacts_user_is_not_here(context):
    assert_that(context.contacts_user.is_here, is_(False))


@then('contacts user is deleted')
def step_contacts_user_is_deleted(context):
    assert_that(context.contacts_user.is_deleted, is_(True))


@then('contacts user is not deleted')
def step_contacts_user_is_not_deleted(context):
    assert_that(context.contacts_user.is_deleted, is_(False))


def contacts_user_has_serials(context):
    context.contacts_serials = context.qs.contacts_serials(
        user_id=context.user_id,
        user_type=context.user_type,
    )


@then('contacts user has serials')
def step_contacts_user_has_serials(context):
    contacts_user_has_serials(context)


@then('contacts user is purged and inited')
def step_contacts_user_is_purged_and_inited(context):
    contacts_lists = context.qs.passport_user_contacts_lists()
    assert_that(contacts_lists, has_length(1))
    assert_that(contacts_lists, only_contains(has_properties(name='Personal', type='personal')))
    contacts_tags = context.qs.passport_user_contacts_tags()
    assert_that(contacts_tags, has_length(2))
    assert_that(contacts_tags, only_contains(
        has_properties(name='Invited', type='system'),
        has_properties(name='Phone', type='system')))
    contacts_contacts = context.qs.passport_user_contacts_contacts()
    assert_that(contacts_contacts, empty())
    contacts_emails = context.qs.passport_user_contacts_emails()
    assert_that(contacts_emails, empty())
    contacts_contacts_tags = context.qs.passport_user_contacts_contacts_tags()
    assert_that(contacts_contacts_tags, empty())
    contacts_emails_tags = context.qs.passport_user_contacts_emails_tags()
    assert_that(contacts_emails_tags, empty())
    contacts_shared_lists = context.qs.passport_user_contacts_shared_lists()
    assert_that(contacts_shared_lists, empty())
    contacts_subscribed_lists = context.qs.passport_user_contacts_subscribed_lists()
    assert_that(contacts_subscribed_lists, empty())


@then('operation result has next revision')
def step_operation_result_has_next_revision(context):
    assert_that(context.last_operation_result, has_item(has_entries(
        revision=equal_to(context.contacts_serials.next_revision),
    )))


@then('operation result has next list_id')
def step_operation_result_has_next_list_id(context):
    assert_that(context.last_operation_result, has_item(has_entries(
        list_id=equal_to(context.contacts_serials.next_list_id),
    )))


@then('operation result has next tag_id')
def step_operation_result_has_next_tag_id(context):
    assert_that(context.last_operation_result, has_item(has_entries(
        tag_id=equal_to(context.contacts_serials.next_tag_id),
    )))


@then('operation result has next contact_id')
def step_operation_result_has_next_contact_id(context):
    operation_result_has_advanced_by_next_contact_id(context=context, shift=0)


def operation_result_has_advanced_by_next_contact_id(context, shift):
    assert_that(context.last_operation_result, has_item(has_entries(
        contact_ids=has_item(context.contacts_serials.next_contact_id + shift),
    )))


@then('operation result has advanced by "{shift:d}" next contact_id')
def step_operation_result_has_advanced_by_next_contact_id(context, shift):
    operation_result_has_advanced_by_next_contact_id(context, shift)


@then('operation result has advanced by "{shift:d}" next email_id')
def step_operation_result_has_advanced_by_next_email_id(context, shift):
    assert_that(context.last_operation_result, has_item(has_entries(
        email_ids=has_item(context.contacts_serials.next_email_id + shift),
    )))


@then('operation result is current revision')
def step_operation_result_is_current_revision(context):
    operation_result_is_next_revision_advanced_by(context=context, shift=-1)


@then('operation result is next revision')
def step_operation_result_is_next_revision(context):
    operation_result_is_next_revision_advanced_by(context=context, shift=0)


def operation_result_is_next_revision_advanced_by(context, shift):
    assert_that(context.last_operation_result, has_item(has_entry(
        anything(),
        context.contacts_serials.next_revision + shift
    )))


@then('operation result is next revision advanced by "{shift:d}"')
def step_operation_result_is_next_revision_advanced_by(context, shift):
    operation_result_is_next_revision_advanced_by(context, shift)


@then('contacts user has "{list_type}" list "{list_name}"')
def step_contacts_user_has_list(context, list_type, list_name):
    assert_that(
        context.qs.contacts_list_by_type_and_name(
            user_id=context.user_id,
            user_type=context.user_type,
            list_type=list_type,
            list_name=list_name,
        ),
        has_item(has_properties(list_id=greater_than_or_equal_to(1), revision=greater_than_or_equal_to(1)))
    )


@then('contacts user has list "{list_id}" with type "{list_type}" and name "{list_name}"')
def step_contacts_user_has_list_with_type_and_name(context, list_id, list_type, list_name):
    assert_that(
        context.qs.contacts_list_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
        ),
        has_item(has_properties(type=list_type, name=list_name))
    )


@then('contacts user has no "{list_type}" list "{list_name}"')
def step_contacts_user_has_no_list(context, list_type, list_name):
    assert_that(context.qs.contacts_list_by_type_and_name(
        user_id=context.user_id,
        user_type=context.user_type,
        list_type=list_type,
        list_name=list_name,
    ), empty())


@then('contacts user has no list "{list_id}"')
def step_contacts_user_has_no_list_by_id(context, list_id):
    assert_that(
        context.qs.contacts_list_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
        ),
        empty()
    )


@then('contacts list "{list_id}" has name "{list_name}"')
def step_contacts_list_has_name(context, list_id, list_name):
    assert_that(
        context.qs.contacts_list_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
        ),
        has_item(has_properties(name=list_name))
    )


@then('contacts list "{list_id}" has next revision')
def step_contacts_list_has_next_revision(context, list_id):
    assert_that(
        context.qs.contacts_list_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision))
    )


@then('contacts list "{list_id}" has previous revision')
def step_contacts_list_has_previous_revision(context, list_id):
    assert_that(
        context.qs.contacts_list_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision - 1))
    )


@then('contacts user has "{tag_type}" tag "{tag_name}"')
def step_contacts_user_has_tag(context, tag_type, tag_name):
    assert_that(
        context.qs.contacts_tag_by_type_and_name(
            user_id=context.user_id,
            user_type=context.user_type,
            tag_type=tag_type,
            tag_name=tag_name,
        ),
        has_item(has_properties(tag_id=greater_than_or_equal_to(1), revision=greater_than_or_equal_to(1)))
    )


@then('contacts user has tag "{tag_id}" with type "{tag_type}" and name "{tag_name}"')
def step_contacts_user_has_tag_with_type_and_name(context, tag_id, tag_type, tag_name):
    assert_that(
        context.qs.contacts_tag_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            tag_id=context.contacts_tag_ids[tag_id],
        ),
        has_item(has_properties(type=tag_type, name=tag_name))
    )


@then('contacts user has no "{tag_type}" tag "{tag_name}"')
def step_contacts_user_has_no_tag(context, tag_type, tag_name):
    assert_that(context.qs.contacts_tag_by_type_and_name(
        user_id=context.user_id,
        user_type=context.user_type,
        tag_type=tag_type,
        tag_name=tag_name,
    ), empty())


@then('contacts tag "{tag_id}" has name "{tag_name}"')
def step_contacts_tag_has_name(context, tag_id, tag_name):
    assert_that(
        context.qs.contacts_tag_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            tag_id=context.contacts_tag_ids[tag_id],
        ),
        has_item(has_properties(name=tag_name))
    )


@then('contacts tag "{tag_id}" has next revision')
def step_contacts_tag_has_next_revision(context, tag_id, tag_name):
    assert_that(
        context.qs.contacts_tag_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            tag_id=context.contacts_tag_ids[tag_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision))
    )


@then('contacts tag "{tag_id}" has previous revision')
def step_contacts_tag_has_previous_revision(context, tag_id, tag_name):
    assert_that(
        context.qs.contacts_tag_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            tag_id=context.contacts_tag_ids[tag_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision - 1))
    )


@then('contact "{contact_id}" has name "{contact_first_name}" "{contact_last_name}"')
def step_contact_has_name(context, contact_id, contact_first_name, contact_last_name):
    assert_that(
        context.qs.contact_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
        ),
        has_item(has_properties(vcard=has_entries(
            names=has_item(has_entries(first=contact_first_name, last=contact_last_name))
        )))
    )


@then('contact "{contact_id}" has format "{contact_format}"')
def step_contact_has_format(context, contact_id, contact_format):
    assert_that(
        context.qs.contact_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
        ),
        has_item(has_properties(format=contact_format))
    )


@then('contact "{contact_id}" has list "{list_id}"')
def step_contact_has_list(context, contact_id, list_id):
    assert_that(
        context.qs.contact_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
        ),
        has_item(has_properties(list_id=context.contacts_list_ids[list_id]))
    )


@then('contact "{contact_id}" has next revision')
def step_contacts_contact_has_next_revision(context, contact_id):
    assert_that(
        context.qs.contact_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision))
    )


@then('contact "{contact_id}" has previous revision')
def step_contact_has_previous_revision(context, contact_id):
    assert_that(
        context.qs.contact_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision - 1))
    )


@then('contact "{contact_id}" has uri "{uri}"')
def step_contact_has_uri(context, contact_id, uri):
    assert_that(
        context.qs.contact_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
        ),
        has_item(has_properties(uri=uri))
    )


@then('contacts user has contact "{contact_id}"')
def step_contacts_user_has_contact(context, contact_id):
    assert_that(context.qs.contact_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        contact_id=context.contact_ids[contact_id],
    ), not_(empty()))


@then('contacts user has no contact "{contact_id}"')
def step_contacts_user_has_no_contact(context, contact_id):
    assert_that(context.qs.contact_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        contact_id=context.contact_ids[contact_id],
    ), empty())


@then('contacts change log ends with')
def step_contacts_change_log_compare(context):
    change_log = context.qs.contacts_change_log(
        user_id=context.user_id,
        user_type=context.user_type,
    )
    for row, values in enumerate(zip(reversed(change_log), reversed(context.table))):
        actual, expected = values
        for k, v in expected.items():
            if isinstance(actual[k], (dict, list)):
                expected[k] = yaml.safe_load(v)
            else:
                expected[k] = type(actual[k])(v)
        assert_that(actual, has_entries(**expected), 'at row %d: %s' % (len(change_log) - row - 1, actual))


@then('contacts "{serial_name}" serial is incremented')
def step_contacts_serial_is_incremented(context, serial_name):
    contacts_serial_is_advanced_by(context=context, serial_name=serial_name, shift=1)


def contacts_serial_is_advanced_by(context, serial_name, shift):
    assert_that(
        getattr(context.qs.contacts_serials(
            user_id=context.user_id,
            user_type=context.user_type,
        ), serial_name),
        getattr(context.contacts_serials, serial_name) + shift
    )


@then('contacts "{serial_name}" serial is advanced by "{shift:d}"')
def step_contacts_serial_is_advanced_by(context, serial_name, shift):
    contacts_serial_is_advanced_by(context, serial_name, shift)


@then('contacts "{serial_name}" serial is not changed')
def step_contacts_serial_is_not_changed(context, serial_name):
    assert_that(
        getattr(context.qs.contacts_serials(
            user_id=context.user_id,
            user_type=context.user_type,
        ), serial_name),
        getattr(context.contacts_serials, serial_name)
    )


@then('contact "{contact_id}" has tag "{tag_id}"')
def step_contact_has_tag(context, contact_id, tag_id):
    assert_that(
        context.qs.contact_tag(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
            tag_id=context.contacts_tag_ids[tag_id]
        ),
        has_item(has_properties(revision=greater_than_or_equal_to(1)))
    )


@then('contact "{contact_id}" has no tag "{tag_id}"')
def step_contact_has_no_tag(context, contact_id, tag_id):
    assert_that(
        context.qs.contact_tag(
            user_id=context.user_id,
            user_type=context.user_type,
            contact_id=context.contact_ids[contact_id],
            tag_id=context.contacts_tag_ids[tag_id]
        ),
        empty()
    )


@then('contacts list "{list_id}" is shared to "{client_user_id}" with type "{client_user_type:ContactsUserType}"')
def step_contacts_list_is_shared_to(context, list_id, client_user_id, client_user_type):
    assert_that(
        context.qs.shared_contacts_list_to(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
            client_user_id=client_user_id,
            client_user_type=client_user_type,
        ),
        has_item(has_properties(revision=greater_than_or_equal_to(1))),
    )


@then('contacts list "{list_id}" is not shared to "{client_user_id}" with type "{client_user_type:ContactsUserType}"')
def step_contacts_list_is_not_shared_to(context, list_id, client_user_id, client_user_type):
    assert_that(
        context.qs.shared_contacts_list_to(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
            client_user_id=client_user_id,
            client_user_type=client_user_type,
        ),
        empty(),
    )


@then('"{list_id}" is subscribed to user "{owner_user_id}" with type "{owner_user_type:ContactsUserType}" contacts list "{owner_list_id}"')
def step_contacts_list_is_subscribed_to(context, list_id, owner_user_id, owner_user_type, owner_list_id):
    assert_that(
        context.qs.subscribed_contacts_list_to(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
            owner_user_id=owner_user_id,
            owner_user_type=owner_user_type,
            owner_list_id=owner_list_id
        ),
        has_item(has_properties(revision=greater_than_or_equal_to(1))),
    )


@then('"{list_id}" is not subscribed to user "{owner_user_id}" with type "{owner_user_type:ContactsUserType}" contacts list "{owner_list_id}"')
def step_contacts_list_is_not_subscribed_to(context, list_id, owner_user_id, owner_user_type, owner_list_id):
    assert_that(
        context.qs.subscribed_contacts_list_to(
            user_id=context.user_id,
            user_type=context.user_type,
            list_id=context.contacts_list_ids[list_id],
            owner_user_id=owner_user_id,
            owner_user_type=owner_user_type,
            owner_list_id=owner_list_id
        ),
        empty()
    )


@then('contacts email "{email_id}" is "{email}"')
def step_contacts_email_is(context, email_id, email):
    assert_that(
        context.qs.contacts_email_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
        ),
        has_item(has_properties(email=equal_to(email)))
    )


@then('contacts email "{email_id}" belongs to contact "{contact_id}"')
def step_contacts_email_belongs_to_contact(context, email_id, contact_id):
    assert_that(
        context.qs.contacts_email_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
        ),
        has_item(has_properties(contact_id=equal_to(context.contact_ids[contact_id])))
    )


@then('contacts email "{email_id}" has type "{type_}"')
def step_contacts_email_has_type(context, email_id, type_):
    assert_that(
        context.qs.contacts_email_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
        ),
        has_item(has_properties(type=equal_to(yaml.safe_load(type_))))
    )


@then('contacts email "{email_id}" has label "{label}"')
def step_contacts_email_has_label(context, email_id, label):
    assert_that(
        context.qs.contacts_email_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
        ),
        has_item(has_properties(label=equal_to(label)))
    )


@then('contacts user has email "{email_id}"')
def step_contacts_user_has_email(context, email_id):
    assert_that(context.qs.contacts_email_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        email_id=context.email_ids[email_id],
    ), not_(empty()))


@then('contacts user has no email "{email_id}"')
def step_contacts_user_has_no_email(context, email_id):
    assert_that(context.qs.contacts_email_by_id(
        user_id=context.user_id,
        user_type=context.user_type,
        email_id=context.email_ids[email_id],
    ), empty())


@then('contacts email "{email_id}" has previous revision')
def step_contacts_email_has_previous_revision(context, email_id):
    assert_that(
        context.qs.contacts_email_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision - 1))
    )


@then('contacts email "{email_id}" has next revision')
def step_contacts_email_has_next_revision(context, email_id):
    assert_that(
        context.qs.contacts_email_by_id(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
        ),
        has_item(has_properties(revision=context.contacts_serials.next_revision))
    )


@then('contacts email "{email_id}" has tag "{tag_id}"')
def step_contacts_email_has_tag(context, email_id, tag_id):
    assert_that(
        context.qs.contacts_email_tag(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
            tag_id=context.contacts_tag_ids[tag_id]
        ),
        has_item(has_properties(revision=greater_than_or_equal_to(1)))
    )


@then('contacts email "{email_id}" has no tag "{tag_id}"')
def step_contacts_email_has_no_tag(context, email_id, tag_id):
    assert_that(
        context.qs.contacts_email_tag(
            user_id=context.user_id,
            user_type=context.user_type,
            email_id=context.email_ids[email_id],
            tag_id=context.contacts_tag_ids[tag_id]
        ),
        empty()
    )


@then('directory sync is enabled for contacts user')
def step_directory_sync_is_enabled_for_contacts_user(context):
    assert_that(
        context.qs.contacts_user(
            user_id=context.user_id,
            user_type=context.user_type,
        ),
        has_properties(is_directory_sync_enabled=True)
    )


@then('last directory event id is "{event_id:d}"')
def step_last_directory_event_id_is(context, event_id):
    assert_that(
        context.qs.contacts_user(
            user_id=context.user_id,
            user_type=context.user_type,
        ),
        has_properties(directory_last_event_id=event_id)
    )


@then('pending directory events count is "{count:d}"')
def step_pending_directory_events_count_is(context, count):
    assert_that(
        context.qs.contacts_user(
            user_id=context.user_id,
            user_type=context.user_type,
        ),
        has_properties(directory_pending_events_count=count)
    )


@then('directory synced revision is "{revision:d}"')
def step_directory_synced_revision_is(context, revision):
    assert_that(
        context.qs.contacts_user(
            user_id=context.user_id,
            user_type=context.user_type,
        ),
        has_properties(directory_synced_revision=revision)
    )


@then('last synced directory event id is "{event_id}"')
def step_last_synced_directory_event_id_is(context, event_id):
    assert_that(
        context.qs.contacts_user(
            user_id=context.user_id,
            user_type=context.user_type,
        ),
        has_properties(directory_last_synced_event_id=context.event_ids[event_id])
    )


def make_vcard(first_name, last_name):
    return json.dumps(dict(names=[dict(first=first_name, last=last_name)]))
