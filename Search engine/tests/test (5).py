import search.geo.tools.social_links.extract_dates.lib.dategram as dategram
import json
import yatest.common as yc


def test_unit():
    dgm = dategram.Dategram()
    output_path = yc.output_path("test_unit.out")
    simple = [
        "с 01.12.2015 по 30.08.2017",
        "с 5 марта 2005 года по 1 мая 2013 года",
        "с 5 марта по 28 февраля 2015 года",
        "с 2 января до 3 февраля",
        "5 мая - 01 июня",
        "с 18:30 до 20:00 часов",
        "18:40 - 19:20",
        "с 2 по 9 апреля 2008 года",
        "19 сентября с 19 до 23 ч",
        "с 5 по 15 августа",
        "в августе, с 8 по 13",
        "3 - 7 ноября",
        "в мае, с 14 до 20 часов",
        "19.05 с 18:30 до 19:30",
        "в 2016-2017 годах",
        "с 2.03.16",
        "с 4 апреля 2017 года",
        "до 25.05.17",
        "до 13 декабря 2015 года",
        "с 1 марта",
        "до 12 августа",
        "с начала ноября",
        "с октября",
        "до конца мая",
        "по июль",
        "до конца марта 2016 года",
        "по июнь 2015 года",
        "в понедельник",
        "каждую пятницу",
        "завтра",
        "сегодня",
        "послезавтра",
        "по будням",
        "в будние дни",
        "в будни",
        "с понедельника по четверг",
        "с четверга по пятницу",
        "С 21 мая. Мусор. Мусор. Мусор. До 1 июня."
    ]
    with open(output_path, "w") as w:
        for item in simple:
            w.write(item + " -> " + str(dgm.resolve_str(item, tsp=1504569600)) + '\n')
    return yc.canonical_file(output_path, local=True)


def test0():
    dgm = dategram.Dategram()
    with open('test0.in') as r:  # , open(ofile) as w:
        for line in r:
            print line.rstrip()
            print dgm.resolve_str(line)


def test_random():
    dgm = dategram.Dategram()
    with open('test_random.in') as r:  # , open(ofile) as w:
        for line in r:
            js = json.loads(line)
            print js['text'].encode('utf-8').rstrip()
            print dgm.resolve_str(js['text'].encode('utf-8'), js['timestamp'])


def test_human():
    dgm = dategram.Dategram()
    output_path = yc.output_path('test_human.out')
    with open('test_human.in', "r") as r, open(output_path, "w") as w:
        for line in r:
            js = json.loads(line)
            result = dgm.resolve_str(js['text'].encode('utf-8'), js['timestamp'])
            w.write(json.dumps({"text": js['text'].rstrip(), "result": result}, ensure_ascii=False).encode("utf-8") + "\n")
    return yc.canonical_file(output_path)
