# -*- coding: utf-8 -*-
import itertools
import mock

from attrdict import AttrDict
from nose_parameterized import parameterized

import mpfs.engine.process

from mpfs.common.static import codes
from mpfs.core import address
from mpfs.config import settings
from mpfs.core.bus import Bus
from test.base import DiskTestCase
from test.base_suit import JsonApiTestCaseMixin
from test.common.sharing import CommonSharingMethods


RATE_LIMITER_GROUP_NAMES_FULL_DIFF = settings.rate_limiter['group_names']['full_diff']
RATE_LIMITER_GROUP_NAMES_VERSION_DIFF = settings.rate_limiter['group_names']['version_diff']
RATE_LIMITER_GROUP_NAMES_PATH_DIFF = settings.rate_limiter['group_names']['path_diff']


dbctl = mpfs.engine.process.dbctl()


class VersionDiffFields(object):
    root_fields = frozenset({
        'result',
        'version',
        'amount',
    })
    base_fields = frozenset({
        'op',
        'type',
        'key',
        'version',
        'resource_id',
    })
    file_extra_fields = frozenset({
        'md5',
        'sha256',
        'size'
    })
    dir_extra_fields = frozenset()

    @classmethod
    def get_by_resource_type(cls, type_name):
        assert type_name in ('file', 'dir')

        fields = cls.base_fields
        if type_name == 'file':
            fields |= cls.file_extra_fields
        else:
            fields |= cls.dir_extra_fields
        return fields


class DiskStateResource(AttrDict):
    """Представления ресурса диска пользователя"""
    @classmethod
    def build_from_fulltree_this(cls, this):
        """Из документа `fulltree->this` выдернуть нужные поля и сделать diff item
        """
        uid, _ = this['id'].split(':', 1)
        path = '/disk%s' % this['path']
        return cls({
            'type': this['type'],
            'raw_address': address.Address.Make(uid, path).id,
            'uid': uid,
            'path': path,
            'resource_id': this['meta'].get('resource_id'),
            'file_id': this['meta'].get('file_id'),
            'md5': this['meta'].get('md5'),
            'sha256': this['meta'].get('sha256'),
            'size': this['meta'].get('size'),
            'group': this['meta'].get('group'),
            'revision': this['meta'].get('revision'),
        })

    def __repr__(self):
        return super(DiskStateResource, self).__repr__().replace('AttrDict', 'DiskStateResource')


class UserDiskState(object):
    """Класс позволяющий зафиксировать структуру дисковых ресурсов пользователя

    Работает на основе `/json/fulltree`
    В Диске ресурс у пользователя можно однозначно определить двумя способами:
        * address - uid + path
        * resource_id - uid + file_id
    Поэтому строим два "индекса": address_item_map, resource_id_item_map
    """
    def __init__(self, test_case, uid):
        self.test_case = test_case
        self.uid = uid
        self.fulltree = None
        self.version = None
        self.address_item_map = None
        self.resource_id_item_map = None
        self.is_captured = False

    def capture_state(self):
        if self.is_captured is True:
            raise RuntimeError()
        self.fulltree = self.get_fulltree()
        self.version = self.get_version()
        self.address_item_map, self.resource_id_item_map = self.build_item_maps(self.fulltree)
        self.is_captured = True

    def get_fulltree(self):
        return self.test_case.json_ok('fulltree', {'uid': self.uid, 'path': '/disk', 'meta': ''})

    def get_version(self):
        return long(self.test_case.json_ok('user_info', {'uid': self.uid})['version'])

    @classmethod
    def build_item_maps(cls, fulltree):
        """Строит два плоских словаря на основе fulltree

        Словари:
            * raw_address -> item
            * resource_id -> item
        """
        item = DiskStateResource.build_from_fulltree_this(fulltree['this'])
        address_map = {
            item['raw_address']: item,
        }
        resource_id_map = {
            item['resource_id']: item,
        }
        for child in fulltree['list']:
            child_address_map, child_resource_id_map = cls.build_item_maps(child)
            address_map.update(child_address_map)
            resource_id_map.update(child_resource_id_map)
        return address_map, resource_id_map


class TwoDiskStatesDiff(object):
    """Сформировать разницу между двумя состояниями диска

    Можно получить diff двух типов:
        * по адресам
        * по resource_id
    В обоих случаях возвращается добавленные и удаленные элементы
    """
    def __init__(self, test_case, uid):
        self.first_state = UserDiskState(test_case, uid)
        self.second_state = UserDiskState(test_case, uid)
        self.test_case = test_case
        self.uid = uid

    def capture_first_state(self):
        """Фиксация первого состояния диска
        """
        assert not self.first_state.is_captured
        assert not self.second_state.is_captured
        self.first_state.capture_state()

    def capture_second_state(self):
        """Фиксация второго состояния диска
        """
        assert self.first_state.is_captured
        assert not self.second_state.is_captured
        self.second_state.capture_state()

    def get_diff(self, item_map_name):
        """Получения изменений между первым и вторым состояниями
        """
        assert self.first_state.is_captured
        assert self.second_state.is_captured
        assert item_map_name in ('address_item_map', 'resource_id_item_map')

        get_diff = lambda a, b: [a[i] for i in a.viewkeys() - b.viewkeys()]

        first_map = getattr(self.first_state, item_map_name)
        second_map = getattr(self.second_state, item_map_name)
        new = get_diff(second_map, first_map)
        deleted = get_diff(first_map, second_map)
        return new, deleted

    def get_address_diff(self):
        """Получить разницу по адресам(путям)
        """
        return self.get_diff('address_item_map')

    def get_resource_id_diff(self):
        """Получить разницу по resource_id(file_id)
        """
        return self.get_diff('resource_id_item_map')

    @classmethod
    def get_diff_for_uid(cls, test_case, uid, action_func):
        """Типичный workflow использования этого класса

        :rtype: TwoDiskStatesDiff
        """
        assert isinstance(uid, basestring)
        return cls.get_diff_for_uids(test_case, [uid], action_func)[uid]

    @classmethod
    def get_diff_for_uids(cls, test_case, uids, action_func):
        """Актуально для тестов ОП

        В качестве uids передаем uid владельца и приглашенных пользователей и
        смотрим как меняются их Диски при изменениях в ОП

        :rtype: dict
        :return: {<uid>: <TwoDiskStatesDiff()>}
        """
        assert isinstance(uids, (list, tuple))
        # должен быть метод `json_ok`
        assert isinstance(test_case, JsonApiTestCaseMixin)
        assert callable(action_func)
        uid_diff_map = {}
        for uid in uids:
            disk_diff = cls(test_case, uid)
            uid_diff_map[uid] = disk_diff
            disk_diff.capture_first_state()

        action_func()

        for disk_diff in uid_diff_map.itervalues():
            disk_diff.capture_second_state()
        return uid_diff_map


############# Тесты #############


class BaseVersionDiffTestCase(DiskTestCase):
    """Базовый класс для тестирования версионных диффов
    """
    @staticmethod
    def filter_childs(items, path_getter):
        items = sorted(items, key=path_getter)
        parent = None
        result = []
        for item in items:
            path = path_getter(item)
            if parent is None or not path.startswith(parent):
                parent = path
                result.append(item)
        return result

    def assert_diff_items_are_equal(self, change_type, test_diff, test_diff_item, mpfs_diff_item):
        # проверяем наличие обязательных полей у изменения в версионном diff-е
        required_fields = VersionDiffFields.get_by_resource_type(mpfs_diff_item['type'])
        for required_field in required_fields:
            assert required_field in mpfs_diff_item
        assert mpfs_diff_item['op'] == change_type
        assert mpfs_diff_item['resource_id'] == test_diff_item.resource_id
        assert mpfs_diff_item['type'] == test_diff_item.type
        assert mpfs_diff_item['key'] == test_diff_item.path
        assert mpfs_diff_item['version'] > test_diff.first_state.version
        assert mpfs_diff_item['version'] <= test_diff.second_state.version

    def assert_diff_groups_are_equal(self, change_type, test_diff, test_diff_group, mpfs_diff_group):
        assert len(test_diff_group) == len(mpfs_diff_group)
        for test_diff_item, mpfs_diff_item in zip(test_diff_group, mpfs_diff_group):
            self.assert_diff_items_are_equal(change_type, test_diff, test_diff_item, mpfs_diff_item)

    def assert_diffs_are_equal(self, mpfs_diff, test_diff):
        """Метод сверки версионного diff-а(/json/diff) и диффа на основе `TwoDiskStatesDiff`
        """
        assert isinstance(test_diff, TwoDiskStatesDiff)
        assert isinstance(mpfs_diff, dict)

        assert not(VersionDiffFields.root_fields ^ mpfs_diff.viewkeys())

        mpfs_diff_groups = {
            'new': [],
            'deleted': [],
            'changed': []
        }
        for change_type, group in itertools.groupby(mpfs_diff['result'], lambda x: x['op']):
            mpfs_diff_groups[change_type] = list(group)

        test_new, test_deleted = test_diff.get_address_diff()
        test_diff_groups = {
            'new': test_new,
            # `_process_chanelog_items` отфильтровывает удаленных детей из результата
            'deleted': self.filter_childs(test_deleted, lambda x: x['path']),
            'changed': []
        }
        # mpfs-ый diff сортирует непонятно как, поэтому выравниваем тут
        for group_name in mpfs_diff_groups:
            mpfs_diff_groups[group_name].sort(key=lambda x: x['key'])
            test_diff_groups[group_name].sort(key=lambda x: x['path'])

        ## для отладки
        #debug_data = {"test_diff_groups": test_diff_groups, "mpfs_diff_groups": mpfs_diff_groups}
        #print json.dumps(debug_data, indent=2)

        for change_type, mpfs_diff_group in mpfs_diff_groups.iteritems():
            test_diff_group = test_diff_groups[change_type]
            self.assert_diff_groups_are_equal(change_type, test_diff, test_diff_group, mpfs_diff_group)


class VersionDiffTestCase(BaseVersionDiffTestCase):
    """Тестирование версионных диффов для обычных файлов(не ОП)
    """
    resource_path = '/disk/1'

    def assert_diffs_equal_after_action(self, test_action):
        test_diff = TwoDiskStatesDiff.get_diff_for_uid(self, self.uid, test_action)
        mpfs_diff = self.json_ok('diff', {'uid': self.uid, 'version': test_diff.first_state.version})
        self.assert_diffs_are_equal(mpfs_diff, test_diff)

    def test_mkdir(self):
        test_action = lambda: self.json_ok('mkdir', {'uid': self.uid, 'path': self.resource_path})
        self.assert_diffs_equal_after_action(test_action)

    def test_store_file(self):
        test_action = lambda: self.upload_file(self.uid, self.resource_path)
        self.assert_diffs_equal_after_action(test_action)

    def test_rm_dir(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.resource_path})
        test_action = lambda: self.json_ok('rm', {'uid': self.uid, 'path': self.resource_path})
        self.assert_diffs_equal_after_action(test_action)

    def test_rm_file(self):
        self.upload_file(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('rm', {'uid': self.uid, 'path': self.resource_path})
        self.assert_diffs_equal_after_action(test_action)

    def test_trash_append_dir(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.resource_path})
        test_action = lambda: self.json_ok('trash_append', {'uid': self.uid, 'path': self.resource_path})
        self.assert_diffs_equal_after_action(test_action)

    def test_trash_append_file(self):
        self.upload_file(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('trash_append', {'uid': self.uid, 'path': self.resource_path})
        self.assert_diffs_equal_after_action(test_action)

    def test_move_dir(self):
        dst = self.resource_path + '_dst'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.resource_path})
        test_action = lambda: self.json_ok('move', {'uid': self.uid, 'src': self.resource_path, 'dst': dst})
        self.assert_diffs_equal_after_action(test_action)

    def test_move_file(self):
        dst = self.resource_path + '_dst'
        self.upload_file(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('move', {'uid': self.uid, 'src': self.resource_path, 'dst': dst})
        self.assert_diffs_equal_after_action(test_action)

    def test_copy_dir(self):
        dst = self.resource_path + '_dst'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.resource_path})
        test_action = lambda: self.json_ok('copy', {'uid': self.uid, 'src': self.resource_path, 'dst': dst})
        self.assert_diffs_equal_after_action(test_action)

    def test_copy_file(self):
        dst = self.resource_path + '_dst'
        self.upload_file(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('copy', {'uid': self.uid, 'src': self.resource_path, 'dst': dst})
        self.assert_diffs_equal_after_action(test_action)

    def _make_folder_with_items(self, uid, path):
        self.json_ok('mkdir', {'uid': uid, 'path': path})
        self.json_ok('mkdir', {'uid': uid, 'path': "%s/1" % path})
        self.upload_file(uid, "%s/1.txt" % path)

    def test_copy_dir_with_items(self):
        dst = self.resource_path + '_dst'
        self._make_folder_with_items(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('copy', {'uid': self.uid, 'src': self.resource_path, 'dst': dst})
        self.assert_diffs_equal_after_action(test_action)

    def test_move_dir_with_items(self):
        dst = self.resource_path + '_dst'
        self._make_folder_with_items(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('move', {'uid': self.uid, 'src': self.resource_path, 'dst': dst})
        self.assert_diffs_equal_after_action(test_action)

    def test_rm_dir_with_items(self):
        self._make_folder_with_items(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('rm', {'uid': self.uid, 'path': self.resource_path})
        self.assert_diffs_equal_after_action(test_action)

    def test_trash_append_dir_with_items(self):
        self._make_folder_with_items(self.uid, self.resource_path)
        test_action = lambda: self.json_ok('trash_append', {'uid': self.uid, 'path': self.resource_path})
        self.assert_diffs_equal_after_action(test_action)


class GroupVersionDiffTestCase(CommonSharingMethods, VersionDiffTestCase):
    """Тесты версионного диффа для ОП
    """
    resource_path = '/disk/1/1'

    def setup_method(self, method):
        super(GroupVersionDiffTestCase, self).setup_method(method)

        self.owner = AttrDict({
            'is_owner': True,
            'uid': self.uid,
            'path': '/disk/1',
        })
        self.invited = AttrDict({
            'is_owner': False,
            'uid': self.uid_3,
            'path': '/disk/2',
        })
        self.json_ok('user_init', {'uid': self.uid_3})

        self.json_ok('mkdir', {'uid': self.owner.uid, 'path': self.owner.path})
        gid = self.create_group(self.owner.uid, self.owner.path)

        hsh = self.invite_user(uid=self.invited.uid, owner=self.owner.uid, email=self.email_3, ext_gid=gid, path=self.owner.path)
        self.activate_invite(uid=self.invited.uid, hash=hsh)
        self.json_ok('move', {'uid': self.invited.uid, 'src': self.owner.path, 'dst': self.invited.path})

        invited_info = self.json_ok('info', {'uid': self.invited.uid, 'path': self.invited.path, 'meta': 'group'})
        owner_info = self.json_ok('info', {'uid': self.owner.uid, 'path': self.owner.path, 'meta': 'group'})
        assert invited_info['path'] == self.invited.path
        assert owner_info['path'] == self.owner.path
        assert owner_info['meta']['group']['owner']['uid'] == invited_info['meta']['group']['owner']['uid'] == self.owner.uid
        assert owner_info['meta']['group']['gid'] == invited_info['meta']['group']['gid']

    def assert_diffs_equal_after_action(self, test_action):
        test_diffs = TwoDiskStatesDiff.get_diff_for_uids(self, [self.owner.uid, self.invited.uid], test_action)
        for uid, test_diff in test_diffs.iteritems():
            mpfs_diff = self.json_ok('diff', {'uid': uid, 'version': test_diff.first_state.version})
            self.assert_diffs_are_equal(mpfs_diff, test_diff)

    def test_activate_invite(self):
        """Принять приглашение в группу"""
        folder_path = '/disk/3'
        self.json_ok('mkdir', {'uid': self.owner.uid, 'path': folder_path})
        self.json_ok('mkdir', {'uid': self.owner.uid, 'path': folder_path + '/1'})
        gid = self.create_group(self.owner.uid, folder_path)

        hsh = self.invite_user(uid=self.invited.uid, owner=self.owner.uid, email=self.email_3, ext_gid=gid, path=folder_path)
        test_action = lambda: self.activate_invite(uid=self.invited.uid, hash=hsh)
        self.assert_diffs_equal_after_action(test_action)

    def test_remove_group_owner(self):
        """Удалить группу владельцем"""
        test_action = lambda: self.json_ok('async_rm', {'uid': self.owner.uid, 'path': self.owner.path})
        self.assert_diffs_equal_after_action(test_action)

    def test_remove_group_invited(self):
        """Удалить группу приглашенным"""
        test_action = lambda: self.json_ok('async_rm', {'uid': self.invited.uid, 'path': self.invited.path})
        self.assert_diffs_equal_after_action(test_action)


class FullDiffTestCase(BaseVersionDiffTestCase):
    def test_full_diff_file_limit(self):
        self.json_ok('diff', {'uid': self.uid})
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.FEATURE_TOGGLES_FULL_DIFF_FILE_LIMIT', 1):
            self.json_error('diff', {'uid': self.uid}, code=codes.TOO_MANY_ELEMENTS_FOR_FULL_DIFF)
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.FEATURE_TOGGLES_FULL_DIFF_FILE_LIMIT', 1000):
            self.json_ok('diff', {'uid': self.uid})

    def test_diff_for_blocked_user(self):
        """Ожидаем 401 код ответа (а не 403, как для остальных ручек)"""
        self.support_ok('block_user', {'uid': self.uid,
                                       'moderator': 'EHOT',
                                       'comment': 'loud neighbor and bad person'})
        self.json_error('diff', {'uid': self.uid}, code=codes.UNAUTHORIZED, status=401)

    def test_russian_symbols_in_path(self):
        folder_path = u'/disk/Папка'
        file_path = folder_path + '/file.txt'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.upload_file(self.uid, file_path)

        diff = self.json_ok('diff', {'uid': self.uid, 'path': folder_path})
        assert diff['amount'] == 2
        assert set(i['key'] for i in diff['result']) == {folder_path, file_path}

    def test_correct_visibility_property(self):
        folder_path = '/disk/folder'
        file_path = folder_path + '/file.txt'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.upload_file(self.uid, file_path)

        diff = self.json_ok('diff', {'uid': self.uid, 'path': folder_path})
        assert diff['amount'] == 2
        for item in diff['result']:
            assert item['visible'] == 1

    def test_external_setprop_flag_is_absent_if_modify_uid_is_set(self):
        folder_path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        dbctl.database()['user_data'].update(
            {'uid': self.uid, 'key': folder_path}, {'$set': {'data.modify_uid': self.uid}})

        diff = self.json_ok('diff', {'uid': self.uid, 'path': folder_path})

        assert diff['amount'] == 1
        assert 'external_setprop' not in diff['result'][0]

    @parameterized.expand([
        (None,),
        ("",),
    ])
    def test_mime_type_is_application_octet_stream_if_its_empty_or_none(self, new_mimetype):
        path = '/disk/test1.jpg'
        self.upload_file(self.uid, path)
        Bus().setprop(self.uid, ':'.join([self.uid, path]), {'mimetype': new_mimetype})
        resp = self.json_ok('diff', {'uid': self.uid, 'meta': 'mimetype'})
        assert resp['result'][0]['mimetype'] == 'application/octet-stream'


class DiffRateLimiterGroupTestCase(DiskTestCase):

    @parameterized.expand([
        ('full_diff_without_path', None, None, RATE_LIMITER_GROUP_NAMES_FULL_DIFF),
        ('full_diff_root', None, '/', RATE_LIMITER_GROUP_NAMES_FULL_DIFF),
        ('full_diff', None, '/disk', RATE_LIMITER_GROUP_NAMES_FULL_DIFF),
        ('full_diff_with_specified_path', None, '/disk/folder', RATE_LIMITER_GROUP_NAMES_PATH_DIFF),
        ('version_diff', 123, '/disk', RATE_LIMITER_GROUP_NAMES_VERSION_DIFF),
        ('version_diff_with_specified_path', 123, '/disk/folder', RATE_LIMITER_GROUP_NAMES_VERSION_DIFF),
    ])
    def test_rate_limiter_group(self, _, version, path, expected_group):
        params = {'uid': self.uid}
        if path:
            params['path'] = path
        if version:
            params['version'] = version
        with mock.patch('mpfs.core.services.rate_limiter_service.rate_limiter.is_limit_exceeded', return_value=False) as rl,\
                mock.patch('mpfs.core.bus.Filesystem.diff', return_value={}),\
                mock.patch('mpfs.frontend.formatter.disk.desktop.Desktop.diff', return_value={}):
            self.json_ok('diff', params)
            rl.assert_called_with(expected_group, self.uid)
