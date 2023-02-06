from datetime import datetime, timedelta

from hamcrest import less_than_or_equal_to, greater_than_or_equal_to
from parse import with_pattern
from parse_type import TypeBuilder
from dateutil.tz import tzlocal

from tests_common.step_types_defs import extra_parsers as base_extra_parsers
from tests_common.pytest_bdd import BehaveParser

EPOCH = datetime.fromtimestamp(0, tzlocal())


@with_pattern(r'\s*with force flag')
def parse_with_force_flag(text):
    return True


@with_pattern(r"enabled|disabled")
def parse_enabled(text):
    return text == 'enabled'


@with_pattern(r"true|false")
def parse_true(text):
    return text == 'true'


@with_pattern(r'\s*with restore deleted messages')
def parse_with_restore_deleted(text):
    return True


@with_pattern(r'\s*right now')
def parse_now(text):
    return True


@with_pattern(r'in past|not in past')
def parse_in_past(text):
    if text == 'in past':
        return less_than_or_equal_to(EPOCH)
    if text == 'not in past':
        return greater_than_or_equal_to(datetime.now(tzlocal()) - timedelta(minutes=5))


def parse_specified_value(text):
    if text == 'epoch':
        return (datetime.fromtimestamp(0, tzlocal())).isoformat()
    return text


SHIVA_TASKS = [
    'purge_deleted_user',
    'purge_storage',
]

parse_del_shiva_task = TypeBuilder.make_choice(SHIVA_TASKS)
parse_del_shiva_tasks_many = TypeBuilder.with_many(parse_del_shiva_task, listsep=',')

TO_FILL = [
    'messages',
    'deleted messages',
    'stids in storage delete queue',
]

parse_to_fill = TypeBuilder.make_choice(TO_FILL)
parse_to_fill_many = TypeBuilder.with_many(parse_to_fill, listsep=',')


def extra_parsers():
    parsers = base_extra_parsers()
    parsers.update(dict(
        WithForceFlag=parse_with_force_flag,
        Enabled=parse_enabled,
        IsTrue=parse_true,
        RightNow=parse_now,
        WithRestoreDeleted=parse_with_restore_deleted,
        InPastMatcher=parse_in_past,
        SpecifiedValue=parse_specified_value,
        ToFill=parse_to_fill,
        ToFillAndMore=parse_to_fill_many,
        ShivaTasks=parse_del_shiva_tasks_many,
    ))
    return parsers


BehaveParser.extra_types.update(extra_parsers())
