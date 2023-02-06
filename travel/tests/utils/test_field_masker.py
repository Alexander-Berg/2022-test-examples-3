# coding: utf-8
from __future__ import unicode_literals

from common.utils.field_masker import FieldMasker


def check_is_masked(data):
    if data is None:
        return True
    for char in data:
        if char not in '*.':
            return False
    return True


def test_unicode():
    masker = FieldMasker(mask_fields={'password': 1, 'name': 1})
    data = {'login': 'user007', 'password': 'secret321', 'name': 'Иван'}
    result = masker.apply(data)
    assert result == {'login': 'user007', 'password': '*********', 'name': '****'}


def test_missed_field():
    masker = FieldMasker(mask_fields={'password': 1, 'secret': 1})
    data = {'login': 'user007', 'password': 'secret321'}
    result = masker.apply(data)
    assert result == {'login': 'user007', 'password': '*********'}


def test_mask_nonstring_field():
    masker = FieldMasker(mask_fields={'secret_user_info': 1, 'browser_history': 1, 'age': 1, 'secret': 1})
    data = {'login': 'user007',
            'secret_user_info': {'address': 'www leningrad', 'phone': '88001111111'},
            'browser_history': ['ya.ru', 'rasp.ya.ru'],
            'age': 42,
            'secret': None
            }
    result = masker.apply(data)
    assert result['login'] == 'user007'
    assert check_is_masked(result['secret_user_info'])
    assert check_is_masked(result['browser_history'])
    assert check_is_masked(result['age'])
    assert check_is_masked(result['secret'])


def test_custom_params():
    masker = FieldMasker(mask_fields={'password': 1, 'secret': 1}, mask_char='?', max_mask_length=2)
    data = {'login': 'user007', 'password': 'secret321', 'secret': 11}
    result = masker.apply(data)
    assert result == {'login': 'user007', 'password': '???..', 'secret': '??'}


def test_deep_field():
    masker = FieldMasker(mask_fields={'data': {'user_info': {'password': 1, 'secret': 1}}})
    data = {'data': {'user_info': {'login': 'user007', 'password': 'secret321'}}}
    result = masker.apply(data)
    assert result == {'data': {'user_info': {'login': 'user007', 'password': '*********'}}}


def test_list_item_field():
    masker = FieldMasker(mask_fields={'data': {'users_info': [{'password': 1, 'secret': 1}]}})
    data = {'data': {'users_info': [
        {'login': 'user007', 'password': 'secret321'},
        {'login': 'user008', 'password': 'secret123'},
    ]}}
    result = masker.apply(data)
    assert result == {'data': {'users_info': [
        {'login': 'user007', 'password': '*********'},
        {'login': 'user008', 'password': '*********'},
    ]}}
