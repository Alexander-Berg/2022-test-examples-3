#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClothesIndex,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    ModelDescriptionTemplates,
    Offer,
    Picture,
    PictureSignature,
    VCluster,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, visual=True),
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=1,
                micromodel="{Type}",
                friendlymodel=["{Type}"],
                model=[("Характеристики", {"Тип": "{Type}"})],
                seo="{return $Type; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(
                hid=1,
                param_id=2000,
                name=u"Тип",
                xslname="Type",
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='носки')],
            )
        ]

        cls.index.vclusters += [
            VCluster(
                hid=1,
                vclusterid=1000000101,
                title='visual cluster 1-1',
                clothes_index=[ClothesIndex([179], [179], [179])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=10)])],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000102,
                title='visual cluster 1-2',
                clothes_index=[ClothesIndex([22], [22], [22])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=11)])],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000103,
                title='visual cluster 1-3',
                clothes_index=[ClothesIndex([174], [174], [174])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=12)])],
                glparams=[GLParam(param_id=2000, value=1)],
            ),
            VCluster(
                hid=2,
                vclusterid=1000000109,
            ),
        ]
        cls.index.offers += [
            Offer(vclusterid=1000000101),
            Offer(vclusterid=1000000102),
            Offer(vclusterid=1000000103),
        ]

    def test_xml_output(self):
        response = self.report.request_xml('place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
                <model id="1000000103">
                    <name>visual cluster 1-3</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

    def test_xml_output_unexisted_version(self):
        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1&rearr-factors=market_picture_version=10'
        )
        self.assertFragmentNotIn(
            response,
            '''
            <search_results>
              <offers>
                <model/>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

    def test_json_output(self):
        response = self.report.request_json('place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 1000000102, "entity": "product", "titles": {"raw": "visual cluster 1-2"}},
                    {"id": 1000000103, "entity": "product", "titles": {"raw": "visual cluster 1-3"}},
                ]
            },
            preserve_order=True,
        )

        # пустая выдача и total=0
        response = self.report.request_json('place=visualanalog&vclusterid=1000000109&hid=2&analog-filter=1')
        self.assertFragmentIn(response, {"total": 0, "results": []})

    def test_missing_pp(self):
        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1&ip=127.0.0.1',
            strict=False,
            add_defaults=False,
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    def test_model_descriptions_existance(self):
        # Ищем аналог и проверяем, то на place=visualanalog работают все виды характеристик модели для аксессуара
        response = self.report.request_json(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1&show-models-specs=full,friendly'
        )
        self.assertFragmentIn(
            response,
            {
                "description": "носки",
                "specs": {
                    "friendly": ["носки"],
                    "full": [{"groupName": "Характеристики", "groupSpecs": [{"name": "Тип", "value": "носки"}]}],
                },
                "lingua": {
                    "type": {
                        "nominative": "носки-nominative",
                        "genitive": "носки-genitive",
                        "dative": "носки-dative",
                        "accusative": "носки-accusative",
                    }
                },
            },
        )

    def test_no_cluster_error(self):
        """
        Проверяем сообщение об ошибке не найденного кластера
        """
        self.error_log.expect("Visual cluster 55 not found. Is it a model?").once()
        try:
            _ = self.report.request_json(
                'place=visualanalog&vclusterid=55&hid=1&analog-filter=1&show-models-specs=full,friendly'
            )
        except:
            pass

    @classmethod
    def prepare_paging(cls):
        cls.index.vclusters += [
            VCluster(
                hid=1,
                vclusterid=1000000104,
                title='visual cluster 1-4',
                clothes_index=[ClothesIndex([174], [174], [174])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=14)])],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000105,
                title='visual cluster 1-5',
                clothes_index=[ClothesIndex([175], [175], [175])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=15)])],
            ),
        ]
        cls.index.offers += [
            Offer(vclusterid=1000000104),
            Offer(vclusterid=1000000105),
        ]

    def test_paging(self):
        """
        Тестирование пэйджинга
        1. Одна первая страница покрывает всё
        2. Одна первая страница и ей не хватает данных
        3. Одна последняя страница и ей не хватает данных
        4. Страница в середине коллекции
        5. Последняя полная страница
        """
        base_query = "place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1"
        base_output_fragment = [
            {"id": 1000000102},
            {"id": 1000000103},
            {"id": 1000000104},
            {"id": 1000000105},
        ]
        total = len(base_output_fragment)

        # 1. numdoc=total
        response = self.report.request_json(base_query + '&numdoc={numdoc}&page=1'.format(numdoc=total))
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment}},
            allow_different_len=False,
            preserve_order=False,
        )
        # 2. page out of range
        numdoc = total + 1
        response = self.report.request_json(base_query + '&numdoc={numdoc}&page=1'.format(numdoc=numdoc))
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment}},
            allow_different_len=False,
            preserve_order=False,
        )
        # 3. incomplete
        numdoc = total - 1
        response = self.report.request_json(base_query + '&numdoc={numdoc}&page=2'.format(numdoc=numdoc))
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment[numdoc:]}},
            allow_different_len=False,
            preserve_order=False,
        )
        # 4.1. regular single
        response = self.report.request_json(base_query + '&numdoc=1&page=2')
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment[1:2]}},
            allow_different_len=False,
            preserve_order=False,
        )

        # 4.2 regular
        response = self.report.request_json(base_query + '&numdoc=2&page=2')
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment[2:4]}},
            allow_different_len=False,
            preserve_order=False,
        )
        # 5. last
        numdoc = total / 2
        page = 2
        response = self.report.request_json(
            base_query + '&numdoc={numdoc}&page={page}'.format(numdoc=numdoc, page=page)
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment[numdoc:]}},
            allow_different_len=False,
            preserve_order=False,
        )

        self.access_log.expect(total_renderable=str(total)).times(6)


if __name__ == '__main__':
    main()
