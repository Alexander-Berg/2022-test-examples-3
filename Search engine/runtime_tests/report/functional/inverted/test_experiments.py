# -*- coding: utf-8 -*-

import os
import pytest
import base64
import urlparse
import json

from report.const import *
from report.functional.web.base import BaseFuncTest

from report.functional.experiments import *

@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestExperiments(BaseExperimentTest):
    def test_experiments_errors(self, query):
        """
        Ошибочный случай
        Вместо base64 какая-то фигня
        """
        query.headers.set_custom_headers({'X-Yandex-ExpBoxes': 'abc,0,76', 'X-Yandex-ExpFlags': '1234431', 'X-Yandex-ExpConfigVersion': '3764'})
        self.request(query)

    def test_experiments_json_errors(self, query):
        """
        Ошибочный случай
        Невалидный json
        """
        query.headers.set_custom_headers({'X-Yandex-ExpBoxes': '123,0,76', 'X-Yandex-ExpFlags': base64.b64encode('[{"HANDLER": "REPORT",}'), 'X-Yandex-ExpConfigVersion': '3764'})
        self.request(query)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_conditions_no_condition(self, query, exp):
        """
        Отсутствие кондишена
        Эксперимент всегда работает
        """
        self.base_test_experiments(query, exp, result=True)

    def test_experiments_conditions_empty_condition(self, query, exp):
        """
        Пустой кондишен
        Эксперимент не срабатывает
        """
        self.base_test_experiments_conditions(query, exp, result=False)

    def test_experiments_handler_error(self, query, exp):
        """
        В Hanlder указан не репорт
        Эксперимент не должен сработать
        """
        exp['HANDLER'] = "REPORT1"
        self.base_test_experiments(query, exp, result=False)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_experiments_report_flags(self, query, exp):
        """
        В эксперименте должны проставиться репортовские флаги
        Выставляем флаг web_rearr, в случае эксперимента он должен отработать и проставить параметры в источник,
        если флаг не применился - то параметр не будет прокинут
        """
        exp['CONTEXT']['MAIN']['REPORT'] = { 'web_rearr': 'FACTORS:textinfo:WRAP_REPORT' }
        resp = self.base_test_experiments(query, exp, result=True)

        assert 'FACTORS:textinfo:WRAP_REPORT' in ';'.join(resp['client_ctx']['WEB']['rearr'])

    @pytest.mark.skipif(True, reason="SEARCH-8142")
    @pytest.mark.parametrize(("ipv6"), [
        True,
        False,
     ])
    def test_experiments_conditions_ipv6(self, query, exp, ipv6):
        """
        Условия ipv6/ipv4
        """
        if ipv6 is True:
            query.headers.set_forward_for_y("2a02:6b8:0:f1f::3b1")
        elif ipv6 is False:
            query.headers.set_forward_for_y("5.45.223.74")
        self.base_test_experiments_conditions(query, exp, condition="ipv6", result=ipv6)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("internal"), [
        True,
        #False,
     ])
    def test_experiments_conditions_internal(self, query, exp, internal):
        """
        Внешняя/внутренняя сеть
        """
        query.set_external(not internal)
        self.base_test_experiments_conditions(query, exp, condition="internal", result=internal)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("tld", "condition"), [
        (RU,    RU),
        (RU,    COMTR),
        (UA,    UA),
        (UA,    COMTR),
        (BY,    BY),
        (BY,    UA),
        (KZ,    KZ),
        (KZ,    COMTR),
        (COMTR, COMTR),
        (COMTR, RU),
        (COM,   COM),
        (COM,   RU),
    ])
    def test_experiments_conditions_tld(self, query, exp, tld, condition):
        """
        Условия по доменам
        """
        self.base_test_experiments_conditions(query, exp, tld=tld, condition="tld:" + condition.lower(), result=(tld == condition))

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize("tld", [ RU, COM, COMTR ])
    @pytest.mark.parametrize("noflags", [ 0, 1 ])
    def test_no_flags_no_experiments(self, query, exp, tld, noflags):
        """
        SERP-51646: no experiments with no-flags param
        """
        self.base_test_experiments_conditions(query, exp, tld=tld, condition="tld:" + tld.lower(),
            result=(False if noflags==1 else True),
            cgi={'no-flags': noflags})

    @pytest.mark.skipif(not (os.environ.get('REPORT_INVERTED') == '1'), reason="Отключение тестов неинвертированной схемы")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("query_type", "condition"), [
        (SMART,    "smart"),
        (SMART,    "desktop"),
        (SMART,    "mobile"),
        (DESKTOP,  "mobile"),
        (DESKTOP,  "desktop"),
        (PAD,      "tablet"),
        (PAD,      "desktop"),
    ])
    def test_experiments_conditions_type(self, query, exp, query_type, condition):
        """
        Условия по типам запроса
        """
        self.base_test_experiments_conditions(query, exp, query_type=query_type, condition=condition, result=type_condition(query_type, condition))

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("l10n", "condition"), [
        (L_RU, L_RU),
        (L_RU, L_UK),
        (L_EN, L_EN),
        (L_EN, L_ID),
        (L_UK, L_UK),
        (L_UK, L_RU),
        (L_BE, L_BE),
        (L_BE, L_UK),
        (L_KK, L_KK),
        (L_KK, L_RU),
        (L_TT, L_TT),
        (L_TT, L_RU),
        (L_TR, L_TR),
        (L_TR, L_UK),
    ])
    def test_experiments_conditions_i18n(self, query, exp,  l10n, condition):
        """
        Условия по выставленному пользователем языку
        """
        tld = RU
        if l10n == L_TR:
            tld = COMTR
        if l10n in [L_EN, L_ID, L_FR, L_DE]:
            tld = COM
        self.base_test_experiments_conditions(query, exp, tld=tld, i18n=l10n, condition='i18n:' + condition, result=(l10n == condition))

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("query_type", "condition", "result"), [
        (PAD_ANDROID_4_3,      "device.OSFamily eq 'Android'", True),
        (PAD_IPAD_7,           "device.OSFamily eq 'Android'", False),
        (PAD_IPAD_7,           "device.OSVersion ge '6'", True),
        (PAD_ANDROID_4_3,      "device.OSVersion ge '6'", False),
        (PAD_IPAD_7,           "device.isTablet", True)
    ])
    def test_experiments_conditions_user_agent(self, query, exp, query_type, condition, result):
        """
        Несколько условий для device
        """
        self.base_test_experiments_conditions(query, exp, query_type=query_type, condition=condition, result=result)

    @pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="GATEWAY не совместим с вывернутой схемо, т.к. нет такого репорта в вывернуй схеме")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("user_agent", "result"), [
        (USER_AGENT_TOUCH_WINDOWS_PHONE,  True),
        (USER_AGENT_PAD_ANDROID_4_3,      False),
    ])
    def test_experiments_conditions_user_agent_gateway(self, query, exp, user_agent, result):
        """
        Несколько условий для device
        """
        query.set_query_type(GATEWAY)
        query.replace_params({'banner_ua': user_agent})
        self.base_test_experiments_conditions(query, exp, condition="device.BrowserEngine eq 'Trident'", result=result)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("cgi", "condition", "result"), [
        ({'text': '123'},      "cgi.text eq '123'", True),
        ({'text': '12'},       "cgi.text eq '123'", False),
        ({'text': '1234'},     "cgi.text eq '123'", False),
        ({'text': 'abs'},      "cgi.text eq '123'", False),
        ({'text': '123'},      "cgi.text == '123'", True),
        ({'text': '123'},      "cgi.text == '321'", False),
        ({'text': '123'},      "cgi.text <= '123'", True),
        ({'text': '123'},      "cgi.text <= '122'", False),
        ({'text': '123'},      "cgi.text >= '123'", True),
        ({'text': '123'},      "cgi.text >= '124'", False),
        ({'text': '123'},      "cgi.text >  '122'", True),
        ({'text': '123'},      "cgi.text >  '123'", False),
        ({'text': '123'},      "cgi.text <  '124'", True),
        ({'text': '123'},      "cgi.text <  '122'", False),
        ({'text': '123'},      "cgi.text != '124'", True),
        ({'text': '123'},      "cgi.text != '123'", False),
        ({'text': '123'},      "cgi.text eq '123'", True),
        ({'text': '123'},      "cgi.text eq '321'", False),
        ({'text': '123'},      "cgi.text le '123'", True),
        ({'text': '123'},      "cgi.text le '122'", False),
        ({'text': '123'},      "cgi.text ge '123'", True),
        ({'text': '123'},      "cgi.text ge '124'", False),
        ({'text': '123'},      "cgi.text gt '122'", True),
        ({'text': '123'},      "cgi.text gt '123'", False),
        ({'text': '123'},      "cgi.text lt '124'", True),
        ({'text': '123'},      "cgi.text lt '122'", False),
        ({'text': '123'},      "cgi.text ne '124'", True),
        ({'text': '123'},      "cgi.text ne '123'", False),
    ])
    def test_experiments_conditions_cgi(self, query, exp, cgi, condition, result):
        """
        Проверяем cgi параметры
        Проверяем все возможные варианты знаков в условиях
        """
        self.base_test_experiments_conditions(query, exp, cgi=cgi, condition=condition, result=result)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("condition", "result"), [
        ("flags.app_host_tag eq 'web'", True),
        ("flags.app_host_tag ne 'web'", False),
    ])
    def test_experiments_conditions_flags(self, query, exp, condition, result):
        """
        Проверяем условия для наличия флагов
        """
        query.set_flags({'app_host_tag': 'web'})
        self.base_test_experiments_conditions(query, exp, condition=condition, result=result)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("query_type", "condition", "result"), [
        (DESKTOP, "handler:YxWeb::Report::Web", True),
        (SMART,   "handler:YxWeb::Report::Web", False),
    ])
    def test_experiments_conditions_report_type(self, query, exp, query_type, condition, result):
        """
        Проверяем тип репорта, репорт, которым обрабатывается запрос определяется его типом
        """
        self.base_test_experiments_conditions(query, exp, query_type=query_type, condition=condition, result=result)

    @pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="GATEWAY не совместим с вывернутой схемо, т.к. нет такого репорта в вывернуй схеме")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("condition", "result"), [
        ("handler:YxWeb::Report::Gateway", True),
        ("handler:YxWeb::Report::Web", False),
    ])
    def test_experiments_conditions_report_type_gateway(self, query, exp, condition, result):
        """
        Проверяем тип репорта, репорт, которым обрабатывается запрос определяется его типом
        """
        query.set_query_type(GATEWAY)
        self.base_test_experiments_conditions(query, exp, condition=condition, result=result)

    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_experiments_log_testid(self, query):
        """
        SERP-32817 Rc Логгировать в access_log сработавшие test-id's
        Кондишен сработал - есть запись в логе
        """
        exp = Experiment(test_id=TEST_ID_LOG)
        resp = self.base_test_experiments_log(query, exp, query_type=DESKTOP, result=True)

        test_id = dict(map(lambda(x): (x, 1), resp.access_log()['test_id'].split(',')))
        assert test_id.get(TEST_ID_LOG)

    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_experiments_log_notestid(self, query, exp):
        """
        SERP-32817 Rc Логгировать в access_log сработавшие test-id's
        Кондишен не сработал - в логе пустое поле
        """
        exp['CONTEXT']['REPORT'] = {"testid": [TEST_ID_NOLOG]}
        exp['CONDITION'] = PAD
        resp = self.base_test_experiments_log(query, exp, query_type=DESKTOP, result=False)

        test_id = dict(map(lambda(x): (x, 1), resp.access_log()['test_id'].split(',')))
        assert not test_id.get(TEST_ID_NOLOG)

    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    def test_experiments_log_testid_302(self, query):
        """
        SERP-32817 Rc Логгировать в access_log сработавшие test-id's
        В случае редиректа: кондишен сработал - есть запись в логе
        """
        exp = Experiment(test_id=TEST_ID_LOG)
        exp['CONDITION'] = "desktop"
        exp['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}

        resp = self.base_test_302(query, exp)

        test_id_str = resp.access_log()['test_id']
        test_id = dict(map(lambda(x): (x, 1), test_id_str.split(',')))
        assert test_id.get(TEST_ID_LOG)

    @pytest.mark.skipif(True, reason="WEBREPORT-582")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_experiments_log_notestid_302(self, query):
        """
        SERP-32817 Rc Логгировать в access_log сработавшие test-id's
        В случае редиректа: кондишен не сработал - в логе пустое поле
        """
        exp = Experiment(test_id=TEST_ID_LOG)
        exp['CONDITION'] = "pad"
        exp['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}

        resp = self.base_test_302(query, exp)

        test_id_str = resp.access_log()['test_id']
        test_id = dict(map(lambda(x): (x, 1), test_id_str.split(',')))
        assert not test_id.get(TEST_ID_LOG)


    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_real_page_yabs_no_A_B(self, query, exp):
        """
        https://st.yandex-team.ru/WEBREPORT-738
        Общий тест для yabs после удаления многоконтекстных экспериентов в c++ модуле experiments
        Передаем для YABS параметр real-page в контексте MAIN
        Проверяем, что запрос к YABS присутствует в контексте МAIN
        Проверяем то, что передается параметр real-page
        """

        p1 = '6'

        exp['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}
        exp['CONTEXT']['MAIN'] = {"YABS": {"rearr": [A_REARR_TEST_PARAM], "real-page": [p1]}}

        query = self.base_query(query, exp)
        exp = self.json_dump_context(query, ['experiments'])
        assert len(exp) > 0
        assert A_REARR_TEST_PARAM in exp[0]['contexts']['MAIN']['YABS']['rearr']
        assert p1 in exp[0]['contexts']['MAIN']['YABS']['real-page']


    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_yabs_rearr_factors(self, query, exp):
        """
        SERP-35008 - Поддержать эксперименты для источника YABS
        Проверяем, что в YABS уходят нужные параметры(передаем в rearr-factors)

        """
        exp['CONTEXT']['MAIN']["YABS"] = {"rearr-factors": [MAIN_YABS_REARR_TEST_PARAM]}
        query = self.base_query(query, exp)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert MAIN_YABS_REARR_TEST_PARAM in ';'.join(yabs_setup[0]['rearr-factors'] if isinstance(yabs_setup[0]['rearr-factors'], list) else [yabs_setup[0]['rearr-factors']])

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_yabs_rearr(self, query, exp):
        """
        SERP-35008 - Поддержать эксперименты для источника YABS
        Проверяем, что в YABS уходят нужные параметры(передаем в rearr)

        """
        exp['CONTEXT']['MAIN']["YABS"] = {"rearr": [MAIN_YABS_REARR_TEST_PARAM]}
        query = self.base_query(query, exp)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        assert MAIN_YABS_REARR_TEST_PARAM in yabs_setup[0]['rearr']

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_yabs_testid(self, query, exp):
        """
        SERP-35089 - Прокидывать test-id в БК
        Просто засетаплен какой-то эксперимент
        """
        exp['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}

        query = self.base_query(query, exp)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        test_id = dict(map(lambda(x): (x, 1), yabs_setup[0]['test-ids'][0].splitlines()))
        assert test_id.get('7049')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_yabs_setup_testid(self, query, exp):
        """
        SERP-35089 - Прокидывать test-id в БК
        Засетаплен источник YABS
        """
        exp['CONTEXT']['MAIN']["YABS"] = {"rearr": [MAIN_YABS_REARR_TEST_PARAM]}

        query = self.base_query(query, exp)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        test_id = dict(map(lambda(x): (x, 1), yabs_setup[0]['test-ids'][0].splitlines()))
        assert test_id.get('7049')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_yabs_testids(self, query):
        """
        SERP-35089 - Прокидывать test-id в БК
        Проверяем для нескольких экспериментов
        """
        exp1 = Experiment(test_id='1234')
        exp1['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}
        exp2 = Experiment(test_id='4321')
        exp1['CONTEXT']['MAIN']["WEB"] = {"rearr": [MAIN_REARR_TEST_PARAM]}
        exp_b64 = ','.join((exp1.to_base64(), exp2.to_base64()))

        self.log(filename="condition.txt", data=exp_b64)

        query.headers.set_custom_headers({'X-Yandex-ExpBoxes': '1234,0,76;4321,0,76', 'X-Yandex-ExpFlags': exp_b64, 'X-Yandex-ExpConfigVersion': '3764'})

        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0

        test_id = dict(map(lambda(x): (x, 1), yabs_setup[0]['test-ids'][0].splitlines()))
        for i in ('1234', '4321'):
            assert test_id.get(i)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_yabs_testids_wizard(self, query, exp_wizard):
        """
        SERP-35089 - Прокидывать test-id в БК
        Визардный эксперимент
        """
        query = self.base_query(query, exp_wizard)
        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0
        test_id = dict(map(lambda(x): (x, 1), yabs_setup[0]['test-ids'][0].splitlines()))
        assert test_id.get(TEST_ID)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_quick(self, query, exp):
        """
        Проверяем какой-то источник за исключением WEB
        """
        exp['CONTEXT']['MAIN']["QUICK"] = {"rearr": [MAIN_REARR_TEST_PARAM]}

        query.set_params({"text": u"новости"})
        resp = self.base_test(query, exp)

        self.base_assert(resp, True, 'QUICK')

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.parametrize("source", [
        "AUTO2"
    ])
    def test_experiments_by_source(self, query, exp, source):
        """
        проверка работы поисточничных экспериментов
        """

        query.add_params({'lr': '2'})
        exp['CONTEXT']['MAIN'][source] = {"rearr": [MAIN_REARR_TEST_PARAM], "weird": ["weird_param"], "gta": ["=qqq"]}

        ctxs = self.base_test_ctxs(query, exp)
        assert ctxs.get('context_rewrite')

        is_complete = None
        for ctx in ctxs['context_rewrite']:
            if ctx['rewrite'].get(source):
                assert 'weird' in ctx['rewrite'][source]
                assert 'weird_param' in ctx['rewrite'][source]['weird'] # SERP-39616
                assert 'gta' in ctx['rewrite'][source]
                assert '=qqq' in ctx['rewrite'][source]['gta']
                is_complete = True
        assert is_complete

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.parametrize(("handler", "result"), [
        ("WEB", True),
        ("VIDEO", False),
        ("REPORT", True),
    ])
    def test_experiments_handler_web(self, query, exp, handler, result):
        """
        SERP-34655 - Для UserSplit cделать возможность задавать HANDLER в конфиге
        Проверяем для WEB
        Для хендлеров WEB и REPORT - работает
        Для других - нет
        """
        exp['HANDLER'] = handler

        self.base_test_experiments(query, exp, result=result)


    @pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="GATEWAY не совместим с вывернутой схемо, т.к. нет такого репорта в вывернуй схеме")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_experiments_perl_gateway(self, query):
        """
        SERP-35089 - Прокидывать test-id в БК
        GATEWAY-3233 - Прокидывать test-ids сработавших экспериментов в perl-gateway
        Передаем test-ids извне и смотрим что ушло в БК
        """
        query.set_query_type(GATEWAY)
        query.add_params({'bpage': '2', 'banner_ua': 'Mobile-Yandex-android-300',
                               'lr': '213', 'search_props': 'test-ids=102 208 4756,something=else', })

        yabs_setup = self.json_dump_context(query, ['yabs_setup'])
        assert len(yabs_setup) > 0

        test_id = dict(map(lambda(x): (x, 1), yabs_setup[0]['test-ids'][0].splitlines()))
        for i in ('102', '208', '4756'):
            assert test_id.get(i)
