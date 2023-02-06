import sqlalchemy.orm

from queue import Queue

from search.martylib.core.logging_utils import configure_binlog
from search.martylib.db_utils import prepare_db
from search.martylib.db_utils import session_scope
from search.martylib.protobuf_utils.patch import patch_enums
from search.martylib.test_utils import TestCase

from search.mon.tickenator.proto.structures import tickenator_pb2
from search.mon.tickenator.proto.structures.ultima import task_pb2
from search.mon.tickenator.sqla.ultima import model
from search.mon.tickenator.src.services.ultima.tickenator import TickenatorUltima


patch_enums()


def _process_task_exception(session: sqlalchemy.orm.Session, ticket_auto_task: task_pb2.AutoTicketTask):
    raise Exception


def _process_task_ok(session: sqlalchemy.orm.Session, ticket_auto_task: task_pb2.AutoTicketTask):
    pass


class TestTaskQueue(TestCase):

    @classmethod
    def setUpClass(cls):
        prepare_db()
        configure_binlog(
            'tickenator',
            loggers=('tickenator', 'ultima', 'martylib', 'zephyr'),
            stdout=True,
        )

    @classmethod
    def tearDownClass(cls):
        # Clear all DB without deleting its schema.
        with session_scope() as session:
            for table in [
                model.AutoTicketTask,
                model.TicketPart,
            ]:
                session.query(table).delete(synchronize_session=False)

    def test_with_empty_db(self):
        tu = TickenatorUltima(Queue())

        tu._process_task = _process_task_ok

        # Run without tickets in db
        tu.handle_ticket_auto_task()

    def test_success(self):
        tu = TickenatorUltima(Queue())

        # Disable exception
        tu._process_task = _process_task_ok

        # Create ticket
        with self.mock_request() as ctx:
            task_id = tu._create_ticket_auto(
                request=tickenator_pb2.AutoTicketForm(),
                context=ctx,
            ).id

        tu.handle_ticket_auto_task()

        # Check status and error_count
        with session_scope() as session:
            task = session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first().to_protobuf()
            assert task.error_count == 0
            assert task.status == task_pb2.AutoTicketTaskStatus.PROCESSED

        # Delete task
        with session_scope() as session:
            session.delete(session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first())

    def test_fail_success(self):
        tu = TickenatorUltima(Queue())

        # Create new ticket
        with self.mock_request() as ctx:
            task_id = tu._create_ticket_auto(
                request=tickenator_pb2.AutoTicketForm(),
                context=ctx,
            ).id

        # Check if ticket has been saved to db
        with session_scope() as session:
            assert session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id) is not None

        # Enable exception
        tu._process_task = _process_task_exception

        # Drop ticket MAX_ERROR_COUNT - 1 times
        for _ in range(tu.MAX_ERROR_COUNT - 1):
            tu.handle_ticket_auto_task()

        assert session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first().to_protobuf().error_count == tu.MAX_ERROR_COUNT - 1

        # Disable exception
        tu._process_task = _process_task_ok

        tu.handle_ticket_auto_task()

        # Check status
        with session_scope() as session:
            task = session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first().to_protobuf()
            assert task.status == task_pb2.AutoTicketTaskStatus.PROCESSED
            assert task.error_count == tu.MAX_ERROR_COUNT - 1

        # Delete task
        with session_scope() as session:
            session.delete(session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first())

    def test_fail(self):
        tu = TickenatorUltima(Queue())

        # Create new ticket
        with self.mock_request() as ctx:
            task_id = tu._create_ticket_auto(
                request=tickenator_pb2.AutoTicketForm(),
                context=ctx,
            ).id

        # Enable exception
        tu._process_task = _process_task_exception

        error_count = 0

        with session_scope() as session:
            # Drop ticket MAX_ERROR_COUNT times
            for _ in range(tu.MAX_ERROR_COUNT):
                tu.handle_ticket_auto_task()
                assert session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first().to_protobuf().error_count == error_count + 1
                error_count += 1

        # Error count must be MAX_ERROR_COUNT
        with session_scope() as session:
            task = session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first().to_protobuf()
            assert task.error_count == tu.MAX_ERROR_COUNT

        # Drop ticket one more time
        tu.handle_ticket_auto_task()

        # Error count still must be MAX_ERROR_COUNT
        with session_scope() as session:
            task = session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first().to_protobuf()
            assert task.error_count == tu.MAX_ERROR_COUNT

        # Delete task
        with session_scope() as session:
            session.delete(session.query(model.AutoTicketTask).filter(model.AutoTicketTask.id == task_id).first())
