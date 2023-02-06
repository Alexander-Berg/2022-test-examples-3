from yatest.common import work_path

from mail.sharpei.unistat.tests.common.prepare import prepare_unistat_daemon
from mail.unistat.cpp.cython.canonize.recipe import find_free_port


if __name__ == "__main__":
    def get_config():
        """
        Calls to work_path should be postponed to be executed within test runtime.
        """
        port = find_free_port()
        config = {
            "dir": work_path(''),
            "from_beginning": True,
            "host": '::',
            "port": port,
            "log": work_path('unistat.log'),
            "sharpei_config_path": work_path('etc/sharpei/config-base.yml'),
            "type": "datasync_and_disk",
        }
        return config

    prepare_unistat_daemon(get_config)
