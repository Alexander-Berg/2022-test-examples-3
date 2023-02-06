# coding: utf-8
from functools import partial
from enum import Enum
from collections import namedtuple

from behave import register_type
from parse import with_pattern
from parse_type import TypeBuilder

from pymdb.types import (
    SubscriptionAction,
    SubscriptionState,
    MailLabelDef,
)

from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.lib.components.fbbdb import FbbDb
from mail.devpack.lib.components.sharddb import ShardDb
from mail.devpack.lib.components.sharpei import Sharpei

from mail.doberman.devpack.components.doberman import Doberman
from mail.devpack.recipe.lib import get_coordinator
from tests_common.coordinator_context import fill_coordinator_context


class Shard(Enum):
    first = 1
    second = 2


def make_url(method, base):
    return base + '/' + method


Config = namedtuple('Config', [
    'shards',
    'sharpei',
    'make_blackbox_url',
    'sharddb_dsn',
    'fbbdb_dsn',
    'mdb_dsn_suffix',
    'worker_id',
])


@with_pattern(r'[\w-]+([\s\w-]+)?')
def parse_user_name(text):
    return text


WORDS_COUNTS = {
    'one': 1,
    'no': 0
}


@with_pattern(r'(is|are) (one|no|"\d+") messages?')
def parse_is_message_count(text):
    count_str = text.split()[1]
    if count_str in WORDS_COUNTS:
        return WORDS_COUNTS[count_str]
    return int(count_str.strip('"'))


@with_pattern(r'"\w+" label "\w+"')
def parse_label_def(text):
    label_type, _, label_name = text.split()
    return MailLabelDef(
        name=label_name.strip('"'),
        type=label_type.strip('"')
    )


@with_pattern(r'[\w ]+')
def parse_words(text):
    return text


def register_our_types():
    register_type(**dict(
        UserName=parse_user_name,
        Shard=TypeBuilder.make_enum(Shard),
        SubscriptionState=TypeBuilder.make_enum(SubscriptionState),
        SubscriptionAction=TypeBuilder.make_enum(SubscriptionAction),
        IsMessageCount=parse_is_message_count,
        LabelDef=parse_label_def,
        Words=parse_words,
    ))


register_our_types()


def before_all(context):
    coord = get_coordinator(Doberman)
    fill_coordinator_context(context, coord)
    comps = context.coordinator.components

    context.doberman_cluster = comps[Doberman]
    context.dobby = context.doberman_cluster.dobby()

    context.config = Config(
        shards=Shard,
        sharpei='http://localhost:{}'.format(comps[Sharpei].webserver_port()),
        make_blackbox_url=partial(make_url, base='http://localhost:{}'.format(comps[FakeBlackbox].port)),
        sharddb_dsn=comps[ShardDb].dsn(),
        fbbdb_dsn=comps[FbbDb].dsn(),
        mdb_dsn_suffix='user=root',
        worker_id='dobby',
    )


def after_step(context, step):
    def has_debug():
        import os
        return os.environ.get('DEBUG')

    if step.status == 'failed' and has_debug():
        # -- ENTER DEBUGGER: Zoom in on failure location.
        # NOTE: Use pdb++ AKA pdbpp debugger, same for pdb (basic python debugger).
        import pdb
        pdb.post_mortem(step.exc_traceback)
