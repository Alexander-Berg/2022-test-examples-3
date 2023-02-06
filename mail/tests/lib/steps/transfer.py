from ora2pg.app.transfer_app import TransferApp, dict2args
from ora2pg.transfer import Transfer, TransferOptions
from ora2pg.transfer_data import DbEndpoint
from tests_common.pytest_bdd import when, given

from .mdb_actions import set_user_not_here_days


@when('we transfer "{user_name:w}" to different shard')
def step_transfer_user(context, user_name):
    return transfer_user(context, user_name)


@given('"{user_name:w}" transfered to different shard "{days:d}" days ago')
def step_transfer_user_by_days(context, user_name, days):
    return transfer_user(context, user_name, days)


@given('"{group_name:w}" group of the {limit:d} users transfered to different shard "{days:d}" days ago')
def step_transfer_user_by_days_for_group_of_users(context, group_name, limit, days):
    for id in range(limit):
        user_name = group_name + str(id)
        transfer_user(context, user_name, days)


def transfer_user(context, user_name, days=None):
    transfer_options = dict(
        fill_change_log=False,
    )
    from_shard_id = context.config['shard_id']
    to_shard_id = context.config['shard_id2']
    Transfer(TransferApp(dict2args(context.config))).transfer(
        user=context.get_user(user_name),
        from_db=DbEndpoint.make_pg(from_shard_id),
        to_db=DbEndpoint.make_pg(to_shard_id),
        options=TransferOptions(
            **transfer_options
        )
    )
    if days is not None:
        set_user_not_here_days(context, user_name, days)
