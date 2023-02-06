from Crypto.Cipher import AES
from base64 import b64encode

class BaseLog(object):
    """Base log class."""

    registry = []

    def __init__(self, path, default_data, table_name="", date="", append=False):
        self.path = path
        self.date = date
        self.default_data = default_data
        self.table_name = table_name
        self.rows = {}
        self.append = append
        self.__class__.registry.append(self)

    def add_row(self, _date="", **kwargs):
        if _date:
            table_path = self.make_path(self.path, _date, self.table_name)
        else:
            table_path = self.make_path(self.path, self.date, self.table_name)
        new_data = self.make_row_data(kwargs)
        if self.rows.get(table_path):
            self.rows[table_path].append(new_data)
        else:
            self.rows[table_path] = [new_data]

    def make_row_data(self, new_params):
        new_data = self.default_data.copy()
        new_data.update(new_params)
        return new_data

    def make_path(self, path, date, name):
        return "/".join([x for x in (path, date, name) if x])

    @property
    def folder_path(self):
        return self.path

    def get_log(self):
        return self.rows


class SingleTableLog(BaseLog):
    """This class contains only one row.
    """
    rows = None

    def __init__(self, path, default_data=None, append=False):
        super(SingleTableLog, self).__init__(path, default_data=default_data, append=append)
        self.path = path
        self.rows = {path: []}

    def add_row(self, *args, **kwargs):
        self.rows[self.path].append(self.make_row_data(kwargs))

    def make_path(self, *args, **kwargs):
        return self.path

    @property
    def folder_path(self):
        return '/'.join(self.path.split('/')[:-1])


class FPLog(BaseLog):
    def make_row_data(self, new_params):
        value_data = self.default_data["value"].split("\t")
        if "history" in new_params:
            value_data[1] = "history=" + new_params["history"]
            del(new_params["history"])
        key_data = {parameter.split("=")[0]: parameter.split("=")[1] for parameter in self.default_data["key"].split("\t")}
        key_data.update(new_params)
        prepared_key_data = [key + "=" + value for key, value in key_data.iteritems()]
        return {"value": "\t".join(value_data), "subkey": "", "key": "\t".join(prepared_key_data)}


class AccessLog(BaseLog):

    def make_row_data(self, new_params):
        cookies = {parameter.split("=")[0]: parameter.split("=")[0] for parameter in self.default_data["cookies"].split("; ")}
        if "cookies" in new_params:
            cookies.update(new_params["cookies"])
            del(new_params["cookies"])
        cookies_list = [key + "=" + value for key, value in cookies.iteritems()]
        prepared_cookies_data = "; ".join(cookies_list)
        new_data = self.default_data.copy()
        del(new_data["cookies"])
        new_data.update(new_params)
        new_data["cookies"] = prepared_cookies_data
        return new_data

SUBPARAMS_NEW_DATA_TYPE = dict
PARAMETERS_DELIMETER = "\t"
NAME_VALUE_DELIMETER = "="


class ComplexParametersLog(BaseLog):
    # TODO: Need complex parameter in default_data, value_is_subparameters check it
    def make_row_data(self, new_params):
        complex_parameters_names = [name for name, value in new_params.iteritems() if isinstance(value, SUBPARAMS_NEW_DATA_TYPE)]
        result_data = {name: value for name, value in self.default_data.iteritems() if name not in complex_parameters_names}
        result_data.update({name: value for name, value in new_params.iteritems() if name not in complex_parameters_names})
        if not all([type(value) is SUBPARAMS_NEW_DATA_TYPE for name, value in new_params.iteritems() if name in complex_parameters_names]):
            raise Exception("ERROR: New test data format failed. Need dict for complex parameters.")
        for name in complex_parameters_names:
            result_data[name] = self.update_data(self.default_data[name], new_params[name])
        return result_data

    def update_data(self, data, new_params):
        data_dict = {prm_name: prm_value for prm_name, prm_value in
                     (prm.split(NAME_VALUE_DELIMETER) for prm in data.split(PARAMETERS_DELIMETER))}
        data_dict.update(new_params)
        return PARAMETERS_DELIMETER.join([name + NAME_VALUE_DELIMETER + value for name, value in data_dict.iteritems()])


def metrica_crypt(msg, rsa):
    aes_key = b'1' * 16
    aes_iv = b'1' * 16
    aes = AES.new(aes_key, AES.MODE_CBC, aes_iv)

    padding_len = 16 - (len(msg) % 16)
    padding = chr(padding_len)*padding_len

    aes_msg = aes.encrypt(msg + padding)
    rsa_msg = rsa.encrypt(aes_key + aes_iv)
    return b64encode(rsa_msg + aes_msg)
