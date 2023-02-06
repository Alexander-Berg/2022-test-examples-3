#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.report import ReportSearchType
from core.types import GLType, Offer
from core.testcase import TestCase, main
from core.matcher import Absent, Greater, NotEmpty, Regex, LessEq

from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [Offer(title='iphone')]

        cls.settings.cloud_service = 'test_report_lite'

    def test_num_columns(self):
        """Тест-ограничение на размер записи лога. Правила добавления колонок в лог:
        https://wiki.yandex-team.ru/market/procedures/pakt-o-degradacii/#praviladobavlenijanovyxlogovidobavlenienovyxpolejjvtekushhielogi
        """
        self.report.request_json('place=prime&text=iphone')
        self.access_log.expect_num_columns(num_columns=LessEq(72)).all()

    def test_total_counts(self):
        self.report.request_json('place=prime&text=iphone', headers={'X-Market-Req-ID': 'test_req_id'})
        self.access_log.expect(
            total_documents_processed=1,
            total_documents_accepted=1,
            x_market_req_id='test_req_id',
            partial_answer=0,
            cloud_service='test_report_lite',
            search_type=ReportSearchType.META_ONLY,
        )

    def test_base_search_elapsed(self):
        '''Проверяем, что записанное время, проведенное в базовом, не нулевое'''
        self.report.request_json('place=prime&text=iphone')
        self.access_log.expect(base_search_elapsed=Greater(0))

    def test_pp(self):
        """
        Что проверяем: наличие поля pp в логе
        """
        self.report.request_json('place=prime&text=iphone&pp=18')
        self.access_log.expect(pp=18)

    def test_rgb_none(self):
        """
        Что проверяем: наличие поля rgb в логе
        По умолчанию GREEN
        """
        self.report.request_json('place=prime&text=iphone')
        self.access_log.expect(rgb='GREEN')

    def test_rgb_green(self):
        """
        Что проверяем: наличие поля rgb в логе
        Зеленый маркет
        """
        self.report.request_json('place=prime&text=iphone&rgb=Green')
        self.access_log.expect(rgb='GREEN')

    def test_rgb_green_with_blue(self):
        """
        Что проверяем: наличие поля rgb в логе
        Зеленый маркет с синими оферами помечается GREEN
        """
        self.report.request_json('place=prime&text=iphone&rgb=Green_with_blue')
        self.access_log.expect(rgb='GREEN')

    def test_rgb_blue(self):
        """
        Что проверяем: в логах пишется всегда rgb=GREEN
        """
        self.report.request_json('place=prime&text=iphone&rgb=BLUE')
        self.access_log.expect(rgb='GREEN')

    def test_cpu_time_us(self):
        """
        Что проверяем: наличие и отличие от нуля поля cpu_time_us_meta в логе
        """
        self.report.request_json('place=prime&text=iphone')
        self.access_log.expect(cpu_time_us=Greater(0), cpu_time_us_meta=Greater(0))

    @classmethod
    def prepare_merge_sub_requests_stats(cls):
        """
        Подготовка данных для проверки сбора статистики с подзапросов
        """

        cls.index.gltypes = [
            GLType(param_id=202, hid=10, gltype=GLType.ENUM, values=list(range(40, 51)), unit_name="Size"),
            GLType(param_id=203, hid=10, gltype=GLType.BOOL, unit_name="Bool which yes"),
            GLType(param_id=204, hid=10, gltype=GLType.BOOL, unit_name="Bool which no"),
            GLType(param_id=205, hid=10, gltype=GLType.BOOL, unit_name="Bool which yes and no"),
            GLType(param_id=206, hid=10, gltype=GLType.BOOL, unit_name="Bool which neither yes nor no"),
            GLType(param_id=210, hid=10, gltype=GLType.ENUM, values=list(range(1, 4)), unit_name="Color"),
        ]

        cls.index.offers += [Offer(title="dress" + str(i), hid=10) for i in range(100)]

    def test_merge_sub_requests_stats_with_discounts(self):
        """
        Проверка объединения статистики для access.log с заданным и включенным фильтром скидок
        Этот запрос вызывает подзапрос, в котором обрабатываются документы (100 штук)
        """

        _ = self.report.request_json('place=prime&text=dress&hid=10', headers={'X-Market-Req-ID': 'merge_req_id'})

        self.access_log.expect(
            total_documents_processed=100, total_documents_accepted=100, x_market_req_id='merge_req_id'
        )

    @skip("Тест не проходит, subplace_access_log не проверялся из-за ошибки в testcase.py")
    def test_merge_sub_requests_stats_with_discounts_optimization_info(self):
        """
        Проверка объединения статистики для access.log с заданным и включенным фильтром скидок
        Этот запрос вызывает подзапрос, в котором обрабатываются документы (100 штук)
        """

        _ = self.report.request_json(
            'place=prime&text=dress&hid=10&rearr-factors=optimization_info_in_access_logs=1',
            headers={'X-Market-Req-ID': 'merge_req_id2'},
        )

        self.subplace_access_log.expect(
            total_documents_processed=100,
            total_documents_accepted=100,
            x_market_req_id='merge_req_id2/1',
            request_gta_count=Greater(4),
            different_doc_used_gta=NotEmpty(),
            different_snippet_used_gta=NotEmpty(),
            different_doc_attrs_sum_size=Greater(50),
            different_snippet_attrs_sum_size=Greater(50),
            extra_data_size_sum=Greater(10),
        )
        self.access_log.expect(
            total_documents_processed=100, total_documents_accepted=100, x_market_req_id='merge_req_id2'
        )

    def test_place_report_status(self):
        """Проверяем, что запросы к плейсу report_status не пишется в access_log"""
        self.report.request_bs('place=report_status')
        self.access_log.expect(url=Regex('.*&place=report_status.*')).never()

    def test_response_size_bytes(self):
        self.report.request_json('place=prime&text=iphone')
        self.access_log.expect(response_size_bytes=Greater(0))

    def test_meta_resource_header(self):
        self.report.request_json(
            'place=prime&text=iphone',
            headers={
                'resource-meta': '%7B%22client%22%3A%22pokupki.touch%22%2C%22pageId%22%3A%22blue-market_product%22%2C%22scenario%22%3A%22fetchSkusWithProducts%22%7D'
            },
        )
        self.access_log.expect(
            resource_meta='{"client":"pokupki.touch","pageId":"blue-market_product","scenario":"fetchSkusWithProducts"}',
            client='pokupki.touch',
            client_page_id='blue-market_product',
            client_scenario='fetchSkusWithProducts',
        )

    def test_suspiciousness_header(self):
        self.report.request_json('place=prime&text=iphone', headers={'x-antirobot-suspiciousness-y': '1.0'})
        self.access_log.expect(is_suspicious=1)

    def test_is_antirobot_degradation_header(self):
        self.report.request_json('place=prime&text=iphone', headers={'x-yandex-antirobot-degradation': '1'})
        self.access_log.expect(is_antirobot_degradation=1)

    @classmethod
    def prepare_adult_offer_info(cls):
        """
        Подготовка данных для проверки записи в логи информации о adult запросах
        """
        cls.index.offers += [
            Offer(fesh=100, hid=6091783, hyperid=100, title="Sasha Grey", adult=1),
            Offer(fesh=100, title="Pasha Grey"),
        ]

    def test_attrs_optimization_info(self):
        request = "place=prime&text=pasha"
        _ = self.report.request_json(request)
        self.access_log.expect(
            request_gta_count=Absent(),
            different_doc_used_gta=Absent(),
            different_snippet_used_gta=Absent(),
            different_doc_attrs_sum_size=Absent(),
            different_snippet_attrs_sum_size=Absent(),
            extra_data_size_sum=Absent(),
        )

        request = "place=prime&text=pasha&rearr-factors=optimization_info_in_access_logs=1"
        _ = self.report.request_json(request)
        self.access_log.expect(
            request_gta_count=Greater(4),
            different_doc_used_gta=NotEmpty(),
            different_snippet_used_gta=NotEmpty(),
            different_doc_attrs_sum_size=Greater(50),
            different_snippet_attrs_sum_size=Greater(50),
            extra_data_size_sum=Greater(10),
        )

    def test_header_encoding(self):
        self.report.request_json(
            'place=prime&text=iphone',
            headers={
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5 (Erg\xe4nzendes Update)) '
                'AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1.1 Safari/605.1.15'
            },
        )
        self.access_log.expect(
            user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5 ("
            "Erg\xc3\xa4nzendes Update)) AppleWebKit/605.1.15 ("
            "KHTML, "
            "like Gecko) Version/12.1.1 Safari/605.1.15"
        )

    def test_smm_default(self):
        """
        Тест наличия поля smm=1 по-умолчанию
        """
        _ = self.report.request_json('place=prime&text=laptop')
        _ = self.report.request_json('place=prime&text=laptop&rearr-factors=smm=1.0')

        self.access_log.expect(smm=1).times(2)

    def test_smm_other(self):
        """
        Тест значений поля smm
        """
        _ = self.report.request_json('place=prime&text=laptop&rearr-factors=smm=0.8')
        _ = self.report.request_json('place=prime&hid=10&rearr-factors=smm=0.5')

        self.access_log.expect(smm=0.8)
        self.access_log.expect(smm=0.5)

    def test_nopruncount(self):
        """
        Тест отсутствия поля pruncount
        """
        _ = self.report.request_json('place=prime&text=laptop')
        self.access_log.expect(pruncount=None)

    def test_pruncount(self):
        """
        Тест значения поля pruncount
        """
        _ = self.report.request_json('place=prime&text=laptop&prun-count=10000')
        self.access_log.expect(pruncount=10000)

    def test_viewtype(self):
        """
        Тест значения поля viewtype
        """
        _ = self.report.request_json('place=prime&text=iphone&viewtype=list')
        _ = self.report.request_json('place=prime&blender=1&text=iphone&viewtype=grid')
        self.access_log.expect(viewtype='list').once()
        self.access_log.expect(viewtype='grid').once()

    def test_configuration_hint_header(self):
        self.report.request_json(
            'place=prime&text=iphone', headers={'X-Market-Req-Ctx-Report-Config-Hint': 'configuration-goes-here'}
        )
        self.access_log.expect(x_market_req_ctx_report_config_hint='configuration-goes-here')


if __name__ == '__main__':
    main()
