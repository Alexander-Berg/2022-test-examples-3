from req_base import DataFile


class DJ(DataFile):
    def __init__(self):
        super(DJ, self).__init__('dj')
        self.__data = {}

    def add_json(self, method, user_id, answer, add_params=None):
        params = add_params or {}
        params.update(user_id)
        params['experiment'] = method
        key = '/recommend?' + '&'.join([k + '=' + params[k] for k in sorted(params.keys())]) + '&clientid=test_user'
        self.__data[key] = answer

    @staticmethod
    def make_userid(puid="", uuid="", yandexuid="", deviceid="", idfa="", gaid=""):
        return {"puid": puid, "uuid": uuid, "yandexuid": yandexuid, "deviceid": deviceid, "idfa": idfa, "gaid": gaid}

    @staticmethod
    def make_rearr_factors(factors):
        return ';'.join(['{}={}'.format(k, v) for k, v in factors])

    @staticmethod
    def userid_to_cgi(user_id, cgi_separator='&'):
        return cgi_separator + cgi_separator.join([k + '=' + v for k, v in user_id.items()])

    @staticmethod
    def get_cgi_ignore(additional_ignore_list=None, cgi_separator='&'):
        l = [
            "puid",
            "yandexuid",
            "uuid",
            "deviceid",
            "idfa",
            "gaid",
            "client",
            "cart",
            "product_id",
            "page",
            "page-view-unique-id",
            "numdoc",
        ]
        l += additional_ignore_list or []
        return cgi_separator + 'ignore_cgi_params=' + ','.join(l)

    @property
    def data(self):
        return self.__data
