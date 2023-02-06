import pytest

from .misc import create_test_chunk
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
)
from psycopg2 import InternalError


@pytest.mark.parametrize(('state'), ('done', 'in_progress'))
def test_revert_chunk_with_valid_state(context, state):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state=state)

        res = db.code.revert_chunk(res.operation.id, res.chunks[0].id, context.request_id)
        assert_that(res, equal_to('in_revert'), 'should transit chunk to in_revert state')


@pytest.mark.parametrize(('state'), ('in_revert', 'initial'))
def test_revert_chunk_with_invalid_state(context, state):
    with context.reflect_db() as db:
        res = create_test_chunk(db, context, state=state)

        assert_that(
            calling(db.code.revert_chunk).with_args(res.operation.id, res.chunks[0].id, context.request_id),
            raises(InternalError, "no transition from {} state with revert_chunk action".format(state))
        )
