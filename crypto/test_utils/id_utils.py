import random

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.match import TMatch
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.test_utils import fields

EXT_ID_TYPE = "ext_ns"
YANDEXUID_TYPE = "yandexuid"
ICOOKIE_TYPE = "icookie"
MIN_TOUCH = 1000 * 1000
MAX_TOUCH = 2000 * 1000
MIN_MATCH_TS = 1500 * 1000 * 1000
MAX_MATCH_TS = 1600 * 1000 * 1000
TTL = 86400


def id_to_dict(id, attributes, cas=None):
    ret = {
        fields.TYPE: id.Type,
        fields.VALUE: id.Value,
        fields.ATTRIBUTES: attributes or {}
    }
    if cas is not None:
        ret[fields.CAS] = cas
    return ret


def create_matched_id(id, synt, match_ts):
    ret = {
        fields.TYPE: id.Type,
        fields.VALUE: id.Value
    }

    if synt is not None:
        ret[fields.ATTRIBUTES] = {fields.SYNT: synt}
    if match_ts is not None:
        ret[fields.MATCH_TS] = match_ts

    return ret


def _get_random_id_value():
    return "{:010d}".format(random.randint(1000 * 1000 * 1000, 1200 * 1000 * 1000))


def _get_random_id(type):
    return TId(type, _get_random_id_value())


def _get_random_match_ts():
    return random.randint(MIN_MATCH_TS, MAX_MATCH_TS)


def _get_random_matched_id(type):
    return TMatchedId(_get_random_id(type), _get_random_match_ts(), 0, dict())


def create_random_id(type, add_prefix_func):
    return TId(type, add_prefix_func(_get_random_id_value()))


def get_random_match(ext_ns, touch=None, ttl=None):
    ext_id = _get_random_id(ext_ns)
    yandexuid = _get_random_matched_id(YANDEXUID_TYPE)
    icookie = _get_random_matched_id(ICOOKIE_TYPE)
    touch = touch or random.randint(MIN_TOUCH, MAX_TOUCH)
    ttl = ttl or TTL

    return TMatch(ext_id, {YANDEXUID_TYPE: yandexuid, ICOOKIE_TYPE: icookie}, touch, ttl)


class IdsForTest(object):
    def __init__(self, ext_id, yuid, icookie, yuid_attrs=None, icookie_attrs=None):
        self.ext_id = ext_id
        self.yuid = yuid
        self.icookie = icookie
        self.matched_yuid = TMatchedId(yuid, 0, 0, yuid_attrs or {})
        self.matched_icookie = TMatchedId(icookie, 0, 0, icookie_attrs or {})
        self.matched_ids_by_type = {
            yuid.Type: self.matched_yuid,
            icookie.Type: self.matched_icookie,
        }
        self.matched_ids = list(self.matched_ids_by_type.values())


class IdGenerator:
    def __init__(self, base=None):
        self.prefix = str(base) + '000' if base else ''

    def __call__(self, id):
        return self.prefix + str(id)


def create_ids_for_test(add_prefix_func, yuid_attrs=None, icookie_attrs=None):
    return IdsForTest(
        create_random_id(EXT_ID_TYPE, add_prefix_func),
        create_random_id(YANDEXUID_TYPE, add_prefix_func),
        create_random_id(ICOOKIE_TYPE, add_prefix_func),
        yuid_attrs,
        icookie_attrs
    )
