#!/usr/bin/python

import json
import os
import imp


BLACK = '\033[0m'
YELLOW = '\033[93m'
GREEN = '\033[92m'
BLUE = '\033[94m'
RED = '\033[91m'


def json_to_str(js):
    return json.dumps(js, sort_keys=True, indent=2)


def get_json(name, flag=False):
    error = ''
    js = None
    try:
        with open(name + '.json', 'r') as file:
            js = json.load(file)
    except ValueError:
        error = 'Incorrect json!\n'
    except IOError:
        error = name + '.json not found\n'
    if flag:
        print(json_to_str(js))
    return js, error


def color_print(string, color):
    print color + string + BLACK,
    return string + ' '


def check_fields(val, fields):
    for field in fields:
        if val.get(field) is None:
            return field + ' not present '
    return ""


def check_path(path):
    if not path.exists(path):
        return 'wrong path ' + path
    return ''


def check_file(path):
    if not os.path.isfile(path):
        return 'there is no file on the path ' + path
    try:
        with open(path) as file:
            file.readlines()
    except Exception as ex:
        return "File" + path + " can't be opened:" + str(ex)
    return ''


def check_func(path, func):
    try:
        module = imp.load_source('module', path)
    except Exception as ex:
        return str(ex)
    if func not in vars(module):
        return "there is no function " + func + " in file " + path
    return ""


def add_parent(error, parent):
    return error + ' in\n' + json_to_str(parent)+'\n'


def check_map(maps, path):
    if not isinstance(maps, list):
        return add_parent('map is not list', maps)
    for map in maps:
        error = check_fields(map, ['path', 'name', 'output'])
        if error:
            return add_parent(error, map)
        for string in ['path', 'output', 'name']:
            error = check_str(map[string])
            if error:
                return add_parent(error, reduce)
        error = check_file(os.path.realpath(path + 'functions/' + map['path']))
        if error:
            return add_parent(error, map)
        error = check_func(path + 'functions/' + map['path'], map['name'])
        if error:
            return add_parent(error, map)
    return ''


def check_reduce(reduces, path):
    if not isinstance(reduces, list):
        return add_parent('reduce is not list', reduces)
    for reduce in reduces:
        error = check_fields(reduce, ['path', 'args', 'sensor_name', 'name'])
        if error:
            return add_parent(error, reduce)
        for string in ['path', 'sensor_name', 'name']:
            error = check_str(reduce[string])
            if error:
                return add_parent(error, reduce)
        error = check_file(os.path.realpath(path + 'functions/' + reduce['path']))
        if error:
            return add_parent(error, reduce)
        error = check_func(path + 'functions/' + reduce['path'], reduce['name'])
        if error:
            return add_parent(error, reduce)
    return ''


def check_str(string):
    if not isinstance(string, basestring):
        return str(string) + 'is not string '
    return ""


def check_list(val):
    if not isinstance(val, list):
        return str(val) + 'is not list '
    return ""


def check_dict(val):
    if not isinstance(val, dict):
        return str(val) + 'is not dictionary '
    return ""


def check_serp_tester(path=''):
    log = ""
    '''
        check config.json
    '''
    print("path = " + path)
    log += color_print(' ===test config.json===\n', BLUE)
    config, configError = get_json(path + 'config')
    names = []

    if not configError:
        goodConfTests = 0
        for idx, box in zip(range(len(config)), config):
            if idx:
                print'',
            print 'testing box ' + str(idx) + ':',
            error = check_fields(box, ['box', 'map', 'reduce', 'name', 'flags', 'owners'])

            if error:
                log += color_print('Error!\n' + add_parent(error, box), RED)
                continue

            for string in ['name', 'flags', 'box']:
                error = check_str(box[string])
                if error:
                    break

            if error:
                log += color_print('Error!\n' + add_parent(error, box), RED)
                continue

            if names.count(box['name']):
                log += color_print('Error!\n' + add_parent('Box name \'' + box['name'] + '\' is not unique!\n', box), RED)
                continue
            names.append(box['name'])

            error = check_map(box['map'], path)
            if error:
                log += color_print('Error!\n' + error, RED)
                continue

            error = check_reduce(box['reduce'], path)
            if error:
                log += color_print('Error!\n' + error, RED)
                continue

            error = check_file(path + 'boxes/' + box['box'])
            if error:
                log += color_print('Error!\n' + error + '\n', RED)
                continue
            error = check_list(box['owners'])
            if error:
                log += color_print('Error!\n Owners must be list' + add_parent(error, box) + '\n', RED)
                continue
            for owner in box['owners']:
                error = check_str(owner)
                if error:
                    break
            if error:
                log += color_print('Error! \n' + add_parent('owner ' + error, box), RED)
                continue

            goodConfTests += 1
            log += color_print("OK\n", GREEN)

        log += color_print('' + str(goodConfTests) + '/' + str(len(config)), BLUE)
        if goodConfTests == len(config):
            log += color_print(':OK\n', GREEN)
        else:
            log += color_print(':Error\n', RED)
    else:
        log += color_print(configError + '\n', RED)

    '''
        check functions tests
    '''

    log += color_print('===test function test(tests.json)===\n', BLUE)
    tests, testsError = get_json(path + 'tests')

    if not testsError:
        goodTests = 0
        for idx, test in zip(range(len(tests)), tests):
            if idx:
                print'',
            print ' checked ' + str(idx) + ' test',
            error = check_fields(test, ['path', 'func', 'tests'])
            if error:
                print ':',
                log += color_print('error in test:' + add_parent(error, test), RED)
                continue

            funcPath = os.path.realpath(path + 'functions/' + test['path'])
            print '(functions/' + test['path'] + ':' + test['func'] + '):',

            error = check_file(funcPath)
            if error:
                log += color_print('Error!' + add_parent(error, test), RED)
                continue
            error = check_func(funcPath, test['func'])
            if error:
                log += color_print('Error!' + add_parent(error, test), RED)
                continue
            module = imp.load_source('module.name', funcPath)
            func = vars(module)[test['func']]

            for subIdx, subTest in zip(range(len(test['tests'])), test['tests']):
                error = check_list(subTest)
                if error:
                    log += color_print('Syntax error in subtest ' + str(subIdx) + add_parent(error, subTest), RED)
                    continue

                if len(subTest) != 2:
                    log += color_print(add_parent('Syntax error in subtest must both params and correct answer' + str(subIdx), subTest), RED)
                    continue

                error = check_dict(subTest[0])
                if error:
                    log += color_print(add_parent('Syntax error in subtest ' + str(subIdx), subTest), RED)
                    continue

                try:
                    out = func(**subTest[0])
                    if json.dumps(out, sort_keys=True) != json.dumps(subTest[1], sort_keys=True):
                        error = 'WA ' + str(subIdx) + '\n'
                        error += '   input = ' + json_to_str(subTest[0]) + '\n'
                        error += '   output = ' + json_to_str(out) + '\n'
                        error += '   correct = ' + json_to_str(subTest[1]) + '\n'
                        break
                except Exception as ex:
                    error = 'RE ' + str(subIdx) + '\n'
                    error += '   Error = ' + str(ex) + '\n'
                    error += '   Input = ' + json_to_str(subTest[0]) + '\n'
                    break

            if error:
                log += color_print(error, RED)
                continue

            goodTests += 1
            log += color_print('OK\n', GREEN)

        log += color_print(str(goodTests) + '/' + str(len(tests)), BLUE)
        if goodTests == len(tests):
            log += color_print(':OK\n', GREEN)
        else:
            log += color_print(':Error\n', RED)
    else:
        log += color_print(configError + '\n', RED)

    '''
    check production_list.json
    '''

    log += color_print('===test production_list.json===\n', BLUE)
    prod, prodError = get_json(path + 'production_list')

    if not prodError:
        prodError = check_fields(prod, ['production', 'test'])
        if prodError:
            log += color_print('Error: ' + add_parent(prodError, prod), RED)
        for box in prod['production'] + prod['test']:
            if box not in names:
                prodError = 'there is no box ' + box + ' in config.json'
                break
        if prodError:
            log += color_print('Error!' + prodError, RED)

    if not prodError:
        log += color_print("OK\n", GREEN)
    else:
        log += color_print(configError + '\n', RED)

    '''
    RESULTS:::
    '''
    result = 0
    log += color_print("===Results:===\n", BLUE)
    log += color_print(" config.json:\n", BLUE)
    if configError:
        log += color_print("error\n" + configError + '\n', RED)
    else:
        if goodConfTests == len(config):
            log += color_print(' correct ' + str(goodConfTests) + ' boxes of ' + str(len(config)), GREEN)
            log += color_print('OK\n\n', GREEN)
            result += 1
        else:
            log += color_print(' correct ' + str(goodConfTests) + ' boxes of ' + str(len(config)), RED)
            log += color_print('Error\n\n', RED)

    log += color_print(" function test(tests.json):\n", BLUE)
    if testsError:
        log += color_print("error\n" + testsError + '\n', RED)
    else:
        if goodTests == len(tests):
            log += color_print(' correct ' + str(goodTests) + ' tests of ' + str(len(tests)), GREEN)
            log += color_print('OK\n\n', GREEN)
            result += 1
        else:
            log += color_print(' correct ' + str(goodTests) + ' tests of ' + str(len(tests)), RED)
            log += color_print('Error\n\n', RED)

    log += color_print(" test production_list:", BLUE)

    if prodError:
        log += color_print("error\n" + prodError + '\n', RED)
    else:
        log += color_print('OK\n', GREEN)
        result += 1

    if result == 3:
        log += color_print("==========ACCEPTED==========\n", GREEN)
        return log, True
    else:
        log += color_print("===========DECLINE===========\n", RED)
        return log, False


def test():
    from yatest import common
    msg, ok = check_serp_tester(common.source_path("search/tools/serp_tester_test/")+'/')
    assert ok

if __name__ == "__main__":
    check_serp_tester()
