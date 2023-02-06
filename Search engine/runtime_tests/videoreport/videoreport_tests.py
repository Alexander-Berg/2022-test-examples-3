#!/usr/bin/env python2
# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import json
import copy
import requests


class ReportAutotestsTestrunError(Exception): pass


class Autotests:
    def get_content(self, url, kwargs):
        """
        Функция для открытия ссылки.
        На вход принимается готовая ссылка, на выходе - текст.
        """
        try:
            content = requests.get(url, verify=False, **kwargs)
        except (requests.ConnectionError, requests.HTTPError) as err:
            raise ReportAutotestsTestrunError("Failed to open '{0}': {1}".format(url,  err.args[0].args[0]))  # Возвращаем только читаемое сообщение об ошибке.
        else:
            # Если возвращает ошибку http, то возвращаем текст этой ошибки:
            if (content.status_code in range(500, 505)) or (content.status_code in range(400, 405)):
                raise ReportAutotestsTestrunError("Failed to open '{0}': {1}".format(url, content.reason))
            else:
                return content.text, content.url

    def get_content_test(self, url, kwargs):
        """
        Отладочная функция.
        """
        if "report." in url:
            result = open("videoreport/beta_json.txt")
        elif "секс" in url:
            result = open("videoreport/sex_json.txt")
        else:
            result = open("videoreport/template_json.txt")
        text = result.read()
        return (text, url)

    def get_json(self, url):
        """
        Функция для преобразования ссылки в json.
        url: кортеж из адреса, параметров и (опционально) имени и значения куки.
        json_path: путь в json через точку, например: "blocks.contents.top.films",
        который передаётся CGI-параметру json_dump.
        На выходе даёт сериализованный json.
        """
        # Проверяем, включёна ли поддержка report renderer:
        if "export" not in url[1]["params"]:
            # Добавляем к запросу json_dump:
            url[1]["params"]["json_dump"] = "1"
        result = self.get_content(url[0], url[1])       # На этой строчке включать тестовую функцию get_content_test.
        try:
            return json.loads(result[0]), result[1]
        except ValueError:
            raise ReportAutotestsTestrunError("Failed to parse '{0}': no json found".format(result[1]))

    def data_control(self, text):
        """
        Функция для унификации строк.
        """
        if isinstance(text, basestring):
            if not isinstance(text, unicode):
                text = unicode(text, 'utf-8', errors='replace')
        else:
            text = unicode(text)
        if len(text) > 50:
            text = text[:51] + "..."
        return text


class Comparison(Autotests):
    """
    Основной тест - сравнение указанных веток дерева json беты и прода и вывод результатов.
    """

    def run(self, prod_url, beta_url, blackitems, viewpathes, failiters):
        """
        Основная функция.
        """
        result = []
        for item in viewpathes:
            # Костыль для "похожих":
            if item.endswith('.related'):
                try:
                    related_params = self.get_related(beta_url, item)
                except Exception as err:
                    result.append(("Error", (str(err),)))
                    continue
                else:
                    temp_beta_url = copy.deepcopy(beta_url)
                    temp_beta_url[1]['params'].update(related_params)   # Добавляем параметры для запроса "похожих" к набору параметров урла.
                    temp_prod_url = copy.deepcopy(prod_url)
                    temp_prod_url[1]['params'].update(related_params)   # Добавляем параметры для запроса "похожих" к набору параметров урла.
                    temp_item = ''
            else:
                temp_item = item
                temp_beta_url = beta_url
                temp_prod_url = prod_url
            ## Подготовка блеклиста:
            # На случай, если блеклист был пустым:
            blacktemp = []
            # На случай, если блеклист не пуст,
            if len(blackitems) != 0:
                # а view_path - пуст,
                if temp_item == '':
                    # то при отсутствии в урле параметра export в начале json оказывается ветка 'tmpl_data':
                    if "export" not in beta_url[1]["params"]:
                        blacktemp = ['tmpl_data' + '.' + b for b in blackitems]
                    else:
                        blacktemp = list(blackitems)
                else:
                    blacktemp = [temp_item + '.' + b for b in blackitems]
            ## Получение json и сравнение с образцом:
            # Делаем, пока не получим положительный результат или не исчерпаем количество попыток:
            while failiters > 0:
                try:
                    # Получаем json для беты и для образца (обычно прода):
                    json_beta = self.get_json(temp_beta_url)
                    json_prod = self.get_json(temp_prod_url)
                # В случае появления непредусмотренной ошибки пишем её в журнал и прерываем цикл:
                except Exception as err:
                    result.append(("Error", (str(err),)))
                    break
                else:
                    # Отбрасываем лишнее, если указана конкретная ветка, с которой начинать сравнение:
                    if temp_item:
                        json_beta = self.cut_excess(json_beta[0], temp_item), json_beta[1]
                        json_prod = self.cut_excess(json_prod[0], temp_item), json_prod[1]
                    # Вызов функции, непосредственно сравнивающей указанные ветки:
                    res = self.compare(json_beta[0], json_prod[0], "", blacktemp, [])
                    if not res:
                        break
                    elif failiters == 1:
                        result.append((json_beta[1], res))
                        break
                    else:
                        failiters -= 1
        return result

    def get_related(self, url, path):
        """
        Функция для формирования URL для запроса "похожих".
        """
        result = self.get_json(url)
        return self.cut_excess(result[0], path)

    def cut_excess(self, content, path):
        """Функция отбрасывает всё лишнее и возвращает словарь content, начинающийся с указанной в path ветки."""
        keys_list = path.split('.')
        keys_changed_list = []
        for k in keys_list:
            try:
                r = int(k)
            except ValueError:
                keys_changed_list.append(k)
            else:
                keys_changed_list.append(r)
        string = ''
        i = 0
        for r in keys_changed_list:
            string = "{0}[keys_changed_list[{1}]]".format(string, i)
            i += 1
        exec "res = {{path: content{}}}".format(string)
        return res

    def compare_dict(self, data1, data2, blacklist, json_position, result):
        """
        Функция для итерирования по словарю.
        """
        for key in data1:
            # Создаём временную переменную, в которой прописываем текущий путь (нужно для сравнения с блеклистом и для сообщений об ошибках):
            if json_position == '':
                json_pos = str(key)
            else:
                json_pos = json_position + '.' + str(key)
            # Если указанный ключ (т.е. указанная ветка json) не в блеклисте, то работаем с ним, иначе пропускаем:
            if json_pos not in blacklist and not (json_pos.endswith("basehost") or json_pos.endswith("metahosts")):
                # Если ключ есть в проде, то делаем следующую итерацию обхода json:
                if key in data2:
                    self.compare(data1[key], data2[key], json_pos, blacklist, result)
                # Иначе пишем ошибку:
                else:
                    to_append = "Key '{0}' not found in the template, path '{1}'.".format(self.data_control(key),
                                 json_position if not json_position.startswith("tmpl_data.") else json_position[len("tmpl_data."):])
                    if to_append not in result:
                        result.append(to_append)

    def compare_list(self, data1, data2, blacklist, json_position, result):
        """
        Функция для итерирования по списку.
        """
        i = 0
        for item in data1:
            # Создаём временную переменную, в которой прописываем текущий путь (нужно для сравнения с блеклистом и для сообщений об ошибках):
            if json_position == '':
                json_pos = "*"
            else:
                json_pos = json_position + "." + "*"
            # Проверяем, есть ли нужная позиция в блеклисте:
            if json_position not in blacklist:
                # Если элементы совпали, то продолжаем:
                if item == data2[i]:
                    self.compare(item, data2[i], json_pos, blacklist, result)
                # Если не совпали,
                else:
                    # то, если тип элемента не относится к словарю или списку, проверяем его наличие в шаблоне:
                    if type(item) not in (list, dict):
                        # приводим его к универсальному виду:
                        temp_item = self.data_control(item)
                        # и записываем ошибку.
                        # Если item вообще нет в шаблоне,
                        if item not in data2:
                            # то пишем одну ошибку,
                            to_append = "Difference in '{1}' on position {2}, missing item: '{0}'.".format(self.data_control(temp_item),
                                        json_position if not json_position.startswith("tmpl_data.") else json_position[len("tmpl_data."):], i)
                            if to_append not in result:
                                result.append(to_append)
                        # если item есть в шаблоне, но на другой позиции, то пишем другую ошибку:
                        else:
                            to_append = "Difference in '{1}' on position {2}, item: '{0}' in template is on different position.".format(self.data_control(temp_item),
                                          json_position if not json_position.startswith("tmpl_data.") else json_position[len("tmpl_data."):], i)
                            if to_append not in result:
                                result.append(to_append)
                    # А если элемент является словарём или списком, то втупую искать его смысла нет,
                    # поэтому запускаем следующую итерацию рекурсии:
                    else:
                        self.compare(item, data2[i], json_pos, blacklist, result)
            i += 1


    def compare(self, data1, data2, json_position, blacklist, result):
        """
        Основная функция для сравнения двух веток json.
        json_position - строка.
        blacklist - список.
        """
        # Если типы данных в соответствующей ветке разные, то ошибка:
        if type(data1) != type(data2):
            to_append = "Data type differences with the template in '{0}'.".format(json_position if not json_position.startswith("tmpl_data.")
                                                                                     else json_position[len("tmpl_data."):])
            if to_append not in result:
                result.append(to_append)
        # Если тип данных - не словарь и не список, то сравниваем их в лоб:
        elif type(data2) not in (dict, list):
            if data1 != data2:
                to_append = "Data in '{0}' differ with the template.".format(json_position if not json_position.startswith("tmpl_data.")
                                                                               else json_position[len("tmpl_data."):])
                if to_append not in result:
                    result.append(to_append)
        # Если длина данных разная, то сравниваем и добавляем то, чего нет в каждой из веток:
        elif len(data1) != len(data2):
            set_beta = set([self.data_control(y) for y in data1])
            set_template = set([self.data_control(y) for y in data2])
            if type(data2) is dict:
                keys1 = set_template - set_beta         # Ключи, которые есть в шаблоне, но отсутствуют на бете.
                keys2 = set_beta - set_template         # Ключи, которые есть на бете, но отсутствуют в шаблоне.
                if len(keys1) == 0:
                    to_append = "Length of structures in '{0}' differ." \
                                   "\n\tNew keys: '{1}'".format(json_position if not json_position.startswith("tmpl_data.")
                                                                else json_position[len("tmpl_data."):], "', '".join(keys2))
                elif len(keys2) == 0:
                    to_append = "Length of structures in '{0}' differ.\n\tRemoved keys: '{1}'".format(json_position if not json_position.startswith("tmpl_data.")
                                                                else json_position[len("tmpl_data."):], "', '".join(keys1))
                else:
                    to_append = "Length of structures in '{0}' differ.\n\tRemoved keys: '{1}'"\
                                   "\n\tNew keys: '{2}'".format(json_position if not json_position.startswith("tmpl_data.")
                                                                else json_position[len("tmpl_data."):], "', '".join(keys1), "', '".join(keys2))
                if to_append not in result:
                    result.append(to_append)
                # Запускаем ещё одну итерацию с тем, что есть общего, отбросив различное:
                new_data_1 = {k:data1[k] for k in data1 if k in data2}
                new_data_2 = {k:data2[k] for k in data2 if k in data1}
                self.compare_dict(new_data_1, new_data_2, blacklist, json_position, result)
            else:
                to_append = "Length of structures in '{0}' differ. {1} list is longer.".format(json_position if not json_position.startswith("tmpl_data.")
                                                            else json_position[len("tmpl_data."):], "Beta" if len(set_beta) > len(set_template) else "Template")
                if to_append not in result:
                    result.append(to_append)
                # Запускаем ещё одну итерацию с тем, что есть общего, отбросив различное:
                new_data_1 = [i for i in data1 if i in data2]
                new_data_2 = [i for i in data2 if i in data1]
                self.compare_list(new_data_1, new_data_2, blacklist, json_position, result)
        # Если итерируем словарь:
        elif type(data1) is dict:
            self.compare_dict(data1, data2, blacklist, json_position, result)
        # Если итерируем список:
        elif type(data1) is list:
            self.compare_list(data1, data2, blacklist, json_position, result)
        # Если найдены какие-то различия, они возвращаются в виде текста:
        if len(result) != 0:
            return tuple(result)

