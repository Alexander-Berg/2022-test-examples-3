# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.tester.helpers.class_counter import inc_class_counter


def test_class_counter_default_params():
    class SomeObjectWithId(object):
        def __init__(self):
            self.id = inc_class_counter(SomeObjectWithId, 'id')
    obj1 = SomeObjectWithId()
    obj2 = SomeObjectWithId()
    obj3 = SomeObjectWithId()
    assert obj1.id < obj2.id
    assert obj2.id < obj3.id


def test_class_counter_with_params():
    class OtherObjectWithId(object):
        def __init__(self):
            self.id = inc_class_counter(OtherObjectWithId, 'id', start_value=1000, increment=3)
    obj1 = OtherObjectWithId()
    obj2 = OtherObjectWithId()
    obj3 = OtherObjectWithId()
    assert obj1.id == 1000
    assert (obj2.id - obj1.id) == 3
    assert (obj3.id - obj2.id) == 3
