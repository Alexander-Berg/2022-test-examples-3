# coding: utf-8
from market.pylibrary.config_parser_utils.lib.config_parser_utils import get_config_flag, get_config_value
from six.moves.configparser import ConfigParser


def test__get_config_flag__section_not_exist__false_returned():
    config = ConfigParser()
    assert not get_config_flag(config, "non_existing_section", "non_exiting_option")


def test__get_config_flag__section_not_exist_default_is_true__true_returned():
    config = ConfigParser()
    assert get_config_flag(config, "non_existing_section", "non_exiting_option", True)


def test__get_config_flag__option_not_exist__false_returned():
    section_name = "test_section"
    config = ConfigParser()
    config.add_section(section_name)
    assert not get_config_flag(config, section_name, "non_exiting_option")


def test__get_config_flag__option_not_exist_default_is_true__true_returned():
    section_name = "test_section"
    config = ConfigParser()
    config.add_section(section_name)
    assert get_config_flag(config, section_name, "non_exiting_option", True)


def test__get_config_flag__option_exist__option_returned():
    section_name = "test_section"
    option_name = "test_option"
    option_value = "true"

    config = ConfigParser()
    config.add_section(section_name)
    config.set(section_name, option_name, option_value)
    assert get_config_flag(config, section_name, option_name)


def test__get_config_value__section_not_exist__none_returned():
    config = ConfigParser()
    assert get_config_value(config, "non_existing_section", "non_exiting_option") is None


def test__get_config_value__section_not_exist_default_is_set__default_returned():
    config = ConfigParser()
    custom_default = "some_string"
    assert get_config_value(config, "non_existing_section", "non_exiting_option", custom_default) is custom_default


def test__get_config_value__option_not_exist__none_returned():
    section_name = "test_section"
    config = ConfigParser()
    config.add_section(section_name)
    assert get_config_value(config, section_name, "non_exiting_option") is None


def test__get_config_value__option_not_exist_default_is_set__default_returned():
    section_name = "test_section"
    custom_default = "some_string"
    config = ConfigParser()
    config.add_section(section_name)
    assert get_config_value(config, section_name, "non_exiting_option", custom_default) is custom_default


def test__get_config_value__option_exist__option_returned():
    section_name = "test_section"
    option_name = "test_option"
    option_value = "some_string"

    config = ConfigParser()
    config.add_section(section_name)
    config.set(section_name, option_name, option_value)
    assert get_config_value(config, section_name, option_name) is option_value
