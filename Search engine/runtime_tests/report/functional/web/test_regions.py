# -*- coding: utf-8 -*-

import os
import pytest

from report.const import *
from report.functional.web.base import BaseFuncTest

TEXT_BY_TLD = {
    RU: 'машина',
    UA: 'машина',
    BY: 'машина',
    KZ: 'машина',
    COMTR: 'araba',
    COM: 'car'
}

BASE_CASES = [
    # TODO пользователь пришел, но мы не можем определить откуда он
    # (RU, None, None, None, None, RU_VLADIVOSTOK),

    # Базовый набор кейсов в рамках одного региона
    # Полный перебор только для одного региона - RU (логика одинакова для всех)
    # Приоритеты параметров в рамках региона: ip < tune < rstr
    (RU, RU_VLADIVOSTOK, None, None, None, RU_VLADIVOSTOK),
    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, None, None, RU_IRKUTSK),
#    (RU, RU_VLADIVOSTOK, None, RU_UFA, None, RU_UFA),
    (RU, RU_VLADIVOSTOK, None, None, RU_SMOLENSK, RU_SMOLENSK),
    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, None, RU_SMOLENSK, RU_SMOLENSK),
#    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, RU_UFA, None, RU_UFA),
#    (RU, RU_VLADIVOSTOK, None, RU_UFA, RU_SMOLENSK, RU_SMOLENSK),
#    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, RU_UFA, RU_SMOLENSK, RU_SMOLENSK),

    # Основной кейс для UA
    (UA, UA_KHARKOV, None, None, None, UA_KHARKOV),
    (UA, UA_KHARKOV, UA_CHERNIGOV, None, None, UA_CHERNIGOV),
#    (UA, UA_KHARKOV, None, UA_LUTSK, None, UA_LUTSK),
    (UA, UA_KHARKOV, None, None, UA_TERNOPL, UA_TERNOPL),

    # Основной кейс для BY
    (BY, BY_GOMEL, None, None, None, BY_GOMEL),
    (BY, BY_GOMEL, BY_VITEBSK, None, None, BY_VITEBSK),
#    (BY, BY_GOMEL, None, BY_LIDA, None, BY_LIDA),
    (BY, BY_GOMEL, None, None, BY_GRODNO, BY_GRODNO),

    # Основной кейс для KZ
    (KZ, KZ_KARAGANDA, None, None, None, KZ_KARAGANDA),
    (KZ, KZ_KARAGANDA, KZ_KOSTANAY, None, None, KZ_KOSTANAY),
#    (KZ, KZ_KARAGANDA, None, KZ_ACTOBE, None, KZ_ACTOBE),
    (KZ, KZ_KARAGANDA, None, None, KZ_ALMATA, KZ_ALMATA),

    # Основные кейсы для COMTR
    # (COMTR, COMTR_BYRSA, None, None, None, COMTR_BYRSA),
    (COMTR, COMTR_IZMIR, None, None, None, COMTR_IZMIR),
    (COMTR, COMTR_IZMIR, COMTR_ANTALIA, None, None, COMTR_ANTALIA),
#    (COMTR, COMTR_IZMIR, None, COMTR_KAYSERI, None, COMTR_KAYSERI),
    (COMTR, COMTR_IZMIR, None, None, COMTR_MERSIN, COMTR_MERSIN),

    # Расширенный поиск параметр города
    # Логика везде одинакова, поэтому проверяем только RU, UA(один из КУБ), COMTR
    # Выставили в расширенном поиске другой регион
    # из КУБ
#    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, RU_UFA, UA_TERNOPL, UA_TERNOPL),
    # из Турции
#    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, RU_UFA, COMTR_MERSIN, COMTR_MERSIN),
    # из остального мира
#    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, RU_UFA, W_OSLO, W_OSLO),
    # другую страну(логика везде одинакова - проверяем один раз)
#    (RU, RU_VLADIVOSTOK, RU_IRKUTSK, RU_UFA, CHINA, CHINA),

    # пришли на UA, но выставили в расширенном поиске другой регион
    # из RU
#    (UA, UA_KHARKOV, UA_CHERNIGOV, UA_LUTSK, RU_SMOLENSK, RU_SMOLENSK),
    # из КУБ
#    (UA, UA_KHARKOV, UA_CHERNIGOV, UA_LUTSK, BY_GRODNO, BY_GRODNO),
    # из Турции
#    (UA, UA_KHARKOV, UA_CHERNIGOV, UA_LUTSK, COMTR_MERSIN, COMTR_MERSIN),
    # из остального мира
#    (UA, UA_KHARKOV, UA_CHERNIGOV, UA_LUTSK, W_OSLO, W_OSLO),

    # Перешли на comtr, но выставили другой регион
    # из КУБР
    (COMTR, COMTR_IZMIR, COMTR_ANTALIA, None, RU_SMOLENSK, RU_SMOLENSK),
    # из остального мира
    (COMTR, COMTR_IZMIR, COMTR_ANTALIA, None, W_OSLO, W_OSLO),
]

COMPLEX_CASES = [
    # Внезапный кейс по мотивам тикета SERP-29685
    # Мы случайно попали c lr!= текущему региону
    # Работает для ru, com, comtr
    (RU, RU_VLADIVOSTOK, None, COMTR_KAYSERI, None, COMTR_KAYSERI),
    (COMTR, RU_VLADIVOSTOK, None, W_OSLO, None, W_OSLO),

    # Переход по присланной ссылке из другого региона
    # Логика везде одинакова, поэтому проверяем только RU, UA(один из КУБ), COMTR
    # пришли на RU по ссылке, но сами из другого региона
    # из КУБ
    (RU, BY_GOMEL, BY_VITEBSK, RU_IRKUTSK, None, RU_IRKUTSK),
    # из Турции
    (RU, COMTR_BYRSA, COMTR_ANTALIA, RU_IRKUTSK, None, RU_IRKUTSK),
    # из остального мира
    (RU, W_SEATTLE, W_SINGAPORE, RU_IRKUTSK, None, RU_IRKUTSK),

    # пришли на UA по ссылке, но сами из другого региона
    # из RU
    (UA, RU_VLADIVOSTOK, RU_IRKUTSK, UA_LUTSK, None, UA_LUTSK),
    # из КУБ
    (UA, KZ_KARAGANDA, KZ_KOSTANAY, UA_LUTSK, None, UA_LUTSK),
    # из Турции
    (UA, COMTR_BYRSA, COMTR_ANTALIA, UA_LUTSK, None, UA_LUTSK),
    # из остального мира
    (UA, W_SEATTLE, W_SINGAPORE, UA_LUTSK, None, UA_LUTSK),

    # Перешли на comtr по ссылке, но сами из другого региона
    # из КУБР
    (COMTR, RU_VLADIVOSTOK, RU_IRKUTSK, COMTR_KAYSERI, None, COMTR_KAYSERI),
    # из остального мира
    (COMTR, W_SEATTLE, W_SINGAPORE, COMTR_KAYSERI, None, COMTR_KAYSERI),

    # Незалогиненый пользователь из другого региона пытается зайти
    # На ru из КУБ зайти не получится см. редиректы
    # КУБ из RU
    (UA, RU_VLADIVOSTOK, None, None, None, RU_VLADIVOSTOK),
    # COMTR из RU
    (COMTR, RU_VLADIVOSTOK, None, None, None, RU_VLADIVOSTOK),
    # COMTR из остального мира
    (COMTR, W_SEATTLE, None, None, None, W_SEATTLE),

    # Залогиненый пользователь из другого региона пытается зайти
    # КУБ из RU
    (UA, RU_VLADIVOSTOK, RU_IRKUTSK, None, None, RU_IRKUTSK),
    # COMTR из остального мира
    # Такой поиск не имеет смысла(так как запрос уйдет на верхние для кубр, а это совсем не то, что турция)
    # Поэтому приводим к стамбулу
    (COMTR, W_SEATTLE, W_SINGAPORE, None, None, W_SINGAPORE),

    # Пользователь выставил себе другой регион
    # В рамках КУБР его надо редиректить на что-то более подходящее, но если подходящего нет, то оставляем как есть
    # RU не из КУБ
    # Такой поиск не имеет смысла(так как запрос уйдет на верхние для кубр, а это совсем не то, что турция),
    #  поэтому приводим к Москве
    (RU, RU_VLADIVOSTOK, COMTR_ANTALIA, None, None, COMTR_ANTALIA),
    # RU не из КУБ
    (RU, RU_VLADIVOSTOK, W_SINGAPORE, None, None, W_SINGAPORE),
    # COMTR
    # Для турции та же фигня, но приводим к Стамбулу
    (COMTR, COMTR_IZMIR, W_SINGAPORE, None, None, W_SINGAPORE),
]

COMPLEX_DIRECT_CASES = [
    # Логика отличается от поисковой: детали можно посмотреть тут - SERP-30273
    # Теперь IP приоритетней для определения рекламы

    # Внезапный кейс по мотивам тикета SERP-29685
    # Мы случайно попали c lr!= текущему региону
    # Работает для ru, com, comtr
#    (RU, RU_VLADIVOSTOK, None, COMTR_KAYSERI, None, COMTR_KAYSERI),
#    (COMTR, RU_VLADIVOSTOK, None, W_OSLO, None, W_OSLO),

    # Переход по присланной ссылке из другого региона
    # Логика везде одинакова, поэтому проверяем только RU, UA(один из КУБ), COMTR
    # пришли на RU по ссылке, но сами из другого региона
    # из КУБ
#    (RU, BY_GOMEL, BY_VITEBSK, RU_IRKUTSK, None, BY_VITEBSK),
    # из Турции
#    (RU, COMTR_BYRSA, COMTR_ANTALIA, RU_IRKUTSK, None, COMTR_ANTALIA),
    # из остального мира
#    (RU, W_SEATTLE, W_SINGAPORE, RU_IRKUTSK, None, W_SINGAPORE),

    # пришли на UA по ссылке, но сами из другого региона
    # из RU
#    (UA, RU_VLADIVOSTOK, RU_IRKUTSK, UA_LUTSK, None, RU_IRKUTSK),
    # из КУБ
#    (UA, KZ_KARAGANDA, KZ_KOSTANAY, UA_LUTSK, None, KZ_KOSTANAY),
    # из Турции
#    (UA, COMTR_BYRSA, COMTR_ANTALIA, UA_LUTSK, None, COMTR_ANTALIA),
    # из остального мира
#    (UA, W_SEATTLE, W_SINGAPORE, UA_LUTSK, None, W_SINGAPORE),

    # Перешли на comtr по ссылке, но сами из другого региона
    # из КУБР
#    (COMTR, RU_VLADIVOSTOK, RU_IRKUTSK, COMTR_KAYSERI, None, COMTR_KAYSERI),
    # из остального мира
#    (COMTR, W_SEATTLE, W_SINGAPORE, COMTR_KAYSERI, None, COMTR_KAYSERI),

    # Незалогиненый пользователь из другого региона пытается зайти
    # На ru из КУБ зайти не получится см. редиректы
    # КУБ из RU
    (UA, RU_VLADIVOSTOK, None, None, None, RU_VLADIVOSTOK),
    # COMTR из RU
    (COMTR, RU_VLADIVOSTOK, None, None, None, RU_VLADIVOSTOK),
    # COMTR из остального мира
    (COMTR, W_SEATTLE, None, None, None, W_SEATTLE),

    # Залогиненый пользователь из другого региона пытается зайти
    # КУБ из RU
    (UA, RU_VLADIVOSTOK, RU_IRKUTSK, None, None, RU_IRKUTSK),
    # COMTR из остального мира
    # Такой поиск не имеет смысла(так как запрос уйдет на верхние для кубр, а это совсем не то, что турция),
    # поэтому приводим к Москве
    (COMTR, W_SEATTLE, W_SINGAPORE, None, None, W_SINGAPORE),

    # Пользователь выставил себе другой регион
    # В рамках КУБР его надо редиректить на что-то более подходящее, но если подходящего нет, то оставляем как есть
    # RU не из КУБ
    # Упячка
    # Так как есть логика для FAS SERP-30273, то там мы определяем по настройке пользователя, так как она приоритетней
    # Эта логика ограничена КУБР
    (RU, RU_VLADIVOSTOK, COMTR_ANTALIA, None, None, COMTR_ANTALIA),
    # RU не из КУБ
    (RU, RU_VLADIVOSTOK, W_SINGAPORE, None, None, W_SINGAPORE),
    # COMTR
    # Тут мы не попадаем под действие FAS SERP-30273, соответственно, работаем как поиск
    (COMTR, COMTR_IZMIR, W_SINGAPORE, None, None, W_SINGAPORE),
]

COM_CASES = [
    # Тут нет рекламы
    # Тут нет основного региона, так что обрабатываем пользователей отовсюду
    # Тикет по теме - SERP-30522

    # Пользователь из случайного региона
    # COM из COMTR
    (COM, COMTR_IZMIR, None, None, None, COMTR_IZMIR),
    # COM из мира
    (COM, W_SEATTLE, None, None, None, W_SEATTLE),

    # Залогиненный пользователь из случайного региона
    # COM из COMTR
    (COM, COMTR_IZMIR, COMTR_ANTALIA, None, None, COMTR_ANTALIA),
    # COM из мира
    (COM, W_SEATTLE, W_SINGAPORE, None, None, W_SINGAPORE),

    # Поискали что-то в неизвестном регионе и прислали ссылку соседу в другом непонятном регионе
    # COM из COMTR
#    (COM, COMTR_IZMIR, COMTR_ANTALIA, COMTR_KAYSERI, None, COMTR_KAYSERI),
    # COM из мира
#    (COM, W_SEATTLE, W_SINGAPORE, W_OSLO, None, W_OSLO),

    # Пришли черте-откуда и выставили регион
    # COM из COMTR
#    (COM, COMTR_IZMIR, COMTR_ANTALIA, COMTR_KAYSERI, COMTR_MERSIN, COMTR_MERSIN),
    # COM из мира
#    (COM, W_SEATTLE, W_SINGAPORE, W_OSLO, W_HARTUM, W_HARTUM),
    # COM из мира(страна)
#    (COM, W_SEATTLE, W_SINGAPORE, W_OSLO, CHINA, CHINA),

    # Внезапный кейс по мотивам тикета SERP-29685
    # Мы случайно попали c lr!= текущему региону
    # Работает для ru, com, comtr
#    (COM, RU_VLADIVOSTOK, None, COMTR_KAYSERI, None, COMTR_KAYSERI),
]


class BaseTestRegions(BaseFuncTest):
    """
    Проверки определения региона
    test_direct UPPER YABS
    test_web UPPER WEB
    com скиппаем
    """
    LAAS = True

    def base_test(self, query, tld, ip, tune, lr, rstr):

        query.add_params({
            'text': TEXT_BY_TLD[tld],
        })

        if lr:
            query.replace_params({'lr': REGION[lr]})

        if rstr:
            query.replace_params({'rstr': "-" + REGION[rstr]})

        if self.LAAS:
            if tune:
                query.headers.set_custom_headers({"X-Region-City-Id": REGION[tune]})
                query.headers.set_custom_headers({"X-Region-Suspected": REGION[ip]})
            else:
                query.headers.set_custom_headers({"X-Region-City-Id": REGION[ip]})
        else:
            if ip:
                query.headers.set_forward_for(IP[ip])
            if tune:
                query.headers.cookie.set_yandex_gid(REGION[tune])

        query.set_host(tld)

        return query

    def base_test_direct(self, query, tld, ip, tune, lr, rstr, result):
        query = self.base_test(
            query, tld, ip, tune, lr, rstr
        )
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert [str(REGION[result])] == yabs_setup[0]['tune-region-id']

    def base_test_web(self, query, tld, ip, tune, lr, rstr, result):
        query = self.base_test(
            query, tld, ip, tune, lr, rstr
        )
        noapache_setup = self.get_noapache_setup(query)
        assert REGION[result] == str(noapache_setup['client_ctx']['WEB']['lr'][0])

    def base_test_web_cr_cookie(self, query, tld, city):
        query.set_params({'text': TEXT_BY_TLD[tld], 'lr': REGION[city]})

        if self.LAAS:
            query.headers.set_custom_headers({'X-Region-City-Id': REGION[city]})
        else:
            query.headers.set_forward_for(IP[city])
        query.headers.cookie.yp.set_cr(tld.lower())

        query.set_host(tld)

        noapache_setup = self.get_noapache_setup(query)
        web_region = noapache_setup['client_ctx']['WEB']["lr"][0]

        assert web_region == int(REGION[city])

    def base_test_web_cr_cookie_redirect(self, query, tld, city, result):
        query.set_params({'text': TEXT_BY_TLD[tld], 'lr': REGION[city]})
        query.add_flags({'infect_mda': 0})

        if self.LAAS:
            query.headers.set_custom_headers({'X-Region-City-Id': REGION[city]})
        else:
            query.headers.set_forward_for_y(IP[city])
        query.headers.cookie.yp.set_cr(result.lower())
        query.set_host(tld)

        resp = self.request(query, require_status=302)

        assert "".join(("yandex.", result)) in resp.headers['location'][0]

        query.set_url(resp.headers['location'][0])
        query.set_host(result)
        self.request(query)

    def base_test_redirects(self, query, tld, ip, tune, lr, result_tld, result_lr):
        query.set_params({'text': TEXT_BY_TLD[tld]})
        query.add_flags({'infect_mda': 0})
        if lr:
            query.replace_params({'lr': REGION[lr]})

        if self.LAAS:
            if tune:
                query.headers.set_custom_headers({"X-Region-City-Id": REGION[tune]})
                query.headers.set_custom_headers({"X-Region-Suspected": REGION[ip]})
            else:
                query.headers.set_custom_headers({"X-Region-City-Id": REGION[ip]})
        else:
            if ip:
                query.headers.set_forward_for(IP[ip])
            if tune:
                query.headers.cookie.set_yandex_gid(REGION[tune])

        query.set_host(tld)
        resp = self.request(query, require_status=302)

        if result_tld:
            assert "".join(("yandex.", result_tld)) in resp.headers['location'][0]
        else:
            assert "yandex." not in resp.headers['location'][0]

        if result_lr:
            assert "".join(("lr=", REGION[result_lr])) in resp.headers['location'][0]
        else:
            assert "lr=" not in resp.headers['location'][0]

        host_tld = result_tld if result_tld else tld

        query.set_url(resp.headers['location'][0])
        query.set_host(host_tld)
        self.request(query)


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestRegions(BaseTestRegions):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("tld", "ip", "tune", "lr", "result_tld", "result_lr"), [
        # Логика для всех стран из КУБ по отношению к RU одинакова
        # Мы не можем зайти на RU из КУБ
        (RU, UA_KHARKOV, None, None, UA, UA_KHARKOV),
        (RU, BY_GOMEL, BY_VITEBSK, None, BY, BY_VITEBSK),
        (RU, KZ_KARAGANDA, None, KZ_ACTOBE, KZ, KZ_ACTOBE),

        # При попытке зайти на КУБ из RU с ru lr - приписываем lr из столицы и редиректим соответствующий поиск
        (UA, RU_VLADIVOSTOK, None, RU_UFA, None, UA_KIEV),

        # Попытки зайти на другой домен в рамках КУБ
        # Тут логика везде симметричная
        # Если IP или tune - редиректим на соответствующий поиск и приписываем lr
        # These cases are failing after WEBREPORT-223 - getting 200 instead of 302 (redirect doesn't happen).
        # In reality there are no redirects in these cases, but the test used flag infect_mda=0,
        # which caused redirects until infect mda code was removed. This flag is not used in production.
        # (UA, BY_GOMEL, None, None, BY, BY_GOMEL),
        # (BY, UA_KHARKOV, UA_CHERNIGOV, None, UA, UA_CHERNIGOV),
        # Если lr, то редиректим на то, откуда lr
        # KZ из UA с lr=UA, редиректим на ua с LR=KZ
        (BY, UA_KHARKOV, None, UA_LUTSK, None, BY_MINSK),
        # COM
        (COM, RU_VLADIVOSTOK, None, None, RU, RU_VLADIVOSTOK),
        (COM, RU_VLADIVOSTOK, RU_IRKUTSK, None, RU, RU_IRKUTSK),
        (COM, UA_KHARKOV, None, None, UA, UA_KHARKOV),
        (COM, BY_GOMEL, BY_VITEBSK, None, BY, BY_VITEBSK),
        # Поискали что-то в неизвестном регионе и прислали ссылку соседу в другом непонятном регионе
        (COM, RU_VLADIVOSTOK, RU_IRKUTSK, RU_UFA, RU, RU_UFA),
        (COM, KZ_KARAGANDA, KZ_KOSTANAY, KZ_ACTOBE, KZ, KZ_ACTOBE),
        # Пришли черте-откуда и выставили регион
        (COM, W_SINGAPORE, RU_IRKUTSK, RU_UFA, RU, RU_UFA),
        (COM, W_SINGAPORE, KZ_KOSTANAY, KZ_ACTOBE, KZ, KZ_ACTOBE),
    ])
    def test_redirects(self, query, tld, ip, tune, lr, result_tld, result_lr):
        self.base_test_redirects(query, tld, ip, tune, lr, result_tld, result_lr)

    @pytest.mark.parametrize(
        ("tld", "ip", "tune", "lr", "rstr", "result"),
        BASE_CASES +
        COMPLEX_DIRECT_CASES +
        COM_CASES
    )
    def test_direct(self, query, tld, ip, tune, lr, rstr, result):
        self.base_test_direct(query, tld, ip, tune, lr, rstr, result)

    @pytest.mark.skipif(True, reason="")
    @pytest.mark.parametrize(
        ("tld", "ip", "tune", "lr", "rstr", "result"),
        BASE_CASES +
        COMPLEX_CASES +
        COM_CASES
    )
    def test_web(self, query, tld, ip, tune, lr, rstr, result):
        self.base_test_web(query, tld, ip, tune, lr, rstr, result)

    @pytest.mark.skipif(True, reason="")
    @pytest.mark.parametrize(("tld", "city"), [
        (UA, SIMFEROPOL),
        (RU, SIMFEROPOL),
    ])
    def test_web_cr_cookie(self, query, tld, city):
        """
        Особые условия - проверяем, что работает крымская кука
        Просто проверяем, что работает
        """
        self.base_test_web_cr_cookie(query, tld, city)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize(("tld", "city", "result"), [
        (RU, SIMFEROPOL, UA),
        # (UA, SIMFEROPOL, RU),
    ])
    def test_web_cr_cookie_redirect(self, query, tld, city, result):
        """
        Особые условия - проверяем, что работает крымская кука
        Просто проверяем, что оно редиректит
        """
        self.base_test_web_cr_cookie_redirect(query, tld, city, result)
