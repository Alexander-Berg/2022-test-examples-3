# -*- coding: utf-8 -*-

import os
import os.path
from functools import wraps
from shutil import rmtree

from django.conf import settings
from travel.avia.admin.lib.tmpfiles import get_tmp_dir


def use_tmp_data_path(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        old_data_dir = settings.DATA_PATH
        tmp_data_dir = get_tmp_dir('tests')

        if os.path.exists(tmp_data_dir):
            rmtree(tmp_data_dir, ignore_errors=True)

        os.makedirs(tmp_data_dir)

        settings.DATA_PATH = tmp_data_dir

        try:
            func(*args, **kwargs)
        finally:
            # rmtree(tmp_data_dir, ignore_errors=True)
            settings.DATA_PATH = old_data_dir

    return wrapper
