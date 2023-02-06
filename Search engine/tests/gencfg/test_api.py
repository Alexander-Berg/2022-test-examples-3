"""
    Testing gencfg api wrapper
"""

import pytest
import requests
import requests_mock

from search.mon.wabbajack.libs.modlib.api_wrappers import gencfg
from search.mon.wabbajack.libs.modlib.api_wrappers.gencfg import exceptions
from . import INSTANCES_JSON, CARD_JSON


class TestBaseGcfg:

    def setup_class(self):
        self.test_response = None
        with requests_mock.Mocker() as m:
            m.register_uri('GET', 'http://api.gencfg.yandex-team.ru', json={'result': 'OK'})
            self.test_response = gencfg.GencfgBase().req(url='')

    def test_req_return_type(self):
        assert isinstance(self.test_response, tuple)

    def test_req_return_len(self):
        assert len(self.test_response) == 2

    def test_req_return_vals(self):
        http_code, robject = self.test_response
        assert isinstance(http_code, int)
        assert isinstance(robject, requests.models.Response)

    def test_req_return_json(self):
        http_code, robject = self.test_response
        assert http_code == 200
        assert robject.json() == {'result': 'OK'}


class TestGencfgInstances:
    VALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/searcherlookup/groups/TICKENATOR/instances'
    INVALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/searcherlookup/groups/TICKENATOR_404/instances'

    def setup_class(self):
        self.test_instances = None
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.VALID_URL,
                           json=INSTANCES_JSON)
            self.test_instances = gencfg.GencfgInstances(group='TICKENATOR')

    def test_instances_property_group_avail(self):
        group = getattr(self.test_instances, 'group', None)
        assert group is not None
        assert group == 'TICKENATOR'

    def test_instances_property_group_ro(self):
        with pytest.raises(AttributeError):
            self.test_instances.group = 'OTHER_VALUE'

    def test_instances_property_tag_avail(self):
        tag = getattr(self.test_instances, 'tag', None)
        assert tag is not None
        assert tag == 'trunk'

    def test_instances_property_tag_ro(self):
        with pytest.raises(AttributeError):
            self.test_instances.tag = 'other_value'

    def test_instances_response(self):
        assert self.test_instances.instances == INSTANCES_JSON['instances']

    def test_instances_response_ro(self):
        with pytest.raises(AttributeError):
            self.test_instances.instances = {}

    def test_404_response(self):
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.INVALID_URL, status_code=404, json={})
            with pytest.raises(exceptions.EGencfgGroupNotFound):
                gencfg.GencfgInstances(group='TICKENATOR_404')


class TestGencfgCard:
    VALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/groups/TICKENATOR/card'
    INVALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/groups/TICKENATOR_404/card'

    def setup_class(self):
        self.test_card = None
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.VALID_URL, json=CARD_JSON)
            self.test_card = gencfg.GencfgGroupCard(group='TICKENATOR')

    def test_card_property_group_avail(self):
        group = getattr(self.test_card, 'group', None)
        assert group is not None
        assert group == 'TICKENATOR'

    def test_card_property_tag_avail(self):
        tag = getattr(self.test_card, 'tag', None)
        assert tag is not None
        assert tag == 'trunk'

    def test_card_property_group_ro(self):
        with pytest.raises(AttributeError):
            self.test_card.group = 'other_value'

    def test_card_property_tag_ro(self):
        with pytest.raises(AttributeError):
            self.test_card.tag = 'other_value'

    def test_card_response(self):
        assert self.test_card.card == CARD_JSON

    def test_card_reponce_ro(self):
        with pytest.raises(AttributeError):
            self.test_card.card = {}

    def test_404_response(self):
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.INVALID_URL, status_code=404, json={})
            with pytest.raises(exceptions.EGencfgGroupNotFound):
                gencfg.GencfgGroupCard(group='TICKENATOR_404')


class TestGencfgInstance:

    TEST_INSTANCE = INSTANCES_JSON['instances'][0]

    def setup_class(self):
        self.test_instance = gencfg.GencfgInstance(instances=self.TEST_INSTANCE, group='TICKENATOR', tag='trunk')

    def test_group_property_avail(self):
        g = getattr(self.test_instance, 'group', None)
        assert g is not None
        assert g == 'TICKENATOR'

    def test_tag_property_avail(self):
        t = getattr(self.test_instance, 'tag', None)
        assert t is not None
        assert t == 'trunk'

    def test_instance_dict_property_avail(self):
        inst_dict = getattr(self.test_instance, 'instance_dict', None)
        assert inst_dict is not None
        assert inst_dict == self.TEST_INSTANCE

    def test_instance_hbf_property_avail(self):
        hbf = getattr(self.test_instance, 'hbf', None)
        assert hbf is not None
        assert hbf == self.TEST_INSTANCE['hbf']

    def test_dc_property_avail(self):
        dc = getattr(self.test_instance, 'dc', None)
        assert dc is not None
        assert dc == self.TEST_INSTANCE['dc']

    def test_host_resources_property_avail(self):
        host_resources = getattr(self.test_instance, 'host_resources', None)
        assert host_resources is not None
        assert host_resources == self.TEST_INSTANCE['host_resources']

    def test_port_property_avail(self):
        port = getattr(self.test_instance, 'port', None)
        assert port is not None
        assert port == self.TEST_INSTANCE['port']

    def test_porto_limits_property_avail(self):
        porto_limits = getattr(self.test_instance, 'porto_limits', None)
        assert porto_limits is not None
        assert porto_limits == self.TEST_INSTANCE['porto_limits']

    def test_tags_property_avail(self):
        tags = getattr(self.test_instance, 'tags', None)
        assert tags is not None
        assert tags == self.TEST_INSTANCE['tags']

    def test_group_property_ro(self):
        with pytest.raises(AttributeError):
            self.test_instance.group = 'other_value'

    def test_tag_property_ro(self):
        with pytest.raises(AttributeError):
            self.test_instance.tag = 'other_value'


class TestGencfgGroup:
    INSTANCES_VALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/searcherlookup/groups/TICKENATOR/instances'
    INSTANCES_INVALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/searcherlookup/groups/TICKENATOR_404/instances'
    CARD_VALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/groups/TICKENATOR/card'
    CARD_INVALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/groups/TICKENATOR_404/card'
    GROUP_NAME = 'TICKENATOR'
    GROUP_TAG = 'trunk'
    INVALID_GROUP_NAME = 'TICKENATOR_404'

    def setup_class(self):
        self.test_group = gencfg.GencfgGroup(group=self.GROUP_NAME)

    def test_group_property_avail(self):
        g = getattr(self.test_group, 'group', None)
        assert g is not None
        assert g == self.GROUP_NAME

    def test_tag_property_avail(self):
        t = getattr(self.test_group, 'tag', None)
        assert t is not None
        assert t == 'trunk'

    def test_instances(self):
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.INSTANCES_VALID_URL, json=INSTANCES_JSON)
            assert self.test_group.instances == INSTANCES_JSON['instances']

    def test_instances_404(self):
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.INSTANCES_INVALID_URL, json={}, status_code=404)
            with pytest.raises(exceptions.EGencfgGroupNotFound):
                gencfg.GencfgGroup(group=self.INVALID_GROUP_NAME).instances

    def test_card(self):
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.CARD_VALID_URL, json=CARD_JSON)
            assert self.test_group.card == CARD_JSON

    def test_card_404(self):
        with requests_mock.Mocker() as m:
            m.register_uri('GET', self.CARD_INVALID_URL, json={}, status_code=404)
            with pytest.raises(exceptions.EGencfgGroupNotFound):
                gencfg.GencfgGroup(group=self.INVALID_GROUP_NAME).card

    def test_iter_instances(self):
        assert self.test_group.instances is not None
        for ins in self.test_group.iter_instances():
            assert isinstance(ins, gencfg.GencfgInstance)
