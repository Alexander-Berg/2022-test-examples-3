import copy

from mail.devpack.lib.components.mdb import Mdb

from hamcrest import (
    assert_that,
    equal_to,
)

from enum import Enum
from itertools import product


class ShardSchema(Enum):
    '''
    you can see them here:
    https://a.yandex-team.ru/arc/trunk/arcadia/mail/sharpei/tests/integration/schemas
    '''
    v2 = 2,
    v3 = 3,


def with_dbs_sorted_by_portnumber(response):
    response['databases'] = sorted(response['databases'], key=lambda db: int(db['address']['port']))
    return response


def generate_instance_description(lag, role, port, status):
    return {"status": status,
            "state": {"lag": lag},
            "role": role,
            "address": {"dataCenter": "local", "host": "localhost", "port": port, "dbname": "maildb"}}


def get_ports_of_configured_instances(role, ports_info, shard_id, status='alive'):
    ports = []
    for (port, info) in ports_info.items():
        if info['role'] == role and info['status'] == status and info['shard_id'] == shard_id:
            assert isinstance(port, int)
            ports.append(port)
    return sorted(ports)


def get_lag_values(db):
    return list(db[i]['state']['lag'] for i in range(len(db)))


def generate_shard_description(shard_id, shard_name, lag_values, ports, role, status):
    return {
        "id": shard_id,
        "name": shard_name,
        "databases": [generate_instance_description(lag, role, port, status) for lag, port in zip(lag_values, ports)],
    }


def generate_stat_v2_shard_description(shard_id, shard_name, lag_values, ports, role, status):
    return {
        "id": str(shard_id),
        "name": shard_name,
        "databases": [generate_instance_description(lag, role, str(port), status) for lag, port in zip(lag_values, ports)],
    }


def get_shard_generator(schema):
    if schema == ShardSchema.v3:
        return generate_shard_description
    elif schema == ShardSchema.v2:
        return generate_stat_v2_shard_description
    assert False


def generate_expected_response(databases, role, context, shard_id, schema, status='alive'):
    component = context.coord.components[Mdb]
    shard = component.shard_by_id(shard_id)
    databases = sorted(databases, key=lambda db: int(db['address']['port']))
    lag_values = get_lag_values(databases)
    ports = get_ports_of_configured_instances(role, context.ports_info, shard_id, status)
    assert len(lag_values) == len(ports)
    shard_generator = get_shard_generator(schema)
    expected = shard_generator(shard_id, shard.shard_name, lag_values, ports, role, status)
    return expected


def check_the_order_for_roles(databases, roles):
    assert_that(len(databases), equal_to(len(roles)))
    for i in range(len(roles)):
        assert_that(databases[i]['role'], equal_to(roles[i]))


def check_the_existence_for_statuses(databases, statuses):
    assert_that(len(databases), equal_to(len(statuses)))
    dbs = sorted(databases, key=lambda db: db['status'])
    for i in range(len(statuses)):
        assert_that(dbs[i]['status'], equal_to(statuses[i]))


# todo(nickitat): audit all the usages of this function in mail_steps.py
def check_for_role(role, response, context, shard_id, status='alive', schema=ShardSchema.v3):
    response = copy.deepcopy(response)

    # filter only the relevant entries
    response['databases'] = filter(lambda db: db['role'] == role and db['status'] == status, response['databases'])

    expected = generate_expected_response(response['databases'], role, context, shard_id, schema, status)

    assert_that(len(response['databases']), equal_to(len(expected['databases'])))
    assert_that(with_dbs_sorted_by_portnumber(response), equal_to(with_dbs_sorted_by_portnumber(expected)))
    return len(response['databases'])


def match_shard(response, context, shard_id, schema=ShardSchema.v3):
    instances_checked = 0
    for role, status in product(('master', 'replica'), ('alive', 'dead')):
        instances_checked += check_for_role(role, response, context, shard_id, status, schema)
    assert_that(instances_checked, equal_to(len(response['databases'])))


def match_shard_instances_with_role(role, response, context, shard_id):
    instances_checked = 0
    for status in ('alive', 'dead'):
        instances_checked += check_for_role(role, response, context, shard_id, status)
    assert_that(instances_checked, equal_to(len(response['databases'])))


def match_shard_with_order(context, response, shard_id, roles):
    check_the_order_for_roles(response['databases'], roles)
    match_shard(response, context, shard_id)


def check_stat_response_for_shard_matches_shard(context, shard_id, schema):
    shard_description = context.response.json()[str(shard_id)]
    match_shard(shard_description, context, shard_id, schema)


def check_stat_response_matches_all_shards(context, schema):
    component = context.coord.components[Mdb]
    shards = len(component.config[component.name]['shards'])
    assert shards
    assert_that(len(context.response.json()), equal_to(shards))
    for shard_id in range(shards):
        check_stat_response_for_shard_matches_shard(context, shard_id + 1, schema)
