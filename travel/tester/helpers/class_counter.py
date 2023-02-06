# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

"""Инструмент для создания классовых счетчиков."""


def inc_class_counter(cls, counter_name, start_value=0, increment=1):
    """Возвращает текущее значеие и инкрементирует счетчик.

    def gen_ticket_number()
        return inc_class_counter(TicketFactory, 'ticket_number', start_value=100000, increment=3)
    gen_ticket_number()
    => 100000
    gen_ticket_number()
    => 100003

    :param cls: класс, для которого используется счетчик
    :param counter_name: имя счетчика
    :param start_value: начальное значение счетчика
    :param increment: инкремент
    :return: возвращает текущее значение счетчика
    """
    field_name = '__counter_{}'.format(counter_name)
    val = getattr(cls, field_name, start_value)
    setattr(cls, field_name, val + increment)
    return val
