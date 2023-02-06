# -*- coding: utf-8 -*-

from mock import patch
import pytest
from rtcc.dataprovider.hwdata import HWData
from rtcc.dataprovider.hwdata import host_id
from rtcc.model.grouping.custom.localhost_first import LocalHostFirstLocationGrouppingPolicy
from rtcc.model.raw import Instance


def test_localfirst_port():
    with patch.object(HWData, '_init_hwdata') as hwdata_mock:
        hwdata_mock.return_value = {host_id("somehost", ".yandex.ru"): {"dc": "man", "cpu": "dummy_value"},
                                    host_id("anotherhost", ".yandex.ru"): {"dc": "sas", "cpu": "dummy_value"}, }
        instance1 = Instance("somehost.yandex.ru", "8080")
        instance2 = Instance("anotherhost.yandex.ru", "8080")

        grouping = LocalHostFirstLocationGrouppingPolicy(HWData())
        grouping.groups([instance1, instance2])
        assert grouping.localhost_instance.port == "8080"


def test_localfirst_single_port():
    with patch.object(HWData, '_init_hwdata') as hwdata_mock, pytest.raises(ValueError):
        hwdata_mock.return_value = {host_id("somehost", ".yandex.ru"): {"dc": "man", "cpu": "dummy_value"},
                                    host_id("anotherhost", ".yandex.ru"): {"dc": "sas", "cpu": "dummy_value"}, }
        instance1 = Instance("somehost.yandex.ru", "8080")
        instance2 = Instance("anotherhost.yandex.ru", "8090")

        grouping = LocalHostFirstLocationGrouppingPolicy(HWData())
        grouping.groups([instance1, instance2])
