import requests

from crypta.lib.python.tvm import helpers as tvm


class CryptaStyxApiClient:
    def __init__(self, host, port):
        self.host = "http://{host}:{port}".format(host=host, port=port)

    def status(self, subclient='', tvm_service_ticket=None):
        return requests.get('{host}/1/takeout/status/'.format(host=self.host), headers=tvm.get_tvm_headers(tvm_service_ticket))

    def status_private(self, puid, subclient='', tvm_service_ticket=None):
        return requests.get(
            '{host}/status_private?puid={puid}&subclient={subclient}'.format(host=self.host, puid=puid, subclient=subclient),
            headers=tvm.get_tvm_headers(tvm_service_ticket),
        )

    def delete(self, subclient='', tvm_service_ticket=None, tvm_user_ticket=None):
        return requests.post('{host}/1/takeout/delete/'.format(host=self.host), headers=tvm.get_tvm_headers_with_user(tvm_service_ticket, tvm_user_ticket))

    def delete_private(self, puid, service_ids, subclient='', tvm_service_ticket=None):
        service_ids = "&".join("service_id={}".format(id) for id in service_ids)
        return requests.get(
            '{host}/delete_private?puid={puid}&id={id}&subclient={subclient}&{service_ids}'.format(host=self.host, puid=puid, id="1", subclient=subclient, service_ids=service_ids),
            headers=tvm.get_tvm_headers(tvm_service_ticket),
        )

    def version(self, subclient='', tvm_service_ticket=None):
        return requests.get('{host}/version?subclient={subclient}'.format(host=self.host, subclient=subclient), headers=tvm.get_tvm_headers(tvm_service_ticket))

    def ping(self, subclient='', tvm_service_ticket=None):
        return requests.get('{host}/ping?subclient={subclient}'.format(host=self.host, subclient=subclient), headers=tvm.get_tvm_headers(tvm_service_ticket))
