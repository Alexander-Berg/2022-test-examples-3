import time

from search.martylib.test_utils import TestCase

from search.stoker.src.stoker_model_lib.cacher import TaggedBalancerCacher


class TestTaggedBalancerCacher(TestCase):
    def test_ttl(self):
        cacher = TaggedBalancerCacher(ttl_seconds=2)
        hosts = ['host1', 'host2']

        cacher['tag'] = hosts
        self.assertEqual(cacher['tag'], hosts)

        time.sleep(2)
        with self.assertRaises(KeyError):
            # noinspection PyStatementEffect
            cacher['tag']
