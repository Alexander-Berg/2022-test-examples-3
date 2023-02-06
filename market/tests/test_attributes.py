from __future__ import unicode_literals, absolute_import

import copy

import pytest
from market.sre.tools.rtc.nanny.models.attributes.abstract import (
    NannyObject,
    Attributes,
)


class TestNannyObject:
    """
    Test methods of NannyObject
    """

    def test_content(self):
        """
        Test property content returns a different object.
        """
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        no = NannyObject(content)
        # Must be different objects
        assert no.content is not content

    def test_init_content_object_is_equal(self):
        """
        Test content is the same object.
        """
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        no = NannyObject(content)
        content["c"] = 1
        # Must be the same objects
        assert no._content is content

    def test_init_content_object_is_not_equal(self):
        """
        Test content is different object.
        """
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        no = NannyObject(copy.deepcopy(content))
        # Must be different objects
        assert no.content is not content

    def test_get_key(self):
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        no = NannyObject(content=content)

        assert no._get_key("b.ba") == content["b"]["ba"]
        with pytest.raises(AssertionError):
            no._get_key("")

    def test_set_key(self):
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        no = NannyObject(content=content)

        bb = [3, 4]
        no._set_key("b.bb", bb)
        assert no._get_key("b.bb") == bb

        a = 2
        no._set_key("a", a)
        assert no._get_key("a") == a

        with pytest.raises(AssertionError):
            no._set_key("", "value")

    def test_append_key_with_one_value(self):
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        no = NannyObject(content=content)

        no._append_key("b.bb", 3)
        assert no._get_key("b.bb") == [1, 2, 3]

    def test_append_key_with_list(self):
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        no = NannyObject(content=content)

        no._append_key("b.bb", [3, 4])
        assert no._get_key("b.bb") == [1, 2, 3, 4]

    def test_update_list_elements_match_one_element(self):
        dict_object = dict(b=2, c=3)
        list_object = [dict(a=1, b=2, c=3), dict(a=2, b=3), dict(a=3, c=4)]
        NannyObject._update_list_elements(
            dict_object=dict_object,
            list_object=list_object,
            fn_filter=lambda i: i["a"] == 3,
        )
        assert list_object[2] == dict(a=3, b=2, c=3)

    def test_update_list_elements_match_no_element(self):
        dict_object = dict(b=2, c=3)
        list_object = [dict(a=1, b=2, c=3), dict(a=2, b=3), dict(a=3, c=4)]
        expected_list_object = list(list_object)
        NannyObject._update_list_elements(
            dict_object=dict_object,
            list_object=list_object,
            fn_filter=lambda i: i["a"] == 5,
        )
        assert list_object == expected_list_object

    def test_update_list_elements_match_all_element(self):
        dict_object = dict(b=2, c=3)
        list_object = [dict(a=1, b=2, c=3), dict(a=2, b=3), dict(a=3, c=4)]
        NannyObject._update_list_elements(
            dict_object=dict_object, list_object=list_object, fn_filter=lambda i: True
        )
        assert all(
            i["b"] == dict_object["b"] and i["c"] == dict_object["c"]
            for i in list_object
        )

    def test_update_list_elements_not_dict_elements(self):
        list_object = [1, 2, 3]
        NannyObject._update_list_elements(
            dict_object={}, list_object=list_object, fn_filter=lambda i: True
        )
        expected_list_object = list(list_object)
        assert list_object == expected_list_object


class TestAttributes:
    def test_from_dict(self):
        content = dict(k1="k1", k2=2)
        attrs = Attributes.from_dict(dict(_id="123", content=content))
        assert attrs.content == content
        # Must be different objects
        assert attrs.content is not content

    def test_to_dict(self):
        content = dict(k1="k1", k2=2)
        attrs = Attributes(snapshot_id="123", content=content)
        assert attrs.content == content
        # Must be different objects
        assert attrs.content is not content
        assert attrs.to_dict() == dict(snapshot_id="123", content=content)
        assert attrs.to_dict()["content"] is not content

    def test_diff_with_changes(self):
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        attrs = Attributes(content=content)
        attrs._set_key("a", 2)
        assert attrs.diff != {}

    def test_diff_wo_changes(self):
        content = {"a": 1, "b": {"ba": 1, "bb": [1, 2]}}
        attrs = Attributes(content=content)
        assert attrs.diff == {}


class TestInfoAttrs:

    def test_info_attrs(self, info_attrs, info_attrs_dict):
        """
        :type info_attrs: market.sre.tools.rtc.nanny.models.InfoAttrs
        :type info_attrs_dict: dict
        """
        assert info_attrs.category == info_attrs_dict["content"]["category"]
        info_attrs.category = "/test/category"
        assert info_attrs.category != info_attrs_dict["content"]["category"]

        assert info_attrs.desc == info_attrs_dict["content"]["desc"]
        info_attrs.desc = "test desc"
        assert info_attrs.desc != info_attrs_dict["content"]["desc"]

        assert info_attrs.queue_id == info_attrs_dict["content"]["queue_id"]
        info_attrs.queue_id = "TEST"
        assert info_attrs.queue_id != info_attrs_dict["content"]["queue_id"]

        assert (
            info_attrs.tickets_integration.content
            == info_attrs_dict["content"]["tickets_integration"]
        )

        assert info_attrs.abc_group == info_attrs_dict["content"]["abc_group"]
        info_attrs.abc_group = 1
        assert info_attrs.abc_group != info_attrs_dict["content"]["abc_group"]

    def test_ticket_integration(self, info_attrs, info_attrs_dict):
        """
        :type auth_attrs: market.sre.tools.rtc.nanny.models.InfoAttrs
        :type info_attrs_dict: dict
        """
        assert (
            info_attrs.tickets_integration.service_release_rules
            == info_attrs_dict["content"]["tickets_integration"][
                "service_release_rules"
            ]
        )
        info_attrs.tickets_integration.service_release_rules = {"test": "test"}
        assert (
            info_attrs.tickets_integration.service_release_rules
            != info_attrs_dict["content"]["tickets_integration"][
                "service_release_rules"
            ]
        )

    def test_monitoring_settings(self, info_attrs, info_attrs_dict):
        """
        :type auth_attrs: market.sre.tools.rtc.nanny.models.InfoAttrs
        :type info_attrs_dict: dict
        """
        assert (
            info_attrs.monitoring_settings.juggler_settings.content["instance_resolve_type"]
            == info_attrs_dict["content"]["monitoring_settings"]["juggler_settings"]["content"]["instance_resolve_type"]
        )
        assert (
            info_attrs.monitoring_settings.juggler_settings.is_enabled
            == info_attrs_dict["content"]["monitoring_settings"]["juggler_settings"]["is_enabled"]
        )

    def test_add_juggler_settings_check(self, info_attrs, info_attrs_dict):
        """
        :type auth_attrs: market.sre.tools.rtc.nanny.models.InfoAttrs
        :type info_attrs_dict: dict
        """
        pass


class TestRuntimeAttrs:
    def test_info_attrs(self, runtime_attrs, runtime_attrs_dict):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        :type runtime_attrs_dict: dict
        """
        assert (
            runtime_attrs.instance_spec.content
            == runtime_attrs_dict["content"]["instance_spec"]
        )
        assert (
            runtime_attrs.instances.content
            == runtime_attrs_dict["content"]["instances"]
        )
        assert runtime_attrs.engines.content == runtime_attrs_dict["content"]["engines"]
        assert (
            runtime_attrs.resources.content
            == runtime_attrs_dict["content"]["resources"]
        )

    def test_instance_spec(self, runtime_attrs, runtime_attrs_dict):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        :type runtime_attrs_dict: dict
        """
        assert (
            runtime_attrs.instance_spec.instancectl
            == runtime_attrs_dict["content"]["instance_spec"]["instancectl"]
        )
        runtime_attrs.instance_spec.instancectl = {"test_instancectl": "test"}
        assert (
            runtime_attrs.instance_spec.instancectl
            != runtime_attrs_dict["content"]["instance_spec"]["instancectl"]
        )

        assert (
            runtime_attrs.instance_spec.aux_daemons
            == runtime_attrs_dict["content"]["instance_spec"]["auxDaemons"]
        )
        runtime_attrs.instance_spec.aux_daemons = {"test_aux_daemons"}
        assert (
            runtime_attrs.instance_spec.aux_daemons
            != runtime_attrs_dict["content"]["instance_spec"]["auxDaemons"]
        )

        assert (
            runtime_attrs.instance_spec.containers
            == runtime_attrs_dict["content"]["instance_spec"]["containers"]
        )
        runtime_attrs.instance_spec.containers = [1, 2, 3]
        assert (
            runtime_attrs.instance_spec.containers
            != runtime_attrs_dict["content"]["instance_spec"]["containers"]
        )

        assert (
            runtime_attrs.instance_spec.init_containers
            == runtime_attrs_dict["content"]["instance_spec"]["initContainers"]
        )
        runtime_attrs.instance_spec.init_containers = [4, 5, 6]
        assert (
            runtime_attrs.instance_spec.init_containers
            != runtime_attrs_dict["content"]["instance_spec"]["initContainers"]
        )

        assert (
            runtime_attrs.instance_spec.notify_action
            == runtime_attrs_dict["content"]["instance_spec"]["notifyAction"]
        )
        runtime_attrs.instance_spec.notify_action = {"test_notify_action": "test"}
        assert (
            runtime_attrs.instance_spec.notify_action
            != runtime_attrs_dict["content"]["instance_spec"]["notifyAction"]
        )

        assert (
            runtime_attrs.instance_spec.volume
            == runtime_attrs_dict["content"]["instance_spec"]["volume"]
        )
        runtime_attrs.instance_spec.volume = [7, 8, 9]
        assert (
            runtime_attrs.instance_spec.volume
            != runtime_attrs_dict["content"]["instance_spec"]["volume"]
        )

        assert (
            runtime_attrs.instance_spec.layers_config.content
            == runtime_attrs_dict["content"]["instance_spec"]["layersConfig"]
        )

    def test_instance_spec_layers_config(self, runtime_attrs, runtime_attrs_dict):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        :type runtime_attrs_dict: dict
        """
        assert (
            runtime_attrs.instance_spec.layers_config.bind
            == runtime_attrs_dict["content"]["instance_spec"]["layersConfig"]["bind"]
        )
        runtime_attrs.instance_spec.layers_config.bind = [1, 2, 3]
        assert (
            runtime_attrs.instance_spec.layers_config.bind
            != runtime_attrs_dict["content"]["instance_spec"]["layersConfig"]["bind"]
        )

        assert (
            runtime_attrs.instance_spec.layers_config.layer
            == runtime_attrs_dict["content"]["instance_spec"]["layersConfig"]["layer"]
        )
        runtime_attrs.instance_spec.layers_config.layer = [4, 5, 6]
        assert (
            runtime_attrs.instance_spec.layers_config.layer
            != runtime_attrs_dict["content"]["instance_spec"]["layersConfig"]["layer"]
        )

    def test_instances(self, runtime_attrs, runtime_attrs_dict):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        :type runtime_attrs_dict: dict
        """
        assert (
            runtime_attrs.instances.extended_gencfg_groups.content
            == runtime_attrs_dict["content"]["instances"]["extended_gencfg_groups"]
        )

    def test_instances_extended_gencfg_groups(self, runtime_attrs, runtime_attrs_dict):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        :type runtime_attrs_dict: dict
        """
        assert (
            runtime_attrs.instances.extended_gencfg_groups.groups
            == runtime_attrs_dict["content"]["instances"]["extended_gencfg_groups"][
                "groups"
            ]
        )
        runtime_attrs.instances.extended_gencfg_groups.groups = [1, 2, 3]
        assert (
            runtime_attrs.instances.extended_gencfg_groups.groups
            != runtime_attrs_dict["content"]["instances"]["extended_gencfg_groups"][
                "groups"
            ]
        )

    def test_instances_extended_gencfg_groups_use_mtn(self, runtime_attrs):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        """
        runtime_attrs.instances.extended_gencfg_groups.use_mtn = False
        assert (
            runtime_attrs.instances.extended_gencfg_groups._get_key(
                "network_settings.use_mtn"
            )
            is False
        )
        runtime_attrs.instances.extended_gencfg_groups.use_mtn = True
        assert (
            runtime_attrs.instances.extended_gencfg_groups._get_key(
                "network_settings.use_mtn"
            )
            is True
        )

    def test_instances_extended_gencfg_groups_change_gencfg_group(self, runtime_attrs):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        """
        runtime_attrs.instances.extended_gencfg_groups.change_gencfg_group("TEST_GROUP")
        assert (
            runtime_attrs.instances.extended_gencfg_groups.groups[0]["name"]
            == "TEST_GROUP"
        )

        with pytest.raises(AssertionError):
            runtime_attrs.instances.extended_gencfg_groups.change_gencfg_group("")

        runtime_attrs.instances.extended_gencfg_groups.groups = [1, 2, 3]
        with pytest.raises(AssertionError):
            runtime_attrs.instances.extended_gencfg_groups.change_gencfg_group(
                "TEST_GROUP"
            )

    def test_instances_extended_gencfg_groups_change_gencfg_release(
        self, runtime_attrs
    ):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        """
        runtime_attrs.instances.extended_gencfg_groups.change_gencfg_release("trunk")
        assert (
            runtime_attrs.instances.extended_gencfg_groups.groups[0]["release"]
            == "trunk"
        )

        with pytest.raises(AssertionError):
            runtime_attrs.instances.extended_gencfg_groups.change_gencfg_release("")

    def test_engines(self, runtime_attrs, runtime_attrs_dict):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        :type runtime_attrs_dict: dict
        """
        assert (
            runtime_attrs.engines.engine_type
            == runtime_attrs_dict["content"]["engines"]["engine_type"]
        )
        runtime_attrs.engines.engine_type = "ISS_TEST"
        assert (
            runtime_attrs.engines.engine_type
            != runtime_attrs_dict["content"]["engines"]["engine_type"]
        )

    def test_resources(self, runtime_attrs, runtime_attrs_dict):
        """
        :type runtime_attrs: market.sre.tools.rtc.nanny.models.RuntimeAttrs
        :type runtime_attrs_dict: dict
        """
        assert (
            runtime_attrs.resources.sandbox_files
            == runtime_attrs_dict["content"]["resources"]["sandbox_files"]
        )
        runtime_attrs.resources.sandbox_files = [1, 2, 3]
        assert (
            runtime_attrs.resources.sandbox_files
            != runtime_attrs_dict["content"]["resources"]["sandbox_files"]
        )

        assert (
            runtime_attrs.resources.static_files
            == runtime_attrs_dict["content"]["resources"]["static_files"]
        )
        runtime_attrs.resources.static_files = [4, 5, 6]
        assert (
            runtime_attrs.resources.static_files
            != runtime_attrs_dict["content"]["resources"]["static_files"]
        )


class TestAuthAttrs:

    def test_info_attrs(self, auth_attrs, auth_attrs_dict):
        """
        :type auth_attrs: market.sre.tools.rtc.nanny.models.AuthAttrs
        :type auth_attrs_dict: dict
        """
        assert auth_attrs.owners.content == auth_attrs_dict["content"]["owners"]

    def test_owners(self, auth_attrs, auth_attrs_dict):
        """
        :type auth_attrs: market.sre.tools.rtc.nanny.models.AuthAttrs
        :type auth_attrs_dict: dict
        """
        assert (
            auth_attrs.owners.logins == auth_attrs_dict["content"]["owners"]["logins"]
        )
        auth_attrs.owners.logins = ["test_login"]
        assert (
            auth_attrs.owners.logins != auth_attrs_dict["content"]["owners"]["logins"]
        )

        assert (
            auth_attrs.owners.groups == auth_attrs_dict["content"]["owners"]["groups"]
        )
        auth_attrs.owners.groups = ["test_group"]
        assert (
            auth_attrs.owners.groups != auth_attrs_dict["content"]["owners"]["groups"]
        )

    def test_append_owners(self, auth_attrs):
        """
        :type auth_attrs: market.sre.tools.rtc.nanny.models.AuthAttrs
        """
        login = "test_login"
        group = "test_group"
        logins = copy.deepcopy(auth_attrs.owners.logins)
        logins.append(login)
        groups = copy.deepcopy(auth_attrs.owners.groups)
        groups.append(group)
        auth_attrs.owners.append(login, group)
        assert auth_attrs.owners.logins == logins
        assert auth_attrs.owners.groups == groups


class TestCurrentState:

    def test_current_state(self, current_state, current_state_dict):
        assert current_state.summary.content['value'] == current_state_dict['content']['summary']['value']
        assert current_state.summary.entered == current_state_dict['content']['summary']['entered']
