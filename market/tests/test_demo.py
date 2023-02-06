import unittest

from demo.demo import app


class TestDemo(unittest.TestCase):
    def test_hellp(self):
        with app.test_client() as c:
            result = c.get('/', environ_base={'REMOTE_ADDR': '127.0.0.1'})
            self.assertEqual(result.status_code, 200)
            self.assertEqual(result.data, 'Hello, 127.0.0.1!\n')

    def test_pingp(self):
        with app.test_client() as c:
            result = c.get('/ping', environ_base={'REMOTE_ADDR': '127.0.0.1'})
            self.assertEqual(result.status_code, 200)
            self.assertEqual(result.data, '0;OK\n')


if __name__ == "__main__":
    unittest.main()
