# -*- coding: utf-8 -*-

from operator import attrgetter

from conftest import INIT_USER_IN_POSTGRES
from mpfs.core.address import Address, ResourceId, GroupAddress


def assert_attribute_equals(left, right, *attributes):
    attr_name = '.'.join(attributes)
    get_attribute = attrgetter(attr_name)
    assert get_attribute(left) == get_attribute(right), \
        "left.%s != right.%s" % (attr_name, attr_name)


def assert_meta_equals(left_meta, right_meta):
    unique_keys = ('digest_url', 'file_url', 'folder_url')  # которые от рандома зависят
    for key in left_meta:
        if key not in unique_keys:
            assert left_meta[key] == right_meta[key], \
                "`left.meta['%s']` != right.meta['%s']`" % (key, key)


def iterate_resource_attributes(resource):
    # объекты, для которых сравнение не определено
    non_comparable = ('children_items', 'child_files', 'children',
                      'child_folders', 'full_index_map', 'listing',
                      '_Resource__service', '_form', '_history')
    for attr_name in dir(resource):
        if not attr_name.startswith('__') and attr_name not in non_comparable:
            yield attr_name, getattr(resource, attr_name)


def assert_resource_equals(left, right, skip_attributes=()):
    """Проверить равенство ресурсов.

    Т.к. не определены компараторы для ресурсов и их атрибутов, то
    делается навивная попытка сравнить два ресурса по значением их атрибутов тех типов,
    для которых определен компаратор.

    :raises AssertionError: Если ресурсы не равны.

    :type left: :class:`Resource`
    :type right: class:`Resource`
    :type skip_attributes: tuple
    :param skip_attributes: Атрибуты, которые не равны по известным причинам и починить это трудно.
    """
    builtin_types = (basestring, long, bool, float,
                     int, set, dict, list, tuple, type(None),
                     property)

    assert type(left) is type(right), \
        "`%s` is not `%s`" % (type(left), type(right))

    left_attribute_set = set(dict(iterate_resource_attributes(left)).viewkeys())
    right_attribute_set = set(dict(iterate_resource_attributes(right)).viewkeys())

    if INIT_USER_IN_POSTGRES and Address.Make(right.uid, right.path).is_storage and 'file_id' in right_attribute_set:
        right_attribute_set.remove('file_id')
        right.meta['file_id'] = right.file_id
        del right.file_id

    assert not left_attribute_set ^ right_attribute_set, \
        "Attributes sym. diff: %s" % (left_attribute_set ^ right_attribute_set)

    for attr_name, attr_value in iterate_resource_attributes(right):
        if attr_name in skip_attributes:
            continue
        elif attr_name == 'meta':
            assert_meta_equals(left.meta, right.meta)
        elif isinstance(attr_value, builtin_types):
            assert_attribute_equals(left, right, attr_name)
        elif isinstance(attr_value, GroupAddress):
            assert_attribute_equals(left, right, attr_name, 'id')
            assert_attribute_equals(left, right, attr_name, 'visible_id')
        elif isinstance(attr_value, Address):
            assert_attribute_equals(left, right, attr_name, 'id')
        elif isinstance(attr_value, ResourceId):
            assert_attribute_equals(left, right, attr_name, 'uid')
            assert_attribute_equals(left, right, attr_name, 'file_id')
