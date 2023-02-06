#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, WebErfEntry, WebErfFeatures, WebHerfEntry, WebHerfFeatures
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_web_features_exp(cls):
        cls.index.offers += [
            Offer(title='pylesos 1', url='http://eldorado.ru/p1', waremd5='x18c9f1gD9IWDI01en4Ouw'),
            Offer(title='pylesos 2', url='https://e96.ru/ololo', waremd5='cxiwaDezlIE_I_hLT_h_vw'),
        ]

        cls.index.web_erf_features += [
            WebErfEntry(url='http://eldorado.ru/p1', features=WebErfFeatures(title_comm=1, f_title_idf_sum=0.5)),
            WebErfEntry(url='http://e96.ru/ololo', features=WebErfFeatures(dater_day=15, mtime=1000)),
        ]

        cls.index.web_erf_exp_features += [
            WebErfEntry(url='http://eldorado.ru/p1', features=WebErfFeatures(title_comm=4, add_time=9))
        ]

        cls.index.web_herf_features += [
            WebHerfEntry(host='http://eldorado.ru', features=WebHerfFeatures(owner_enough_clicked=1)),
            WebHerfEntry(host='http://e96.ru', features=WebHerfFeatures(owner_enough_clicked=3, add_time_mp=91)),
        ]

        cls.index.web_herf_exp_features += [
            WebHerfEntry(host='http://eldorado.ru', features=WebHerfFeatures(owner_enough_clicked=2, add_time_mp=12)),
            WebHerfEntry(host='http://e96.ru', features=WebHerfFeatures(owner_enough_clicked=4)),
        ]

        cls.index.models += [Model(title="holodylnik 1", hyperid=201), Model(title="holodylnik 2", hyperid=202)]

        cls.index.web_erf_features += [
            WebErfEntry(
                url='https://market.yandex.ru/product/201',
                features=WebErfFeatures(dater_day=20, fem_and_mas_nouns_portion=4),
            ),
            WebErfEntry(url='market.yandex.ru/product/202', features=WebErfFeatures(title_comm=6)),
        ]

        cls.index.web_erf_exp_features += [
            WebErfEntry(url='https://market.yandex.ru/product/201', features=WebErfFeatures(dater_day=18)),
            WebErfEntry(url='market.yandex.ru/product/202', features=WebErfFeatures(title_comm=7, add_time=2)),
        ]

        cls.index.web_herf_features += [
            WebHerfEntry(host='https://market.yandex.ru', features=WebHerfFeatures(is_wikipedia=1, mascot_13=11))
        ]

        cls.index.web_herf_exp_features += [
            WebHerfEntry(host='https://market.yandex.ru', features=WebHerfFeatures(mascot_13=12))
        ]

    def test_web_features_exp(self):
        """
        Проверяем, что под флагом market_use_exp_static_features используются экспериментальные
        факторы при наличии файлов экспериментальных erf/herf-индексов, даже если они не заданы
        """
        flag = '&rearr-factors=market_use_exp_static_features=1'

        request = 'place=prime&text=pylesos&show-urls=external'

        self.report.request_json(request + '&reqid=1')

        self.feature_log.expect(
            title_comm=1,
            f_title_idf_sum=0.5,
            owner_enough_clicked=1,
            add_time=Absent(),
            add_time_mp=Absent(),
            req_id=1,
            ware_md5='x18c9f1gD9IWDI01en4Ouw',
        )

        self.feature_log.expect(
            dater_day=15,
            mtime=1000,
            owner_enough_clicked=3,
            add_time_mp=91,
            req_id=1,
            ware_md5='cxiwaDezlIE_I_hLT_h_vw',
        )

        self.report.request_json(request + flag + '&reqid=2')

        self.feature_log.expect(
            title_comm=4,
            f_title_idf_sum=Absent(),
            owner_enough_clicked=2,
            add_time=9,
            add_time_mp=12,
            req_id=2,
            ware_md5='x18c9f1gD9IWDI01en4Ouw',
        )

        self.feature_log.expect(
            dater_day=Absent(),
            mtime=Absent(),
            owner_enough_clicked=4,
            add_time_mp=Absent(),
            req_id=2,
            ware_md5='cxiwaDezlIE_I_hLT_h_vw',
        )

        request = 'place=prime&text=holodylnik&show-urls=external'

        self.report.request_json(request + '&reqid=3')

        self.feature_log.expect(
            dater_day=20, fem_and_mas_nouns_portion=4, is_wikipedia=1, mascot_13=11, req_id=3, model_id=201
        )

        self.feature_log.expect(title_comm=6, add_time=Absent(), is_wikipedia=1, mascot_13=11, req_id=3, model_id=202)

        self.report.request_json(request + flag + '&reqid=4')

        self.feature_log.expect(
            dater_day=18,
            fem_and_mas_nouns_portion=Absent(),
            is_wikipedia=Absent(),
            mascot_13=12,
            req_id=4,
            model_id=201,
        )

        self.feature_log.expect(title_comm=7, add_time=2, is_wikipedia=Absent(), mascot_13=12, req_id=4, model_id=202)


if __name__ == '__main__':
    main()
