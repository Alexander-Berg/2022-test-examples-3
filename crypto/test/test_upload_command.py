from crypta.cm.services.common.changes.python.upload_command import TUploadCommand
from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.match import TMatch
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.test_utils import id_utils

EXT_NS = "ext_ns"
VALUE = "100500"


def test_to_string():
    incomingMatch = TMatch(TId(EXT_NS, VALUE), {}, 1700000000, 86400)
    incomingMatch.AddId(TMatchedId(TId(id_utils.YANDEXUID_TYPE, "1500000000"), 1500000000, 0, {"synt": "1"}))
    incomingMatch.AddId(TMatchedId(TId(id_utils.ICOOKIE_TYPE, "1600000000"), 1600000000, 1, {"synt": "0"}))
    timestamp = 1700000100

    upload_command = TUploadCommand(VALUE, incomingMatch, timestamp)
    serialized_command = TUploadCommand.ToString(upload_command)

    ref = ('{"cmd":"upload",'
           '"match":"ChAKBmV4dF9ucxIGMTAwNTAwElgKKgoVCgdpY29va2llEgoxNjAwMDAwMDAwEICg+PoFGgkKBHN5bnQSATAgAQoqChcKCXlhbmRleHVpZBIKMTUwMDAwMDAwMBCA3qDLBRoJCgRzeW50EgExGIDiz6oGIICjBQ==",'
           '"unixtime":1700000100,'
           '"sharding_key":"100500"}')
    assert ref == serialized_command
