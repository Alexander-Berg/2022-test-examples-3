from ora2pg.disable_filters import disable_filters_without_type, disable_filter_without_type
from ora2pg.pg_types import PgUser
from pymdb.types import Filter, FilterAction, FilterActionOperType

ACTION_PARAMS = dict(
    action_id=1,
    param='archive',
    verified=True
)
FILTER_PARAMS = dict(
    rule_id=42,
    name='name',
    prio=42,
    stop=False,
    created=None,
    type='unsubscribe',
    conditions=[]
)


def make_filter(filter_type, enabled):
    return Filter(
        enabled=enabled,
        actions=[FilterAction(
            oper=filter_type,
            **ACTION_PARAMS
        )],
        **FILTER_PARAMS
    )


def test__disable_filter_without_type__disables_bad_type():
    filter_types = [FilterActionOperType.reply]
    filter_type = FilterActionOperType.move
    flt = make_filter(filter_type, enabled=True)

    assert not disable_filter_without_type(flt, filter_types).enabled


def test__disable_filter_without_type__does_not_disable_good_type():
    filter_type = FilterActionOperType.move
    flt = make_filter(filter_type, enabled=True)

    assert disable_filter_without_type(flt, [filter_type]).enabled


def test__disable_filter_without_type__does_not_enable_good_type():
    filter_type = FilterActionOperType.move
    flt = make_filter(filter_type, enabled=False)

    assert not disable_filter_without_type(flt, [filter_type]).enabled


def test__disable_filters_without_type():
    types = [
        FilterActionOperType.forward,
        FilterActionOperType.reply,
    ]
    user_data = PgUser()
    user_data.filters = [
        # Filter type is not in "types"
        make_filter(FilterActionOperType.status, enabled=True),
        # Filter type is in "types"
        make_filter(FilterActionOperType.forward, enabled=True),
    ]

    disable_filters_without_type(user_data, types)

    assert user_data.filters == [
        make_filter(FilterActionOperType.status, enabled=False),
        make_filter(FilterActionOperType.forward, enabled=True),
    ]
