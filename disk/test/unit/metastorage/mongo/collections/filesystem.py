# -*- coding: utf-8 -*-

from mpfs.metastorage.mongo.collections.filesystem import UserCollectionZipped

from test.unit.base import NoDBTestCase


class UserCollectionZippedMocked(UserCollectionZipped):
    def __init__(self, example_subtree):
        self.example_subtree = example_subtree
        pass

    def iter_subtree(self, uid, key, limit=None):
        return iter(self.example_subtree)


class UserCollectionZippedTestCase(NoDBTestCase):
    @classmethod
    def sort_tree_index_results(cls, list_result):
        list_result.sort(key=lambda x: x['this']['key'])
        for i in list_result:
            cls.sort_tree_index_results(i['list'])

    def test_query_optimizer(self):
        # иначе mock из NoDBTestCase.setup_class не работает
        qo = UserCollectionZipped.query_optimizer

        args = ({'data.mt': 1}, [])
        assert qo(*args) == args
        args = ({'data.mt': {'$in': []}}, [])
        assert qo(*args) == args
        args = ({'data.mt': {'$in': []}, 'type': 'dir'}, [])
        assert qo(*args) == args

        args = ({'data.mt': {'$in': []}, 'type': 'file'}, [])
        assert qo(*args) == ({'data.mt': {'$in': []}}, [])
        args = ({'data.mt': {'$in': []}, 'type': 'file'}, [('other_sort_field', 1), ('type', 1)])
        assert qo(*args) == ({'data.mt': {'$in': []}}, [('other_sort_field', 1)])
        # исходную структуру не поменяли
        assert args == ({'data.mt': {'$in': []}, 'type': 'file'}, [('other_sort_field', 1), ('type', 1)])

    def test_tree_index_on_folder_with_files(self):
        correct_index_tree_result = [
            {
                'this': {
                    'data': {'meta': {}, 'name': 'dir_1', 'type': 'dir'},
                    'key': '/disk/dir_1',
                    'type': 'dir'
                },
                'list': [
                    {
                        'this': {
                            'data': {'meta': {}, 'name': 'dir_1_1', 'type': 'dir'},
                            'key': '/disk/dir_1/dir_1_1',
                            'type': 'dir'
                        },
                        'list': [
                            {
                                'this': {
                                    'data': {'meta': {}, 'name': 'file_3', 'type': 'file'},
                                    'key': '/disk/dir_1/dir_1_1/file_3',
                                    'type': 'file'
                                },
                                'list': [],
                            },
                            {
                                'this': {
                                    'data': {'meta': {}, 'name': 'file_4', 'type': 'file'},
                                    'key': '/disk/dir_1/dir_1_1/file_4',
                                    'type': 'file'
                                },
                                'list': []
                            }
                        ],
                    },
                    {
                        'this': {
                            'data': {'meta': {}, 'name': 'file_2', 'type': 'file'},
                            'key': '/disk/dir_1/file_2',
                            'type': 'file'
                        },
                        'list': [],
                    }
                ]
            },
            {
                'this': {
                    'data': {'meta': {}, 'name': 'dir_2', 'type': 'dir'},
                    'key': '/disk/dir_2',
                    'type': 'dir'
                },
                'list': [],
            },
            {
                'this': {
                    'data': {'meta': {}, 'name': 'file_1', 'type': 'file'},
                    'key': '/disk/file_1',
                    'type': 'file'
                },
                'list': [],
            }
        ]

        subtree = [
            {'key': '/disk/dir_1', 'type': 'dir', 'data': {}},
            {'key': '/disk/dir_2', 'type': 'dir', 'data': {}},
            {'key': '/disk/dir_1/dir_1_1', 'type': 'dir', 'data': {}},
            {'key': '/disk/file_1', 'type': 'file', 'data': {}},
            {'key': '/disk/dir_1/file_2', 'type': 'file', 'data': {}},
            {'key': '/disk/dir_1/dir_1_1/file_3', 'type': 'file', 'data': {}},
            {'key': '/disk/dir_1/dir_1_1/file_4', 'type': 'file', 'data': {}}
        ]
        user_collection_mock = UserCollectionZippedMocked(subtree)

        result = user_collection_mock.tree_index('123', '/disk')
        self.sort_tree_index_results(result)
        self.sort_tree_index_results(correct_index_tree_result)
        assert result == correct_index_tree_result

    def test_tree_index_files_only_in_folder(self):
        correct_index_tree_result = [
            {
                'this': {
                    'data': {'meta': {}, 'name': 'file_1', 'type': 'file'},
                    'key': '/disk/file_1',
                    'type': 'file'
                },
                'list': []

            },
            {
                'this': {
                    'data': {'meta': {}, 'name': 'file_2', 'type': 'file'},
                    'key': '/disk/file_2',
                    'type': 'file'
                },
                'list': []
            }
        ]

        subtree = [
            {'key': '/disk/file_1', 'type': 'file', 'data': {}},
            {'key': '/disk/file_2', 'type': 'file', 'data': {}},
        ]
        user_collection_mock = UserCollectionZippedMocked(subtree)
        result = user_collection_mock.tree_index('123', '/disk')
        self.sort_tree_index_results(result)
        self.sort_tree_index_results(correct_index_tree_result)
        assert result == correct_index_tree_result

    def test_tree_index_folders_only_in_folder(self):
        correct_index_tree_result = [
            {
                'this': {
                    'data': {'meta': {}, 'name': 'folder_1', 'type': 'folder'},
                    'key': '/disk/folder_1',
                    'type': 'folder'
                },
                'list': []
            },
            {
                'this': {
                    'data': {'meta': {}, 'name': 'folder_2', 'type': 'folder'},
                    'key': '/disk/folder_2',
                    'type': 'folder'
                },
                'list': []
            }
        ]

        subtree = [
            {'key': '/disk/folder_1', 'type': 'folder', 'data': {}},
            {'key': '/disk/folder_2', 'type': 'folder', 'data': {}},
        ]
        user_collection_mock = UserCollectionZippedMocked(subtree)
        result = user_collection_mock.tree_index('123', '/disk')
        self.sort_tree_index_results(result)
        self.sort_tree_index_results(correct_index_tree_result)
        assert result == correct_index_tree_result

    def test_tree_index_empty_folder(self):
        correct_index_tree_result = []
        user_collection_mock = UserCollectionZippedMocked([])

        result = user_collection_mock.tree_index('123', '/disk')
        self.sort_tree_index_results(result)
        self.sort_tree_index_results(correct_index_tree_result)
        assert result == correct_index_tree_result
