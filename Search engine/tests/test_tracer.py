import random

from search.martylib.core.exceptions import NotFound, ValidationError
from search.martylib.db_utils import prepare_db, to_model, session_scope, clear_db
from search.martylib.test_utils import TestCase

from search.sawmill.proto import trace_pb2
from search.sawmill.src.services.storage import Storage
from search.sawmill.src.services.tracer import Tracer
from search.sawmill.tests.common import generate_traces

from sqlalchemy.orm import Session


def stored_trace_key_params(trace: trace_pb2.StoredTrace):
    return trace.id, trace.ttl, trace.added, trace.ended


class TestTracer(TestCase):
    @classmethod
    def setUpClass(cls):
        prepare_db()

    def setUp(self):
        clear_db()
        prepare_db()

        self.tracer = Tracer(Storage())
        self.stored_traces = list(generate_traces.generate_stored_traces(100))
        self.upload_traces = list(generate_traces.generate_upload_request(100))

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def test_list_traces(self):
        TestTracer.load_to_db(self.stored_traces)

        with self.mock_request() as ctx:
            returned_traces = self.tracer.list_traces(
                request=None,
                context=ctx,
            )

        self.assertEqual(
            set(map(stored_trace_key_params, self.stored_traces)),
            set(map(stored_trace_key_params, returned_traces.objects))
        )

    def test_upload(self):
        with self.mock_request() as ctx:
            uploaded_traces = []
            for upload_trace in self.upload_traces:
                uploaded_traces.append(upload_trace)
                self.tracer.upload(
                    request=upload_trace,
                    context=ctx,
                )

                returned_traces = self.tracer.list_traces(
                    request=None,
                    context=ctx,
                )

                self.assertEqual(
                    set(map(
                        lambda x: (x.parser_context.timeout, x.parser_context.offset),
                        uploaded_traces
                    )),
                    set(map(
                        lambda x: (x.request.parser_context.timeout, x.request.parser_context.offset),
                        returned_traces.objects
                    ))
                )

    def test_get_trace(self):
        with self.mock_request() as ctx:
            for upload_trace in self.upload_traces:
                self.tracer.upload(
                    request=upload_trace,
                    context=ctx,
                )

            returned_traces = self.tracer.list_traces(
                request=None,
                context=ctx,
            )

            for get_request in returned_traces.objects:
                retrieved_trace = self.tracer.get_trace(
                    request=trace_pb2.StoredTraceFilter(id=get_request.id, received_at=0),
                    context=ctx,
                )

                self.assertEqual(
                    stored_trace_key_params(get_request),
                    stored_trace_key_params(retrieved_trace.stored_trace)
                )

    def test_fail_get_trace(self):
        not_uploaded_trace = self.stored_traces[0]
        with self.mock_request() as ctx:
            with self.assertRaises(NotFound):
                self.tracer.get_trace(
                    request=not_uploaded_trace,
                    context=ctx,
                )

    def test_delete_traces(self):
        with self.mock_request() as ctx:
            first_half_traces = self.upload_traces[::2]
            second_half_traces = self.upload_traces[1::2]

            for upload_trace in first_half_traces:
                self.tracer.upload(
                    request=upload_trace,
                    context=ctx,
                )

            returned_traces = self.tracer.list_traces(
                request=None,
                context=ctx,
            )

            for upload_trace in second_half_traces:
                self.tracer.upload(
                    request=upload_trace,
                    context=ctx,
                )

            # deleting first_half_traces
            self.tracer.delete_traces(
                request=returned_traces,
                context=ctx,
            )

            # expecting second_half_traces
            left_traces = self.tracer.list_traces(
                request=None,
                context=ctx,
            )

        self.assertEqual(
            set(map(
                lambda x: (x.parser_context.timeout, x.parser_context.offset),
                second_half_traces
            )),
            set(map(
                lambda x: (x.request.parser_context.timeout, x.request.parser_context.offset),
                left_traces.objects
            ))
        )

    def test_set_ttl(self):
        with self.mock_request() as ctx:
            for upload_trace in self.upload_traces:
                self.tracer.upload(
                    request=upload_trace,
                    context=ctx,
                )

            returned_traces = self.tracer.list_traces(
                request=None,
                context=ctx,
            )

            new_ttl = {
                stored_trace.id: round(random.uniform(-10**4, 10**4), 3)
                for stored_trace in returned_traces.objects
            }

            default_ttl_ratio = 0.1 + random.random() / 4
            default_ttl_count = int(default_ttl_ratio * len(new_ttl.keys()))
            for trace_id in list(new_ttl.keys())[:default_ttl_count]:
                new_ttl[trace_id] = 0

            for stored_trace in returned_traces.objects:
                old_ttl = stored_trace.ttl
                stored_trace.ttl = new_ttl[stored_trace.id]
                if stored_trace.ttl < 0:
                    with self.assertRaises(ValidationError):
                        self.tracer.set_ttl(
                            request=stored_trace,
                            context=ctx,
                        )
                    stored_trace.ttl = old_ttl
                else:
                    self.tracer.set_ttl(
                        request=stored_trace,
                        context=ctx,
                    )

            for stored_trace in returned_traces.objects:
                returned_trace = self.tracer.get_trace(
                    request=trace_pb2.StoredTraceFilter(id=stored_trace.id, received_at=0),
                    context=ctx,
                )
                self.assertEqual(returned_trace.stored_trace.ttl, stored_trace.ttl)

    @staticmethod
    def load_to_db(traces):
        with session_scope() as session:
            session: Session
            for trace in traces:
                session.merge(to_model(trace))
