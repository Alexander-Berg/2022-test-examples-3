# -*- coding: utf-8 -*-
import mock

from test.base import DiskTestCase
from mpfs.config import settings
from mpfs.core.services.hbf_service import HbfService
from mpfs.common.util import from_json, to_json
from test.helpers.stubs.services import HbfServiceStub


def create_tmp_hbf_cache(fn):
    def wrapped(self, *args, **kwargs):
        with mock.patch.dict(settings.services['HbfService'], {'cache_file_path': self.tmp_cache_filename}):
            return fn(self, *args, **kwargs)

    return wrapped


# noinspection PyMethodMayBeStatic
class HbfTestCase(DiskTestCase):
    def test_fetch_networks_by_macros(self):
        hbf = HbfService()
        networks = hbf.fetch_networks_by_macros('_DISKNETS_')
        assert HbfServiceStub.ip_included in networks

    def test_update_cache(self):
        hbf = HbfService()
        hbf.update_cache()
        fileobj = open(hbf.cache_file_path)
        data = fileobj.read()
        assert len(data)
        result = from_json(data)
        assert result
        assert all(macros in result for macros in hbf.macroses)

    def test_get_network(self):
        HbfService().update_cache()
        networks = HbfService().networks['_DISKNETS_']
        assert HbfServiceStub.ip_included in networks

    def test_update_cache_with_request_failed(self):
        requests = []

        def side_effect(*args, **kwargs):
            requests.append((args, kwargs))
            raise Exception("Test")

        with HbfServiceStub() as hbf_stub:
            hbf_stub.get_default_network_list.side_effect = side_effect
            HbfService().update_cache()
            assert len(requests) > 0
