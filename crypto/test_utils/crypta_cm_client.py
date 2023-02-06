from library.python.protobuf.json import proto2json
import requests
import six.moves.urllib.parse as urlparse

from crypta.cm.services.api.lib.logic.touch.request.proto import touch_request_body_pb2
from crypta.lib.proto.identifiers import id_pb2
from crypta.lib.python.tvm import helpers as tvm


class CryptaCmClient:
    def __init__(self, host, port, service=None):
        self.host = "http://{host}:{port}".format(host=host, port=port)
        self.service = service

    def upload(self, data, subclient='', tvm_ticket=None):
        url = '{host}/upload?subclient={subclient}'.format(host=self.host, subclient=subclient)
        return requests.post(url, data=data, headers=tvm.get_tvm_headers(tvm_ticket))

    def _get_by_id(self, handle, id, subclient, tvm_ticket):
        return requests.get('{host}/{handle}?subclient={subclient}&{id}'.format(
            host=self.host,
            handle=handle,
            subclient=subclient,
            id=urlparse.urlencode([("type", id.Type), ("value", id.Value)])),
            headers=tvm.get_tvm_headers(tvm_ticket)
        )

    def identify(self, id, subclient='', tvm_ticket=None):
        return self._get_by_id('identify', id, subclient, tvm_ticket)

    def delete(self, id, subclient='', tvm_ticket=None):
        return self._get_by_id('delete', id, subclient, tvm_ticket)

    def expire(self, id, subclient='', tvm_ticket=None):
        return self._get_by_id('expire', id, subclient, tvm_ticket)

    def version(self, subclient='', tvm_ticket=None):
        return requests.get('{host}/version?subclient={subclient}'.format(host=self.host, subclient=subclient), headers=tvm.get_tvm_headers(tvm_ticket))

    def ping(self, subclient='', tvm_ticket=None):
        return requests.get('{host}/ping?subclient={subclient}'.format(host=self.host, subclient=subclient), headers=tvm.get_tvm_headers(tvm_ticket))

    def touch(self, ids, touch_ts, subclient='', tvm_ticket=None):
        url = '{host}/touch?subclient={subclient}'.format(host=self.host, subclient=subclient)
        proto_ids = touch_request_body_pb2.TTouchRequestBody(Items=[
            touch_request_body_pb2.TTouchRequestBody.TItem(
                Id=id_pb2.TId(Type=id_.Type, Value=id_.Value),
                TouchTimestamp=touch_ts,
            )
            for id_ in ids
        ])

        data = proto2json.proto2json(proto_ids, proto2json.Proto2JsonConfig(field_name_mode=proto2json.FldNameMode.FieldNameSnakeCase))
        return requests.post(url, data=data, headers=tvm.get_tvm_headers(tvm_ticket))
