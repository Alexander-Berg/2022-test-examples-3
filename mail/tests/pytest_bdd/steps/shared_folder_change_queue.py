# coding: utf-8

import yaml
from hamcrest import (assert_that,
                      equal_to,
                      has_length,
                      empty)

from .shared_folders import get_queries_by_user
from tests_common.pytest_bdd import then


def get_change_queue(context, user_name, owner_ref):
    owner_qs = get_queries_by_user(context, owner_ref.user_name)
    owner_folder = owner_qs.folder_by(
        folder_type=owner_ref.folder_type,
        folder_name=owner_ref.folder_name
    )

    subscription = [
        s for s in owner_qs.shared_folder_subscriptions()
        if s.fid == owner_folder.fid and s.subscriber_uid == context.users[user_name]
    ]
    assert_that(subscription, has_length(1))
    return owner_qs.shared_folder_change_queue(subscription_id=subscription[0].subscription_id)


def cast_changed_queue(context):
    ids_queue = yaml.safe_load(context.text)
    return [context.cids[c] for c in ids_queue]


@then('"{user_name:UserName}" change queue for "{owner_ref:FolderRef}" is')
def step_check_change_queue(context, user_name, owner_ref):
    real_change_queue = get_change_queue(context, user_name, owner_ref)
    assert_that(real_change_queue, equal_to(cast_changed_queue(context)))


@then('"{user_name:UserName}" change queue for "{owner_ref:FolderRef}" is empty')
def step_check_change_queue_is_empty(context, user_name, owner_ref):
    assert_that(get_change_queue(context, user_name, owner_ref), empty())
