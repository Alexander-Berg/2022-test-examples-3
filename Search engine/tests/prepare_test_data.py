#!/usr/bin/env python
# -*- coding: utf-8 -*-

from collections import defaultdict
import json
import io


def main():
    TESTS_COUNT_BY_TYPE = 1
    types_count = defaultdict(int)
    out_actual = []
    out_bad_links = []

    with open('raw_data') as data:
        for line in data.readlines():
            row = json.loads(line)
            profile = row['profile_json']
            origin = 'vk'
            if 'facebook.com' in row['profile_url']:
                origin = 'fb'
            if not profile['posts']:
                if types_count[origin + 'empty'] < TESTS_COUNT_BY_TYPE:
                    types_count[origin + 'empty'] += 1
                    out_actual.append(row)
            elif len(profile['posts']) == 1:
                if types_count[origin + 'one_bad'] < TESTS_COUNT_BY_TYPE:
                    types_count[origin + 'one_bad'] += 1
                    out_actual.append(row)
                    out_bad_links.append(profile['posts'][0]['url'])
                elif types_count[origin + 'one_good'] < TESTS_COUNT_BY_TYPE:
                    types_count[origin + 'one_good'] += 1
                    out_actual.append(row)
            else:
                if types_count[origin + 'many_bad'] < TESTS_COUNT_BY_TYPE:
                    types_count[origin + 'many_bad'] += 1
                    out_actual.append(row)
                    for post in profile['posts']:
                        out_bad_links.append(post['url'])
                elif types_count[origin + 'many_part'] < TESTS_COUNT_BY_TYPE:
                    types_count[origin + 'many_part'] += 1
                    out_actual.append(row)
                    for i, post in enumerate(profile['posts']):
                        if i % 2 == 1:
                            out_bad_links.append(post['url'])
                elif types_count[origin + 'many_good'] < TESTS_COUNT_BY_TYPE:
                    types_count[origin + 'many_good'] += 1
                    out_actual.append(row)
                elif types_count[origin + 'wrong_links'] < TESTS_COUNT_BY_TYPE:
                    types_count[origin + 'wrong_links'] += 1
                    out_bad_links.append(profile['posts'][0]['url'])

    with io.open('input_data/actual_profiles.json', 'w', encoding='utf8') as f:
        data = json.dumps(out_actual,
                          ensure_ascii=False,
                          sort_keys=True,
                          indent=2)
        f.write(unicode(data))

    bad_links = [{'key': 'host', 'value': x} for x in out_bad_links]

    json.dump(bad_links,
              open('input_data/bad_links.json', 'w'),
              sort_keys=True,
              indent=2)

if __name__ == '__main__':
    main()
