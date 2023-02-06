# coding: utf-8

import json

from pymdb.types import (
    ContactsUserType,
    NewContact,
    NewContactsEmail
)

from pymdb.operations import (
    CreateContacts,
    CreateContactsEmails,
    CreateContactsList,
    CreateContactsTag,
    TagContacts,
    TagContactsEmails,
    SubscribeToContactsList
)

from pymdb.queries import Queries
from tests_common.pytest_bdd import given
from tests_common.steps.mdb import current_user_connection


def make_vcard(first_name, last_name):
    return json.dumps(dict(names=[dict(first=first_name, last=last_name)]))


@given('user has contacts')
def step_given_user_has_contacts(context):
    uid = context.user.uid
    user_type = ContactsUserType('passport_user')
    with current_user_connection(context) as conn:
        queries = Queries(conn, uid)
        default_list_id = queries.passport_user_default_contacts_list_id()[0]

        new_contacts = [NewContact(
            list_id=default_list_id,
            format='vcard_v1',
            vcard=make_vcard('first_name', 'last_name'),
            uri='uri')]
        created_contacts = CreateContacts(conn, uid, user_type)(
            new_contacts=new_contacts,
            x_request_id='tests').commit().result
        created_contact_id = created_contacts[0]['contact_ids'][0]

        new_contact_emails = [NewContactsEmail(
            contact_id=created_contact_id,
            email='email',
            type=['type'],
            label='label')]
        created_contact_emails = CreateContactsEmails(conn, uid, user_type)(
            new_emails=new_contact_emails,
            x_request_id='tests').commit().result
        created_contact_email_id = created_contact_emails[0]['email_ids'][0]

        created_tag = CreateContactsTag(conn, uid, user_type)(
            tag_name='tag_name',
            tag_type='user',
            x_request_id='tests').commit().result
        created_tag_id = created_tag[0]['tag_id']

        TagContacts(conn, uid, user_type)(
            tag_id=created_tag_id,
            contact_ids=[created_contact_id],
            x_request_id='tests').commit()

        TagContactsEmails(conn, uid, user_type)(
            tag_id=created_tag_id,
            email_ids=[created_contact_email_id],
            x_request_id='tests').commit()

        created_list = CreateContactsList(conn, uid, user_type)(
            list_name='list_name',
            list_type='user',
            x_request_id='tests').commit().result
        created_list_id = created_list[0]['list_id']

        SubscribeToContactsList(conn, uid, user_type)(
            list_id=created_list_id,
            owner_user_id=1,
            owner_user_type=ContactsUserType('connect_organization'),
            owner_list_id=1,
            x_request_id='tests').commit()
