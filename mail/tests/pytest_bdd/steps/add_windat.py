from pymdb.operations import AddWindat
from pymdb.tools import generate_st_id
from tests_common.pytest_bdd import when

mime_part_fields = [
    'hid',
    'content_type',
    'content_subtype',
    'boundary',
    'name',
    'charset',
    'encoding',
    'content_disposition',
    'filename',
    'cid',

    'offset_begin',
    'offset_end',
]


def make_add_windat(context, operation_maker, mid, st_id, hid, windat):
    op = operation_maker(AddWindat)
    i_fields = windat.split(':')
    windat = tuple(i_fields) + (None,) * (len(mime_part_fields) - len(i_fields))
    op(context.res.get_mid(mid), st_id, hid, windat)
    return op


@when('we add windat attachment to "{mid:Mid}"')
def step_add_windat_attach(context, mid):
    add_windat_attach(**locals())


@when('we add windat attachment to "{mid:Mid}" with "{st_id}" st_id')
def step_add_windat_attach_stid(context, mid, st_id):
    add_windat_attach(**locals())


@when('we add "{mid:Mid}" "{st_id}" "{hid}" "{windat}" to windat')
def step_add_windat_attach_full_info(context, mid, st_id, hid, windat):
    add_windat_attach(**locals())


def add_windat_attach(context, mid, st_id=None, hid='1.1', windat='1.1:xx'):
    st_id = st_id or generate_st_id('windat')
    make_add_windat(context, context.make_operation, mid, st_id, hid, windat).commit()


@when('we add windat attachment to "{mid}" with our new st_id')
def step_add_windat_attach_with_stid(context, mid):
    context.our_st_id = generate_st_id('windat')
    make_add_windat(context, context.make_operation, mid, context.our_st_id, "1.1", "1.1:xx").commit()


@when('we add windat attachment to "{mid:Mid}" with "{st_id}" st_id as "{op_id}"')
def step_async_add_windat_attach_stid(context, mid, st_id, op_id):
    async_add_windat_attach(**locals())


@when('we try add "{mid}" "{st_id}" "{hid}" "{windat}" to windat as "{op_id}"')
def step_async_add_windat_attach_full_info(context, mid, st_id, op_id, hid, windat):
    async_add_windat_attach(**locals())


def async_add_windat_attach(context, mid, st_id, op_id, hid='1.1', windat='1.1:xx'):
    op = make_add_windat(context, context.make_async_operation, mid, st_id, hid, windat)
    context.operations[op_id] = op
