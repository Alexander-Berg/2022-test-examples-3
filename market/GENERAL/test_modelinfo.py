import market.mars.lite.env as env


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {'api_report': cls.mars.api_report}

    @classmethod
    def prepare(cls):
        model_info = cls.mars.api_report.model_info()
        model_info.add(
            model_info.create_model_info(1, "one"),
            model_info.create_model_info(2, "two"),
            model_info.create_model_info(3, "three"),
        )
        model_info.add_code(hyperids=[4], code=500)

    def test_error_handling(self):
        """Проверяем, что при ошибке в запросе, клиент возвращает сообщение со status=error"""
        response = self.mars.request_json('modelinfo?hyperid=4')
        self.assertFragmentIn(response, {"status": "error"})

    def test_modelinfo_request(self):
        """Проверяем, что запросы из сервиса мокаются конфигом для репорта"""
        response = self.mars.request_json('modelinfo?hyperid=1&hyperid=2&hyperid=3')
        self.assertFragmentIn(
            response, [{"Id": 1, "Title": "one"}, {"Id": 2, "Title": "two"}, {"Id": 3, "Title": "three"}]
        )

    def test_trace_log_contains_out_request(self):
        self.mars.request_json('modelinfo?hyperid=1')
        self.trace_log.expect(request_method='/yandsearch', http_code=200, type='OUT')


if __name__ == '__main__':
    env.main()
