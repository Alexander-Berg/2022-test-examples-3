#!/usr/bin/env python2
# -*- coding: utf-8 -*-

import requests

class VideotestError(Exception): pass


def make_url(url, domain, path=''):
    return "https://" + url + "." + domain + "/video" + path

def get_content(url, params=None): # Ожидаем, что сюда придёт только словарь доп. CGI-параметров.
    """
    Функция для открытия ссылки.
    На вход принимается готовая ссылка, на выходе - текст.
    """
    content = requests.get(url, params=params, verify=False)  # Ожидаем, что сюда придёт только словарь доп. CGI-параметров.
    assert content.status_code == 200
    return content.json()
