import typing

from search.martylib.core.logging_utils import configure_binlog
from search.martylib.db_utils import prepare_db
from search.martylib.db_utils import session_scope, to_protobuf
from search.martylib.protobuf_utils.patch import patch_enums
from search.martylib.test_utils import TestCase

from search.mon.tickenator_on_db.proto.structures import tickenator_pb2, task_pb2, ticket_pb2, progress_pb2
from search.mon.tickenator_on_db.sqla.tickenator import model
from search.mon.tickenator_on_db.src.reducers.progress_steps.tasks import RUNNERS_ORDER
from search.mon.tickenator_on_db.src.services.tickenator import Tickenator


class TestException(Exception):
    pass


patch_enums()


def _run_success(self, ticket_task: task_pb2.TicketTask, ticket: typing.Optional[ticket_pb2.Ticket]) -> typing.Optional[ticket_pb2.Ticket]:
    pass


def _run_fail(self, ticket_task: task_pb2.TicketTask, ticket: typing.Optional[ticket_pb2.Ticket]) -> typing.Optional[ticket_pb2.Ticket]:
    raise TestException


def _check_data_readiness_pass(self, ticket_task: task_pb2.TicketTask, ticket: ticket_pb2.Ticket) -> bool:
    return True


class TestTaskQueue(TestCase):

    @classmethod
    def setUpClass(cls):
        prepare_db()
        configure_binlog(
            'tickenator',
            loggers=('tickenator', 'martylib', 'zephyr'),
            stdout=True,
        )

    @classmethod
    def tearDownClass(cls):
        # Clear all DB without deleting its schema.
        with session_scope() as session:
            for table in [
                model.TicketTask,
                model.Ticket,
            ]:
                session.query(table).delete(synchronize_session=False)

    @staticmethod
    def _mock_steps(tickenator: Tickenator, fail: bool = False):
        """
        Disable exception as default (fail=False), otherwise enable exception (fail=True).
        """
        for runner_class in RUNNERS_ORDER[tickenator_pb2.CreationSource.BEHOLDER]:
            runner_class._run = _run_fail if fail else _run_success
            runner_class._check_data_readiness = _check_data_readiness_pass

    def _create_ticket_tasks(self, tu: Tickenator, count: int) -> typing.List[str]:
        task_ids = []
        with self.mock_request() as ctx:  # Create tasks
            for _ in range(count):
                task_ids.append(tu.create_merge_ticket_task(
                    request=tickenator_pb2.AlertCheck(),
                    context=ctx,
                ).id)
        return task_ids

    def _check_all_tasks_created(self, task_ids: typing.List[str]):
        with session_scope() as session:
            self.assertEqual(session.query(model.TicketTask).filter(model.TicketTask.id.in_(task_ids)).count(), len(task_ids))

    def _assert_status_and_error_count(
        self,
        task_ids: typing.List[str],
        status: progress_pb2.ProgressStatus = progress_pb2.ProgressStatus.PROCESSED,
        error_count: int = 0
    ):
        with session_scope() as session:
            tasks = session.query(model.TicketTask).filter(model.TicketTask.id.in_(task_ids))
            tasks = list(map(to_protobuf, tasks))
            for task in tasks:
                self.assertEqual(task.error_count, error_count)
                self.assertEqual(task.status, status)

    @staticmethod
    def _delete_ticket_tasks(task_ids: typing.List[str]):
        with session_scope() as session:
            for task_id in task_ids:
                session.delete(session.query(model.TicketTask).filter(model.TicketTask.id == task_id).first())

    def test_with_empty_db(self):
        tu = Tickenator()
        self._mock_steps(tu)
        tu.handle_tasks_reducer.handle_ticket_tasks()

    def test_success(self):
        tu = Tickenator()
        self._mock_steps(tu)

        test_tasks_count = min(5, tu.HANDLE_TASK_QUERY_LIMIT)
        task_ids = self._create_ticket_tasks(tu, test_tasks_count)
        self._check_all_tasks_created(task_ids)

        tu.handle_tasks_reducer.handle_ticket_tasks()  # Should resolve all TicketTasks

        self._assert_status_and_error_count(task_ids)

        self._delete_ticket_tasks(task_ids)

    def test_fail_success(self):
        tu = Tickenator()

        test_tasks_count = min(5, tu.HANDLE_TASK_QUERY_LIMIT)
        task_ids = self._create_ticket_tasks(tu, test_tasks_count)
        self._check_all_tasks_created(task_ids)

        # Enable exceptions and drop tickets MAX_ERROR_COUNT - 1 times
        self._mock_steps(tu, fail=True)
        for _ in range(tu.MAX_ERROR_COUNT - 1):
            tu.handle_tasks_reducer.handle_ticket_tasks()
        self._assert_status_and_error_count(task_ids, status=progress_pb2.ProgressStatus.PROCESSING, error_count=tu.MAX_ERROR_COUNT - 1)

        self._mock_steps(tu, fail=False)
        tu.handle_tasks_reducer.handle_ticket_tasks()
        self._assert_status_and_error_count(task_ids, error_count=tu.MAX_ERROR_COUNT - 1)

        self._delete_ticket_tasks(task_ids)

    def test_fail(self):
        tu = Tickenator()

        test_tasks_count = min(5, tu.HANDLE_TASK_QUERY_LIMIT)
        task_ids = self._create_ticket_tasks(tu, test_tasks_count)
        self._check_all_tasks_created(task_ids)

        self._mock_steps(tu, fail=True)
        for error_count in range(1, tu.MAX_ERROR_COUNT + 2):
            tu.handle_tasks_reducer.handle_ticket_tasks()
            if error_count <= tu.MAX_ERROR_COUNT:
                self._assert_status_and_error_count(task_ids, status=progress_pb2.ProgressStatus.PROCESSING, error_count=error_count)
            else:
                self._assert_status_and_error_count(task_ids, status=progress_pb2.ProgressStatus.BROKEN, error_count=tu.MAX_ERROR_COUNT)

        self._delete_ticket_tasks(task_ids)
