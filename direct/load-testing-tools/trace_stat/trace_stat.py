#!/usr/bin/env python
# -*- coding: utf-8 -*-

from trace_format import Trace
import sys
import argparse

reload(sys)  
sys.setdefaultencoding('utf8')

def create_parser():
    parser = argparse.ArgumentParser(description='Скрипт анализа trace логов', formatter_class=argparse.RawDescriptionHelpFormatter, 
            epilog='''
            Скрипт умеет выводить суммарную статистику по профайлам и сравнивать трейслоги по информации в профайлах.
            Примеры использования:
            trace_stat - считаем статистику на основании информации из stdin
            trace_stat trace.log - получить суммарную статистику о про трейс логу
            trace_stat trace1.log trace2.log - расхождение по статистике между трейслогом 1 и 2 
                   ''')
    parser.add_argument('-a', '--avg', help='считать статистику в среднем на запрос', action='store_true')
    parser.add_argument('-p1', '--period1', help='период времени за который собираем статистику из первого лога в формате  от hh.mm.ss:hh.mm.ss, начало и конец опциональны, по дефолту от начала до конца')
    parser.add_argument('-p2', '--period2', help='период времени за который собираем статистику из второго лога в формате  от hh.mm.ss:hh.mm.ss, начало и конец опциональны, по дефолту от начала до конца')
    parser.add_argument('-m', '--method', help='метод по которому отбираем статистику')
    parser.add_argument('file1', nargs='?', type=argparse.FileType('r'), default=sys.stdin)
    parser.add_argument('file2', nargs='?', type=argparse.FileType('r'))
    return parser

def get_stat(lines, period, method, avg):
    result_stat = {}
    count = 0

    periods = period.split(':') if period else ['', '']
    start_time = parse_time(periods[0]) if len(periods[0]) != 0 else float('-inf')
    stop_time =  parse_time(periods[1]) if len(periods[1]) != 0 else float('inf')
    for line in lines:
        try:
            prefix, trace_str = '', line
            trace = Trace.decode(trace_str)
            request_time = parse_time('.'.join( str(x) for x in [trace.span_start.hour, trace.span_start.minute, trace.span_start.second]))
            if (method and trace.method != method) or request_time < start_time or request_time > stop_time:
                continue
            count += 1
            for profile in trace.profiles:
                name = profile.func + ("" if not profile.tags else "/" + profile.tags)
                result_stat[name] = result_stat.get(name, 0) + profile.all_ela
        except Exception as e:
            print(str(e))
            sys.stdout.write(line)
    if avg:
        for key in result_stat:
            result_stat[key] = result_stat[key] / float(count)
    return (count, result_stat)

def parse_time(time_str):
    try:
        hours, minutes, seconds = [0 if len(t) == 0 else int(t) for t in time_str.split('.')]
        return hours * 3600 + minutes * 60 + seconds 
    except Exception as e:
        print('Ошибка формата периода:"%s" - %s' % (time_str, e.message))

def print_stat(stat, count):
    total_time = 0
    for key in sorted(stat.keys()):
        print('%s: %0.2f секунд' % (key, stat[key]))
        total_time += stat[key]
    print('\nИтого: %0.2f секунд %d запросов' % (total_time, count)) 

def print_diff(stat1, count1, stat2, count2):
    common_keys = set(stat1.keys()).intersection(set(stat2.keys()))
    print('Общие профайлы\n')
    print('%-70s %15s %15s %10s' % ('Profile name', 'file1', 'file2', 'diff'))
    for key in common_keys:
        diff = stat2[key] - stat1[key]
        print('%-70s %15.2fs %15.2fs %+10.2f(%+.2f%%)' % (key, stat1[key], stat2[key], diff, diff/float(stat1[key]) * 100))
    print("=" * 150) 
    print('file1 %d запросов' % count1)
    print('file2 %d запросов' % count2)
    print("=" * 150) 
    print_local_profiles('file1', common_keys, stat1)
    print_local_profiles('file2', common_keys, stat2)

def print_local_profiles(file_name, common_keys, stat):
    diff_set = set(stat).difference(common_keys)
    if len(diff_set):
        print('Дополнительные профайлы %s\n' % file_name)
        print('%-70s %15s' % ('Profile name', file_name))
        for key in diff_set:
            print('%-70s %15.2fs' % (key, stat[key]))

params = create_parser().parse_args(sys.argv[1:])
(count1, stat1) = get_stat(params.file1.readlines(), params.period1, params.method, params.avg)

if params.file2:
    (count2, stat2) = get_stat(params.file2.readlines(), params.period2, params.method, params.avg)
    print_diff(stat1, count1, stat2, count2)
else:
    print_stat(stat1, count1)
