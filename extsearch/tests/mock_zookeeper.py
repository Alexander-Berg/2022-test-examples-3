from collections import defaultdict

import kazoo
import os


def tree():
    return defaultdict(tree)


class DotDict(dict):
    __getattr__ = dict.get
    __setattr__ = dict.__setitem__
    __delattr__ = dict.__delitem__


class Node(object):
    def __init__(self, name, value=b''):
        self.name = name
        self.value = value
        self.children = {}
        self.next_in_sequence = 0

    def add_child(self, name, value=b'', sequence=False):
        if sequence:
            node_name = '{}{}'.format(name, '{0:010d}'.format(self.next_in_sequence))
            self.next_in_sequence += 1
        else:
            node_name = name

        child = Node(node_name, value)
        self.children[node_name] = child
        return self.children[node_name]

    def remove_child(self, name):
        del self.children[name]

    def get_children(self):
        return self.children

    def get_child(self, name):
        return self.children[name]

    def set_value(self, value):
        self.value = value

    def get_value(self):
        return self.value


class ZookeperMock(object):
    def __init__(self):
        self.__data_tree = Node('/')
        self.connected = False

    def start(self):
        self.connected = True

    def stop(self):
        self.connected = False

    def close(self):
        pass

    def __walk_to_path(self, path, makepath=False):
        folder = self.__data_tree
        for name in path.split('/'):
            if not name:
                continue

            if name in folder.get_children():
                folder = folder.get_child(name)
            elif makepath:
                folder = folder.add_child(name)
            else:
                raise kazoo.exceptions.NoNodeError()

        return folder

    def __check_connection(self):
        if not self.connected:
            raise kazoo.exceptions.ConnectionLoss()

    def get_children(self, path, watch=None, include_data=False):
        self.__check_connection()
        folder = self.__walk_to_path(path)
        return sorted([name for name, _ in folder.get_children().iteritems()])

    def create(self, path, value=b"", acl=None, ephemeral=False, sequence=False, makepath=False):
        self.__check_connection()

        parent = os.path.dirname(path)
        child = os.path.basename(path)

        folder = self.__walk_to_path(parent, makepath)
        folder.add_child(child, value, sequence)

    def get(self, path, watch=None):
        self.__check_connection()
        value = self.__walk_to_path(path).get_value()
        children = self.get_children(path)

        znode_stat = DotDict({'children_count': len(children)})
        return value, znode_stat

    def set(self, path, value, version=-1):
        self.__check_connection()
        folder = self.__walk_to_path(path)
        folder.set_value(value)

    def exists(self, path, watch=None):
        self.__check_connection()
        folder = self.__data_tree
        for name in path.split('/'):
            if not name:
                continue
            if name not in folder.get_children():
                return False
            folder = folder.get_child(name)

        return True

    def delete(self, path, version=-1, recursive=False):
        self.__check_connection()
        parent = os.path.dirname(path)
        child = os.path.basename(path)

        folder = self.__walk_to_path(parent)
        folder.remove_child(child)

    def ensure_path(self, path, acl=None):
        self.__check_connection()
        self.create(path, makepath=True)

    def retry(self, func, *args, **kwargs):
        return func(*args, **kwargs)
