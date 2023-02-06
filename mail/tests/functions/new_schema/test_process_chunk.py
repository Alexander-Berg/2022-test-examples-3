import pytest

from .misc import create_test_chunk
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
)
from psycopg2 import InternalError


def test_process_chunk_with_initial_state(context):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state='initial')

        res = db.code.process_chunk(res.operation.id, res.chunks[0].id, context.request_id)
        assert_that(res, equal_to('in_progress'), 'should transit chunk to in_progress state')


@pytest.mark.parametrize(('state'), ('in_revert', 'in_progress', 'done'))
def test_process_chunk_with_invalid_state(context, state):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state=state)

        assert_that(
            calling(db.code.process_chunk).with_args(res.operation.id, res.chunks[0].id, context.request_id),
            raises(InternalError, "no transition from {} state with process_chunk action".format(state))
        )
