import market.mars.lite.env as env


class T(env.TestSuite):
    @classmethod
    def prepare(cls):
        pass

    def test_hello_json(self):
        response = self.mars.request_json('/hello?name=Thor')
        self.assertFragmentIn(response, {'greetings': 'Hello, Thor!'})


if __name__ == '__main__':
    env.main()
