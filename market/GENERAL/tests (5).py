#!/usr/bin/python
# -*- coding: utf8 -*-

import unittest
import datetime
import parse


class T(unittest.TestCase):

    def _check_dict(self, must_have, d):
        for k, v in must_have.iteritems():
            self.assertEqual(v, d[k])

    def test_calendar_text(self):
        text = u'''Евгения Мордашова
        2.Карты (КР 2-2)
        четверг, 21 января, 15:00—16:00
        https://staff.yandex-team.ru/recruitment/319977

        https://femida.yandex-team.ru/interviews/6612
        Организатор:ОрганизаторВладислав Шаргин (принято)
        Участники:УчастникиАндрей Мещеряков (под вопросом), Юрий Манушкин, Сергей Перминов (принято)
        Просмотреть Принять Под вопросом  Отказаться от встречи'''
        d = parse.parse_sections_raw_info(text)[0]
        info = {'url': u'https://staff.yandex-team.ru/recruitment/319977', 'type': 'ftf', 'femida_url': u'https://femida.yandex-team.ru/interviews/6612', 'name': u'Евгения Мордашова', 'time': datetime.datetime(2016, 1, 21, 15, 0)}
        self._check_dict(info, d)

    def test_parse_text_from_email(self):
        text = u'''«Алгоритмы (Любимцева Мария)»
        В четверг, 28-го января с 13:00 до 14:00 (Europe/Moscow, GMT+03:00)
        ПойдуВозможно, пойдуНе пойду
        Где1. Красная Шапочка (КР 1-25, 2128)
        Описаниеhttps://femida.yandex-team.ru/interviews/6645

        https://staff.yandex-team.ru/recruitment/326319
        ОрганизаторВалентина Новокрещенова
        ПриглашеныЮлия Шурдук
        Андрей Мещеряков
        Посмотреть, кто пойдёт'''
        d = parse.parse_sections_raw_info(text)[0]
        info = {'url': u'https://staff.yandex-team.ru/recruitment/326319', 'type': 'ftf', 'femida_url': u'https://femida.yandex-team.ru/interviews/6645', 'name': u'«Алгоритмы (Любимцева Мария)»', 'time': datetime.datetime(2016, 1, 28, 13, 0)}
        self._check_dict(info, d)

    def test_parse_time_in_email_format(self):
        text = u'''Денис Дятлов
            Во вторник, 31-го мая с 11:00 до 12:00 (Europe/Moscow, GMT+03:00)
            Во вторник, 31-го мая с 12:00 до 12:00 (Europe/Moscow, GMT+03:00)
            Во вторник, 31-го мая с 13:00 до 12:00 (Europe/Moscow, GMT+03:00)
            Пойду	Возможно, пойду	Не пойду
            Где		3.Поросенка (КР 3-1)
            Описание		https://femida.yandex-team.ru/candidates/87939073
            Организатор		Людмила Ширяева
            Приглашены		Владислав Шаргин
            Кирилл Горелов
            Юрий Манушкин
            Андрей Мещеряков
            Посмотреть, кто пойдёт'''
        for d, tm in zip(
            parse.parse_sections_raw_info(text),
            [
                datetime.datetime(2016, 5, 31, 11, 00),
                datetime.datetime(2016, 5, 31, 12, 00),
                datetime.datetime(2016, 5, 31, 13, 00)
            ]):
            info = {
                'url': None,
                'type': 'ftf',
                'femida_url': u'https://femida.yandex-team.ru/candidates/87939073',
                'name': u'Денис Дятлов',
                'time': tm
            }
            self._check_dict(info, d)


if __name__ == '__main__':
    from parse import TestParsingFromRaw
    unittest.main()
