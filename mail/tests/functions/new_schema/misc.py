import json
from uuid import uuid4 as rand_uuid
from collections import namedtuple

OperationAndChunks = namedtuple('OperationAndChunks', ['operation', 'chunks'])

default_uid = 1000


def add_test_operation(db, context, op_id=None, uid=None, state=None):
    if op_id is None:
        op_id = rand_uuid()
    if uid is None:
        uid = default_uid
    data = {'type': 'test', 'param': 'value'}
    res = db.code.add_operation(op_id, 'complex_move', uid, json.dumps(data), context.request_id)
    assert res.inserted, "operation is not inserted - looks like database is not empty"
    if state is not None:
        db.mops.operations.update({'state': state}, id=op_id)
        return db.mops.operations.select(id=op_id)[0]
    return res.operation


def create_test_chunk(db, context, uid=None, op_id=None, state=None):
    op = add_test_operation(db, context, op_id, uid)
    chunks = [db.code.messages_chunk([9, 8, 7, 6, 5, 4, 3, 2, 1], '')]
    db.code.process_operation(op.id, chunks, context.request_id)
    if state is not None:
        db.mops.message_chunks.update({'state': state}, op_id=op.id, id=1)
    return OperationAndChunks(op, db.mops.message_chunks.select(op_id=op.id, id=1))


def last_change_log_entry(db, uid):
    res = db.mops.change_log.select('ORDER BY cid DESC', uid=uid)
    assert len(res) > 0, "no change log entry found for uid={}".format(uid)
    return res[0]
