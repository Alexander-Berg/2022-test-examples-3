from req_base import DataFile


class Cataloger(DataFile):
    def __init__(self):
        super(Cataloger, self).__init__('cataloger')
        self.__data = {}

    def __json_answer(self, answer):
        return '{"__info__": {"servant": "marketcataloger"}, "result": %s}' % answer

    def add_json(self, method, params, answer):
        key = self._calc_key(method, params)
        value = self.__json_answer(answer)
        self.__data[key] = value

    @property
    def data(self):
        return self.__data
