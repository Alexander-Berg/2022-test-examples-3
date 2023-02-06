import yaml
from pymdb.operations import CreateCollector, DeleteCollector, UpdateCollectorsMetadata
from tests_common.pytest_bdd import when, then
from hamcrest import assert_that, has_entries


@when(u'we create collector from "{name:UserName}"')
def step_create_collector(context, name):
    assert context.text
    kwargs = yaml.safe_load(context.text)
    assert 'src_uid' not in kwargs, "Can't set src_uid expicitly"

    context.last_collector_id = context.apply_op(
        CreateCollector,
        src_uid=context.users[name],
        **kwargs
    ).result[0].collector_id


@when(u'we try create collector from "{name:UserName}" as "{op_id}"')
def step_try_create_collector(context, name, op_id):
    assert context.text
    kwargs = yaml.safe_load(context.text)
    assert 'src_uid' not in kwargs, "Can't set src_uid expicitly"

    context.operations[op_id] = context.make_async_operation(CreateCollector)(
        src_uid=context.users[name],
        **kwargs
    )


def update_collectors_metadata(context, collector_id, data, need_log):
    context.apply_op(
        UpdateCollectorsMetadata,
        collector_id=collector_id,
        data=data,
        need_log=need_log
    )


@when(u'we update metadata for last created collector')
def step_update_collectors_metadata(context):
    assert context.text
    data = yaml.safe_load(context.text)
    update_collectors_metadata(context, context.last_collector_id, data, True)


@when(u'we silently update metadata for last created collector')
def step_update_collectors_metadata_silent(context):
    assert context.text
    data = yaml.safe_load(context.text)
    update_collectors_metadata(context, context.last_collector_id, data, False)


@when(u'we delete last created collector')
def step_delete_collector(context):
    context.apply_op(
        DeleteCollector,
        collector_id=context.last_collector_id,
    )


@then(u'users collectors count is "{count:d}"')
def step_check_collectors_count(context, count):
    collectors = context.qs.collectors()
    assert len(collectors) == count, \
        'user has wrong collectors count, actual={0}, expected={1}'.format(len(collectors), count)


@then(u'in metadata for last created collector')
def step_check_collectors_metadata(context):
    meta = context.qs.collectors_metadata(collector_id=context.last_collector_id)
    assert len(meta) == 1, 'collector "{0}" has wrong metadata size'.format(context.last_collector_id)
    meta = meta[0]

    assert context.text
    data = yaml.safe_load(context.text)
    assert_that(meta, has_entries(data))
