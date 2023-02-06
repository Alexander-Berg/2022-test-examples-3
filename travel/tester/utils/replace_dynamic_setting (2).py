# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import six
if six.PY3:
    from contextlib import contextmanager, ExitStack
else:
    from contextlib2 import contextmanager, ExitStack

from functools import partial

from travel.rasp.library.python.common23.dynamic_settings import default
from travel.rasp.library.python.common23.tester.utils.replace_setting import ReplaceKey, ReplaceAttr


@contextmanager
def replace_dynamic_setting_keys(setting_name, **items):
    changes = [replace_dynamic_setting_key(setting_name, k, v) for k, v in items.items()]
    with ExitStack() as stack:
        for change_context in changes:
            stack.enter_context(change_context)
        yield


replace_dynamic_setting = partial(ReplaceAttr, obj=default.conf)
replace_dynamic_setting_key = lambda attr, key, value: ReplaceKey(getattr(default.conf, attr), key, value)
