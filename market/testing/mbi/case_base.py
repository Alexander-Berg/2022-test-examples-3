#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import case_log
import mbi_common


class MbiTestCase(unittest.TestCase):
    # Пароль на пользователей: Alstom_2013
    # Если пользователь забился магазинами, то надо его закоментить и раскоментить следующего

    # Дохляки:
    # USER = mbi_common.User(id=505977008, email='ikhudyshev38@yandex.ru')  # этот засорился
    # USER = mbi_common.User(id=511648751, email='ikhudyshev56@yandex.ru')
    # USER = mbi_common.User(id=513905494, email='ikhudyshev551@yandex.ru')  # password: Alstom_2013
    # USER = mbi_common.User(id=513905631, email='ikhudyshev552@yandex.ru')  # password: Alstom_2013
    # USER = mbi_common.User(id=513905781, email='ikhudyshev553@yandex.ru')  # password: Alstom_2013
    # USER = mbi_common.User(id=515930759, email='ikhudyshev571@yandex.ru')  # password: Alstom_2013

    # текущий:
    USER = mbi_common.User(id=513906051, email='ikhudyshev554@yandex.ru')  # password: Alstom_2013

    # Запас:
    #USER = mbi_common.User(id=513906179, email='ikhudyshev555@yandex.ru')  # password: Alstom_2013



    def setUp(self):
        message = '{id} / {descr}'.format(
            id=self.id(),
            descr=self.shortDescription())
        case_log.CaseLogger.get().log(message)


def main():
    pass


if __name__ == '__main__':
    if os.environ.get('UNITTEST') == '1':
        unittest.main()
    else:
        main()
