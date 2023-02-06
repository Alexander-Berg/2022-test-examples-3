#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import texttable


class CaseFeature:
    def __init__(self, name, values):
        self.name = name
        self.values = values


features = []
features.append(CaseFeature('is_global', [True, False]))
features.append(CaseFeature('cpa', [True, False]))
features.append(CaseFeature('cpc', [True, False]))
features.append(CaseFeature('front', ['old', 'new']))
features.append(CaseFeature('prepay', [True, False]))
features.append(CaseFeature('api', [True, False]))
features.append(CaseFeature('moder', ['ok', 'common_clone', 'common_low_quality', 'common_critical_quality']))
# features.append(CaseFeature('postmoder', [
#     'cpc_site', 'cpc_quality', 'cpc_quality_other',
#     'cpф_site', 'cpc_quality', 'cpc_quality_other',
#     'common_clone', 'common_fraud', 'common_other',
#     'cpa_']))

# light: cpc_quality_other, common_other->light_tikcet_general_other
# cheesy* -> только через саппорт
# cpa_quality_other -> light
# cpa_quality_api -> light
# cpa_quality -> premoderation   все три тоже через саппорт только
# cpc_quality -> premoderation
# common_quality -> premoderation

# когда уже на проверке, а катофы еще прилетают
# фатальное прилетело:
#   если cpa-фатальное, то не трогать cpc, но дропнуть light-cpa
#       Грубил по телефону. Находится на light-common. В это время прилетает cpc-fatal. Надо: включить только в cpa


def get_matirx(features):
    if len(features) > 0:
        for f_value in features[0].values:
            for row in get_matirx(features[1:]):
                new_row = [f_value] + list(row)
                yield new_row
    else:
        yield []


class T(unittest.TestCase):
    def test_(self):
        pass


def main():
    table = list(get_matirx(features))
    t = texttable.Texttable()
    t.header(['N'] + [f.name for f in features])
    for i, row in enumerate(table, 1):
        t.add_row([i] + row)
    print t.draw()

if __name__ == '__main__':
    if os.environ.get('UNITTEST') == '1':
        unittest.main()
    else:
        main()
