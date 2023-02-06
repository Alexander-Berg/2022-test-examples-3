import unittest

from watcher.iptruler import Iptruler


class TestIptruler(unittest.TestCase):
    def test_is_open_true(self):
        iptruler = Iptruler()

        iptruler.iptruler_bin = 'echo'
        iptruler.open()
        self.assertTrue(iptruler.status)

        iptruler.iptruler_bin = 'echo "{} down {}" #'.format(iptruler.port, iptruler.options)
        iptruler.close()
        self.assertFalse(iptruler.status)


if __name__ == "__main__":
    unittest.main()
