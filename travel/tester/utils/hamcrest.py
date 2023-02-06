# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import range
from hamcrest import has_entries, contains_inanyorder, all_of
from hamcrest.core.helpers.wrap_matcher import wrap_matcher


def has_only_entries(*keys_valuematchers, **kv_args):
    """
    Матчер, проверяющий, что в дикте есть заданные ключи-значения и нет лишних ключей.
    В отличие от прямого сравнения дикта с диктом, позволяет использовать hamcrest-матчеры для проверки значений ключей.

    assert_that(dct, has_entries_exact(**{
        "a": 123,
        "b": contains_inanyorder(2, 3),
    }))

    Вся логика подготовки аргументов скопипасчена из оригинального has_entries, т.к. он нерасширяем.
    """
    if len(keys_valuematchers) == 1:
        try:
            base_dict = keys_valuematchers[0].copy()
            for key in base_dict:
                base_dict[key] = wrap_matcher(base_dict[key])
        except AttributeError:
            raise ValueError('single-argument calls to has_entries must pass a dict as the argument')
    else:
        if len(keys_valuematchers) % 2:
            raise ValueError('has_entries requires key-value pairs')
        base_dict = {}
        for index in range(int(len(keys_valuematchers) / 2)):
            base_dict[keys_valuematchers[2 * index]] = wrap_matcher(keys_valuematchers[2 * index + 1])

    for key, value in kv_args.items():
        base_dict[key] = wrap_matcher(value)

    return all_of(
        has_entries(**base_dict),
        contains_inanyorder(*list(base_dict.keys()))
    )
