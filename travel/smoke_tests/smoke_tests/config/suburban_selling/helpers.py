class GetTariffsEmptinessCheck(object):
    def __call__(self, checker, response):
        resp = response.json()
        assert len(resp['result']['selling_tariffs']) == 0


class GetTariffsCheck(object):
    def __init__(self, provider, key_should_exist):
        self.provider = provider
        self.key_should_exist = key_should_exist

    def __call__(self, checker, response):
        resp = response.json()
        seg_tariffs = resp['result']['selling_tariffs'][0]
        try:
            assert seg_tariffs['provider'] == self.provider
            tariff = seg_tariffs['tariffs'][0]
            assert self.key_should_exist in tariff, self.key_should_exist
            assert tariff['price'] > 0
        except Exception as ex:
            raise Exception(repr(ex), resp)
