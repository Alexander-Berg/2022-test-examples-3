# -*- coding: utf-8 -*-
from travel.rasp.admin.lib.logs import get_collector_context
from travel.rasp.admin.scripts.schedule.bus.dyc import RawGroup


pac_data = u'''
6350(701)\tВОЛОГДА\tБАБАЕВО\t298,9\t06:15\tПн,Вт,Чт,Вс\t12:45\t
\t\t\t\t\tСр,Пт\t16:20\t
'''

sma_data = u'''
701 (6350)\tВОЛОГДА\tБАБАЕВО\tМ/РАЙ\tМ/Г\tАСУДЮК\t
Класс\t1
Пункт     \tРасст  \tВремя \tПолный  \t Ск30%  \t Ск50% \t Багаж\t
ВОЛОГДА   \t    0,0\t00:00\t       0\t       0\t      0\t     0\t
ЛАПАЧ     \t   23,9\t00:24\t   56,00\t   39,00\t  28,00\t  8,00\t
СВЕТИЛОВО \t   47,1\t00:50\t  111,00\t   78,00\t  55,00\t 16,00\t
\t\
'''

pts_data = u'''
\tДЮК 5.9.1610M\t
701\t(6350)БАБАЕВО\tМ/Г\tМ/Р\t
 \t  \t
\tКЛАСС 1
0,0\tВОЛОГДА\tВОЛО\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
23,9\tЛАПАЧ\t56,00\tЛАПА\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t39,00\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t28,00\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t8,00\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
47,1\tСВЕТИЛОВО\t111,00\t54,00\tСВЕТ\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t78,00\t38,00\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t55,00\t27,00\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t
\t\t16,00\t8,00\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t

'''


def test_raw_group_simple_case(tmpdir):
    pac_file = tmpdir.join('pac.txt')
    sma_file = tmpdir.join('sma.txt')
    pts_file = tmpdir.join('pts.txt')

    with pac_file.open('wb') as f:
        f.write(pac_data.encode('cp1251'))
    with sma_file.open('wb') as f:
        f.write(sma_data.encode('cp1251'))
    with pts_file.open('wb') as f:
        f.write(pts_data.encode('cp1251'))

    raw_group = RawGroup('voltest', str(pac_file), [str(sma_file)], [str(pts_file)])
    raw_threads = list(raw_group.thread_iter())
    assert len(raw_threads) == 1


pac_data_route_error = u'''
6350(701)\tВОЛОГДА\tБАБАЕВО\t298,9\t06:15\tПн,Вт,Чт,Вс\t12:45\t
\t\t\t\t\tСр,Пт\t16:20\t
'''

sma_data_route_error = u'''
701 (6350)\tВОЛОГДА\tБАБАЕВО\tМ/РАЙ\tМ/Г\tАСУДЮК\t
Класс\t1
Пункт     \tРасст  \tВремя \tПолный  \t Ск30%  \t Ск50% \t Багаж\t
ВОЛОГДА СТРОКА 5  \t    0,0\t00:00\t       0\t       0\t      0\t     0\t
          \t   23,9\t00:24\t   56,00\t   39,00\t  28,00\t  8,00\t
СВЕТИЛОВО \t   47,1\t00:50\t  111,00\t   78,00\t  55,00\t 16,00\t
\t\
'''


def test_log_route_error(tmpdir):
    pac_file = tmpdir.join('pac.txt')
    sma_file = tmpdir.join('sma.txt')
    pts_file = tmpdir.join('pts.txt')

    with pac_file.open('wb') as f:
        f.write(pac_data_route_error.encode('cp1251'))
    with sma_file.open('wb') as f:
        f.write(sma_data_route_error.encode('cp1251'))
    with pts_file.open('wb') as f:
        f.write('')

    with get_collector_context('') as collector:
        raw_group = RawGroup('voltest', str(pac_file), [str(sma_file)], [str(pts_file)])
        raw_threads = list(raw_group.thread_iter())

        assert len(raw_threads) == 1

        lines = collector.get_collected().splitlines()
        log_error_index = [i for i, l in enumerate(lines) if l.count(u'при разборе заголовка маршрута')][0]

        assert u'строка 7' in lines[log_error_index]
        assert u'СВЕТИЛОВО' in lines[log_error_index + 1]


pac_data_stop_error = u'''
6350(701)\tВОЛОГДА\tБАБАЕВО\t298,9\t06:15\tПн,Вт,Чт,Вс\t12:45\t
\t\t\t\t\tСр,Пт\t16:20\t
'''

sma_data_stop_error = u'''
701 (6350)\tВОЛОГДА\tБАБАЕВО\tМ/РАЙ\tМ/Г\tАСУДЮК\t
Класс\t1
Пункт     \tРасст  \tВремя \tПолный  \t Ск30%  \t Ск50% \t Багаж\t
ВОЛОГДА СТРОКА 5  \t    0,0\t00:00\t       0\t       0\t      0\t     0\t
ЛАПАЧ     \t   23,9
СВЕТИЛОВО \t   47,1\t00:50\t  111,00\t   78,00\t  55,00\t 16,00\t
\t\
'''


def test_log_stop_error(tmpdir):
    pac_file = tmpdir.join('pac.txt')
    sma_file = tmpdir.join('sma.txt')
    pts_file = tmpdir.join('pts.txt')

    with pac_file.open('wb') as f:
        f.write(pac_data_stop_error.encode('cp1251'))
    with sma_file.open('wb') as f:
        f.write(sma_data_stop_error.encode('cp1251'))
    with pts_file.open('wb') as f:
        f.write('')

    with get_collector_context('') as collector:
        raw_group = RawGroup('voltest', str(pac_file), [str(sma_file)], [str(pts_file)])
        raw_threads = list(raw_group.thread_iter())

        assert len(raw_threads) == 0

        lines = collector.get_collected().splitlines()
        log_error_index = [i for i, l in enumerate(lines) if l.count(u'при разборе остановки')][0]

        assert u'строка 6' in lines[log_error_index]
        assert u'ЛАПАЧ' in lines[log_error_index + 1]
