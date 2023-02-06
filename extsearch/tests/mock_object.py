import os
from collections import defaultdict

import kazoo
from interface.zookeeper import IRecipe, IZookeeper


class Recipe(IRecipe):
    def __init__(self):
        self.acquire_count = 0
        self.release_count = 0

    def __enter__(self):
        self.acquire()

    def __exit__(self, tp, value, traceback):
        self.release()
        return False

    def acquire(self, blocking=True, timeout=None):
        self.acquire_count += 1
        return True

    def release(self):
        self.release_count += 1
        return True


def tree():
    return defaultdict(tree)


class Zookeeper(IZookeeper):
    def __init__(self):
        self.__host_semaphore = Recipe()
        self.__global_semaphore = Recipe()
        self.__shard_semaphore = Recipe()

        self.__data_tree = tree()

    def __walk_to_path(self, path, makepath=False):
        folder = self.__data_tree
        for name in path.split('/'):
            if not name:
                continue
            if name not in folder and not makepath:
                raise kazoo.exceptions.NoNodeError()
            folder = folder[name]

        return folder

    @property
    def host_semaphore(self):
        return self.__host_semaphore

    @property
    def global_semaphore(self):
        return self.__global_semaphore

    @property
    def shard_semaphore(self):
        return self.__shard_semaphore

    def get_children(self, path, watch=None, include_data=False):
        folder = self.__walk_to_path(path)
        return [name for name, _ in folder.iteritems()]

    def create(self, path, value=b"", acl=None, ephemeral=False,
               sequence=False, makepath=False):
        dirname = os.path.dirname(path)
        basename = os.path.basename(path)
        folder = self.__walk_to_path(dirname, makepath)
        folder[basename] = value

    def get(self, path, watch=None):
        value = self.__walk_to_path(path)
        return value

    def delete(self, path, version=-1, recursive=False):
        pass


class Config(object):
    def __init__(self):
        self.shard_dir = 'imgtest-000-20160306-235044'
        self.required_shard_dir = 'imgtest-000-20160306-000000'
        self.shard_id = 0
        self.log_file = 'current-log-shard_tool-log'
        self.log_level = 'DEBUG'
        self.max_host_activities = 1
        self.max_global_activities = 2000
        self.max_shard_activities = 1
        self.shard_download_attempts = 3
        self.make_bsconfig_shard = True
        self.cms_rpc_addr = 'http://cmsearch.yandex.ru/xmlrpc/bs'
        self.shard_register_tries = 10
        self.shard_initial_register_delay = 1
        self.shard_max_register_delay = 3600
        self.shard_register_backoff = 2.5
        self.skynet_tries = 3
        self.skynet_initial_delay = 1
        self.skynet_max_delay = 10
        self.skynet_backoff = 2.5
        self.tier = 'ImgTier0'
        self.mr_server = 'mrserver'
        self.prefix = 'images_test'
        self.app_logger_name = 'application'
        self.mapreduce_env = {}

    @classmethod
    def get_statistic_path(cls, shard_dir):
        return shard_dir
