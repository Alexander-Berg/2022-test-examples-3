from search.martylib.config_utils.geo import mock_geo
from search.martylib.core.date_utils import mock_now
from search.martylib.enigma.martylib.test.models import YTestYtModel
from search.martylib.test_utils import TestCase
from search.martylib.unistat.metrics import MetricStorageMeta
from search.martylib.yt_utils.exceptions import NoAvailableReplicas
from search.martylib.yt_utils.manager import YtManager
from search.martylib.yt_utils.replica_controller import ReplicaController


class TestYtReplicaController(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.rc = ReplicaController('arnold', 'hahn')

        for cluster in cls.rc.states:
            client = YtManager().create_client(cluster, proxy=cluster, token='NO')
            client.config['proxy']['url'] = '<invalid test url>'

    @classmethod
    def _rerate_replicas(cls):
        # noinspection PyUnresolvedReferences
        cls.rc.rating = cls.rc.rate_replicas(*cls.rc.states.keys())

    def setUp(self):
        self._rerate_replicas()

        for _, state in self.rc.states.items():
            state.ready = True
            state.last_fail_time = 0.0
            state.reconnect_count = 0

    def test_init(self):
        with self.assertRaises(ValueError):
            ReplicaController()

        self.assertIs(
            ReplicaController('hahn'),
            ReplicaController('hahn'),
        )

        rc = ReplicaController('hahn')
        self.assertEqual(len(rc.states), 1)
        self.assertTrue(rc.states['hahn'].ready)
        self.assertEqual(rc.states['hahn'].last_fail_time, 0.0)

    def test_generated_models(self):
        self.assertIs(YTestYtModel.read_client, self.rc)
        self.assertEqual(YTestYtModel.DEFAULT_WRITE_CLIENT, 'markov')

    def test_picker(self):
        with mock_geo('sas'):
            self._rerate_replicas()
            self.assertEqual(self.rc.pick_replica(), 'hahn')
            self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 1)
            self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 0)

        with mock_geo('vla'):
            self._rerate_replicas()
            self.assertEqual(self.rc.pick_replica(), 'arnold')
            self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 0)
            self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 1)

        with mock_geo('man'):
            self._rerate_replicas()
            self.assertEqual(self.rc.pick_replica(), 'arnold')
            self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 0)
            self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 1)

    def test_fallback(self):
        with mock_geo('sas'):
            self._rerate_replicas()

            with mock_now(1):
                # Everything is fine, use closest cluster – Hahn.
                self.assertEqual(self.rc.pick_replica(), 'hahn')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 1)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 0)

                # Oops, Hahn seems to be unavailable – switch to Arnold.
                self.rc.states['hahn'].register_fail(RuntimeError())
                self.assertFalse(self.rc.states['hahn'].ready)
                self.assertEqual(self.rc.states['hahn'].last_fail_time, 1.0)

                self.assertEqual(self.rc.pick_replica(), 'arnold')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 0)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 1)

            with mock_now(11):
                # 10 seconds have passed, Hahn may be back online, but it's too soon to try and reconnect.
                self.assertEqual(self.rc.pick_replica(), 'arnold')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 0)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 1)

            with mock_now(56):
                # 56 seconds have passed since Hahn's fail, try to use it again.
                self.assertEqual(self.rc.pick_replica(), 'hahn')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 1)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 0)

                # Oh, but it's still down!
                self.rc.states['hahn'].register_reconnect(success=False, exception=RuntimeError())

                self.assertEqual(self.rc.pick_replica(), 'arnold')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 0)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 1)

            with mock_now(136):  # 55 (last fail time) + 82 (second backoff) - 1 second
                # 136 seconds have passed, but it's too soon to reconnect
                self.assertEqual(self.rc.pick_replica(), 'arnold')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 0)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 1)

            with mock_now(138):  # 55 (last fail time) + 82 (second backoff) + 1 second
                # 138 seconds have passed, let's see if Hahn's okay.
                self.assertEqual(self.rc.pick_replica(), 'hahn')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 1)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 0)

                # It is!
                self.rc.states['hahn'].register_reconnect(success=True, exception=None)

                # Closest cluster is online, so next picks should only yield Hahn.
                for x in range(10):
                    with mock_now(138 + x * 10):
                        self.assertEqual(self.rc.pick_replica(), 'hahn')
                        self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 1)
                        self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 0)

            with mock_now(900):
                # Oh now! ALL YT CLUSTERS ARE DOWN! RUN!
                self.rc.states['hahn'].register_fail(RuntimeError())
                self.rc.states['arnold'].register_fail(RuntimeError())

                with self.assertRaises(NoAvailableReplicas):
                    self.rc.pick_replica()

                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 0)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 0)

            with mock_now(901):
                # Only for a second though, they're okay.
                self.rc.states['hahn'].register_reconnect(success=True, exception=None)
                self.rc.states['arnold'].register_reconnect(success=True, exception=None)

                self.assertEqual(self.rc.pick_replica(), 'hahn')
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-hahn_ammv'], 1)
                self.assertEqual(MetricStorageMeta.to_protobuf().numerical['yt-picked-replica-arnold_ammv'], 0)
