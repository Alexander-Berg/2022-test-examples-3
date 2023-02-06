# -*- coding: utf-8 -*-
from attrdict import AttrDict


default_user = AttrDict({'login': 'mpfs-test',
                         'uid': '128280859',
                         'email': 'mpfs-test@yandex.ru',
                         'suid': '367560950',
                         'display_name': 'Mpfs Test'})

# common users
usr = AttrDict({
    'uid': '415264318',
    'email': 'mpfs.tester@yandex.ru',
    'login': 'mpfs.tester',
    'universe_login': 'mpfs.tester',
    'universe_service': 'email',
    'first_name': u'Тест',
    'last_name': u'Тестович',
    'password': 'mpfsTest',
    'secret': u'Русинов',
})
usr_1 = AttrDict({
    'uid': '415264988',
    'email': 'mpfs.tester.1@yandex.ru',
    'login': 'mpfs.tester.1',
    'universe_login': 'mpfs.tester.1',
    'universe_service': 'email',
    'first_name': u'Тест',
    'last_name': u'Тестович',
    'password': 'mpfsTest',
    'secret': u'Русинов',
})
usr_2 = AttrDict({
    'uid': '415265582',
    'email': 'mpfs.tester.2@yandex.ru',
    'login': 'mpfs.tester.2',
    'universe_login': 'mpfs.tester.2',
    'universe_service': 'email',
    'first_name': u'Тест',
    'last_name': u'Тестович',
    'password': 'mpfsTest',
    'secret': u'Русинов',
})
usr_3 = AttrDict({
    'uid': '415265999',
    'email': 'mpfs.tester.3@yandex.ru',
    'login': 'mpfs.tester.3',
    'universe_login': 'mpfs.tester.3',
    'universe_service': 'email',
    'first_name': u'Тест',
    'last_name': u'Тестович',
    'password': 'mpfsTest',
    'secret': u'Русинов',
})
usr_4 = AttrDict({
    'uid': '415266357',
    'email': 'mpfs.tester.4@yandex.ru',
    'login': 'mpfs.tester.4',
    'universe_login': 'mpfs.tester.4',
    'universe_service': 'email',
    'first_name': u'Тест',
    'last_name': u'Тестович',
    'password': 'mpfsTest',
    'secret': u'Русинов',
})
usr_5 = AttrDict({
    'uid': '415266694',
    'email': 'mpfs.tester.5@yandex.ru',
    'login': 'mpfs.tester.5',
    'universe_login': 'mpfs.tester.5',
    'universe_service': 'email',
    'first_name': u'Тест',
    'last_name': u'Тестович',
    'password': 'mpfsTest',
    'secret': u'Русинов',
})

user_1 = AttrDict({'uid': '166843460',
                   'email': 'mpfs-test-1@yandex.ru',
                   'login': 'mpfs-test-1'})
user_3 = AttrDict({'uid': '166843649',
                   'email': 'mpfs-test-3@yandex.ru'})

# Данный пользователь используется для теста, где проверяются результаты индексации диска. Поэтому
# не следует использовать этого пользователя в тестах, которые могут изменить данные индексации.
user_4 = AttrDict({
        "login": "mpfs-test-4",
        "uid": "376338392",
        "email": "mpfs-test-4@yandex.ru"
})

# password=mpfsTest / secret: Русинов
user_6 = AttrDict({'login': 'mpfs-test-6',
                   'uid': '410033615',
                   'email': 'mpfs-test-6@yandex.ru'})

# password=mpfsTest / secret: Русинов
user_7 = AttrDict({'login': 'mpfs-test-7',
                   'uid': '410034243',
                   'email': 'mpfs-test-7@yandex.ru'})

user_9 = AttrDict({'login': 'ya.t1p',
                   'uid': '397139837',
                   'email': 'ya.t1p@yandex.ru',
                   'firstname': 'Vasily',
                   'lastname': 'Pupkin'})

test_user = AttrDict({'uid': '3000257047',
                      'suid': '1120000000022787'})

# 89031628 - пользователь mikhail.v.belov
# дата регистрации Thu, 15 Apr 2010 20:52:06 GMT
old_time_registered_user = AttrDict({'uid': '89031628'})

turkish_user = AttrDict({'uid': '132494351'})

pdd_user = AttrDict({'uid': '1130000011801531',
                     'email': 'robbitter-3368777510@mellior.ru'})

pdd_user_1 = AttrDict({'uid': '1130000000156353',
                       'email': '32max@pochta.tvoe.tv'})

pdd_user_2 = AttrDict({'uid': '1130000000156355',
                       'email': 'amba@pochta.tvoe.tv'})

pdd_user_3 = AttrDict({'uid': '1130000000156481',
                       'email': 'sokolik@pochta.tvoe.tv'})

user_with_plus = AttrDict({'login': 'ya.plus',
                           'uid': '112358',
                           'email': 'ya.plus@yandex.ru',
                           'firstname': 'Fake',
                           'lastname': 'Plus'})

email_cyrillic = 'мпфс-тест@письмо.рф' #password=мпфс
email_cyrillic_dots = 'мпфс..тест@письмо.рф' #password=мпфс
email_dots = 'mpfs..test@pismorf.com' #password=мпфс
uid_a = '112'
uid_b = '222'
uid_c = '334'

# Пользователи для тестовых ручек общих папок
share_master = AttrDict({
    'uid': '4009138240',
    'login': 'yndx-share-master',
    'email': 'yndx-share-master@yandex.ru',
    'universe_login': 'yndx-share-master',
    'universe_service': 'email',
    'display_name': 'Share Master',
    'password': 'share-master'
})
share_slave = AttrDict({
    'uid': '4009138248',
    'login': 'yndx-share-slave',
    'email': 'yndx-share-slave@yandex.ru',
    'universe_login': 'yndx-share-slave',
    'universe_service': 'email',
    'display_name': 'Share Slave',
    'password': 'share-slave'
})
