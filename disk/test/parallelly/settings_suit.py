#!/usr/bin/python
# -*- coding: utf-8 -*-
import mock

from hamcrest import assert_that, is_, equal_to

from test.base import DiskTestCase

from mpfs.core.user.base import User
from mpfs.core import base as core
from mpfs.core.user.settings import Settings, States, DEFAULT_NAMESPACE
from mpfs.core.user.devices import Devices
import mpfs.common.errors as errors


class TestSettings(DiskTestCase):

    def test_set_and_get_settings(self):
        '''
        Проставляем настройки и пытаемся их получить
        '''
        settings = Settings(self.uid, 'disk')
        settings.set('key1', 'value1')
        settings.set('key2.super|test', 'value2')
        settings.set(u'это юникодный ключ!', {'inner_key': 'inner_value'})
        self.assertEqual(settings.list().get('key1'), 'value1')
        self.assertEqual(settings.list().get('key2.super|test'), 'value2')
        self.assertEqual(settings.list().get(u'это юникодный ключ!'), {'inner_key': 'inner_value'})

    def test_set_settings_parallelly_insert_update(self):
        '''
        Settings хранит в себе кеширующий словарь, из-за этого может быть гонка.
        Cоздаеются 2 объекта, при создании version пуст.
        При сохранении второй объект не должен переписать первый.
        '''
        settings = Settings(self.uid, 'disk')
        settings2 = Settings(self.uid, 'disk')
        settings.set('key1', 'value')
        settings2.set('key2', 'value')
        settings_check = Settings(self.uid, 'disk')
        self.assertEqual(settings_check.list().get('key1'), 'value')
        self.assertEqual(settings_check.list().get('key2'), 'value')

    def test_set_settings_parallelly_update(self):
        '''
        Settings хранит в себе кеширующий словарь, из-за этого может быть гонка.
        В тесте заполняется первычной значение, потом создаеются 2 объекта,
        при создании version одинаковая. При сохранении второй объект не должен
        переписать первый.
        '''
        settings = Settings(self.uid, 'disk')
        settings.set('init_key', 'value')
        settings = Settings(self.uid, 'disk')
        settings2 = Settings(self.uid, 'disk')
        settings.set('key1', 'value')
        settings2.set('key2', 'value')
        settings_check = Settings(self.uid, 'disk')
        self.assertEqual(settings_check.list().get('key1'), 'value')
        self.assertEqual(settings_check.list().get('key2'), 'value')

    def test_set_settings_parallelly_update_removed(self):
        '''
        Обновление паралелльно удаленного ключа
        '''
        settings = Settings(self.uid, 'disk')
        settings.set('key1', 'value1')
        settings2 = Settings(self.uid, 'disk')
        settings.remove('key1')
        settings2.set('key1', 'value2')

        settings_check = Settings(self.uid, 'disk')
        self.assertEqual(settings_check.list().get('key1'), 'value2')

    def test_list_settings(self):
        '''
        Проставляем настройки и листаем их

        Получение первичного кол-ва нужно, чтобы обойти стороной автопроставление
        дефолтных флагов тестовому юзеру
        '''
        settings = Settings(self.uid, 'disk')
        initial_settings_len = len(settings.list())
        settings.set('key1', 'value1')
        settings.set('key2.super|test', 'value2')
        self.assertEqual(len(settings.list()), initial_settings_len + 2)

    def test_remove_setting(self):
        '''
        Проставляем настройку и удаляем ее

        Получение первичного кол-ва нужно, чтобы обойти стороной автопроставление
        дефолтных флагов тестовому юзеру
        '''
        settings = Settings(self.uid, 'disk')
        initial_settings_len = len(settings.list())
        settings.set('key1', 'value1')
        settings.set('key2.super|test', 'value2')
        self.assertEqual(len(settings.list()), initial_settings_len + 2)
        settings.remove('key1')
        self.assertEqual(len(settings.list()), initial_settings_len + 1)

    def test_remove_setting_parallelly(self):
        '''
        Проставляем настройку и удаляем ее

        Получение первичного кол-ва нужно, чтобы обойти стороной автопроставление
        дефолтных флагов тестовому юзеру
        '''
        settings = Settings(self.uid, 'disk')
        initial_settings_len = len(settings.list())
        settings2 = Settings(self.uid, 'disk')
        settings.set('key1', 'value')
        settings2.set('key2', 'value')
        # в data нет key2
        settings.remove('key2')
        settings_check = Settings(self.uid, 'disk')
        self.assertEqual(len(settings_check.list()), initial_settings_len+1)
        self.assertEqual(settings_check.list().get('key1'), 'value')

    def test_set_and_get_state(self):
        '''
        Проставляем состояния и пытаемся их получить
        '''
        states = States(self.uid, 'disk')
        states.set('key1', 'value1')
        states.set('key2.super|test', 'value2')
        states.set(u'это юникодный ключ!', {'inner_key': 'inner_value'})
        self.assertEqual(states.list().get('key1'), 'value1')
        self.assertEqual(states.list().get('key2.super|test'), 'value2')
        self.assertEqual(states.list().get(u'это юникодный ключ!'), {'inner_key': 'inner_value'})

    def test_list_states(self):
        '''
        Проставляем состояния и листаем их

        Получение первичного кол-ва нужно, чтобы обойти стороной автопроставление
        дефолтных флагов тестовому юзеру
        '''
        states = States(self.uid, 'disk')
        initial_states_len = len(states.list())
        states.set('key1', 'value1')
        states.set('key2.super|test', 'value2')
        print states.list()
        self.assertEqual(len(states.list()), initial_states_len + 2)

    def test_remove_state(self):
        '''
        Проставляем состояние и удаляем его

        Получение первичного кол-ва нужно, чтобы обойти стороной автопроставление
        дефолтных флагов тестовому юзеру
        '''
        states = States(self.uid, 'disk')
        initial_states_len = len(states.list())
        states.set('key1', 'value1')
        states.set('key2.super|test', 'value2')
        self.assertEqual(len(states.list()), initial_states_len + 2)
        states.remove('key1')
        self.assertEqual(len(states.list()), initial_states_len + 1)

    def test_install_devices(self):
        '''
        Инсталлируем устройства и смотрим, что все параметры на месте
        '''
        devices = Devices(self.uid, 'disk')
        devices.install('desktop', '123456', {'some key': 'some info'})
        devices.install('mobile', '443221', {'some key A': 'some info A'})
        devices.install('desktop', '666666', {'some key B': 'some info B'})

        device = devices.list('desktop').get('123456')
        for key, value in {'some key': 'some info'}.iteritems():
            self.assertEqual(device[key], value)

        device = devices.list('mobile').get('443221')
        for key, value in {'some key A': 'some info A'}.iteritems():
            self.assertEqual(device[key], value)

        device = devices.list('desktop').get('666666')
        for key, value in {'some key B': 'some info B'}.iteritems():
            self.assertEqual(device[key], value)

    def test_device_count(self):
        devices = Devices(self.uid, 'disk')
        devices.install('desktop', '1', {'some key': 'some info'})
        devices.install('mobile', '2', {'some key': 'some info'})
        devices.install('desktop', '3', {'some key': 'some info'})
        assert_that(devices._device_count(), is_(equal_to(3)))

    def test_list_devices(self):
        '''
        Устанавливаем устройства и листаем их все
        '''
        devices = Devices(self.uid, 'disk')
        devices.install('desktop', '123456', {'some key': 'some info'})
        devices.install('mobile', '443221', {'some key A': 'some info A'})
        devices.install('desktop', '666666', {'some key B': 'some info B'})
        self.assertEqual(len(devices.list('desktop')) + len(devices.list('mobile')), 3)

    def test_device_removal_after_limit(self):
        with mock.patch('mpfs.core.user.devices.USER_MAX_DEVICE_COUNT', new=3):
            with mock.patch('mpfs.core.user.devices.USER_ENABLE_REMOVE_EXTRA_DEVICES', new=True):
                devices = Devices(self.uid, 'disk')
                devices.install('desktop', '1', {'some key': 'some info'})
                devices.install('mobile', '2', {'some key': 'some info'})
                devices.install('desktop', '3', {'some key': 'some info'})
                self.assertEqual(2, len(devices.list('desktop')))
                self.assertEqual(1, len(devices.list('mobile')))
                devices.install('desktop', '4', {'some key': 'some info'}) # all previous devices are deleted
                self.assertEqual(1, len(devices.list('desktop')))
                self.assertEqual(0, len(devices.list('mobile')))

    def test_uninstall_device(self):
        '''
        Устанавливаем пару устройств и одно удаляем
        '''
        devices = Devices(self.uid, 'disk')
        devices.install('desktop', '123456', {'some key': 'some info'})
        devices.install('mobile', '443221', {'some key A': 'some info A'})
        devices.uninstall('desktop', '123456')
        self.assertEqual(len(devices.list('desktop')) + len(devices.list('mobile')), 1)

    def test_match_device(self):
        '''
        Проверяем алгоритм матчинга устройства по базовым параметрам
        '''
        devices = Devices(self.uid, 'disk')
        devices.install('mobile', '443221', {'some key A': 'some info A'})
        self.assertTrue(devices.match('443221'))
        self.assertFalse(devices.match('43423434233'))
        self.assertFalse(devices.match('443221', 'desktop'))

    def test_not_match_device_with_new_info(self):
        '''
        Проверяем матчинг устройства по дополнительным параметрам
        '''
        devices = Devices(self.uid, 'disk')
        devices.install('mobile', '443221', {'some key A': 'some info A'})
        self.assertTrue(devices.match('443221', info={'some key A': 'some info A'}))
        self.assertFalse(devices.match('443221', info={'some key': 'some info'}))

    def test_all_user_info(self):
        '''
        Проверяем, что в полном user info есть информация по состояниям и настройкам
        '''
        states = States(self.uid, 'disk')
        states.set(u'это юникодный ключ!', {'inner_key': 'inner_value'})

        settings = Settings(self.uid, 'disk')
        settings.set(u'это юникодный ключ!', {'inner_key': 'inner_value'})

        info = User(self.uid, 'disk').info()
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get(u'это юникодный ключ!'), {'inner_key': 'inner_value'})
        self.assertEqual(info.get('states').get(DEFAULT_NAMESPACE).get(u'это юникодный ключ!'), {'inner_key': 'inner_value'})

    def test_set_setting_via_core_custom_namespace(self):
        '''
        Проверяем установку настройки через core-метод
        '''
        namespace = 'custom'
        request = self.get_request({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                                    'value': 'value1', 'type': 'settings',
                                    'namespace': namespace})
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(namespace).get('key1'), 'value1')

    def test_set_setting_via_core(self):
        '''
        Проверяем установку настройки через core-метод
        '''
        request = self.get_request({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                                    'value': 'value1', 'type': 'settings',
                                    'namespace': DEFAULT_NAMESPACE})
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key1'), 'value1')

    def test_set_none_setting_via_core(self):
        '''
        Проверяем установку настройки через core-метод
        '''
        request = self.get_request({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                                    'value': None, 'type': 'settings',
                                    'namespace': DEFAULT_NAMESPACE})
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key1'), None)

    def test_set_setting_via_core_verstka(self):
        '''
        Проверяем установку настройки через core-метод
        '''
        namespace = 'verstka'
        key = 'MSOfficeHeaderPromoState'
        value = "/recent"
        request = self.get_request({'uid': self.uid, 'key': key, 'project': 'disk',
                                    'value': '0', 'type': 'settings',
                                    'namespace': namespace})
        core.set_user_var(request)

        request = self.get_request({'uid': self.uid, 'key': key, 'project': 'disk',
                                    'value': value, 'type': 'settings',
                                    'namespace': namespace})
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(namespace).get(key), value)

    def test_set_setting_via_core_update_add(self):
        '''
        Проверяем установку настройки через core-метод
        '''
        d = {
            'uid': self.uid, 'key': 'key1', 'project': 'disk',
             'value': 'value1', 'type': 'settings',
             'namespace': DEFAULT_NAMESPACE
         }
        request = self.get_request(d)
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key1'), 'value1')

        d['key'] = 'key2'
        d['value'] = 'value2'
        request = self.get_request(d)
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key2'), 'value2')
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key1'), 'value1')

    def test_set_setting_via_core_update(self):
        '''
        Проверяем установку настройки через core-метод
        '''
        d = {
            'uid': self.uid, 'key': 'key1', 'project': 'disk',
             'value': 'value1', 'type': 'settings',
             'namespace': DEFAULT_NAMESPACE
         }
        request = self.get_request(d)
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key1'), 'value1')

        d['value'] = 'value2'
        request = self.get_request(d)
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key1'), 'value2')

    def test_remove_last_setting_via_core(self):
        '''
        Проверяем удаление настройки через core-метод
        '''
        request = self.get_request({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                                    'value': 'value1', 'type': 'settings',
                                    'namespace': DEFAULT_NAMESPACE})
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                          'type': 'settings', 'namespace': DEFAULT_NAMESPACE})
        core.remove_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertRaises(Exception, info.get('settings').get(DEFAULT_NAMESPACE).get('key1'))

    def test_remove_setting_via_core(self):
        '''
        Проверяем удаление настройки через core-метод
        '''
        request = self.get_request({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                                    'value': 'value1', 'type': 'settings',
                                    'namespace': DEFAULT_NAMESPACE})
        core.set_user_var(request)

        request = self.get_request({'uid': self.uid, 'key': 'key2', 'project': 'disk',
                                    'value': 'value2', 'type': 'settings',
                                    'namespace': DEFAULT_NAMESPACE})
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                          'type': 'settings', 'namespace': DEFAULT_NAMESPACE})
        core.remove_user_var(request)

        request.set_args({'uid': self.uid, 'project': 'disk'})
        info = core.user_info(request)
        self.assertRaises(Exception, info.get('settings').get(DEFAULT_NAMESPACE).get('key1'))
        self.assertEqual(info.get('settings').get(DEFAULT_NAMESPACE).get('key2'), 'value2')

    def test_remove_undef_setting_via_core(self):
        '''
        Проверяем удаление настройки через core-метод
        '''
        request = self.get_request({'uid': self.uid, 'key': 'key1', 'project': 'disk',
                                    'value': 'value1', 'type': 'settings',
                                    'namespace': DEFAULT_NAMESPACE})
        core.set_user_var(request)

        request.set_args({'uid': self.uid, 'key': 'key2', 'project': 'disk',
                          'type': 'settings', 'namespace': DEFAULT_NAMESPACE})
        self.assertRaises(errors.SettingStateNotFound, core.remove_user_var, request)

    def test_install_device_via_core(self):
        '''
        Проверяем установку устройства через core-метод
        '''
        request = self.get_request({'uid': self.uid, 'id': '123456', 'project': 'disk',
                                    'type': 'desktop', 'info': {'some key': 'some info'}})
        core.user_install_device(request)

        request.set_args({'uid' : self.uid, 'project': 'disk'})
        info = core.user_info(request)
        device = info.get('devices').get('desktop').get('123456')
        for key, value in {'some key': 'some info'}.iteritems():
            self.assertEqual(device[key], value)
