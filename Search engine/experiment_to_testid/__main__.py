#!/usr/bin/env python
# coding=utf-8
import json
import sys
import urllib

if __name__ == '__main__':
    exp = sys.argv[1]
    testids = json.loads(urllib.urlopen('https://ab.yandex-team.ru/api/task/%s' % exp).read())['testids']
    output = {'testids': map(int, testids)}
    print json.dumps(output)
