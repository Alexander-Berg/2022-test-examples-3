import concurrent.futures
import grpc
import uuid

from search.martylib.core.exceptions import format_exception

from search.stoker.proto.structures.record_pb2 import Record, RecordContent, RecordMatcher
from search.stoker.src.stoker_model_lib import Model
from search.stoker.src.stoker_model_lib.test import ModelTestCase


class TestModel(ModelTestCase):
    THREAD_COUNT = 100

    pool = concurrent.futures.ThreadPoolExecutor(max_workers=THREAD_COUNT)

    @staticmethod
    def await_and_log_futures(*futures):
        concurrent.futures.wait(futures)
        for f in futures:
            try:
                TestModel.logger.info('FUTURE: %s\n%s', f, f.result())
            except Exception as e:
                TestModel.logger.info('FUTURE: %s\n%s', f, format_exception(e, with_traceback=True))

    def _test_concurrent_insert(self, request, get_method, put_method, delete_method):
        futures = set()
        roles = self.model.WRITE_ROLES + self.model.ADMIN_ROLES

        def create_func():
            with self.mock_auth(login='test-user', roles=roles), self.mock_request() as threaded_context:
                # noinspection PyBroadException
                try:
                    getattr(self.model, put_method)(request, threaded_context)
                except:
                    pass

        with self.mock_auth(login='test-user', roles=roles):
            # Ensure that `request` is not already written to database.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.NOT_FOUND) as ctx:
                getattr(self.model, get_method)(request, ctx)

            try:
                # Call `put_method` from THREAD_COUNT threads.
                for _ in range(TestModel.THREAD_COUNT):
                    futures.add(self.pool.submit(create_func))

                concurrent.futures.wait(futures)

                # Ensure that sanity check passes.
                with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                    getattr(self.model, get_method)(request, ctx)
                    self.logger.info('sanity check response: code="%s", details="%s"', ctx._state.code, ctx._state.details)

            finally:
                # Delete `request` from database.
                with self.mock_request() as ctx:
                    getattr(self.model, delete_method)(request, ctx)

    def test_concurrent_record_insert(self):
        record = Record(type=Record.Type.YAPPY, key=str(uuid.uuid4()))
        self._test_concurrent_insert(
            request=record,
            get_method='_get_record',
            put_method='_put_record',
            delete_method='delete_record',
        )

    @ModelTestCase.mock_auth(login='test-user', roles=Model.WRITE_ROLES)
    def test_diff_put_records(self):
        r1 = Record(
            key='ydo-pll-3853-rr-templates',
            matcher=RecordMatcher(tag='some_tag'),
            content=RecordContent(
                request_id='bb',
                ttl=0,
            ),
        )

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            r1 = self.model._put_record(r1, ctx)

        r2 = Record()
        r2.CopyFrom(r1)
        r2.content.request_id = 'aa'
        r2.latest = False

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.assertEqual(r1, self.model._put_record(r2, ctx))

        r2.matcher.tag = 'test'
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.assertEqual(r2, self.model._put_record(r2, ctx))
