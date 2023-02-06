#!/usr/bin/env python2
# -*- coding: utf-8 -*-

from __future__ import unicode_literals, print_function

import datetime
import os
import copy
import time

import videoreport_tests


class ReportAutotestsError(Exception): pass


class RunTests():
    """
    Класс для запуска всех тестов.
    """

    def __init__(self, paramdict, beta_url, prod_url=None, failiters=3):
        self.failiters = failiters
        for key in paramdict:
            setattr(self, key, paramdict[key])
        # Заполняем необязательные параметры, которых может не быть в конфиге:
        for attr in (("view_path", ("",)), ("headers", {})):
            if not hasattr(self, attr[0]):
                setattr(self, *attr)
        # Список доменов и собранных урлов вида [{домен1: урл для домена}, {домен2: урл для домена}]
        self.beta_dict = self.url_string(beta_url)
        if prod_url:
            self.prod_dict = self.url_string(prod_url)

    def address(self, domain, url, prefix=None):
        """
        Формирование основной части строки адреса.
        """
        if prefix:
            url = "https://" + prefix + "." + url
        else:
            url = "https://" + url
        url += "." + domain
        return url

    def url_string(self, url):
        """
        Формирует базовую часть адресной строки для каждого домена.
        Возвращает словарь вида {домен: (строка адреса, {дополнительные параметры})}.
        """
        if type(self.service) in (tuple, list):
            prefix = self.service[0]
            postfix = self.service[1]
        else:
            prefix = ""
            postfix = self.service
        url_list = []   # Будет список словарей, в каждом словаре - набор готовых ссылок, по ссылке на домен.
        for p in self.queries:
            url_dict = {}
            for d in self.domains:
                temp_params = self.optional_parameter.copy()
                if type(p) in (tuple, list):
                    temp_params.update(p[0])
                    # {"ru": ("https://beta.yandex.ru/video/search", {"params": {params}, "cookies": {"cookie_name": "cookie_value", ...}, {headers:{}}), "by": (), ...}
                    url_dict[d] =  ("{0}/{1}".format(self.address(d, url, prefix=prefix), postfix), {"params": temp_params, "cookies": p[1], "headers": self.headers})
                else:
                    temp_params.update(p)
                    # {"ru": ("https://beta.yandex.ru/video/search", {params}), "by": (), ...}
                    url_dict[d] =  ("{0}/{1}".format(self.address(d, url, prefix=prefix), postfix), {"params": temp_params, "headers": self.headers})
            url_list.append(url_dict)
        return url_list

    def comparison_test(self):
        """
        Тест на сравнение данных.
        """
        # Удаляем файл журнала. Если всё будет ок, то он и не появится.
        self.remove_log(self.log)
        final_result = {}
        bugs_list = []  # Список ошибок без урлов. Используется для исключения повторов.
        for number, item in enumerate(self.beta_dict):
            for key in item:
                # Инициализируем класс теста сравнения.
                run = videoreport_tests.Comparison()
                # Запускаем сам тест, передавая ему адрес прода, список исключаемых веток json и список тех веток, где смотреть:
                result = run.run(self.prod_dict[number][key], item[key], self.blacklist,
                                      self.view_path, self.failiters)
                # Кусок кода, в котором исключаем все повторы:
                if len(result) > 0:
                    for i in result:
                        for message in i[1]:
                            if message not in bugs_list:
                                if i[0] not in final_result:
                                    final_result[i[0]] = [message]
                                else:
                                    final_result[i[0]].append(message)
                                bugs_list.append(message)
        if len(final_result) > 0:
            self.write_result(self.html(final_result))

    def remove_log(self, logname):
        """
        Удаление файла без сообщений об ошибках.
        :param logname:
        :return:
        """
        try:
            os.remove(logname)
        except (IOError, OSError):
            pass

    def html(self, content):
        """
        Функция создаёт табличную страницу HTML из пар "{Адрес: [текст, текст1, ..]}".
        :param content: Dictionary of lists.
        :return: Text (html).
        """
        title = self.log[:-5]
        begin = """
        <!doctype html>\n
        <html>\n
            <head>\n
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">\n
        	    <title>{0}</title>\n
        	</head>\n
            <body>\n
                <table border="1">\n
                <caption>{1}</caption>\n
        """.format(title, self.current_time())
        middle = '\n'.join(["<tr><td>{0}</td><td><pre>{1}</pre></td></tr>".format(i, "\n".join(content[i])) for i in content])
        end = """
                </table>\n
            </body>\n
        </html>\n"""
        return begin + middle + end

    def current_time(self):
        """
        Возвращает строчку с текущим временем для записи в начало лога.
        :param text:
        :param filename:
        :return:
        """
        t = datetime.datetime.now()
        return "Test end time: %s" % t.strftime("%d.%m.20%y %H:%M:%S")

    def write_result(self, text):
        """
        Запись результата text в файл filename.
        """
        try:
            with open(self.log, "a") as file:
                file.write(text.encode('utf-8'))
        except IOError:
            raise ReportAutotestsError("Unable to write log file!")


def base_test_compare(betahost, templatehost, settings_dict):
    result = copy.deepcopy(global_config)
    result.update(settings_dict)
    # Добавляем в словарь настроек дополнительные cgi-параметры, если такие есть в командной строке:
    if len(result["cgi"]) != 0:
        for param in result["cgi"].split(","):
            key = param.strip().split("=", 1)
            result["optional_parameter"][key[0]] = key[1]
            #result["log"] = "cgi_" + result["log"]
    del(result["cgi"])
    # Инициализируем класс, отвечающий за запуск тестов:
    run = RunTests(result, betahost, templatehost, fail_iterations)
    # Запускаем сравнительный тест:
    run.comparison_test()
    # Фейлим тест, если находим файл журнала:
    assert result["log"] not in os.listdir(os.curdir)


##### Описание параметров конфигурации #####
### Общие параметры: ###
## fail_iterations: количество повторов, если тест фейлится. Речь о конкретном запросе, а не о всём списке. Число.
##
### Отдельные параметры для каждого запуска (собираются в словарь с произвольным именем): ###
## domains: домены, на которых будет проводиться тест. Кортеж строк.
## service: сервис, включая путь (всё, что после домена и до "?", т.е. параметров). Строка или кортеж строк.
##          Если кортеж, то параметра может быть два, тогда первый добавляется перед адресом, а второй - после.
## optional_parameter: дополнительные CGI-параметры. Словарь.
## queries: запросы, которые будут сделаны в рамках теста. Кортеж словарей или кортежей, состоящих из двух словарей.
##          Если значение - кортеж, то первый словарь в нём - запросы, а второй - куки.
##          Пустой словарь означает пустой запрос.
## log: имя файла журнала. Строка.
## view_path: ветки в json, с которых начинается просмотр. Кортеж строк. Необязательный параметр, по умолчанию - корень.
##          Если требуется указать корень вручную, то он должен быть пустой строкой: ("",)
##          Если параметр будет заканчиваться на ".related", то будет запущен механизм проверки похожих роликов.
## blacklist: набор путей в json, которые будут игнорироваться при рекурсивном сравнении json с образцом.
##          Если требуется указать путь внутри переменной (элемента списка),
##          такую переменную заменять звёдочкой. Путь задавать относительно view_path! Кортеж строк.
## headers: набор того, что можно отправить в качестве заголовка HTTP (напр., юзерагент). Словарь. Необязательный параметр.


def test_top(betahost, templatehost, cgi):
    """Морда."""
    config = {
        "queries": ({},
                    ({}, {"my": "YycCAAIA"}),
                    ({}, {"my": "YycCAAEA"}),
                    ({}, {"my": "YycCAAUA"}),
                    ({}, {"my": "YycCAAQA"}),
                    {"category": "vtm", "block": "top"},
                    {"category": "vts", "block": "top"},
                    {"category": "vth", "block": "top"},
                    {"category": "vtn", "block": "top"},
                    {"category": "vtf", "block": "top"},
                    {"category": "vtc", "block": "top"},
                    {"category": "vtp", "block": "top"},
                    {"category": "vtt", "block": "top"},
                    {"block": "top", "p": 1},
                    {"block": "top", "p": 2}
        ),
        "log": "log_top.html",
        "blacklist": global_config["blacklist"] + ("i18n", "banner", "rdat.http_host", "rdat.url", "links.abuse",
                  "highlighter", "search_props", "search", "unanswer_data",
                  "reqparam.wizard", "top.films.*.raw_json", "blocks.contents.top.films.*.raw_json"
        ),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp(betahost, templatehost, cgi):
    """Выдача."""
    config = {
        "service": "video/search",
        "queries": ({'text': 'tesla'},
                    {'text': 'scorpions'},
                    {'text': 'плонеты салнечной сестемы'},
                    {'text': 'териер'},
                    {'text': 'Глухарь'},
                    {'text': ''},
                    {'text': 'doctor who'},
                    {'text': '[$@]'},
                    {'text': 'какжеяусталотручныхтестовхочуавтотесты'},
                    {'text': '[№%]'},
                    {'text': 'zyltrc'},
                    {'text': 'фззду'},
                    {'text': 'kedi'},
                    {'text': 'тойка'},
                    {'text': 'sexxx'},
                    {'text': 'азиатк'},
                    {'text': 'шкальница'},
                    {'text': 'мстители'},
                    {'text': 'секс', 'family': 'none'},
                    {'text': 'секс', 'family': 'moderate'},
                    {'text': 'секс', 'family': 'strict'},
                    {'text': 'очко', 'family': 'none'},
                    {'text': 'очко', 'family': 'moderate'},
                    {'text': 'bmw', 'p': 1},
                    {'text': 'bmw', 'p': 2},
                    {'text': 'mercedes', 'hd': '1'},
                    {'text': 'mercedes', 'within': '9'},
                    {'text': 'mercedes', 'duration': 'long'},
                    {'text': 'mercedes', 'duration': 'short'},
                    {'text': 'mercedes', 'duration': 'medium'},
                    {'text': 'song', 'hd': '1', 'duration': 'long'},
                    {'text': 'song', 'hd': '1', 'duration': 'medium'},
                    {'text': 'song', 'hd': '1', 'duration': 'short'},
                    {'text': 'lincoln', 'how': 'tm'},
                    {'text': 'song', 'hd': '1', 'how': 'tm', 'duration': 'short'},
                    {'text': 'авиабилеты'},
                    {'text': 'market'},
                    {'text': 'hava durumu'},
                    {"text": "очко", "family": "strict"},
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "школьница"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "школьница"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "школьница"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({'text': 'test'}, {"my": "YycCAAIA"}),
                    ({'text': 'test'}, {"my": "YycCAAEA"}),
                    ({'text': 'test'}, {"my": "YycCAAUA"}),
                    ({'text': 'test'}, {"my": "YycCAAQA"}),
                    {'text': 'abba', 'exp_flags': 'video_top_tracks'},
                    {'text': 'madonna', 'exp_flags': 'video_top_tracks'},
        ),
        "log": "log_serp.html",
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp_com_tr_spec(betahost, templatehost, cgi):
    """Специфические запросы для com.tr"""
    config = {
        "service": "video/search",
        "domains": ("com.tr",),
        "queries": ({'text': 'фильмы', 'rearr': 'scheme_Local/FreshVideoClassifier/ForceIntentWeight=0.6'},
                    {'text': 'путин', 'rearr': 'scheme_Local/FreshVideoClassifier/ForceIntentWeight=0.6'},
                    {'text': 'свежие новости в мире', 'rearr': 'scheme_Local/FreshVideoClassifier/ForceIntentWeight=0.6'}
        ),
        "log": "log_serp_com_tr_spec.html",
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp_ru_spec(betahost, templatehost, cgi):
    """Специфические запросы для ru"""
    config = {
        "service": "video/search",
        "domains": ("ru",),
        "queries": ({'text': 'фильмы', 'rearr': 'scheme_Local/Vertical/dgq/IntentWeight=%220.55%22'},
                    {'text': 'путин', 'rearr': 'scheme_Local/Vertical/dgq/IntentWeight=%220.55%22'},
                    {'text': 'свежие новости в мире', 'rearr': 'scheme_Local/Vertical/dgq/IntentWeight=%220.55%22'},
                    {'text': 'test', 'srcask': "VIDEO"},
                    {'text': 'test', 'srcask': "VIDEOQUICK"},
                    {'text': 'test', 'srcskip': "VIDEO"},
                    {'text': 'test', 'srcskip': "VIDEOQUICK"}
        ),
        "log": "log_serp_ru_spec.html",
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_xml_morda(betahost, templatehost, cgi):
    """Проверка ручки xml для морды"""
    config = {
        "service": "search/xml",
        "queries": (({'type': 'video', 'vtop': '1',
                     # Костыляем, чтобы всегда принудительно добавлялся json_dump с нужным значением:
                     'json_dump': 'yandexsearch', 'export': ''},
                     {'Session_id': session_cookie}
                     ),
                   ),
        "log": "log_xml_morda.html",
        "blacklist": global_config["blacklist"] + ("yandexsearch.response.results.grouping.*.group.*.doc.*.properties._SearcherHostname",
                                                   "yandexsearch.response.results.grouping.*.group.*.doc.*.properties._MetaSearcherHostname",
                                                   "yandexsearch.response.results.grouping. *.group. *.doc. *.properties._MimeType"
                                                   ),
        "cgi": cgi
              }
    base_test_compare(betahost, templatehost, config)

def test_aile(betahost, templatehost, cgi):
    """Aile."""
    config = {
        "domains": ("com.tr",),
        "service": ("aile", "video/search"),
        "queries": (({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    {"text": "mercedes", "hd": "1"},
                    {"text": "очко", "family": "none"}
        ),
        "log": "log_aile.html",
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_aile_touch(betahost, templatehost, cgi):
    """Тачёвый aile."""
    config = {
        "domains": ("com.tr",),
        "service": ("aile", "video/touch/search"),
        "queries": (({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    {"text": "очко", "family": "none"}
        ),
        "log": "log_aile_touch.html",
        "headers": {"User-Agent": "Mozilla/5.0 (Linux; U; Android 4.0; en-us; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"},
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_aile_pad(betahost, templatehost, cgi):
    """Aile падов."""
    config = {
        "domains": ("com.tr",),
        "service": ("aile", "video/pad/search"),
        "queries": (({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    {"text": "очко", "family": "none"}
        ),
        "log": "log_aile_pad.html",
        "headers": {"User-Agent": "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"},
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_landings1(betahost, templatehost, cgi):
    """Landing pages - yerli."""
    config = {
        "domains": ("com.tr",),
        "service": "video/yerli-dizi-izle",
        "queries": ({},
        ),
        "log": "log_landings1.html",
        "blacklist": global_config["blacklist"] + ("rdat.http_host", "rdat.url"),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_landings2(betahost, templatehost, cgi):
    """Landing pages - yabanci."""
    config = {
        "domains": ("com.tr",),
        "service": "video/yabanci-dizi-izle",
        "queries": ({},
        ),
        "log": "log_landings2.html",
        "blacklist": global_config["blacklist"] + ("rdat.http_host", "rdat.url"),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_landings3(betahost, templatehost, cgi):
    """Landing pages - dizi-izle."""
    config = {
        "domains": ("com.tr",),
        "service": "video/dizi-izle",
        "queries": ({},
        ),
        "log": "log_landings3.html",
        "blacklist": global_config["blacklist"] + ("rdat.http_host", "rdat.url"),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_db(betahost, templatehost, cgi):
    """Запросы к БД (напр., музыкальные табики)."""
    config = {
        "service": "video/db",
        "queries": ({"artist_id": "1813"},
                    {"artist_id": "9367"},
                    {"artist_id": "90"}
        ),
        "log": "log_db.html",
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_top_touch(betahost, templatehost, cgi):
    """Морда тачей."""
    config = {
        "service": "video/touch",
        "queries": ({},
                    {"category": "vtm", "block": "top"},
                    {"category": "vts", "block": "top"},
                    {"category": "vth", "block": "top"},
                    {"category": "vtn", "block": "top"},
                    {"category": "vtf", "block": "top"},
                    {"category": "vtc", "block": "top"},
                    {"category": "vtp", "block": "top"},
                    {"category": "vtt", "block": "top"},
                    {"block": "top", "p": 1},
                    {"block": "top", "p": 6}
        ),
        "log": "log_top_touch.html",
        "blacklist": global_config["blacklist"] + ("i18n", "banner", "rdat.http_host", "rdat.url", "links.abuse",
                      "highlighter", "search_props", "search", "unanswer_data",
                      "reqparam.wizard", "top.films.*.raw_json", "blocks.contents.top.films.*.raw_json"
        ),
        "headers": {"User-Agent": "Mozilla/5.0 (Linux; U; Android 4.0; en-us; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"},
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp_touch(betahost, templatehost, cgi):
    """Выдача тачей."""
    config = {
        "service": "video/touch/search",
        "queries": ({'text': 'tesla'},
                    {'text': 'плонеты салнечной сестемы'},
                    {'text': 'Глухарь'},
                    {'text': ''},
                    {'text': 'doctor who'},
                    {'text': '[$@]'},
                    {'text': 'какжеяусталотручныхтестовхочуавтотесты'},
                    {'text': '[№%]'},
                    {'text': 'тойка'},
                    {'text': 'kedi'},
                    {'text': 'sexxx'},
                    {'text': 'азиатк'},
                    {'text': 'мстители'},
                    {'text': 'bmw', 'p': 1},
                    {'text': 'bmw', 'p': 6},
                    {"text": "очко", "family": "strict"},
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    {"text": "host:www.pirsushaber.com"}
        ),
        "log": "log_serp_touch.html",
        "headers": {"User-Agent": "Mozilla/5.0 (Linux; U; Android 4.0; en-us; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"},
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_top_pad(betahost, templatehost, cgi):
    """Морда падов."""
    config = {
        "service": "video/pad",
        "queries": ({},
                    ({}, {"my": "YycCAAIA"}),
                    ({}, {"my": "YycCAAEA"}),
                    ({}, {"my": "YycCAAUA"}),
                    ({}, {"my": "YycCAAQA"}),
                    {"category": "vtm", "block": "top"},
                    {"category": "vts", "block": "top"},
                    {"category": "vth", "block": "top"},
                    {"category": "vtn", "block": "top"},
                    {"category": "vtf", "block": "top"},
                    {"category": "vtc", "block": "top"},
                    {"category": "vtp", "block": "top"},
                    {"category": "vtt", "block": "top"},
                    {"block":"top", "p": 1},
                    {"block":"top", "p": 6}
        ),
        "log": "log_top_pad.html",
        "blacklist": global_config["blacklist"] + ("i18n", "banner", "rdat.http_host", "rdat.url", "links.abuse",
                      "highlighter", "search_props", "search", "unanswer_data",
                      "reqparam.wizard", "top.films.*.raw_json", "blocks.contents.top.films.*.raw_json"
        ),
        "headers": {"User-Agent": "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"},
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp_pad(betahost, templatehost, cgi):
    """Выдача падов."""
    config = {
        "service": "video/pad/search",
        "queries": ({'text': 'авиабилеты'},
                    {'text': 'market'},
                    {'text': 'hava durumu'},
                    {"text": "очко", "family": "strict"},
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    {"text": "host:www.pirsushaber.com"},
                    ({'text': 'test'}, {"my": "YycCAAIA"}),
                    ({'text': 'test'}, {"my": "YycCAAEA"}),
                    ({'text': 'test'}, {"my": "YycCAAUA"}),
                    ({'text': 'test'}, {"my": "YycCAAQA"}),
                    {'text': 'мстители'},
                    {'text': 'kedi'}
        ),
        "log": "log_serp_pad.html",
        "headers": {"User-Agent": "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"},
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_top_related(betahost, templatehost, cgi):
    """Похожие на морде."""
    config = {
        "queries": ({},
        ),
        "log": "log_top_related.html",
        "blacklist": global_config["blacklist"] + ("i18n", "banner", "rdat.http_host", "rdat.url", "links.abuse",
                      "highlighter", "search_props", "search", "unanswer_data",
                      "reqparam.wizard", "top.films.*.raw_json", "blocks.contents.top.films.*.raw_json"
        ),
        "view_path": ("blocks.contents.top.films.3.extra.related",),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp_related(betahost, templatehost, cgi):
    """Похожие на выдаче."""
    config = {
        "service": "video/search",
        "queries": ({'text': 'scorpions'},
                    {'text': 'bmw', 'p': 1},
                    {'text': 'bmw', 'p': 6},
                    {'text': 'bmw', 'hd': '1'},
                    {'text': 'bmw', 'within': '9'},
                    {'text': 'bmw', 'duration': 'long'},
                    {'text': 'bmw', 'duration': 'short'},
                    {'text': 'bmw', 'duration': 'medium'},
                    {'text': 'song', 'hd': '1', 'duration': 'long'},
                    {'text': 'song', 'hd': '1', 'duration': 'medium'},
                    {'text': 'song', 'hd': '1', 'duration': 'short'},
                    {'text': 'lincoln', 'how': 'tm'},
                    {'text': 'song', 'hd': '1', 'how': 'tm', 'duration': 'short'},
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "секс"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "очко"}, {"Cookie": "yp=1751816838.sp.family%3A2"}),
                    ({"text": "школьница"}, {"Cookie": "yp=1751816838.sp.family%3A0"}),
                    ({"text": "школьница"}, {"Cookie": "yp=1751816838.sp.family%3A1"}),
                    ({"text": "школьница"}, {"Cookie": "yp=1751816838.sp.family%3A2"})
        ),
        "log": "log_serp_related.html",
        "view_path": ("searchdata.clips.3.extra.related",),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_top_pad_related(betahost, templatehost, cgi):
    """Похожие на морде падов."""
    config = {
        "service": "video/pad",
        "queries": ({},
        ),
        "log": "log_top_pad_related.html",
        "blacklist": global_config["blacklist"] + ("i18n", "banner", "rdat.http_host", "rdat.url", "links.abuse",
                      "highlighter", "search_props", "search", "unanswer_data",
                      "reqparam.wizard", "top.films.*.raw_json", "blocks.contents.top.films.*.raw_json"
        ),
        "headers": {"User-Agent": "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"},
        "view_path": ("blocks.contents.top.films.3.extra.related",),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp_pad_related(betahost, templatehost, cgi):
    """Похожие на выдаче падов."""
    config = {
        "service": "video/pad/search",
        "queries": ({'text': 'авиабилеты'},
        ),
        "log": "log_serp_pad_related.html",
        "headers": {"User-Agent": "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53"},
        "view_path": ("searchdata.clips.3.extra.related",),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_top_touch_related(betahost, templatehost, cgi):
    """Похожие на морде тачей."""
    config = {
        "service": "video/touch",
        "queries": ({},
                    {"block":"top", "p": 6}
        ),
        "log": "log_top_touch_related.html",
        "blacklist": global_config["blacklist"] + ("i18n", "banner", "rdat.http_host", "rdat.url", "links.abuse",
                      "highlighter", "search_props", "search", "unanswer_data",
                      "reqparam.wizard", "top.films.*.raw_json", "blocks.contents.top.films.*.raw_json"
        ),
        "headers": {"User-Agent": "Mozilla/5.0 (Linux; U; Android 4.0; en-us; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"},
        "view_path": ("blocks.contents.top.films.3.extra.related",),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_serp_touch_related(betahost, templatehost, cgi):
    """Похожие на выдаче тачей."""
    config = {
        "service": "video/touch/search",
        "queries": ({'text': 'tesla'},
                    {'text': 'bmw', 'p': 6}
        ),
        "log": "log_serp_touch_related.html",
        "headers": {"User-Agent": "Mozilla/5.0 (Linux; U; Android 4.0; en-us; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"},
        "view_path": ("searchdata.clips.3.extra.related",),
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

def test_favorites(betahost, templatehost, cgi):
    """Мои видео."""
    config = {
        "service": "video/favorites",
        "queries": (({'exp_flags': 'video_enable_https'}, {'Session_id': session_cookie}),),
        "log": "log_favorites.html",
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)


"""
def test_search_isolated(betahost, templatehost, cgi):
    # Тестовые проверки.
    config = {
        "service": "video/search",
        "queries": ({'text': 'zyltrc'},
        ),
        "log": "log_serp_isolated.html",
        "cgi": cgi
    }
    base_test_compare(betahost, templatehost, config)

"""

fail_iterations = 3

global_config = {
    "domains": ("ru", "ua", "by", "kz", "com", "com.tr"),
    "service": "video",
    "optional_parameter": {"yandexuid": 444, "myreqid": 444, "nocookiesupport": "yes", "nocache": "da",
                             "time": "20150626T120000", "no-tests": 1,
                             "waitall": "da", "timeout": 10000000
                           },
    "blacklist": ("i18n", "banner", "links.abuse", "rdat.http_host", "rdat.url", "highlighter",
                  "search_props", "search", "unanswer_data", "searchdata.numitems", "searchdata.numdocs",
                  "searchdata.reask", "searchdata.clips.*.metahosts", "reqparam.wizard", "searchdata.clips.*.basehost",
                  "rdat.deajaxed_url", "rdat.etext", "rdat.flags.its_location", "rdat.flags", "rdat.hostname",
                  "searchdata.clips.*._markers", "top.films.*._markers", "reqdata.experiments", "rdat.experiments"
                  )
                 }

session_cookie = '3:' + str(int(time.time())) + '.5.0.1438356181221:igBYTQ:1d.1|307276731.5957.2.0:4.2:5957|132018.428289.N5DO4R-TgcPFdkk1M5x9NPep7Gc; z=s:l:0xde25d1b:1438353674813; yandexuid=1656452661435929066; yabs-frequency=/4/2W0007rudbK00000/2I5oSDGVOSGKht3K7sSNFR1mZ1_r/; fuid01=550ae3a35531e1e0.5Rqhj7HpuoIZp8uHWZ1hDElspOzaTzXYmS7F57XMYURwndd6AMVghVffQ2tTq-BRI5WmxqtU2ywmlzuojV9m071XTp7O2BUmisWQgaYJcVvQGGLUEQsPpBzsIYwXPRRb; sessionid2=3:' + str(int(time.time())) + '.5.0.1438356181221:igBYTQ:1d.1|307276731.5957.2.0:4.2:5957|132018.217376.Dh4wyX2WRC5zTJ_3e6eG-yamAZY; L=YQxzWARRAFdRfXNhZAdLXVZFBmZEXnlbJVEFFTcDaUMMF0NBQAM=.1438362138.11829.399114.afa7f9d9e83b4474e93ae28c40be3c82; yandex_login=report.test.py; yp=1444224536.ww.1#1753722138.udn.cDpyZXBvcnQudGVzdC5weQ%3D%3D; ys=vb.ff.2.20.2#bar.ff.8.11.1#wprid.1438944311219806-538036065096079887014927-gamgy-tests#gpauto.59_9593086%3A30_4063911%3A140_0000000%3A1%3A1438944278'
