import copy
import json
import jsonschema
import os.path
import requests

from time import sleep

from tests_common.pytest_bdd import (
    given,
    when,
    then,
)

from hamcrest import (
    anything,
    assert_that,
    equal_to,
    has_entries,
    contains_string,
)

from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse

from mail.devpack.lib.components.sharpei import SharpeiWithBlackboxMock
from mail.devpack.lib.components.sharddb import ShardDb
from mail.devpack.lib.components.mdb import Mdb
from yatest.common import source_path

from .common_steps import (
    ShardSchema,
    check_for_role,
    check_stat_response_for_shard_matches_shard,
    check_stat_response_matches_all_shards,
    check_the_existence_for_statuses,
    check_the_order_for_roles,
    get_ports_of_configured_instances,
    match_shard_with_order,
    with_dbs_sorted_by_portnumber,
)


def generate_ports_info(component):
    config = component.config
    name = component.name
    ports_info = {}
    for shards in range(len(config[name]['shards'])):
        for db in config[name]['shards'][shards]['dbs']:
            ports_info[db['port']] = {'status': 'alive', 'role': db['type'], 'shard_id': shards + 1}
    return ports_info


@given('sharpei is started')
def step_sharpei_is_started(context):
    context.pyremock.reset()
    context.coord.components[SharpeiWithBlackboxMock].restart()
    context.sharpei_api = context.coord.components[SharpeiWithBlackboxMock].api()
    if not hasattr(context, 'ports_info'):
        context.ports_info = generate_ports_info(context.coord.components[Mdb])


@given('sharpei response to ping')
def step_sharpei_response_for_ping(context):
    step_we_ping_sharpei(context)
    step_response_status_code_is(context, 200)
    step_response_body_is(context, 'pong')


def step_update_ShardDb_for_roles_in_shard_common(context, role, shard_id):
    for info in context.ports_info.values():
        if info['status'] == 'dead':
            return
    old_port = get_ports_of_configured_instances(role, context.ports_info, shard_id)[0]
    new_port = context.pm.get_port()
    context.coord.components[ShardDb].execute('''
        UPDATE shards.instances
        SET port = %(new_port)s
        WHERE port = %(old_port)s
    ''', new_port=new_port, old_port=old_port)
    update_port_and_status(context.ports_info, old_port, new_port)
    sleep(5)  # to have time to update the cache


@given('the first alive host with the role "{role}" was killed in shard "{shard_id:d}"')
def step_update_ShardDb_for_roles_in_shard(context, role, shard_id):
    step_update_ShardDb_for_roles_in_shard_common(context, role, shard_id)


@given('the first alive host with the role "{role}" was killed')
def step_update_ShardDb_for_roles(context, role):
    step_update_ShardDb_for_roles_in_shard_common(context, role, shard_id=1)


@given('two hosts in the shard "{shard_id:d}" identify themselves as masters')
def step_change_shards(context, shard_id):
    if hasattr(context, 'shards_already_updated'):
        return
    context.shards_already_updated = True
    context.coord.components[ShardDb].execute('''
        UPDATE shards.instances
        SET shard_id = %(shard_id)s
    ''', shard_id=shard_id)
    for info in context.ports_info.values():
        info['shard_id'] = shard_id
    sleep(5)  # to have time to update the cache


def step_delete_ShardDb_for_role_in_shard_common(context, role, shard_id):
    old_port = get_ports_of_configured_instances(role, context.ports_info, shard_id)[0]
    context.coord.components[ShardDb].execute('''
        DELETE FROM shards.instances
        WHERE port = %(old_port)s
    ''', old_port=old_port)
    del context.ports_info[old_port]
    sleep(5)  # to have time to update the cache


@given('the first alive host with the role "{role}" was deleted in shard "{shard_id:d}"')
def step_delete_ShardDb_for_role_in_shard(context, role, shard_id):
    step_delete_ShardDb_for_role_in_shard_common(context, role, shard_id)


@given('the first alive host with the role "{role}" was deleted')
def step_delete_ShardDb_for_role(context, role):
    step_delete_ShardDb_for_role_in_shard_common(context, role, shard_id=1)


def update_port_and_status(ports_info, old_port, new_port, status='dead'):
    ports_info[new_port] = ports_info.pop(old_port)
    ports_info[new_port]['status'] = status
    if ports_info[new_port]['role'] == 'master' and ports_info[new_port]['status'] == 'dead':  # [MAILPG-532] Must be a default replica when unreachable
        ports_info[new_port]['role'] = 'replica'


@when('we /ping sharpei')
def step_we_ping_sharpei(context):
    context.response = context.sharpei_api.ping(request_id=context.request_id)


@when('we /pingdb sharpei')
def step_we_pingdb_sharpei(context):
    context.response = context.sharpei_api.pingdb(request_id=context.request_id)


@when('we stop sharddb')
def step_we_stop_sharddb(context):
    context.coord.components[ShardDb].stop()


@when('we start sharddb')
def step_we_start_sharddb(context):
    context.coord.components[ShardDb].start()


@when('there is domain in blackbox')
def step_there_is_domain_in_blackbox(context):
    context.domain_id = context.id_generator()
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/blackbox'),
            headers=has_entries(**{
                'X-Request-Id': anything(),
                'User-Agent': equal_to(['sharpei.mail.yandex.net']),
            }),
            params=has_entries(
                method=equal_to(['hosted_domains']),
                format=equal_to(['json']),
                domain_id=equal_to([str(context.domain_id)]),
            ),
        ),
        response=MockResponse(
            status=200,
            body='''
            {
                "hosted_domains" : [
                    {
                        "domid": "13",
                        "mx": "0",
                        "default_uid": "0",
                        "ena": "1",
                        "options": "",
                        "master_domain": "",
                        "admin": "10",
                        "domain": "ru.ya",
                        "born_date": "2009-04-05 15:13:11"
                    }
                ]
            }
            '''
        ),
    )


@when('there is organization domain in blackbox')
def step_there_is_organization_domain_in_blackbox(context):
    context.domain_id = context.id_generator()
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/blackbox'),
            headers=has_entries(**{
                'X-Request-Id': anything(),
                'User-Agent': equal_to(['sharpei.mail.yandex.net']),
            }),
            params=has_entries(
                method=equal_to(['hosted_domains']),
                format=equal_to(['json']),
                domain=equal_to([context.org_domain]),
            ),
        ),
        response=MockResponse(
            status=200,
            body='''
            {
                "hosted_domains" : [
                    {
                        "domid": "%s",
                        "mx": "0",
                        "default_uid": "0",
                        "ena": "1",
                        "options": "",
                        "master_domain": "",
                        "admin": "10",
                        "domain": "sunday30test.yaconnect.com",
                        "born_date": "2009-04-05 15:13:11"
                    }
                ]
            }
            ''' % context.domain_id
        ),
    )


@when('user with uid "{uid:d}" is not registered in blackbox')
def step_user_not_registered_in_blackbox(context, uid):
    non_existent_user = open(source_path('mail/sharpei/tests/integration/blackbox/non_existent_user.xml'), 'r')
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/blackbox'),
            headers=has_entries(**{
                'User-Agent': equal_to(['sharpei.mail.yandex.net']),
            }),
            params=has_entries(
                method=equal_to(['userinfo']),
                uid=equal_to([str(uid)]),
                userip=equal_to(['::1']),
                dbfields=equal_to(['account_info.country.uid,hosts.db_id.2,subscription.suid.2,userinfo.lang.uid']),
                aliases=equal_to(['all']),
            ),
        ),
        response=MockResponse(
            status=200,
            body=non_existent_user.read()
        ),
    )


@when('user with uid "{uid:d}" is registered in blackbox')
def step_user_is_registered_in_blackbox(context, uid):
    existing_user = open(source_path('mail/sharpei/tests/integration/blackbox/existing_user.xml'), 'r')
    context.pyremock.expect(
        times=(uid == 12345) + 1,
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/blackbox'),
            headers=has_entries(**{
                'User-Agent': equal_to(['sharpei.mail.yandex.net']),
            }),
            params=has_entries(
                method=equal_to(['userinfo']),
                uid=equal_to([str(uid)]),
                userip=equal_to(['::1']),
                dbfields=equal_to(['account_info.country.uid,hosts.db_id.2,subscription.suid.2,userinfo.lang.uid']),
                aliases=equal_to(['all']),
            ),
        ),
        response=MockResponse(
            status=200,
            body=existing_user.read()
        ),
    )


@when('blackbox returns not pg for user with uid "{uid:d}"')
def step_blackbox_returns_not_pg_for_user(context, uid):
    not_pg_user = open(source_path('mail/sharpei/tests/integration/blackbox/not_pg_user.xml'), 'r')
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/blackbox'),
            headers=has_entries(**{
                'User-Agent': equal_to(['sharpei.mail.yandex.net']),
            }),
            params=has_entries(
                method=equal_to(['userinfo']),
                uid=equal_to([str(uid)]),
                userip=equal_to(['::1']),
                dbfields=equal_to(['account_info.country.uid,hosts.db_id.2,subscription.suid.2,userinfo.lang.uid']),
                aliases=equal_to(['all']),
            ),
        ),
        response=MockResponse(
            status=200,
            body=not_pg_user.read()
        ),
    )


@when('user with uid "{uid:d}" deleted from mdb')
def delete_from_maildb_with_uid(context, uid):
    context.coord.components[Mdb].execute('''
        UPDATE mail.users
        SET is_here = false
        WHERE uid = %(uid)s
    ''', uid=uid)


@when('there is no domain in blackbox')
def step_there_is_no_domain_in_blackbox(context):
    context.domain_id = context.id_generator()
    context.pyremock.expect(
        request=MatchRequest(
            method=equal_to('get'),
            path=equal_to('/blackbox'),
            headers=has_entries(**{
                'X-Request-Id': anything(),
                'User-Agent': equal_to(['sharpei.mail.yandex.net']),
            }),
            params=has_entries(
                method=equal_to(['hosted_domains']),
                format=equal_to(['json']),
                domain_id=equal_to([str(context.domain_id)]),
            ),
        ),
        response=MockResponse(
            status=200,
            body='{"hosted_domains": []}'
        ),
    )


@when('we register uid "{uid:d}" in mdb shard "{shard_id:d}"')
def step_we_register_uid_in_mdb(context, uid, shard_id):
    context.coord.components[Mdb].shard_by_id(shard_id).master.query('''
        SELECT code.register_user(
            i_uid := %(uid)s::bigint,
            i_country := 'ru',
            i_lang := 'ru',
            i_need_welcomes := false
        )
    ''', uid=uid, shard_id=shard_id)


@when('we register uid "{uid:d}" in sharddb shard "{shard_id:d}"')
def step_we_register_uid_in_sharddb(context, uid, shard_id):
    context.coord.components[ShardDb].query('''
        SELECT code.register_user(
            i_uid := %(uid)s::bigint,
            i_shard_id := %(shard_id)s::integer
        )
    ''', uid=uid, shard_id=shard_id)


@when('delete the user with uid "{uid:d}" from sharddb')
def step_delete_user_from_sharddb(context, uid):
    context.coord.components[ShardDb].query('''
        SELECT code.move_user_to_deleted({uid})
    '''.format(uid=uid))


@when('there is domain in sharddb')
def step_there_is_domain_in_sharddb(context):
    context.domain_id = context.id_generator()
    result = context.coord.components[ShardDb].query('''
        SELECT code.create_organization(
            i_shard_id := (
                SELECT shard_id
                  FROM shards.shards
                 LIMIT 1
            ),
            i_domain_id := %(domain_id)s::bigint,
            i_org_id := NULL
        )
    ''', domain_id=context.domain_id)
    context.domain_shard_id = int(result[0][0])


@when('there is organization in sharddb')
def step_there_is_organization_in_sharddb(context):
    context.org_id = context.id_generator()
    result = context.coord.components[ShardDb].query('''
        SELECT code.create_organization(
            i_shard_id := (
                SELECT shard_id
                  FROM shards.shards
                 LIMIT 1
            ),
            i_domain_id := NULL,
            i_org_id := %(org_id)s::bigint
        )
    ''', org_id=context.org_id)
    context.org_shard_id = int(result[0][0])


@when('we request sharpei for domain conninfo')
def step_request_sharpei_for_domain_conninfo(context):
    context.response = context.sharpei_api.domain_conninfo(
        domain_id=context.domain_id,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for organization conninfo')
def step_request_sharpei_for_organization_conninfo(context):
    if not hasattr(context, 'org_id'):
        context.org_id = context.id_generator()
    context.response = context.sharpei_api.org_conninfo(
        org_id=context.org_id,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for conninfo with uid "{uid:d}" mode "{mode}" and force is "{force}"')
def step_request_sharpei_for_conninfo(context, uid, mode, force=None):
    context.response = context.sharpei_api.conninfo(
        uid=uid,
        mode=mode,
        force=force,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for deleted_conninfo with uid "{uid:d}" and mode "{mode}"')
def step_request_sharpei_for_deleted_conninfo(context, uid, mode):
    context.response = context.sharpei_api.deleted_conninfo(
        uid=uid,
        mode=mode,
        request_id=context.request_id,
    )


def without_lag(response):
    response = copy.deepcopy(response)
    for db in response['databases']:
        lag = db['state']['lag']
        if lag < 0 or (db['status'] == 'alive' and lag > 100500):
            raise RuntimeError('the lag calculation mechanics is probably broken')
        del db['state']['lag']
    return response


@then('deleted_response is equal to conninfo response with uid "{uid:d}" and mode "{mode}"')
def step_deleted_response_is_equal_to_response(context, uid, mode):
    deleted_response = context.response
    step_request_sharpei_for_conninfo(context, uid, mode)
    assert_that(deleted_response.status_code, equal_to(200))
    assert_that(context.response.status_code, equal_to(200))
    deleted_conninfo_response_cf = with_dbs_sorted_by_portnumber(without_lag(deleted_response.json()))
    conninfo_response_cf = with_dbs_sorted_by_portnumber(without_lag(context.response.json()))
    assert_that(deleted_conninfo_response_cf, equal_to(conninfo_response_cf))


@when('we request sharpei for deleted_conninfo without uid and mode "{mode}"')
def _step_request_sharpei_for_deleted_conninfo(context, mode):
    step_request_sharpei_for_deleted_conninfo(context, "", mode)


@when('we request sharpei for deleted_conninfo with uid "{uid:d}" and without mode')
def __step_request_sharpei_for_deleted_conninfo(context, uid):
    step_request_sharpei_for_deleted_conninfo(context, uid, "")


@when('we request sharpei for conninfo with uid "{uid:d}" and mode "{mode}"')
def _step_request_sharpei_for_conninfo(context, uid, mode):
    step_request_sharpei_for_conninfo(context, uid, mode)


@when('we request sharpei for conninfo with uid "{uid:d}" and mode <mode>')
def __step_request_sharpei_for_conninfo(context, uid, mode):
    step_request_sharpei_for_conninfo(context, uid, mode)


@when('we request sharpei for conninfo with uid "{uid:d}" mode <mode> and force is "{force}"')
def ___step_request_sharpei_for_conninfo(context, uid, mode, force):
    step_request_sharpei_for_conninfo(context, uid, mode, force)


@when('we request sharpei for conninfo without uid and mode "{mode}"')
def ____step_request_sharpei_for_conninfo(context, mode):
    step_request_sharpei_for_conninfo(context, "", mode)


@when('we request sharpei for conninfo with uid "{uid:d}" and without mode')
def _____step_request_sharpei_for_conninfo(context, uid):
    step_request_sharpei_for_conninfo(context, uid, "")


@when('we request sharpei for v2/stat')
def step_request_stat_v2(context):
    context.response = context.sharpei_api.stat_v2(
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for v2/stat with shard_id "{shard_id}"')
def step_request_stat_v2_with_shard_id(context, shard_id):
    context.response = context.sharpei_api.stat_v2(
        request_id=context.request_id,
        shard_id=shard_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for v3/stat')
def step_request_stat_v3(context):
    context.response = context.sharpei_api.stat_v3(
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for sharddb_stat')
def step_request_sharpei_for_sharddb_stat(context):
    context.response = context.sharpei_api.sharddb_stat()
    context.pyremock.assert_expectations()


@when('we request sharpei for v3/stat with shard_id "{shard_id}"')
def step_request_stat_v3_with_shard_id(context, shard_id):
    context.response = context.sharpei_api.stat_v3(
        request_id=context.request_id,
        shard_id=shard_id,
    )
    context.pyremock.assert_expectations()


@then('v2/stat response matches shard "{shard_id:d}"')
def step_v2_stat_response_for_shard_matches_shard(context, shard_id):
    assert_that(len(context.response.json()), equal_to(1))
    check_stat_response_for_shard_matches_shard(context, shard_id, ShardSchema.v2)


@then('v2/stat response matches all shards')
def step_v2_stat_response_matches_all_shards(context):
    check_stat_response_matches_all_shards(context, ShardSchema.v2)


@then('v3/stat response matches shard "{shard_id:d}"')
def step_v3_stat_response_for_shard_matches_shard(context, shard_id):
    assert_that(len(context.response.json()), equal_to(1))
    check_stat_response_for_shard_matches_shard(context, shard_id, ShardSchema.v3)


@then('v3/stat response matches all shards')
def step_v3_stat_response_matches_all_shards(context):
    check_stat_response_matches_all_shards(context, ShardSchema.v3)


@when('we request sharpei for reset where shard "{shard_id:d}"')
def step_request_sharpei_for_reset(context, shard_id):
    context.response = context.sharpei_api.reset(
        shard=shard_id,
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for reset with GET method where shard "{shard_id:d}"')
def step_request_sharpei_for_reset_with_get(context, shard_id):
    context.response = requests.get(
        context.sharpei_api.location + '/reset?shard={shard}'.format(shard=shard_id),
        timeout=3,
        headers=context.sharpei_api.make_headers(context.request_id),
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for stat')
def step_request_sharpei_for_stat(context):
    context.response = context.sharpei_api.stat(
        request_id=context.request_id,
    )
    context.pyremock.assert_expectations()


@when('we request sharpei for reset without shard')
def step_request_sharpei_for_reset_without_shard(context):
    step_request_sharpei_for_reset(context, "")


@then('response body is "{response_body}"')
def step_response_body_is(context, response_body):
    assert_that(context.response.text, equal_to(response_body))


@then('response body contains "{substr}"')
def step_response_body_contains(context, substr):
    assert_that(context.response.text, contains_string(substr))


@then('response json matches expected "{role}" where shard "{shard_id:d}"')
def step_response_matches_expected_where_shard(context, role, shard_id):
    response = context.response.json()
    check_for_role(role, response, context, shard_id)


@then('response contains instances in order "{first_role}"-"{second_role}"-"{third_role}" from shard "{shard_id:d}"')
def step_response_matches_expected_first_is_second_is_where_shard(context, first_role, second_role, third_role, shard_id):
    response = context.response.json()
    match_shard_with_order(context, response, shard_id, [first_role, second_role, third_role])


@then('response matches shard "{shard_id:d}" hosts with role "{role}" and status "{status}"')
def step_response_matches_exepcted_role_shards_metainfo(context, shard_id, role, status):
    shard_description = context.response.json()[str(shard_id)]
    check_for_role(role, shard_description, context, shard_id, status=status)


@then('response contains instances in order "{roles}" with statuses "{statuses}"')
def step_response_contains_instances_in_order(context, roles, statuses):
    response = context.response.json()
    check_the_order_for_roles(response['databases'], roles.split('-'))
    check_the_existence_for_statuses(response['databases'], statuses.split('-'))


@then('response contains all instances from shard "{shard_id:d}"')
def step_response_matches_expected_all_where_shard(context, shard_id):
    response = context.response.json()
    check_for_role('master', response, context, shard_id)
    check_for_role('replica', response, context, shard_id)


@then('response json matches expected dead "{dead_role}" where shard "{shard_id:d}"')
def step_response_matches_expected_dead_role_where_shard(context, dead_role, shard_id):
    response = context.response.json()
    check_for_role(dead_role, response, context, shard_id, status='dead')


@then('response status code is "{status_code:d}"')
def step_response_status_code_is(context, status_code):
    assert_that(context.response.status_code, equal_to(status_code))


@then('response is verified by json schema "{json_schema}"')
def step_response_is_verified_by_json_schema(context, json_schema):
    SCHEMAS = source_path('mail/sharpei/tests/integration/schemas')
    path = os.path.join(SCHEMAS, json_schema)
    jsonschema.validate(
        context.response.json(),
        read_json(path),
        resolver=jsonschema.RefResolver(
            base_uri='file://%s/' % os.path.dirname(os.path.abspath(path)),
            referrer=json_schema,
        ),
    )


def read_json(path):
    with open(path) as stream:
        return json.load(stream)
