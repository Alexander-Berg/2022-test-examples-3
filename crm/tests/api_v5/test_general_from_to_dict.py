from dataclasses import dataclass, field
from enum import Enum
from typing import List
from crm.supskills.common.direct_client.structs.general import from_dict, to_dict


class ABCEnum(str, Enum):
    A = 'A'
    B = 'B'
    C = 'C'


@dataclass
class MyTestClassWithNone:
    arg_str: str = field(default=None)
    arg_int: int = field(default=None)
    arg_list: List[str] = field(default=None)
    arg_enum: ABCEnum = field(default=None)


def test_create_from_empty_dict():
    my_class = from_dict(MyTestClassWithNone, {})
    assert my_class == MyTestClassWithNone()


def test_create_from_dict_with_str():
    my_class = from_dict(MyTestClassWithNone, {'arg_str': 'some info'})
    assert my_class == MyTestClassWithNone(arg_str='some info')


def test_create_from_dict_with_int():
    my_class = from_dict(MyTestClassWithNone, {'arg_int': 42})
    assert my_class == MyTestClassWithNone(arg_int=42)


def test_create_from_dict_with_list():
    my_class = from_dict(MyTestClassWithNone, {'arg_list': ['A', 'B', 'C']})
    assert my_class == MyTestClassWithNone(arg_list=['A', 'B', 'C'])


def test_create_from_dict_with_enum():
    my_class = from_dict(MyTestClassWithNone, {'arg_enum': 'B'})
    assert my_class == MyTestClassWithNone(arg_enum=ABCEnum.B)


def test_create_from_dict_with_extra_info():
    my_class = from_dict(MyTestClassWithNone, {'arg_enum': 'B', 'did_you_now_that': 'lol = kek'})
    assert my_class == MyTestClassWithNone(arg_enum=ABCEnum.B)


def test_on_sync_to_from_dict_eq():
    assert to_dict(from_dict(MyTestClassWithNone, {'arg_enum': 'B'})) == {'arg_enum': 'B'}


def test_on_sync_to_from_dict_add():
    assert to_dict(from_dict(MyTestClassWithNone, {'arg_enum': 'B', 'did_you_now_that': 'lol = kek'})) == \
           {'arg_enum': 'B'}


def test_on_sync_to_from_dict_eq_none():
    assert to_dict(from_dict(MyTestClassWithNone, {'arg_enum': 'B', 'arg_str': None})) == {'arg_enum': 'B'}
