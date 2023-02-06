# coding: utf-8

from datetime import timedelta, datetime

import dateutil.parser
import yaml
from dateutil.tz.tz import tzlocal
from hamcrest import (assert_that,
                      has_length,
                      has_entries,
                      greater_than_or_equal_to,
                      all_of,
                      has_entry,
                      contains,
                      contains_inanyorder,
                      has_value)

from pymdb.operations import (FindAJob,
                              ConfirmTheJob,
                              AddDobermanJob, )
from pymdb.shard_queries import ShardQueries
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import given, then, when

Q = load_from_my_file(__file__)


# https://stackoverflow.com/questions/13294186/can-pyyaml-parse-iso8601-dates
def timestamp_constructor(loader, node):
    return dateutil.parser.parse(node.value)


def timedelta_yaml_constructor(loader, node):
    return timedelta(**loader.construct_mapping(node))


yaml.add_constructor('!TimeDelta', timedelta_yaml_constructor, Loader=yaml.SafeLoader)
yaml.add_constructor(u'tag:yaml.org,2002:timestamp', timestamp_constructor, Loader=yaml.SafeLoader)


def cleanup_doberman_jobs(conn):
    for q in [Q.unset_all_workers, Q.delete_all_jobs]:
        qexec(conn, q)
    conn.commit()


def get_doberman_jobs(conn):
    return ShardQueries(conn).doberman_jobs()


def get_last_record_from_dorberman_changelog(conn):
    return ShardQueries(conn).last_record_from_dorberman_jobs_changelog()


def context_text_as_yaml(context):
    return yaml.safe_load(context.text)


@given('empty doberman jobs')
def step_clenup_doberman_jobs(context):
    cleanup_doberman_jobs(context.conn)


@given('"{worker_id:DashedWord}" worker')
def step_given_add_worker(context, worker_id):
    add_worker(**locals())


@when('we add "{worker_id:DashedWord}" worker')
def step_when_add_worker(context, worker_id):
    add_worker(**locals())


def add_worker(context, worker_id):
    context.apply_op(AddDobermanJob, worker_id=worker_id)
    if 'job_creation_time' not in context:
        context.job_creation_time = {}
    context.job_creation_time[worker_id] = datetime.now(tzlocal())


@given('only "{job:DashedWord}" in doberman jobs')
def step_make_only_one_worker(context, job):
    cleanup_doberman_jobs(context.conn)
    context.job_creation_time = {}
    context.job_creation_time[job] = datetime.now(tzlocal())

    add_jobs_args = {}
    if context.text:
        add_jobs_args.update(context_text_as_yaml(context))
    context.apply_op(
        AddDobermanJob,
        worker_id=job,
        **add_jobs_args)


@when('some doberman try find job')
def step_find_a_job(context):
    find_a_job(**locals())


@when('"{launch_id:w}" try find job')
def step_find_job_by_launch_id(context, launch_id):
    find_a_job(**locals())


@when('"{launch_id:w}" try find job as "{op_id:OpID}"')
def step_find_job_by_launch_id_and_op_id(context, launch_id, op_id):
    find_a_job(**locals())


def find_a_job(context, launch_id='default test launch_id', op_id=None):
    find_jobs_args = dict(
        i_timeout=timedelta(seconds=20),
        i_launch_id=launch_id,
        i_hostname='localhost',
        i_worker_version='0.1-default-test-version',
    )
    if context.text:
        find_jobs_args.update(context_text_as_yaml(context))

    if op_id:
        context.operations[op_id] = context.make_async_operation(
            FindAJob
        )(
            **find_jobs_args
        )
    else:
        context.apply_op(
            FindAJob,
            **find_jobs_args
        )


@when('"{launch_id:w}" try confirm "{worker_id:DashedWord}" job')
def step_confirm_job(context, launch_id, worker_id):
    context.apply_op(
        ConfirmTheJob,
        i_launch_id=launch_id,
        i_worker_id=worker_id
    )


@when('"{launch_id:w}" try confirm "{worker_id:DashedWord}" job as "{op_id:OpID}"')
def step_confirm_job_async(context, launch_id, worker_id, op_id):
    context.operations[op_id] = context.make_async_operation(ConfirmTheJob)(
        i_launch_id=launch_id,
        i_worker_id=worker_id
    )


def get_last_op_result(context, LastOpClass):
    for op_id in reversed(context.operations):
        op = context.operations[op_id]
        if isinstance(op, LastOpClass):
            return op.result
    raise RuntimeError(
        "Can't find %s operation in %r" % (LastOpClass, context.operations)
    )


@then('he got NULL job')
def step_check_find_result(context):
    check_find_result(**locals())


@then('he got "{worker_id:DashedWord}" job')
def step_check_target_worker_find_result(context, worker_id):
    check_find_result(**locals())


def check_find_result(context, worker_id=None):
    assert_that(
        get_last_op_result(context, FindAJob),
        all_of(
            has_length(1),
            contains(
                has_value(worker_id)
            )
        )
    )


@then('he got confirmed "{confirmed:YAML}"')
def step_check_confirm_result(context, confirmed):
    assert_that(
        get_last_op_result(context, ConfirmTheJob),
        all_of(
            has_length(1),
            contains(
                has_entry(
                    'confirm_the_job', confirmed
                )
            )
        )
    )


@then('doberman jobs are')
def step_compare_doberman_jobs(context):
    assert_that(
        get_doberman_jobs(context.conn),
        contains_inanyorder(*[
            has_entries(**d)
            for d in context_text_as_yaml(context)
        ])
    )


@then('"{job:DashedWord}" job has recent {date_key:w}')
def step_check_date_fields(context, job, date_key):
    feature_start_at = context.job_creation_time[job]
    assert_that(
        get_doberman_jobs(context.conn),
        contains(
            has_entries(
                'worker_id', job,
                date_key, greater_than_or_equal_to(feature_start_at)
            )
        )
    )


@then('last record in doberman changelog has "{worker_id:DashedWord}" worker with info')
def step_get_last_record_from_dobby_log(context, worker_id):
    assert_that(
        get_last_record_from_dorberman_changelog(context.conn),
        has_entries(
            'worker_id', worker_id,
            'info', has_entries(**context_text_as_yaml(context)))
    )
