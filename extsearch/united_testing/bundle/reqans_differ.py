#!/usr/bin/python3.6

import os
import sys
import json


def extra_handler(value, result, delim, equality_delim, sub_delim=''):
    subitems = value.split(delim)
    for subitem in subitems:
        try:
            [subkey, subvalue] = subitem.split(equality_delim, 1)
            if sub_delim:
                result[subkey] = dict()
                extra_handler(subvalue, result[subkey], sub_delim, '=')
            else:
                result[subkey] = subvalue
        except:
            result[subitem] = ''


def reqans2json(dir, filename):
    with open(os.path.join(dir, filename)) as f_in:
        if filename.endswith('left'):
            f_in.readline()
        raw = json.load(f_in)

    reqans = raw['answers'][0]['data'].split('\n')[1].split('@@')
    result = dict()
    for item in reqans:
        [key, value] = item.split('=', 1)
        if key in ['rearr', 'reqrelev']:
            result[key] = dict()
            extra_handler(value, result[key], ';', '=')
        elif key in ['search_props']:
            result[key] = dict()
            extra_handler(value, result[key], ';', ':', sub_delim=',')
        else:
            result[key] = value
    with open(os.path.join(dir, 'reqans_'+filename), 'w+') as f_out:
        json.dump(result, f_out, sort_keys=True, indent=2)


def read_responses():
    i = 0
    while os.path.isfile(os.path.join(sys.argv[1], str(i)+'_left')) and os.path.isfile(os.path.join(sys.argv[1], str(i)+'_right')):
        reqans2json(sys.argv[1], str(i)+'_left')
        reqans2json(sys.argv[1], str(i)+'_right')
        i += 1


if __name__ == '__main__':
    read_responses()
