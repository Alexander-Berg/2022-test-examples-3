import concurrent.futures

from infra.yp_service_discovery.api.api_pb2 import TRspResolveEndpoints, TEndpointSet, TEndpoint

from search.martylib.config_utils.geo import mock_geo
from search.martylib.core.date_utils import mock_now
from search.martylib.sd import ServiceDiscoveryClient, WeightedGroup, WeightedInstance
from search.martylib.test_utils import TestCase


class TestWeightedGroup(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sd = ServiceDiscoveryClient()

    def setUp(self):
        self.sd.clear()

        self.sd[('test', 'alpha')] = TRspResolveEndpoints(endpoint_set=TEndpointSet(endpoints=(
            TEndpoint(
                id='alpha',
                fqdn='alpha.sas.yp-c.yandex.net',
                port=80,
            ),
        )))
        self.sd[('test', 'bravo')] = TRspResolveEndpoints(endpoint_set=TEndpointSet(endpoints=(
            TEndpoint(
                id='bravo',
                fqdn='bravo.vla.yp-c.yandex.net',
                port=80,
            ),
        )))

        with mock_geo('sas'):
            self.group = WeightedGroup('test/alpha', 'test/bravo')
            self.group.update_endpoints(ignore_cache=False)

        for i in self.group._instances:
            i.reset()

    def test_re_resolve(self):
        self.sd[('test', 'alpha')] = TRspResolveEndpoints(endpoint_set=TEndpointSet(endpoints=(
            TEndpoint(
                id='charlie',
                fqdn='charlie.sas.yp-c.yandex.net',
                port=80,
            ),
        )))

        for i in self.group._instances:
            i.register_usage(success=False)

        self.group.update_endpoints(ignore_cache=False)
        with self.group.endpoint(return_endpoint=False) as e:
            self.assertEqual(e.endpoint.id, 'charlie')

    def test_weights(self):
        with mock_now(1):
            # First request, no usages on any instances.
            with self.group.endpoint(return_endpoint=False) as e:
                # Alpha is used, because it's in the same DC as a client.
                alpha = e
                self.assertEqual(e.endpoint.id, 'alpha')

        # While alpha is healthy, bravo is not used.
        for i in range(100):
            with mock_now(1 + i):
                with self.group.endpoint(return_endpoint=False) as e:
                    self.assertEqual(e.endpoint.id, 'alpha')

        # Alpha's weight is decreasing with each fail, until it's lower than bravo's weight.
        # 3 fails should be enough to attempt XDC (fail count scales with instance count in closest DC).
        for i in range(3):
            with mock_now(102 + i):
                try:
                    with self.group.endpoint(return_endpoint=False) as e:
                        self.assertEqual(e.endpoint.id, 'alpha', self.group._instances)
                        raise ValueError
                except ValueError:
                    pass

        # Just checking if fail tracking works...
        self.assertEqual(list(alpha._usages), 17 * [True] + 3 * [False])

        with mock_now(108):
            with self.group.endpoint(return_endpoint=False) as e:
                self.assertEqual(e.endpoint.id, 'bravo', self.group._instances)

        # After 3 alpha fails, bravo should be used for at least 50 seconds.
        # Alpha's last request was at 104s.
        for i in range(45):
            with mock_now(109 + i):
                with self.group.endpoint(return_endpoint=False) as e:
                    self.assertEqual(e.endpoint.id, 'bravo', i)

        # But after 50 seconds alpha gets a second chance. And fails again.
        with mock_now(154):
            try:
                with self.group.endpoint(return_endpoint=False) as e:
                    self.assertEqual(e.endpoint.id, 'alpha', self.group._instances)
                    raise ValueError
            except ValueError:
                pass

        # Since it's still failing, Alpha won't get another chance for at least 67 more seconds.
        # Alpha's last request was at 154s.
        with mock_now(220):
            with self.group.endpoint(return_endpoint=False) as e:
                self.assertEqual(e.endpoint.id, 'bravo', self.group._instances)

        with mock_now(221):
            with self.group.endpoint(return_endpoint=False) as e:
                self.assertEqual(e.endpoint.id, 'alpha', self.group._instances)

        # Once recovered, alpha gets a weight coefficient reset.
        self.assertEqual(alpha._get_weight_coefficient(), 1)

        for i in range(3):
            with mock_now(222 + i):
                with self.group.endpoint(return_endpoint=False) as e:
                    self.assertEqual(e.endpoint.id, 'alpha', self.group._instances)

    def test_with_usage(self):
        with self.group.endpoint(return_endpoint=False) as instance:  # type: WeightedInstance
            def fn(i):
                raise RuntimeError(i)

            with self.assertRaises(RuntimeError):
                instance.with_usage(fn)()

            self.assertEqual(list(instance._usages), [False])

    def test_map(self):
        results = set()
        pool = concurrent.futures.ThreadPoolExecutor(2)

        def worker(instance):
            # type: (WeightedInstance) -> None
            results.add(instance.endpoint.id)

        futures = list(self.group.map(pool, worker))
        for f in futures:
            f.result()

        self.assertEqual(results, {'alpha', 'bravo'})

        for i in self.group._instances:
            self.assertEqual(list(i._usages), [True])
