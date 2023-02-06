import collections
import copy
import grpc
import itertools
import uuid

from google.protobuf.empty_pb2 import Empty
from search.martylib.core.exceptions import NotAuthenticated, NotAuthorized, ValidationError
from search.martylib.db_utils import clear_db, prepare_db
from search.martylib.proto.structures import auth_pb2
from search.martylib.test_utils import TestCase

from search.stoker.proto.structures import balancer_pb2, record_pb2, stats_pb2
from search.stoker.src.stoker_model_lib import Model
from search.stoker.src.stoker_model_lib.test import ModelTestCase


# noinspection PyProtectedMember
class TestApi(ModelTestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def create_tagged_balancers(self, tag, hosts):
        for host in hosts:
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(host=host, tag=tag), ctx)

    def test_auth(self):
        InputSuite = collections.namedtuple(
            'InputSuite',
            (
                'put',
                'batch_put',
                'raw_put',
                'delete',

                'list',
                'get',
                'get_revisions',

                'request',
                'batch_request',
                'filter',
                'batch_filter',
            ),
        )

        record = record_pb2.Record(key=str(uuid.uuid4()), latest=True, revision=1.0)
        record_list = record_pb2.RecordList(objects=(record, ))
        record_batch_filter = record_pb2.RecordBatchFilter()

        auth_info = auth_pb2.AuthInfo(login='test-user', roles=self.model.WRITE_ROLES, groups=(1, 1234))

        # Can't use underscored methods here - middleware will catch exceptions.
        input_suites = (
            InputSuite(
                put='put_record',
                batch_put='put_records',
                raw_put='put_raw_records',
                delete='delete_record',

                list='list_records',
                get='get_record',
                get_revisions='retrieve_record_revisions',

                request=record,
                batch_request=record_list,
                filter=record,
                batch_filter=record_batch_filter,
            ),
        )

        for suite in input_suites:
            self.logger.info('testing auth suite:\n%s', suite)

            rpc_put = getattr(self.model, suite.put)
            rpc_batch_put = getattr(self.model, suite.batch_put)
            rpc_raw_put = getattr(self.model, suite.raw_put)
            rpc_delete = getattr(self.model, suite.delete)
            rpc_list = getattr(self.model, suite.list)
            rpc_get = getattr(self.model, suite.get)
            rpc_get_revisions = getattr(self.model, suite.get_revisions)

            try:
                # All write methods should raise NotAuthenticated on empty AuthInfo.
                with self.mock_request() as ctx:
                    with self.assertRaisesWithMessage(NotAuthenticated, message=suite.put):
                        rpc_put(copy.deepcopy(suite.request), ctx)
                    with self.assertRaisesWithMessage(NotAuthenticated, message=suite.batch_put):
                        rpc_batch_put(copy.deepcopy(suite.batch_request), ctx)
                    with self.assertRaisesWithMessage(NotAuthenticated, message=suite.raw_put):
                        rpc_raw_put(copy.deepcopy(suite.batch_request), ctx)
                    with self.assertRaisesWithMessage(NotAuthenticated, message=suite.delete):
                        rpc_delete(copy.deepcopy(suite.request), ctx)

                # All write methods except for raw put should raise NotAuthorized for users with no WRITE_ROLES, even for users with ADMIN_ROLES.
                with self.mock_request() as ctx, self.mock_auth(login='test-user', roles=self.model.ADMIN_ROLES):
                    with self.assertRaisesWithMessage(NotAuthorized, message=suite.put):
                        rpc_put(copy.deepcopy(suite.request), ctx)
                    with self.assertRaisesWithMessage(NotAuthorized, message=suite.batch_put):
                        rpc_batch_put(copy.deepcopy(suite.batch_request), ctx)
                    with self.assertRaisesWithMessage(NotAuthorized, message=suite.delete):
                        rpc_delete(copy.deepcopy(suite.request), ctx)

                # Raw put method should raise NotAuthorized for users with no ADMIN_ROLES, even for users with WRITE_ROLES.
                with self.mock_request() as ctx, self.mock_auth(auth_info=auth_info):
                    with self.assertRaisesWithMessage(NotAuthorized, message=suite.raw_put):
                        rpc_raw_put(copy.deepcopy(suite.batch_request), ctx)

                # All write methods except for raw put should work for users with WRITE_ROLES.
                with self.mock_request() as ctx, self.mock_auth(auth_info=auth_info):
                    rpc_put(copy.deepcopy(suite.request), ctx)
                    response = rpc_batch_put(copy.deepcopy(suite.batch_request), ctx)
                    rpc_delete(copy.deepcopy(response.objects[0]), ctx)

                # Raw put should work for users with ADMIN_ROLES.
                with self.mock_request() as ctx, self.mock_auth(login='test-user', roles=self.model.ADMIN_ROLES):
                    rpc_raw_put(copy.deepcopy(suite.batch_request), ctx)

                # All read-only methods should work for anybody.
                with self.mock_request() as ctx:
                    rpc_list(suite.batch_filter, ctx)
                    rpc_get(suite.filter, ctx)
                    rpc_get_revisions(suite.filter, ctx)

            finally:
                pass

        # Reverse suites so quotas aren't deleted before RPS limiter records using them.
        with self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
            for suite in reversed(input_suites):
                rpc_get = getattr(self.model, suite.get)
                rpc_delete = getattr(self.model, suite.delete)

                with self.mock_request() as ctx:
                    latest = rpc_get(copy.deepcopy(suite.filter), ctx)

                # Clear database after suit is ran.
                with self.mock_request() as ctx:
                    rpc_delete(copy.deepcopy(latest), ctx)

    def test_record_validation(self):
        valid_key = str(uuid.uuid4())
        with self.mock_request() as ctx, self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
            # Ensure that record has a key.
            with self.assertRaises(ValidationError):
                self.model.put_record(record_pb2.Record(), ctx)

            # Ensure that record's key matches KEY_REGEXP.
            with self.assertRaises(ValidationError):
                self.model.put_record(record_pb2.Record(key='spaces are bad and you should feel bad'), ctx)

            # Records with valid key should be valid.
            self.model.put_record(record_pb2.Record(key=valid_key), ctx)

            # Delete method should check record key.
            with self.assertRaises(ValidationError):
                self.model.delete_record(record_pb2.Record(), ctx)

            self.model.delete_record(record_pb2.Record(key=valid_key), ctx)

    def test_functionality_for_records(self):
        with self.mock_auth(login='test-user', roles=itertools.chain(self.model.WRITE_ROLES, self.model.ADMIN_ROLES)):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._list_records(record_pb2.RecordBatchFilter(all=True), ctx).objects), 0,
                    'database is not empty'
                )

            id1 = str(uuid.uuid4())
            id2 = str(uuid.uuid4())

            # `put_record` creates new record, if records with given key don't exist.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_record(record_pb2.Record(key=id1), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._list_records(record_pb2.RecordBatchFilter(all=True), ctx).objects), 1,
                    'put_record: new record is not created'
                )

            # `put_record` creates new revision for existing record.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_record(record_pb2.Record(key=id1), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._list_records(record_pb2.RecordBatchFilter(), ctx).objects), 1,
                    'put_record: new record revision is not created'
                )

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._list_records(record_pb2.RecordBatchFilter(all=True), ctx).objects), 2,
                    'put_record: new record revision is not created'
                )

            # `get_record` returns latest revision for given key.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                response = self.model._get_record(record_pb2.Record(key=id1), ctx)
                self.assertTrue(response.latest)

            # `get_record` returns NOT_FOUND for non-existing keys.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.NOT_FOUND) as ctx:
                self.model._get_record(record_pb2.Record(key=id2), ctx)

            # `retrieve_record_revisions` returns all revisions for given key.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                response = self.model._retrieve_record_revisions(record_pb2.Record(key=id1), ctx)
                self.assertEqual(len(response.objects), 2)

            # `retrieve_record_revisions` returns OK for non-existing keys.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                response = self.model._retrieve_record_revisions(record_pb2.Record(key=id2), ctx)
                self.assertEqual(len(response.objects), 0)

            # `put_records` supports both existing and non-existing records.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model.put_records(
                    record_pb2.RecordList(objects=(
                        record_pb2.Record(key=id1),
                        record_pb2.Record(key=id2),
                    )),
                    ctx,
                )

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._retrieve_record_revisions(record_pb2.Record(key=id1), ctx).objects), 3,
                    'put_records: existing record is not updated'
                )

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._retrieve_record_revisions(record_pb2.Record(key=id2), ctx).objects), 1,
                    'put_records: new record is not created'
                )

            # `put_records` raises exception on non-unique keys.
            with self.mock_request(
                expected_grpc_status_code=grpc.StatusCode.INVALID_ARGUMENT,
                expected_grpc_details_regexp=r'attempt to batch create/update records with non-unique key',
            ) as ctx:
                self.model._put_records(
                    record_pb2.RecordList(objects=(
                        record_pb2.Record(key=id1),
                        record_pb2.Record(key=id1),
                    )),
                    ctx,
                )

            # `put_raw_records` blindly merges request, breaking database in certain cases.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_raw_records(
                    record_pb2.RecordList(objects=(
                        record_pb2.Record(key=id1, latest=True),
                        record_pb2.Record(key=id1, latest=True),
                        record_pb2.Record(key=id1, latest=True),
                    )),
                    ctx,
                )

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.INTERNAL, expected_grpc_details_regexp=r'multiple latest revisions') as ctx:
                self.model._get_record(record_pb2.Record(key=id1), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.INTERNAL, expected_grpc_details_regexp=r'multiple latest revisions') as ctx:
                self.model._list_records(record_pb2.RecordBatchFilter(), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.INTERNAL, expected_grpc_details_regexp=r'multiple latest revisions') as ctx:
                self.model._list_records(record_pb2.RecordBatchFilter(all=True), ctx)

            self.assertEqual(self.model.global_metrics['global-failed-record-sanity-checks_summ'], 3)

            # `delete_record` deletes all record revisions.
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._delete_record(record_pb2.Record(key=id1), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._delete_record(record_pb2.Record(key=id2), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._list_records(record_pb2.RecordBatchFilter(all=True, include_deleted=False), ctx).objects), 5,
                )

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                response = self.model._list_records(record_pb2.RecordBatchFilter(all=True, include_deleted=True), ctx)
                self.assertEqual(
                    len(response.objects), 7,
                    'records are not deleted',
                )

                self.assertEqual(len([record for record in response.objects if record.deleted]), 2)

            # FIXME: test tagged matcher

    def test_sanity_check_on_duplicated_keys(self):
        key = str(uuid.uuid4())

        with self.mock_auth(login='test-user', roles=itertools.chain(self.model.WRITE_ROLES, self.model.ADMIN_ROLES)):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_record(record_pb2.Record(type=record_pb2.Record.Type.HAMSTER, key=key), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_record(record_pb2.Record(type=record_pb2.Record.Type.YAPPY, key=key), ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._list_records(record_pb2.RecordBatchFilter(), ctx)

    def test_tagged_balancer_auth(self):
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.UNAUTHENTICATED) as ctx:
            self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(), ctx)

        with self.mock_auth(login='test-user'), self.mock_request(expected_grpc_status_code=grpc.StatusCode.PERMISSION_DENIED) as ctx:
            self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(), ctx)

        with self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.PERMISSION_DENIED) as ctx:
                self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(), ctx)

        with self.mock_auth(login='test-user', roles=self.model.ADMIN_ROLES), self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(), ctx)

    @TestCase.mock_auth(login='test-user', roles=Model.ADMIN_ROLES)
    def test_tagged_balancer_functionality(self):
        tags = [
            str(uuid.uuid4()),
            str(uuid.uuid4()),
        ]
        hosts = [
            'yappy.yandex',
            'hamster.yandex',
        ]

        for host in hosts:
            for tag in tags:
                with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                    self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(host=host, tag=tag), ctx)

                # Ensure that duplicate tagging is allowed.
                with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                    self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(host=host, tag=tag), ctx)

        # Ensure that filtering works as expected.
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response = self.model._list_tagged_balancers(balancer_pb2.TaggedBalancerFilter(tags=tags[:1]), ctx)
            self.assertEqual(len(response.objects), 2)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response = self.model._list_tagged_balancers(balancer_pb2.TaggedBalancerFilter(tags=tags), ctx)
            self.assertEqual(len(response.objects), 4)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response = self.model._list_tagged_balancers(balancer_pb2.TaggedBalancerFilter(hosts=hosts[:1]), ctx)
            self.assertEqual(len(response.objects), 2)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response = self.model._list_tagged_balancers(balancer_pb2.TaggedBalancerFilter(hosts=hosts), ctx)
            self.assertEqual(len(response.objects), 4)

        # Ensure that deletion works for non-existent balancers.
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.model._delete_tagged_balancer(balancer_pb2.TaggedBalancer(host='whatever', tag=str(uuid.uuid4())), ctx)

        # Ensure that deletion works for existing balancers.
        for host in hosts:
            for tag in tags:
                with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                    self.model._delete_tagged_balancer(balancer_pb2.TaggedBalancer(host=host, tag=tag), ctx)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response = self.model._list_tagged_balancers(balancer_pb2.TaggedBalancerFilter(), ctx)
            self.assertEqual(len(response.objects), 0)

    @TestCase.mock_auth(login='test-user', roles=Model.ADMIN_ROLES + Model.WRITE_ROLES)
    def test_hosts_functionality(self):
        tag = str(uuid.uuid4())
        hosts = [
            'yappy.yandex',
            'hamster.yandex',
        ]

        record = record_pb2.Record(
            key=str(uuid.uuid4()),
            type=record_pb2.Record.Type.TAGGED,
            matcher=record_pb2.RecordMatcher(tag=tag),
        )

        for host in hosts:
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_tagged_balancer(balancer_pb2.TaggedBalancer(host=host, tag=tag), ctx)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.model._put_record(record, ctx)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response = self.model._get_record(record, ctx)

        self.assertEqual(
            response.matcher.hosts,
            ['{}.{}'.format(response.key, host) for host in hosts],
            'get_record did not inflate tagged hosts matcher',
        )

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response = self.model._list_records(record_pb2.RecordBatchFilter(all=True), ctx)

        self.assertEqual(
            response.objects[0].matcher.hosts,
            ['{}.{}'.format(response.objects[0].key, host) for host in hosts],
            'list_records did not inflate tagged hosts matcher',
        )

    @TestCase.mock_auth(login='test-user', roles=Model.ADMIN_ROLES + Model.WRITE_ROLES)
    def test_record_caching(self):
        tag = str(uuid.uuid4())
        hosts = [
            'yappy.yandex',
            'hamster.yandex',
        ]

        record = record_pb2.Record(
            key=str(uuid.uuid4()),
            type=record_pb2.Record.Type.TAGGED,
            matcher=record_pb2.RecordMatcher(tag=tag),
        )

        self.create_tagged_balancers(tag, hosts)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.model._put_record(record, ctx)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response_a = self.model._list_records(record_pb2.RecordBatchFilter(), ctx)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            response_b = self.model._list_records(record_pb2.RecordBatchFilter(), ctx)

        self.assertEqual(len(response_a.objects), 1)
        self.assertEqual(response_a, response_b)

    def test_match_stats_auth(self):
        self.skipTest('auth is temporarily disabled')

        # Listing requires no auth.
        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.model._list_match_stats(Empty(), ctx)

        # Reporting requires `stoker.reporter` role.
        with self.mock_auth(login='test-user'), self.mock_request(expected_grpc_status_code=grpc.StatusCode.PERMISSION_DENIED) as ctx:
            self.model._report_match_stats(stats_pb2.MatchStatList(), ctx)

        with self.mock_auth(login='test-user', roles=self.model.REPORT_ROLES), self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.model._report_match_stats(stats_pb2.MatchStatList(), ctx)

        # Deleting requires `stoker.admin` role.
        for current_roles in ((), self.model.REPORT_ROLES):
            with self.mock_auth(login='test-user', roles=current_roles):
                with self.mock_request(expected_grpc_status_code=grpc.StatusCode.PERMISSION_DENIED) as ctx:
                    self.model._delete_match_stats(stats_pb2.MatchStatList(), ctx)

        with self.mock_auth(login='test-user', roles=self.model.ADMIN_ROLES), self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.model._delete_match_stats(stats_pb2.MatchStatList(), ctx)

    def test_match_stats_functionality(self):
        with self.mock_auth(login='test-user', roles=self.model.REPORT_ROLES):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.assertEqual(
                    len(self.model._list_match_stats(Empty(), ctx).objects), 0
                )

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.INVALID_ARGUMENT, expected_grpc_details_regexp='key is required') as ctx:
                self.model._report_match_stats(stats_pb2.MatchStatList(objects=(
                    stats_pb2.MatchStat(),
                )), ctx)

            stats = stats_pb2.MatchStatList(objects=(
                stats_pb2.MatchStat(key='test1'),
                stats_pb2.MatchStat(key='test2'),
            ))

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._report_match_stats(stats, ctx)

            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                response = self.model._list_match_stats(Empty(), ctx)

            self.assertEqual(sorted((x.key for x in response.objects)), ['test1', 'test2'])

        with self.mock_auth(login='test-user', roles=self.model.ADMIN_ROLES):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._delete_match_stats(stats, ctx)

        with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
            self.assertEqual(
                len(self.model._list_match_stats(Empty(), ctx).objects),
                0
            )

    def test_regexp_record_api(self):
        k1 = str(uuid.uuid4())
        k2 = str(uuid.uuid4())

        record = record_pb2.Record(
            key=k1,
            type=record_pb2.Record.Type.TAGGED,
            matcher=record_pb2.RecordMatcher(regexp=r'.*', tag='yappy'),
        )

        with self.mock_auth(login='test-user', roles=self.model.WRITE_ROLES):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.PERMISSION_DENIED) as ctx:
                self.model._put_record(record, ctx)

        with self.mock_auth(login='test-user', roles=self.model.ADMIN_ROLES + self.model.WRITE_ROLES):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_record(record, ctx)

        record.key = k2
        with self.mock_auth(login='test-user', roles=self.model.REGEXP_ADMIN_ROLES + self.model.WRITE_ROLES):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                self.model._put_record(record, ctx)

        for k in (k1, k2):
            record.key = k
            with self.mock_auth(login='test-user', roles=self.model.REGEXP_ADMIN_ROLES + self.model.WRITE_ROLES):
                with self.mock_request(expected_grpc_status_code=grpc.StatusCode.OK) as ctx:
                    record = self.model._get_record(record, ctx)
                    self.assertEqual(record.matcher.regexp, r'.*')

        record.type = record_pb2.Record.Type.HAMSTER
        with self.mock_auth(login='test-user', roles=self.model.REGEXP_ADMIN_ROLES + self.model.WRITE_ROLES):
            with self.mock_request(expected_grpc_status_code=grpc.StatusCode.INVALID_ARGUMENT) as ctx:
                self.model._put_record(record, ctx)
