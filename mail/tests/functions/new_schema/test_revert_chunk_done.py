import pytest

from .misc import create_test_chunk
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
)
from psycopg2 import InternalError


def test_revert_chunk_done_with_in_revert_state(context):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state='in_revert')

        res = db.code.revert_chunk_done(res.operation.id, res.chunks[0].id, context.request_id)
        assert_that(res, equal_to('initial'), 'should transit chunk to initial state')


@pytest.mark.parametrize(('state'), ('done', 'in_progress', 'initial'))
def test_revert_chunk_done_with_invalid_state(context, state):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state=state)

        assert_that(
            calling(db.code.revert_chunk_done).with_args(res.operation.id, res.chunks[0].id, context.request_id),
            raises(InternalError, "no transition from {} state with revert_chunk_done action".format(state))
        )
