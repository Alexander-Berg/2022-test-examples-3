# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import TLD, GEO, REGION, CTXS, IP, HNDL

TEXT_BY_TLD = {
    TLD.RU: 'машина',
    TLD.UA: 'машина',
    TLD.BY: 'машина',
    TLD.KZ: 'машина',
    TLD.COMTR: 'araba',
    TLD.COM: 'car'
}

BASE_CASES = [
    # TODO пользователь пришел, но мы не можем определить откуда он
    # (RU, None, None, None, None, RU_VLADIVOSTOK),

    # Базовый набор кейсов в рамках одного региона
    # Полный перебор только для одного региона - RU (логика одинакова для всех)
    # Приоритеты параметров в рамках региона: ip < tune < rstr
    (TLD.RU, GEO.RU_VLADIVOSTOK, None, None, None, GEO.RU_VLADIVOSTOK),
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, None, None, GEO.RU_IRKUTSK),
    (TLD.RU, GEO.RU_VLADIVOSTOK, None, GEO.RU_UFA, None, GEO.RU_UFA),
    (TLD.RU, GEO.RU_VLADIVOSTOK, None, None, GEO.RU_SMOLENSK, GEO.RU_SMOLENSK),
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, None, GEO.RU_SMOLENSK, GEO.RU_SMOLENSK),
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.RU_UFA, None, GEO.RU_UFA),
    (TLD.RU, GEO.RU_VLADIVOSTOK, None, GEO.RU_UFA, GEO.RU_SMOLENSK, GEO.RU_SMOLENSK),
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.RU_UFA, GEO.RU_SMOLENSK, GEO.RU_SMOLENSK),

    # Основной кейс для UA
    pytest.param(TLD.UA, GEO.UA_KHARKOV, None, None, None, GEO.UA_KHARKOV, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    pytest.param(TLD.UA, GEO.UA_KHARKOV, GEO.UA_CHERNIGOV, None, None, GEO.UA_CHERNIGOV, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    pytest.param(TLD.UA, GEO.UA_KHARKOV, None, GEO.UA_LUTSK, None, GEO.UA_LUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    pytest.param(TLD.UA, GEO.UA_KHARKOV, None, None, GEO.UA_TERNOPL, GEO.UA_TERNOPL, marks=pytest.mark.xfail(reason="SEARCH-11856")),

    # Основной кейс для BY
    (TLD.BY, GEO.BY_GOMEL, None, None, None, GEO.BY_GOMEL),
    (TLD.BY, GEO.BY_GOMEL, GEO.BY_VITEBSK, None, None, GEO.BY_VITEBSK),
    (TLD.BY, GEO.BY_GOMEL, None, GEO.BY_LIDA, None, GEO.BY_LIDA),
    (TLD.BY, GEO.BY_GOMEL, None, None, GEO.BY_GRODNO, GEO.BY_GRODNO),

    # Основной кейс для KZ
    (TLD.KZ, GEO.KZ_KARAGANDA, None, None, None, GEO.KZ_KARAGANDA),
    (TLD.KZ, GEO.KZ_KARAGANDA, GEO.KZ_KOSTANAY, None, None, GEO.KZ_KOSTANAY),
    (TLD.KZ, GEO.KZ_KARAGANDA, None, GEO.KZ_ACTOBE, None, GEO.KZ_ACTOBE),
    (TLD.KZ, GEO.KZ_KARAGANDA, None, None, GEO.KZ_ALMATA, GEO.KZ_ALMATA),

    # Основные кейсы для COMTR
    # (COMTR, COMTR_BYRSA, None, None, None, COMTR_BYRSA),
    (TLD.COMTR, GEO.COMTR_IZMIR, None, None, None, GEO.COMTR_IZMIR),
    (TLD.COMTR, GEO.COMTR_IZMIR, GEO.COMTR_ANTALIA, None, None, GEO.COMTR_ANTALIA),
    (TLD.COMTR, GEO.COMTR_IZMIR, None, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),
    (TLD.COMTR, GEO.COMTR_IZMIR, None, None, GEO.COMTR_MERSIN, GEO.COMTR_MERSIN),

    # Расширенный поиск параметр города
    # Логика везде одинакова, поэтому проверяем только RU, UA(один из КУБ), COMTR
    # Выставили в расширенном поиске другой регион
    # из КУБ
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.RU_UFA, GEO.UA_TERNOPL, GEO.UA_TERNOPL),
    # из Турции
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.RU_UFA, GEO.COMTR_MERSIN, GEO.COMTR_MERSIN),
    # из остального мира
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.RU_UFA, GEO.W_OSLO, GEO.W_OSLO),
    # другую страну(логика везде одинакова - проверяем один раз)
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.RU_UFA, GEO.CHINA, GEO.CHINA),

    # пришли на UA, но выставили в расширенном поиске другой регион
    # из RU
    pytest.param(TLD.UA, GEO.UA_KHARKOV, GEO.UA_CHERNIGOV, GEO.UA_LUTSK, GEO.RU_SMOLENSK, GEO.RU_SMOLENSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из КУБ
    pytest.param(TLD.UA, GEO.UA_KHARKOV, GEO.UA_CHERNIGOV, GEO.UA_LUTSK, GEO.BY_GRODNO, GEO.BY_GRODNO, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из Турции
    pytest.param(TLD.UA, GEO.UA_KHARKOV, GEO.UA_CHERNIGOV, GEO.UA_LUTSK, GEO.COMTR_MERSIN, GEO.COMTR_MERSIN, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из остального мира
    pytest.param(TLD.UA, GEO.UA_KHARKOV, GEO.UA_CHERNIGOV, GEO.UA_LUTSK, GEO.W_OSLO, GEO.W_OSLO, marks=pytest.mark.xfail(reason="SEARCH-11856")),

    # Перешли на comtr, но выставили другой регион
    # из КУБР
    (TLD.COMTR, GEO.COMTR_IZMIR, GEO.COMTR_ANTALIA, None, GEO.RU_SMOLENSK, GEO.RU_SMOLENSK),
    # из остального мира
    (TLD.COMTR, GEO.COMTR_IZMIR, GEO.COMTR_ANTALIA, None, GEO.W_OSLO, GEO.W_OSLO),
]

COMPLEX_CASES = [
    # Внезапный кейс по мотивам тикета SERP-29685
    # Мы случайно попали c lr!= текущему региону
    # Работает для ru, com, comtr
    (TLD.RU, GEO.RU_VLADIVOSTOK, None, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),
    (TLD.COMTR, GEO.RU_VLADIVOSTOK, None, GEO.W_OSLO, None, GEO.W_OSLO),

    # Переход по присланной ссылке из другого региона
    # Логика везде одинакова, поэтому проверяем только RU, UA(один из КУБ), COMTR
    # пришли на RU по ссылке, но сами из другого региона
    # из КУБ
    (TLD.RU, GEO.BY_GOMEL, GEO.BY_VITEBSK, GEO.RU_IRKUTSK, None, GEO.RU_IRKUTSK),
    # из Турции
    (TLD.RU, GEO.COMTR_BYRSA, GEO.COMTR_ANTALIA, GEO.RU_IRKUTSK, None, GEO.RU_IRKUTSK),
    # из остального мира
    (TLD.RU, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.RU_IRKUTSK, None, GEO.RU_IRKUTSK),

    # пришли на UA по ссылке, но сами из другого региона
    # из RU
    pytest.param(TLD.UA, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.UA_LUTSK, None, GEO.UA_LUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из КУБ
    pytest.param(TLD.UA, GEO.KZ_KARAGANDA, GEO.KZ_KOSTANAY, GEO.UA_LUTSK, None, GEO.UA_LUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из Турции
    pytest.param(TLD.UA, GEO.COMTR_BYRSA, GEO.COMTR_ANTALIA, GEO.UA_LUTSK, None, GEO.UA_LUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из остального мира
    pytest.param(TLD.UA, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.UA_LUTSK, None, GEO.UA_LUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),

    # Перешли на comtr по ссылке, но сами из другого региона
    # из КУБР
    (TLD.COMTR, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),
    # из остального мира
    (TLD.COMTR, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),

    # Незалогиненый пользователь из другого региона пытается зайти
    # На ru из КУБ зайти не получится см. редиректы
    # КУБ из RU
    pytest.param(TLD.UA, GEO.RU_VLADIVOSTOK, None, None, None, GEO.RU_VLADIVOSTOK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # COMTR из RU
    (TLD.COMTR, GEO.RU_VLADIVOSTOK, None, None, None, GEO.RU_VLADIVOSTOK),
    # COMTR из остального мира
    (TLD.COMTR, GEO.W_SEATTLE, None, None, None, GEO.W_SEATTLE),

    # Залогиненый пользователь из другого региона пытается зайти
    # КУБ из RU
    pytest.param(TLD.UA, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, None, None, GEO.RU_IRKUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # COMTR из остального мира
    # Такой поиск не имеет смысла(так как запрос уйдет на верхние для кубр, а это совсем не то, что турция)
    # Поэтому приводим к стамбулу
    (TLD.COMTR, GEO.W_SEATTLE, GEO.W_SINGAPORE, None, None, GEO.W_SINGAPORE),

    # Пользователь выставил себе другой регион
    # В рамках КУБР его надо редиректить на что-то более подходящее, но если подходящего нет, то оставляем как есть
    # RU не из КУБ
    # Такой поиск не имеет смысла(так как запрос уйдет на верхние для кубр, а это совсем не то, что турция),
    #  поэтому приводим к Москве
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.COMTR_ANTALIA, None, None, GEO.COMTR_ANTALIA),
    # RU не из КУБ
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.W_SINGAPORE, None, None, GEO.W_SINGAPORE),
    # COMTR
    # Для турции та же фигня, но приводим к Стамбулу
    (TLD.COMTR, GEO.COMTR_IZMIR, GEO.W_SINGAPORE, None, None, GEO.W_SINGAPORE),
]

COMPLEX_DIRECT_CASES = [
    # Логика отличается от поисковой: детали можно посмотреть тут - SERP-30273
    # Теперь IP приоритетней для определения рекламы

    # Внезапный кейс по мотивам тикета SERP-29685
    # Мы случайно попали c lr!= текущему региону
    # Работает для ru, com, comtr
    (TLD.RU, GEO.RU_VLADIVOSTOK, None, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),
    (TLD.COMTR, GEO.RU_VLADIVOSTOK, None, GEO.W_OSLO, None, GEO.W_OSLO),

    # Переход по присланной ссылке из другого региона
    # Логика везде одинакова, поэтому проверяем только RU, UA(один из КУБ), COMTR
    # пришли на RU по ссылке, но сами из другого региона
    # из КУБ
    (TLD.RU, GEO.BY_GOMEL, GEO.BY_VITEBSK, GEO.RU_IRKUTSK, None, GEO.BY_VITEBSK),
    # из Турции
    (TLD.RU, GEO.COMTR_BYRSA, GEO.COMTR_ANTALIA, GEO.RU_IRKUTSK, None, GEO.COMTR_ANTALIA),
    # из остального мира
    (TLD.RU, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.RU_IRKUTSK, None, GEO.W_SINGAPORE),

    # пришли на UA по ссылке, но сами из другого региона
    # из RU
    pytest.param(TLD.UA, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.UA_LUTSK, None, GEO.RU_IRKUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из КУБ
    pytest.param(TLD.UA, GEO.KZ_KARAGANDA, GEO.KZ_KOSTANAY, GEO.UA_LUTSK, None, GEO.KZ_KOSTANAY, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из Турции
    pytest.param(TLD.UA, GEO.COMTR_BYRSA, GEO.COMTR_ANTALIA, GEO.UA_LUTSK, None, GEO.COMTR_ANTALIA, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # из остального мира
    pytest.param(TLD.UA, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.UA_LUTSK, None, GEO.W_SINGAPORE, marks=pytest.mark.xfail(reason="SEARCH-11856")),

    # Перешли на comtr по ссылке, но сами из другого региона
    # из КУБР
    (TLD.COMTR, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),
    # из остального мира
    (TLD.COMTR, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),

    # Незалогиненый пользователь из другого региона пытается зайти
    # На ru из КУБ зайти не получится см. редиректы
    # КУБ из RU
    pytest.param(TLD.UA, GEO.RU_VLADIVOSTOK, None, None, None, GEO.RU_VLADIVOSTOK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # COMTR из RU
    (TLD.COMTR, GEO.RU_VLADIVOSTOK, None, None, None, GEO.RU_VLADIVOSTOK),
    # COMTR из остального мира
    (TLD.COMTR, GEO.W_SEATTLE, None, None, None, GEO.W_SEATTLE),

    # Залогиненый пользователь из другого региона пытается зайти
    # КУБ из RU
    pytest.param(TLD.UA, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, None, None, GEO.RU_IRKUTSK, marks=pytest.mark.xfail(reason="SEARCH-11856")),
    # COMTR из остального мира
    # Такой поиск не имеет смысла(так как запрос уйдет на верхние для кубр, а это совсем не то, что турция),
    # поэтому приводим к Москве
    (TLD.COMTR, GEO.W_SEATTLE, GEO.W_SINGAPORE, None, None, GEO.W_SINGAPORE),

    # Пользователь выставил себе другой регион
    # В рамках КУБР его надо редиректить на что-то более подходящее, но если подходящего нет, то оставляем как есть
    # RU не из КУБ
    # Упячка
    # Так как есть логика для FAS SERP-30273, то там мы определяем по настройке пользователя, так как она приоритетней
    # Эта логика ограничена КУБР
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.COMTR_ANTALIA, None, None, GEO.COMTR_ANTALIA),
    # RU не из КУБ
    (TLD.RU, GEO.RU_VLADIVOSTOK, GEO.W_SINGAPORE, None, None, GEO.W_SINGAPORE),
    # COMTR
    # Тут мы не попадаем под действие FAS SERP-30273, соответственно, работаем как поиск
    (TLD.COMTR, GEO.COMTR_IZMIR, GEO.W_SINGAPORE, None, None, GEO.W_SINGAPORE),
]

COM_CASES = [
    # Тут нет рекламы
    # Тут нет основного региона, так что обрабатываем пользователей отовсюду
    # Тикет по теме - SERP-30522

    # Пользователь из случайного региона
    # COM из COMTR
    (TLD.COM, GEO.COMTR_IZMIR, None, None, None, GEO.COMTR_IZMIR),
    # COM из мира
    (TLD.COM, GEO.W_SEATTLE, None, None, None, GEO.W_SEATTLE),

    # Залогиненный пользователь из случайного региона
    # COM из COMTR
    (TLD.COM, GEO.COMTR_IZMIR, GEO.COMTR_ANTALIA, None, None, GEO.COMTR_ANTALIA),
    # COM из мира
    (TLD.COM, GEO.W_SEATTLE, GEO.W_SINGAPORE, None, None, GEO.W_SINGAPORE),

    # Поискали что-то в неизвестном регионе и прислали ссылку соседу в другом непонятном регионе
    # COM из COMTR
    (TLD.COM, GEO.COMTR_IZMIR, GEO.COMTR_ANTALIA, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),
    # COM из мира
    (TLD.COM, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.W_OSLO, None, GEO.W_OSLO),

    # Пришли черте-откуда и выставили регион
    # COM из COMTR
    (TLD.COM, GEO.COMTR_IZMIR, GEO.COMTR_ANTALIA, GEO.COMTR_KAYSERI, GEO.COMTR_MERSIN, GEO.COMTR_MERSIN),
    # COM из мира
    (TLD.COM, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.W_OSLO, GEO.W_HARTUM, GEO.W_HARTUM),
    # COM из мира(страна)
    (TLD.COM, GEO.W_SEATTLE, GEO.W_SINGAPORE, GEO.W_OSLO, GEO.CHINA, GEO.CHINA),

    # Внезапный кейс по мотивам тикета SERP-29685
    # Мы случайно попали c lr!= текущему региону
    # Работает для ru, com, comtr
    (TLD.COM, GEO.RU_VLADIVOSTOK, None, GEO.COMTR_KAYSERI, None, GEO.COMTR_KAYSERI),
]


def region_by_city(city):
    return None if city is None else REGION[city]


def ip_by_city(city):
    return None if city is None else IP[city]


class BaseTestRegions():

    """
    Проверки определения региона
    test_direct UPPER YABS
    test_web UPPER WEB
    com скиппаем
    """
    def SetupQuery(self, query, tld=None, ip_reg=None, tune=None, lr=None, rstr=None, ip=None):
        # ip - ip адрес, ip_reg - регион определенный по ip
        query.SetExternal(ip=ip)
        if tld:
            query.SetDomain(tld)
            query.SetParams({
                'text': TEXT_BY_TLD[tld],
            })
        if lr:
            query.SetParams({'lr': lr})
        if rstr:
            query.SetParams({'rstr': "-" + rstr})

        query.SetHeaders({"X-LaaS-Answered": "1"})
        if tune:
            query.SetHeaders({"X-Region-City-Id": tune})
            if ip_reg:
                query.SetHeaders({"X-Region-Suspected": ip_reg})
        else:
            if ip_reg:
                query.SetHeaders({"X-Region-City-Id": ip_reg})
        # TODO(kozunov) what is it?
        return query


class TestRegions(BaseTestRegions):
    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(("tld", "ip_city", "tune_city", "city", "result_tld", "result_lr"), [
        # Логика для всех стран из КУБ по отношению к RU одинакова
        # Мы не можем зайти на RU из КУБ
        pytest.param(TLD.RU, GEO.UA_KHARKOV, None, None, TLD.UA, GEO.UA_KHARKOV, marks=pytest.mark.xfail(strict=True, reason="SEARCH-11856")),
        (TLD.RU, GEO.BY_GOMEL, GEO.BY_VITEBSK, None, TLD.BY, GEO.BY_VITEBSK),
        (TLD.RU, GEO.KZ_KARAGANDA, None, GEO.KZ_ACTOBE, TLD.KZ, GEO.KZ_ACTOBE),

        # При попытке зайти на КУБ из RU с ru lr - приписываем lr из столицы и редиректим соответствующий поиск
        pytest.param(TLD.UA, GEO.RU_VLADIVOSTOK, None, GEO.RU_UFA, None, GEO.UA_KIEV, marks=pytest.mark.xfail(reason="RUNTIMETESTS-143")),

        # Попытки зайти на другой домен в рамках КУБ
        # Тут логика везде симметричная
        # Если IP или tune - редиректим на соответствующий поиск и приписываем lr
        # Если lr, то редиректим на то, откуда lr
        # KZ из UA с lr=UA, редиректим на ua с LR=KZ
        (TLD.BY, GEO.UA_KHARKOV, None, GEO.UA_LUTSK, None, GEO.BY_MINSK),
        # COM
        (TLD.COM, GEO.RU_VLADIVOSTOK, None, None, TLD.RU, GEO.RU_VLADIVOSTOK),
        (TLD.COM, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, None, TLD.RU, GEO.RU_IRKUTSK),
        pytest.param(TLD.COM, GEO.UA_KHARKOV, None, None, TLD.UA, GEO.UA_KHARKOV, marks=pytest.mark.xfail(strict=True, reason="SEARCH-11856")),
        (TLD.COM, GEO.BY_GOMEL, GEO.BY_VITEBSK, None, TLD.BY, GEO.BY_VITEBSK),
        # Поискали что-то в неизвестном регионе и прислали ссылку соседу в другом непонятном регионе
        (TLD.COM, GEO.RU_VLADIVOSTOK, GEO.RU_IRKUTSK, GEO.RU_UFA, TLD.RU, GEO.RU_UFA),
        (TLD.COM, GEO.KZ_KARAGANDA, GEO.KZ_KOSTANAY, GEO.KZ_ACTOBE, TLD.KZ, GEO.KZ_ACTOBE),
        # Пришли черте-откуда и выставили регион
        (TLD.COM, GEO.W_SINGAPORE, GEO.RU_IRKUTSK, GEO.RU_UFA, TLD.RU, GEO.RU_UFA),
        (TLD.COM, GEO.W_SINGAPORE, GEO.KZ_KOSTANAY, GEO.KZ_ACTOBE, TLD.KZ, GEO.KZ_ACTOBE),
    ])
    @TSoY.yield_test
    def test_redirects(self, query, tld, ip_city, tune_city, city, result_tld, result_lr):
        self.SetupQuery(query=query, tld=tld, ip_reg=region_by_city(ip_city), tune=region_by_city(tune_city), lr=region_by_city(city), ip=ip_by_city(ip_city))
        query.SetFlags({
            'infect_mda': 0
        })
        query.SetRequireStatus(302)

        resp = yield query

        if result_tld:
            assert 'yandex.{}'.format(result_tld) == resp.GetLocation().hostname
        else:
            assert resp.GetLocation().hostname is None

        if result_lr:
            assert resp.GetLocationParams()['lr'] == [REGION[result_lr]]
        else:
            assert 'lr' not in resp.GetLocationParams()

        # Проверка существования выдачи по редиректу
        # host_tld = result_tld if result_tld else tld
        # query.SetPath(resp.GetLocation().path)
        # query.SetDomain(host_tld)
        # query.SendRequest()
        # query.GetResponse(require_status=200)

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(
        ("tld", "ip_city", "tune_city", "city", "rstr_city", "result"),
        BASE_CASES +
        COMPLEX_DIRECT_CASES +
        COM_CASES
    )
    @TSoY.yield_test
    def test_direct(self, query, tld, ip_city, tune_city, city, rstr_city, result):
        query = self.SetupQuery(
            query, tld=tld, ip_reg=region_by_city(ip_city), tune=region_by_city(tune_city),
            lr=region_by_city(city), rstr=region_by_city(rstr_city), ip=ip_by_city(ip_city)
        )
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['yabs_setup']) > 0
        assert str(REGION[result]) in ctxs['yabs_setup'][0]['tune-region-id']

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(
        ("tld", "ip_city", "tune_city", "city", "rstr_city", "result"),
        BASE_CASES +
        COMPLEX_CASES +
        COM_CASES
    )
    @TSoY.yield_test
    def test_web(self, query, tld, ip_city, tune_city, city, rstr_city, result):
        query = self.SetupQuery(
            query, tld=tld, ip_reg=region_by_city(ip_city), tune=region_by_city(tune_city),
            lr=region_by_city(city), rstr=region_by_city(rstr_city), ip=ip_by_city(ip_city)
        )
        query.SetDumpFilter(resp=[CTXS.NOAPACHE])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert REGION[result] == str(ctxs['noapache_setup'][0]['client_ctx']['WEB']['lr'][0])

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(("tld", "city"), [
        pytest.param(TLD.UA, GEO.SIMFEROPOL, marks=pytest.mark.xfail(strict=True, reason="SEARCH-11856")),
        (TLD.RU, GEO.SIMFEROPOL),
    ])
    @TSoY.yield_test
    def test_web_cr_cookie(self, query, tld, city):
        """
        Особые условия - проверяем, что работает крымская кука
        Просто проверяем, что работает
        """
        query = self.SetupQuery(query, tld=tld, lr=region_by_city(city), tune=region_by_city(city), ip_reg=region_by_city(city), ip=ip_by_city(city))
        query.SetNoAuth()
        query.ReqYpCookie.set_cr(tld.lower())
        query.SetDumpFilter(resp=[CTXS.NOAPACHE, CTXS.INIT_HTTP_RESPONSE])
        # если делать дамп(json_dump_response), то код ответа всегда будет 200, поэтому код ответа надо проверять в контексте http_response
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()

        assert ctxs["http_response"][-1]["status_code"] == 200

        web_region = ctxs['noapache_setup'][0]['client_ctx']['WEB']["lr"][0]
        assert web_region == int(region_by_city(city))

    @pytest.mark.soy_http('SCRAPEROVERYT-430')
    @pytest.mark.parametrize(("tld", "city", "result"), [
        pytest.param(TLD.RU, GEO.SIMFEROPOL, TLD.UA, marks=pytest.mark.xfail(strict=True, reason="http 200 OK")),
        pytest.param(TLD.UA, GEO.SIMFEROPOL, TLD.RU, marks=pytest.mark.xfail(strict=True, reason="http 200 OK")),
    ])
    @TSoY.yield_test
    def test_web_cr_cookie_redirect(self, query, tld, city, result):
        """
        Особые условия - проверяем, что работает крымская кука
        Просто проверяем, что оно редиректит
        """
        query = self.SetupQuery(query, tld=tld, lr=region_by_city(city), tune=region_by_city(city), ip_reg=region_by_city(city), ip=ip_by_city(city))
        query.SetNoAuth()
        query.ReqYpCookie.set_cr(result.lower())
        query.SetRequireStatus(302)

        resp = yield query

        assert resp.GetLocation().hostname == 'yandex.{}'.format(result)

        # query.set_url(resp.headers['location'][0])
        # query.set_host(result)
        # self.request(query)

    @pytest.mark.ticket('SEARCH-12003')
    @TSoY.yield_test
    def test_laas_by_ip_answer(self, query):
        """
        Тестируем информацию о регионе, основанном на новом заголовке x-region-by-ip-orig-id от LaaS.
        Чтобы он появился надо спровоцировать LaaS по-разному определить регион по ip и на основе более полной информации.
        Поэтому подкладываем ip одного региона через X-Forwarded-For и координаты другого региона в cgi.
        """

        query.SetDumpFilter(resp=[CTXS.INIT])
        query.SetPath(HNDL.SEARCH)
        query.SetHeaders({
            'X-Forwarded-For': '::ffff:212.47.241.82'
        })
        query.SetParams({
            'text': 'test',
            'lat': '44.9482',
            'lon': '34.1003',
            'location_accuracy': '123',
            'location_recency': '12345',
        })
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        region = ctxs['region'][-1]

        assert region['laas_by_ip']
        by_ip_name = region['laas_by_ip']['name']
        assert by_ip_name and by_ip_name != region['laas_real']['name']
