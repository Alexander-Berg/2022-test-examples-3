# coding: utf-8
from tests_common.holders import (
    UIDHolder,
    UIDRanges,
    UsersHolder,
)
from tests_common.coordinator_context import fill_coordinator_context
from mail.devpack.recipe.lib import get_coordinator
from mail.york.devpack.components.with_unimock import YorkService
from mail.york.devpack.components.york import York


def before_all(context):
    coord = get_coordinator(YorkService)
    fill_coordinator_context(context, coord)
    comps = context.coordinator.components

    context.york = comps[York]

    context.get_free_uid = UIDHolder(
        UIDRanges.york,
        sharddb_conn=context.sharddb_conn,
    )
    context.users = UsersHolder()


def before_scenario(context, scenario):
    context.payload = {}
    context.shared_folders_parents = {}


def after_scenario(context, scenario):
    context.users.forget()


def after_step(context, step):
    def has_debug():
        import os
        return os.environ.get('DEBUG')

    if step.status == 'failed' and has_debug():
        # -- ENTER DEBUGGER: Zoom in on failure location.
        # NOTE: Use pdb++ AKA pdbpp debugger, same for pdb (basic python debugger).
        import pdb
        pdb.post_mortem(step.exc_traceback)
