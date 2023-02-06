from tests_common.pytest_bdd import when


@when('we clean shards.users')
def step_impl(context):
    from ora2pg.clean_shards_users import clean

    clean(
        args=context.config,
        shard_ids=[1],
    )
