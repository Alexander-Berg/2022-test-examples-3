import pytest

from .misc import create_test_chunk
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
)
from psycopg2 import InternalError


def test_process_chunk_done_with_in_progress_state(context):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state='in_progress')

        res = db.code.process_chunk_done(res.operation.id, res.chunks[0].id, context.request_id)
        assert_that(res, equal_to('done'), 'should transit chunk to done state')


@pytest.mark.parametrize(('state'), ('in_revert', 'initial', 'done'))
def test_process_chunk_done_with_invalid_state(context, state):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state=state)

        assert_that(
            calling(db.code.process_chunk_done).with_args(res.operation.id, res.chunks[0].id, context.request_id),
            raises(InternalError, "no transition from {} state with process_chunk_done action".format(state))
        )
