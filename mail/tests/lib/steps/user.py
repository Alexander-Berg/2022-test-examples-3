import yaml
from datetime import datetime, timedelta
from tests_common.pytest_bdd import given, when, then
from tests_common.register import make_stids_for_user
from tests_common.fbbdb import remove_user as remove_user_from_fbb, update_user_sids as update_user_sids_in_fbb
from mail.pypg.pypg.common import transaction
from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse, HttpMethod
from ora2pg.sharpei import init_in_sharpei
from ora2pg.storage import MulcaGate

import pymdb.operations as OPS
import pymdb.types as TYP
from pymdb.queries import Queries
from pymdb.vegetarian import fill_messages_in_folder, messages_in_folder_generator, MAILISH_AUTH_DATA

from .sharddb import move_user_to_deleted

from hamcrest import (
    equal_to,
)


def create_stids(config, suid, limit):
    return list(make_stids_for_user(
        MulcaGate(
            host=config['mulcagate'].host,
            port=config['mulcagate'].port,
            mg_ca_path=config['mulcagate'].ssl_cert_path,
        ),
        str(suid),
        limit=limit,
    ))


@given('he is absent in blackbox')
def given_new_registered_pg_user(context):
    user = context.get_user()
    with transaction(context.config['fbbdb']) as fbbdb_conn:
        remove_user_from_fbb(fbbdb_conn, login=user.login)


@given('new user "{user_name:w}" in blackbox')
def step_new_user_in_blackbox(context, user_name):
    assert user_name not in context.users, \
        'user_name %s already used %r' % (user_name, context.users[user_name])

    user = context.make_new_user_in_blackbox(user_name)
    context.users[user_name] = user
    context.last_user_name = user_name


def new_user(context, user_name):
    assert user_name not in context.users, \
        'user_name %s already used %r' % (user_name, context.users[user_name])

    user = context.make_new_user(user_name)
    context.users[user_name] = user
    context.last_user_name = user_name


@given('new user "{user_name:w}"')
@given('new user "{user_name:w}" in main shard')
def step_new_user(context, user_name):
    new_user(context, user_name)


@given('new user "{user_name:w}" with uid "{uid}"')
def step_new_user_with_uid(context, user_name, uid):
    assert user_name not in context.users, \
        'user_name %s already used %r' % (user_name, context.users[user_name])

    user = context.make_new_user(user_name, uid)
    context.users[user_name] = user
    context.last_user_name = user_name


@given('{limit:d} new users named as "{group_name:w}" group')
def step_new_group_of_users(context, group_name, limit):
    for n in iter(range(limit)):
        user_name = group_name + str(n)
        new_user(context, user_name)


@given('"{user_name:w}" exists in sharddb')
def step_user_in_sharddb(context, user_name):
    user = context.get_user(user_name)
    init_in_sharpei(
        uid=user.uid,
        dsn=context.config['sharddb'],
        allow_inited=False,
        shard_id=context.config['shard_id'])


@given('"{user_name:w}" does not exists in sharddb')
def step_user_not_in_sharddb(context, user_name):
    pass


@given('"{user_name:w}" has one message in "{folder_type:w}"')
def step_store_messages_user(context, folder_type, user_name):
    return step_store_messages_impl(**locals())


@given('she has "{limit:d}" messages in "{folder_type:w}"')
def step_given_store_messages_limit(context, folder_type, limit):
    return step_store_messages_impl(**locals())


@when('she has "{limit:d}" messages in "{folder_type:w}"')
def step_when_store_messages_limit(context, folder_type, limit):
    return step_store_messages_impl(**locals())


def step_store_messages_impl(context, folder_type, user_name=None, limit=1):
    user = context.get_user(user_name)
    with transaction(context.config['maildb']) as conn:
        folder = Queries(conn, user.uid).folder_by_type(folder_type)
        fill_messages_in_folder(
            conn=conn,
            uid=user.uid,
            folder=folder,
            limit=limit,
            stids=create_stids(
                context.config,
                user.suid,
                limit,
            )
        )


@given('"{user_name:w}" has one "{stid_type:StidType}" message in "{folder_type:w}"')
def step_store_typed_messages_user(context, folder_type, stid_type, user_name):
    return step_store_typed_messages_impl(**locals())


@given('she has "{limit:d}" "{stid_type:StidType}" messages in "{folder_type:w}"')
def step_given_store_typed_messages_limit(context, folder_type, stid_type, limit):
    return step_store_typed_messages_impl(**locals())


@when('she has "{limit:d}" "{stid_type:StidType}" messages in "{folder_type:w}"')
def step_when_store_typed_messages_limit(context, folder_type, stid_type, limit):
    return step_store_typed_messages_impl(**locals())


def step_store_typed_messages_impl(context, folder_type, stid_type, user_name=None, limit=1):
    user = context.get_user(user_name)
    type_to_suid = {
        "shared": 0,
        "welcome": 66466005
    }
    with transaction(context.config['maildb']) as conn:
        folder = Queries(conn, user.uid).folder_by_type(folder_type)
        fill_messages_in_folder(
            conn=conn,
            uid=user.uid,
            folder=folder,
            limit=limit,
            stids=create_stids(
                context.config,
                type_to_suid[stid_type],
                limit,
            )
        )


def apply_override(message_data_dicts, override_data, conn, user):
    labels_def = [
        TYP.MailLabelDef(**ld)
        for ld in override_data.pop('labels', [])
    ]
    if labels_def:
        resolve_op = OPS.ResolveLabels(
            conn, user.uid
        )
        resolve_op(labels_def)
        resolve_op.commit()
        message_data_dicts['lids'] = [l.lid for l in resolve_op.result]
    for override_key, override_as_types in [
            ('recipients', TYP.StoreRecipient),
            ('attaches', TYP.StoreAttach),
    ]:
        if override_key in override_data:
            message_data_dicts[override_key] = [
                override_as_types(**td)
                for td in override_data.pop(override_key)
            ]
    if 'coords' in override_data:
        coords = override_data.pop('coords')
        for name in coords.keys()[:]:
            setattr(
                message_data_dicts['coords'],
                name,
                coords.pop(name)
            )
        if coords:
            raise RuntimeError(
                'Don\'t known how override coords: %r, '
                'write some logic here...' % coords
            )
    if override_data:
        raise RuntimeError(
            'Don\'t known how override: %r, '
            'write some logic here...' % override_data
        )
    return message_data_dicts


@given('"{user_name:w}" has one message in "{folder_type:w}" with')
def step_given_store_custom_message(context, user_name, folder_type):
    user = context.get_user(user_name)
    if not context.text:
        raise SyntaxError('This step require override data in context.text')
    override_data = yaml.safe_load(context.text)

    with transaction(context.config['maildb']) as conn:
        conn.set_client_encoding('UTF8')
        folder = Queries(conn, user.uid).folder_by_type(folder_type)

        message_data_dicts = next(
            messages_in_folder_generator(
                conn=conn,
                uid=user.uid,
                folder=folder,
                limit=1,
                stids=create_stids(
                    config=context.config,
                    suid=user.suid,
                    limit=1,
                )
            )
        )

        message_data_dicts = apply_override(
            message_data_dicts=message_data_dicts,
            override_data=override_data,
            conn=conn,
            user=user)

        o = OPS.StoreMessage(conn, user.uid)
        o(**message_data_dicts)
        o.commit()


@given('he has mailish account entry')
def step_given_he_has_mailish_account(context):
    user = context.get_user()
    with transaction(context.config['maildb']) as conn:
        OPS.SaveMailishAccount(conn, user.uid)(MAILISH_AUTH_DATA).commit()


@given('surveillance will respond without errors')
def step_given_surveillance_will_respond_without_errors(context):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/monitoring/'),
    ), response=MockResponse(status=200, body='uid1;uid2'))


@given('surveillance will respond with "{user_name:w}" uid in response')
def step_given_surveillance_will_respond_with_specified_uid(context, user_name):
    user = context.get_user(user_name)
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/monitoring/'),
    ), response=MockResponse(status=200, body=f'uid1;{user.uid}'))


@given('surveillance will respond with invalid data')
def step_given_surveillance_will_respond_ivalid_data(context):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/monitoring/'),
    ), response=MockResponse(status=200, body='uid1;uid2;uid3'))


@given('surveillance will respond with 500 {times:d} times')
def step_given_surveillance_will_respond_with_500(context, times):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/monitoring/'),
    ), response=MockResponse(status=500, body=''), times=times)


@given('passport will respond without errors')
def step_given_passport_will_respond_without_errors(context):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/options/'),
    ), response=MockResponse(status=200, body='{"status":"ok"}'))


@given('passport will respond with 500 {times:d} times')
def step_given_passport_will_respond_with_500(context, times):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/options/'),
    ), response=MockResponse(status=500, body=''), times=times)


@given('passport will respond with 400 {times:d} times')
def step_given_passport_will_respond_with_400(context, times):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/options/'),
    ), response=MockResponse(status=400, body=''), times=times)


@given('passport will respond with retriable errors {times:d} times')
def step_given_passport_will_respond_with_retriable_errors(context, times):
    pyremock = context.coordinator.pyremock
    body = '{"status":"error", "errors":["backend.blackbox_failed","backend.yasms_failed"]}'
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/options/'),
    ), response=MockResponse(status=200, body=body), times=times)


@given('passport will respond with nonretriable errors {times:d} times')
def step_given_passport_will_respond_with_nonretriable_errors(context, times):
    pyremock = context.coordinator.pyremock
    body = '{"status":"error", "errors:["some error"]"}'
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/options/'),
    ), response=MockResponse(status=200, body=body), times=times)


@given('passport will respond with illformed response {times:d} times')
def step_given_passport_will_respond_with_illwormed_response(context, times):
    pyremock = context.coordinator.pyremock
    body = '<xml>'
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/2/account/options/'),
    ), response=MockResponse(status=200, body=body), times=times)


@when('she has mailish account entry')
def step_when_store_custom_message(context):
    user = context.get_user()
    with transaction(context.config['maildb']) as conn:
        OPS.SaveMailishAccount(conn, user.uid)(MAILISH_AUTH_DATA).commit()


@given('"{user_name:w}" was deleted "{days:d}" days ago')
def step_delete_user(context, user_name, days):
    user = context.get_user(user_name)
    with transaction(context.config['maildb']) as conn:
        OPS.DeleteUser(conn, user.uid)(datetime.now() - timedelta(days)).commit()
    move_user_to_deleted(context, user_name)


@given('"{user_name:w}" is absent in the destination shard in maildb')
def step_drop_user_from_destination_shard(context, user_name):
    user = context.get_user(user_name)
    with transaction(context.config['maildb2']) as conn:
        OPS.PurgeUser(conn, user.uid)().commit()


@given('"{user_name:w}" has enabled filter "{filter_type:w}"')
def step_create_filter(context, user_name, filter_type):
    user = context.users.get(user_name)
    with transaction(context.config['maildb']) as conn:
        OPS.CreateRule(conn, user.uid)(
            name='test filter for %s' % user_name,
            enabled=True,
            stop=False,
            last=False,
            acts=[filter_type, 'param', 'yes'],
            conds=['header', 'from', 'apple', 'contains', 'or', 'no'],
            old_rule_id=None
        ).commit()


@then('there are no unexpected requests to passport')
def step_then_there_are_no_unexpected_requests_to_passport(context):
    context.coordinator.pyremock.assert_expectations()


@given('he has sids "{sids:OneOrMore}" in blackbox')
def given_user_has_sids(context, sids):
    user = context.get_user()
    with transaction(context.config['fbbdb']) as fbbdb_conn:
        update_user_sids_in_fbb(fbbdb_conn, login=user.login, sids=[int(s) for s in sids])
