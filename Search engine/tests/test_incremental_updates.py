import grpc

from search.martylib.core.date_utils import mock_now

from search.stoker.proto.structures import record_pb2
from search.stoker.src.stoker_model_lib.test import ModelTestCase


class TestIncrementalUpdates(ModelTestCase):
    maxDiff = None

    PHONETIC_ALPHABET = (
        'alpha', 'bravo', 'charlie', 'delta',
        'echo', 'foxtrot', 'golf', 'hotel',
        'india', 'juliett', 'kilo', 'lima',
        'mike',
    )

    def test_incremental_updates(self):
        # Push some records first.
        for i in range(10):
            with mock_now(i):
                with self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
                    with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                        self.model._put_record(record_pb2.Record(key=self.PHONETIC_ALPHABET[i]), ctx)

        # Check if `listRecords` and `getUpdates` yield the same result.
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            list_records_response = self.model._list_records(record_pb2.RecordBatchFilter(include_deleted=True), ctx)
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            get_updates_response_alpha = self.model._get_updates(record_pb2.UpdateRequest(), ctx)

        self.assertEqual(list_records_response, get_updates_response_alpha)

        # Update some records.
        for i in range(3):
            with mock_now(i + 20):
                with self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
                    with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                        self.model._put_record(record_pb2.Record(key=self.PHONETIC_ALPHABET[i]), ctx)

        # And also add some new records:
        for i in range(3):
            with mock_now(i + 40):
                with self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
                    with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                        self.model._put_record(record_pb2.Record(key=self.PHONETIC_ALPHABET[i + 10]), ctx)

        # And also delete some records:
        for i in range(3):
            with mock_now(i + 50):
                with self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
                    with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                        self.model._delete_record(record_pb2.Record(key=self.PHONETIC_ALPHABET[i + 6]), ctx)

        # Calculate latest revision.
        latest_revision = max(record.revision for record in get_updates_response_alpha.objects)

        # Request records again.
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            list_records_response = self.model._list_records(record_pb2.RecordBatchFilter(include_deleted=True), ctx)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            get_updates_response_bravo = self.model._get_updates(record_pb2.UpdateRequest(revision=latest_revision), ctx)

        # Emulate bolver merge.
        bolver_state = record_pb2.RecordList()
        index_alpha = {}
        index_bravo = {}

        for record in get_updates_response_alpha.objects:
            index_alpha[(record.type, record.key)] = record
        for record in get_updates_response_bravo.objects:
            index_bravo[(record.type, record.key)] = record

        new_keys = set(index_bravo) - set(index_alpha)

        for key, record in index_alpha.items():
            if key in index_bravo:
                bolver_state.objects.extend((index_bravo[key], ))
            else:
                bolver_state.objects.extend((record, ))

        for key in new_keys:
            bolver_state.objects.extend((index_bravo[key], ))

        # Check if resulting state is correct.
        self.assertEqual(
            sorted(bolver_state.objects, key=lambda record: (record.type, record.key)),
            sorted(list_records_response.objects, key=lambda record: (record.type, record.key)),
        )
