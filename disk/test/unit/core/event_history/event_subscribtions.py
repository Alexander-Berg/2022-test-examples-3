# -*- coding: utf-8 -*-

from mpfs.core.event_history.event_subscribtions import should_drop_event_message_based_on_data


def test_should_drop_event_message_based_on_data_by_tgt_raw_address_notes():
    """Проверить что если в сообщении `tgt_rawaddress` относится к разделу /notes,
    то оно дропается."""
    assert should_drop_event_message_based_on_data({'tgt_rawaddress': '123456789:/notes/path/to/folder_or_file'})


def test_should_drop_event_message_based_on_data_by_src_raw_address_notes():
    """Проверить что если в сообщении `src_rawaddress` относится к разделу /notes,
    то оно дропается."""
    assert should_drop_event_message_based_on_data({'src_rawaddress': '123456789:/notes/path/to/folder_or_file'})


def test_should_drop_event_message_based_on_data_by_tgt_raw_address_attach_ya_fotki():
    """Проверить что если в сообщении `tgt_rawaddress` относится к разделу /attach/YaFotki,
    то оно дропается."""
    assert should_drop_event_message_based_on_data({'tgt_rawaddress': '123456789:/attach/YaFotki/path/to/folder_or_file'})
    assert not should_drop_event_message_based_on_data({'tgt_rawaddress': '123456789:/attach/path/to/folder_or_file'})


def test_should_drop_event_message_based_on_data_by_src_raw_address_attach_ya_fotki():
    """Проверить что если в сообщении `src_rawaddress` относится к разделу /attach/YaFotki,
    то оно дропается."""
    assert should_drop_event_message_based_on_data({'src_rawaddress': '123456789:/attach/YaFotki/path/to/folder_or_file'})
    assert not should_drop_event_message_based_on_data({'src_rawaddress': '123456789:/attach/path/to/folder_or_file'})
