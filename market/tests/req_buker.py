from req_base import DataFile


class BukerBase(DataFile):
    def __init__(self, name):
        super(BukerBase, self).__init__(name)
        self.__data = {'__empty_json__': self.__json_empty_answer()}

    def __xml_answer(self, answer):
        return '<buker servant="buker" collection="cms-context-relations">' + answer + '</buker>'

    def __json_answer(self, answer):
        answer_str = ','.join(answer) if isinstance(answer, list) else answer
        return '{"__info__": {"servant": "buker"}, "result": [%s]}' % answer_str

    def __json_empty_answer(self):
        return self.__json_answer('')

    def _modify_params(self, params):
        pass

    def _key(self, method, params):
        self._modify_params(params)
        return self._calc_key(method, params)

    def add_empty_json(self, method, params):
        key = self._key(method, params)
        value = self.__json_empty_answer()
        self.__data[key] = value

    def add_json(self, method, params, answer):
        key = self._key(method, params)
        value = self.__json_answer(answer)
        self.__data[key] = value

    def add_xml(self, method, params, answer):
        key = self._key(method, params)
        value = self.__xml_answer(answer)
        self.__data[key] = value

    @property
    def data(self):
        return self.__data


class Buker(BukerBase):
    def __init__(self):
        super(Buker, self).__init__('buker')

    def _modify_params(self, params):
        params.append(("collection", "cms-context-relations"))


class Templator(BukerBase):
    def __init__(self):
        super(Templator, self).__init__('templator')

    def add_from_known_response(self, filename):

        def add_json_from_response(key, value):
            prefix, cgi = key.split('?', 1)
            method = prefix.split('/')[-1]
            params = {i.split('=', 1)[0] : i.split('=', 1)[1] for i in cgi.split('&')}
            params['depth_tarantino'] = '4'
            self.data[self._key(method, [(k, v) for k, v in params.items()])] = value

        with open(filename, 'r') as f:
            key = None
            value = ''
            for line in f.read().splitlines():
                if line.startswith('[') and line.endswith(']'):
                    if value != '' and key is not None:
                        add_json_from_response(key, value)
                        value = ''
                    key = line[1:-1]
                else:
                    value += line + '\n'
        if value != '' and key is not None:
            add_json_from_response(key, value)
