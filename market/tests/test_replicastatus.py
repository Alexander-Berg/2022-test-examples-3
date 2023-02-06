import unittest

from mock import patch
from watcher.replicastatus import ReplicaStatus, requests


class TestReplicaStatus(unittest.TestCase):
    class Response(object):
        def __init__(self, content):
            self.content = content
            self.url = 'http://example.com'
            self.status_code = 200

    def test_is_valid(self):
        rs = ReplicaStatus()

        with patch('watcher.replicastatus.requests.get') as mock_get:
            mock_get.return_value = self.Response('Ok.\n')
            self.assertTrue(rs.is_valid)

            mock_get.return_value = self.Response('NotOk.\n')
            self.assertFalse(rs.is_valid)

            mock_get.side_effect = requests.exceptions.Timeout
            self.assertFalse(rs.is_valid)

            mock_get.side_effect = Exception
            with self.assertRaises(Exception):
                rs.is_valid


if __name__ == "__main__":
    unittest.main()
