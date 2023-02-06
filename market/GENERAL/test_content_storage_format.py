#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import json

from core.types import (
    Shop,
    GLParam,
    GLType,
    HyperCategory,
    Model,
    NavCategory,
    Opinion,
    Vendor,
    GLValue,
    VendorLogo,
    ModelDescriptionTemplates,
    MarketSku,
    Picture,
)
from core.testcase import TestCase, main
from market.proto.cs.CsCardInfo_pb2 import TModelCard, TMSkuCard

from market.proto.cs.common.common_pb2 import (
    TVendor,
    TPicture,
    TTitle,
    THighlightedTitle,
    TCategory,
    TNavCategory,
    TPictureContainer,
    TFilter,
    TFilterValue,
    TValueGroup,
    TFormattedDescription,
)
from market.proto.content.mbo.CsGumoful_pb2 import (
    Lingua,
    FriendlyExtValue,
    UsedParamsType,
    UsedValueType,
    FullSpecGroup,
    FullSpecValue,
    FullUsedValue,
)


# В этом тесте проверяется идентичность ответов modelinfo и нового сервис быстрого контента content-storage.
# Он живет тут: https://a.yandex-team.ru/svn/trunk/arcadia/market/content_storage_service
# Если у вас упал этот тест, то с вопросами можно идти к: d-burkov (https://abc.yandex-team.ru/services/content_storage/)
# Важно, чтобы при изменении, например, формата спеков, мы поддерживали это в content-storage. Иначе может поломаться фронт


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [Shop(fesh=213, priority_region=213, regions=[213])]

        cls.index.vendors += [
            Vendor(
                vendor_id=100500,
                name='Хороший производитель',
                website='www.nice.ru',
                logos=[VendorLogo(url='//avatars.mds.yandex.net/get-mpic/1234/vendor_logo_2/orig')],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=11, name='Разные карточки', uniq_name='Разные карточки (уникальное)'),
        ]

        cls.index.navtree += [NavCategory(nid=110, hid=11, name='Нид разных карточек')]

        cls.index.gltypes += [
            GLType(
                param_id=100,
                xslname='Material',
                hid=11,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='железо'), GLValue(value_id=2, text='золото')],
                position=1,
            ),
            GLType(param_id=101, xslname='Size', hid=11, gltype=GLType.NUMERIC, unit_name='см', position=2),
            GLType(param_id=102, hid=11, gltype=GLType.BOOL, xslname="WithWifi", position=3),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                vendor_id=100500,
                title='Хорошая модель',
                title_no_vendor='Хорошая модель (без вендора)',
                full_description='Замечательное описание',
                hid=11,
                picinfo='//avatars.mdst.yandex.net/get-mpic/209514/img.jpg#640#480',
                add_picinfo='',
                model_name='Модельное имя',
                opinion=Opinion(total_count=10, rating=4.5, precise_rating=4.71, rating_count=15, reviews=5),
                glparams=[
                    GLParam(param_id=100, value=1),
                    GLParam(param_id=101, value=55),
                    GLParam(param_id=102, value=1),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=10,
                hyperid=1,
                vendor_id=100500,
                title='Хорошая скю',
                descr='Описание скю',
                hid=11,
                picture=Picture(picture_id='sku_pic', width=100, height=200, group_id=1234),
                glparams=[
                    GLParam(param_id=100, value=1),
                    GLParam(param_id=101, value=55),
                    GLParam(param_id=102, value=1),
                ],
            ),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=11,
                micromodel="Из {Material}, размером {Size}",
                friendlymodel=[
                    "Карточка {WithWifi#ifnz}{WithWifi:с вайфаем}{#endif}",
                    "Длиной: {Size}",
                    "Материал: {Material}",
                ],
                model=[
                    (
                        "Технические характеристики",
                        {"Материал": "{Material}", "Наличие wifi": "{WithWifi}", "Размер": "{Size}"},
                    ),
                ],
                seo="{return $Material; #exec}",
            ),
        ]

        # Добавляем модель
        cs_model = TModelCard()
        cs_model.Id = 1
        cs_model.Vendor.CopyFrom(
            TVendor(
                Id=100500,
                Name='Хороший производитель',
                Website='www.nice.ru',
                Logo=TPicture(Key='img.jpg', Namespace='mpic', GroupId=209514),
            )
        )
        cs_model.Title.CopyFrom(TTitle(Raw='Хорошая модель', Highlighted=[THighlightedTitle(Value='Хорошая модель')]))
        cs_model.TitleWithoutVendor.CopyFrom(
            TTitle(
                Raw='Хорошая модель (без вендора)',
                Highlighted=[THighlightedTitle(Value='Хорошая модель (без вендора)')],
            )
        )
        cs_model.Description = 'Из железо, размером 55'
        cs_model.FullDescription = 'Замечательное описание'

        cs_model.Categories.append(
            TCategory(
                Id=11,
                Nid=110,
                Name='Разные карточки',
                FullName='Разные карточки (уникальное)',
                Type='simple',
                CpaType='cpc_and_cpa',
            )
        )
        cs_model.NavNodes.append(TNavCategory(Id=110, Name='Нид разных карточек', FullName='UNIQ-NID-110'))
        cs_model.Pictures.append(
            TPictureContainer(Original=TPicture(Key='img.jpg', Height=480, Width=640, Namespace='mpic', GroupId=209514))
        )

        filter = TFilter()
        filter.Id = 100
        filter.Type = 'enum'
        filter.Name = 'GLPARAM-100'
        filter.XslName = 'Material'
        filter.Kind = 1
        filter.IsGuruLight = True
        filter.Position = 1
        filter.ValuesCount = 1
        filter.Values.append(TFilterValue(Id=1, Value='железо', Found=1, InitialFound=1))
        filter.ValuesGroups.append(TValueGroup(Type='all', ValuesIds=[1]))
        cs_model.Filters.append(filter)

        filter = TFilter()
        filter.Id = 101
        filter.Type = 'number'
        filter.Name = 'GLPARAM-101'
        filter.XslName = 'Size'
        filter.Kind = 1
        filter.IsGuruLight = True
        filter.Position = 2
        filter.Precision = 0
        filter.Unit = 'см'
        filter.Values.append(TFilterValue(Min=5, Max=5, InitialMin=5, InitialMax=5))
        cs_model.Filters.append(filter)

        filter = TFilter()
        filter.Id = 102
        filter.Type = 'boolean'
        filter.Name = 'GLPARAM-102'
        filter.XslName = 'WithWifi'
        filter.Kind = 1
        filter.IsGuruLight = True
        filter.Position = 3
        filter.Values.append(TFilterValue(Id=1, Found=1, InitialFound=1, Value='1'))
        filter.Values.append(TFilterValue(Id=0, Found=0, InitialFound=0, Value='0'))
        cs_model.Filters.append(filter)

        cs_model.IsVirtual = False
        cs_model.ModelCreator = 'market'
        cs_model.ModelName = 'Модельное имя'
        cs_model.Opinions = 10
        cs_model.PreciseRating = 4.71
        cs_model.Rating = 4.5
        cs_model.RatingCount = 15
        cs_model.Reviews = 5

        cs_model.Lingua.CopyFrom(
            Lingua(
                Nominative='железо-nominative',
                Genitive='железо-genitive',
                Dative='железо-dative',
                Accusative='железо-accusative',
            )
        )

        def add_friendly_specs(card):
            card.Specs.Friendly.append('Карточка с вайфаем')
            card.Specs.Friendly.append('Длиной: 55')
            card.Specs.Friendly.append('Материал: железо')

        def add_friendlyext_specs(card):
            ext_value = FriendlyExtValue()
            ext_value.Type = 'spec'
            ext_value.Value = 'Карточка с вайфаем'
            ext_value.UsedParams.append(102)
            ext_value.UsedParamsWithValues.append(
                UsedParamsType(Id=102, UsedValues=[UsedValueType(Value="1", IsFilterable=True)])
            )
            card.Specs.FriendlyExt.append(ext_value)

            ext_value = FriendlyExtValue()
            ext_value.Type = 'spec'
            ext_value.Value = 'Длиной: 55'
            ext_value.UsedParams.append(101)
            ext_value.UsedParamsWithValues.append(
                UsedParamsType(Id=101, UsedValues=[UsedValueType(Value="55", IsFilterable=True)])
            )
            card.Specs.FriendlyExt.append(ext_value)

            ext_value = FriendlyExtValue()
            ext_value.Type = 'spec'
            ext_value.Value = 'Материал: железо'
            ext_value.UsedParams.append(100)
            ext_value.UsedParamsWithValues.append(
                UsedParamsType(Id=100, UsedValues=[UsedValueType(Value="1", IsFilterable=True)])
            )
            card.Specs.FriendlyExt.append(ext_value)

        def add_full_specs(card):
            group = FullSpecGroup()
            group.GroupName = 'Технические характеристики'

            spec = FullSpecValue()
            spec.Name = 'Материал'
            spec.Value = 'железо'
            spec.Description = 'Материал parameter description'
            spec.UsedParams.append(FullUsedValue(Id=100, Name='GLPARAM-100'))
            spec.UsedParamsWithValues.append(
                UsedParamsType(Id=100, Name='GLPARAM-100', UsedValues=[UsedValueType(Value="1", IsFilterable=True)])
            )
            group.GroupSpecs.append(spec)

            spec = FullSpecValue()
            spec.Name = 'Наличие wifi'
            spec.Value = 'есть'
            spec.Description = 'Наличие wifi parameter description'
            spec.UsedParams.append(FullUsedValue(Id=102, Name='GLPARAM-102'))
            spec.UsedParamsWithValues.append(
                UsedParamsType(Id=102, Name='GLPARAM-102', UsedValues=[UsedValueType(Value="1", IsFilterable=True)])
            )
            group.GroupSpecs.append(spec)

            spec = FullSpecValue()
            spec.Name = 'Размер'
            spec.Value = '55'
            spec.Description = 'Размер parameter description'
            spec.UsedParams.append(FullUsedValue(Id=101, Name='GLPARAM-101'))
            spec.UsedParamsWithValues.append(
                UsedParamsType(Id=101, Name='GLPARAM-101', UsedValues=[UsedValueType(Value="55", IsFilterable=True)])
            )
            group.GroupSpecs.append(spec)

            card.Specs.Full.append(group)

        add_friendly_specs(cs_model)
        add_friendlyext_specs(cs_model)
        add_full_specs(cs_model)

        cls.content_storage.add_model_card(cs_model)

        # Добавляем скю
        cs_sku = TMSkuCard()
        cs_sku.Id = 10
        cs_sku.ModelId = 1
        cs_sku.Vendor.CopyFrom(
            TVendor(
                Id=100500,
                Name='Хороший производитель',
                Website='www.nice.ru',
                Logo=TPicture(Key='img.jpg', Namespace='mpic', GroupId=209514),
            )
        )
        cs_sku.Title.CopyFrom(TTitle(Raw='Хорошая cкю', Highlighted=[THighlightedTitle(Value='Хорошая cкю')]))
        cs_sku.Description = 'Описание скю'
        cs_sku.Categories.append(
            TCategory(
                Id=11,
                Nid=110,
                Name='Разные карточки',
                FullName='Разные карточки (уникальное)',
                Type='simple',
                CpaType='cpc_and_cpa',
            )
        )
        cs_sku.NavNodes.append(TNavCategory(Id=110, Name='Нид разных карточек', FullName='UNIQ-NID-110'))
        cs_sku.Pictures.append(
            TPictureContainer(
                Original=TPicture(Key='market_sku_pic,', Height=200, Width=100, Namespace='marketpic', GroupId=1234)
            )
        )
        cs_sku.MarketSkuCreator = 'market'
        add_friendly_specs(cs_sku)
        add_friendlyext_specs(cs_sku)
        add_full_specs(cs_sku)
        cs_sku.FormattedDescription.CopyFrom(
            TFormattedDescription(
                ShortPlain='Описание скю', FullPlain='Описание скю', ShortHtml='Описание скю', FullHtml='Описание скю'
            )
        )

        cls.content_storage.add_sku_card(cs_sku)

    def get_known_thumbnails(self):
        return [
            {
                'namespace': 'marketpic',
                'thumbnails': [
                    {"name": "50x50", "width": 50, "height": 50},
                    {"name": "55x70", "width": 55, "height": 70},
                    {"name": "60x80", "width": 60, "height": 80},
                    {"name": "74x100", "width": 74, "height": 100},
                    {"name": "75x75", "width": 75, "height": 75},
                    {"name": "90x120", "width": 90, "height": 120},
                    {"name": "100x100", "width": 100, "height": 100},
                    {"name": "120x160", "width": 120, "height": 160},
                    {"name": "150x150", "width": 150, "height": 150},
                    {"name": "180x240", "width": 180, "height": 240},
                    {"name": "190x250", "width": 190, "height": 250},
                    {"name": "200x200", "width": 200, "height": 200},
                    {"name": "240x320", "width": 240, "height": 320},
                    {"name": "300x300", "width": 300, "height": 300},
                    {"name": "300x400", "width": 300, "height": 400},
                    {"name": "600x600", "width": 600, "height": 600},
                    {"name": "600x800", "width": 600, "height": 800},
                    {"name": "900x1200", "width": 900, "height": 1200},
                    {"name": "x124_trim", "width": 166, "height": 124},
                    {"name": "x166_trim", "width": 248, "height": 166},
                    {"name": "x248_trim", "width": 332, "height": 248},
                    {"name": "x332_trim", "width": 496, "height": 332},
                ],
            },
            {
                'namespace': 'marketpic_scaled',
                'thumbnails': [
                    {"name": "50x50", "width": 50, "height": 50},
                    {"name": "55x70", "width": 55, "height": 70},
                    {"name": "60x80", "width": 60, "height": 80},
                    {"name": "74x100", "width": 74, "height": 100},
                    {"name": "75x75", "width": 75, "height": 75},
                    {"name": "90x120", "width": 90, "height": 120},
                    {"name": "100x100", "width": 100, "height": 100},
                    {"name": "120x160", "width": 120, "height": 160},
                    {"name": "150x150", "width": 150, "height": 150},
                    {"name": "180x240", "width": 180, "height": 240},
                    {"name": "190x250", "width": 190, "height": 250},
                    {"name": "200x200", "width": 200, "height": 200},
                    {"name": "240x320", "width": 240, "height": 320},
                    {"name": "300x300", "width": 300, "height": 300},
                    {"name": "300x400", "width": 300, "height": 400},
                    {"name": "600x600", "width": 600, "height": 600},
                    {"name": "600x800", "width": 600, "height": 800},
                    {"name": "900x1200", "width": 900, "height": 1200},
                    {"name": "x124_trim", "width": 166, "height": 124},
                    {"name": "x166_trim", "width": 248, "height": 166},
                    {"name": "x248_trim", "width": 332, "height": 248},
                    {"name": "x332_trim", "width": 496, "height": 332},
                ],
            },
            {
                'namespace': 'mpic',
                'thumbnails': [
                    {"name": "1hq", "width": 50, "height": 50},
                    {"name": "2hq", "width": 100, "height": 100},
                    {"name": "3hq", "width": 75, "height": 75},
                    {"name": "4hq", "width": 150, "height": 150},
                    {"name": "5hq", "width": 200, "height": 200},
                    {"name": "6hq", "width": 250, "height": 250},
                    {"name": "7hq", "width": 120, "height": 120},
                    {"name": "8hq", "width": 240, "height": 240},
                    {"name": "9hq", "width": 500, "height": 500},
                    {"name": "x124_trim", "width": 166, "height": 124},
                    {"name": "x166_trim", "width": 248, "height": 166},
                    {"name": "x248_trim", "width": 332, "height": 248},
                    {"name": "x332_trim", "width": 496, "height": 332},
                ],
            },
        ]

    def check_entity(self, report_keys, cs_keys, dont_check_keys):
        for key in report_keys:
            if key not in dont_check_keys:
                assert key in cs_keys

    def test_cs_response_model_diff(self):
        '''
        Проверяем формат ответа модели. Подробности в начале файла
        '''
        response_report = self.report.request_json(
            'place=modelinfo&pp=18&hyperid=1&rids=213&new-picture-format=1&bsformat=2&show-models-specs=friendly,full'
        )
        self.assertFragmentIn(
            response_report,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 1,
                            "titles": {"highlighted": [{"value": "Хорошая модель"}], "raw": "Хорошая модель"},
                            "titlesWithoutVendor": {
                                "highlighted": [{"value": "Хорошая модель (без вендора)"}],
                                "raw": "Хорошая модель (без вендора)",
                            },
                            "description": "Из железо, размером 55",
                            "fullDescription": "Замечательное описание",
                            "categories": [
                                {
                                    "id": 11,
                                    "name": "Разные карточки",
                                    "fullName": "Разные карточки (уникальное)",
                                    "isLeaf": True,
                                    "cpaType": "cpc_and_cpa",
                                    "nid": 110,
                                    "type": "simple",
                                }
                            ],
                            "navnodes": [
                                {
                                    "id": 110,
                                    "name": "Нид разных карточек",
                                    "fullName": "UNIQ-NID-110",
                                    "isLeaf": True,
                                }
                            ],
                            "pictures": [
                                {
                                    "original": {
                                        "key": "img.jpg",
                                        "namespace": "mpic",
                                        "groupId": 209514,
                                        "height": 480,
                                        "width": 640,
                                    },
                                }
                            ],
                            "filters": [
                                {
                                    "id": "100",
                                    "type": "enum",
                                    "name": "GLPARAM-100",
                                    "xslname": "Material",
                                    "subType": "",
                                    "originalSubType": "",
                                    "kind": 1,
                                    "isGuruLight": True,
                                    "position": 1,
                                    "valuesCount": 1,
                                    "values": [
                                        {
                                            "id": "1",
                                            "value": "железо",
                                            "found": 1,
                                            "initialFound": 1,
                                        }
                                    ],
                                    "valuesGroups": [{"type": "all", "valuesIds": ["1"]}],
                                },
                                {
                                    "id": "101",
                                    "type": "number",
                                    "name": "GLPARAM-101",
                                    "xslname": "Size",
                                    "subType": "",
                                    "originalSubType": "",
                                    "kind": 1,
                                    "isGuruLight": True,
                                    "position": 2,
                                    "precision": 0,
                                    "unit": "см",
                                    "values": [
                                        {
                                            "id": "found",
                                            "initialMax": "55",
                                            "initialMin": "55",
                                            "max": "55",
                                            "min": "55",
                                        }
                                    ],
                                },
                                {
                                    "id": "102",
                                    "type": "boolean",
                                    "name": "GLPARAM-102",
                                    "xslname": "WithWifi",
                                    "subType": "",
                                    "originalSubType": "",
                                    "isGuruLight": True,
                                    "kind": 1,
                                    "position": 3,
                                    "values": [
                                        {"id": "1", "found": 1, "initialFound": 1, "value": "1"},
                                        {"id": "0", "found": 0, "initialFound": 0, "value": "0"},
                                    ],
                                },
                            ],
                            "isVirtual": False,
                            "lingua": {
                                "type": {
                                    "accusative": "железо-accusative",
                                    "dative": "железо-dative",
                                    "genitive": "железо-genitive",
                                    "nominative": "железо-nominative",
                                }
                            },
                            "modelCreator": "market",
                            "modelName": {"raw": "Модельное имя"},
                            "opinions": 10,
                            "preciseRating": 4.71,
                            "rating": 4.5,
                            "ratingCount": 15,
                            "reviews": 5,
                            "specs": {
                                "friendly": ["Карточка с вайфаем", "Длиной: 55", "Материал: железо"],
                                "friendlyext": [
                                    {
                                        "type": "spec",
                                        "value": "Карточка с вайфаем",
                                        "usedParams": [102],
                                        "usedParamsWithValues": [
                                            {"id": 102, "values": [{"isFilterable": True, "value": "1"}]}
                                        ],
                                    },
                                    {
                                        "type": "spec",
                                        "value": "Длиной: 55",
                                        "usedParams": [101],
                                        "usedParamsWithValues": [
                                            {"id": 101, "values": [{"isFilterable": True, "value": "55"}]}
                                        ],
                                    },
                                    {
                                        "type": "spec",
                                        "value": "Материал: железо",
                                        "usedParams": [100],
                                        "usedParamsWithValues": [
                                            {"id": 100, "values": [{"isFilterable": True, "value": "1"}]}
                                        ],
                                    },
                                ],
                                "full": [
                                    {
                                        "groupName": "Технические характеристики",
                                        "groupSpecs": [
                                            {
                                                "name": "Материал",
                                                "value": "железо",
                                                "desc": "Материал parameter description",
                                                "usedParams": [{"id": 100, "name": "GLPARAM-100"}],
                                                "usedParamsWithValues": [
                                                    {
                                                        "id": 100,
                                                        "name": "GLPARAM-100",
                                                        "values": [{"isFilterable": True, "value": "1"}],
                                                    }
                                                ],
                                            },
                                            {
                                                "name": "Наличие wifi",
                                                "value": "есть",
                                                "desc": "Наличие wifi parameter description",
                                                "usedParams": [{"id": 102, "name": "GLPARAM-102"}],
                                                "usedParamsWithValues": [
                                                    {
                                                        "id": 102,
                                                        "name": "GLPARAM-102",
                                                        "values": [{"isFilterable": True, "value": "1"}],
                                                    }
                                                ],
                                            },
                                            {
                                                "name": "Размер",
                                                "value": "55",
                                                "desc": "Размер parameter description",
                                                "usedParams": [{"id": 101, "name": "GLPARAM-101"}],
                                                "usedParamsWithValues": [
                                                    {
                                                        "id": 101,
                                                        "name": "GLPARAM-101",
                                                        "values": [{"isFilterable": True, "value": "55"}],
                                                    }
                                                ],
                                            },
                                        ],
                                    }
                                ],
                            },
                            "vendor": {
                                "id": 100500,
                                "name": "Хороший производитель",
                                "website": "www.nice.ru",
                                "logo": {
                                    # Тут всегда старый формат!
                                    "url": "//avatars.mds.yandex.net/get-mpic/1234/vendor_logo_2/orig"
                                },
                            },
                        }
                    ],
                }
            },
            allow_different_len=False,
        )
        report_model = response_report.root['search']['results'][0]

        request_cs = {"model_ids": [1]}
        response_cs = self.content_storage.request_card_info(json.dumps(request_cs))
        cs_model = json.loads(response_cs.content)['models'][0]

        # Как проверяем формат:
        # Все ключи из ответа репорта должны быть в ответе content-storage
        # За исключением тех, что в dont_check_keys

        # Категория
        self.check_entity(report_model['categories'][0], cs_model['categories'][0], ['entity', 'kinds', 'slug'])
        # Навнода
        self.check_entity(report_model['navnodes'][0], cs_model['navnodes'][0], ['entity', 'rootNavnode', 'slug'])
        # Вендор
        self.check_entity(report_model['vendor'], cs_model['vendor'], ['entity', 'slug'])
        # Тайтл
        self.check_entity(report_model['titles'], cs_model['title'], ['highlighted'])
        # Тайтл без вендора
        self.check_entity(report_model['titlesWithoutVendor'], cs_model['titleWithoutVendor'], ['highlighted'])
        # Картинки (в cs только новый формат)
        self.check_entity(report_model['pictures'][0], cs_model['pictures'][0], ['entity', 'signatures'])
        self.check_entity(report_model['pictures'][0]['original'], cs_model['pictures'][0]['original'], ['entity'])
        # Фильтры
        # Енам
        self.check_entity(report_model['filters'][0], cs_model['filters'][0], ['meta'])
        self.check_entity(report_model['filters'][0]['values'][0], cs_model['filters'][0]['values'][0], [])
        self.check_entity(report_model['filters'][0]['valuesGroups'][0], cs_model['filters'][0]['valuesGroups'][0], [])
        # Нумерик
        self.check_entity(report_model['filters'][1], cs_model['filters'][1], ['meta'])
        self.check_entity(report_model['filters'][1]['values'][0], cs_model['filters'][1]['values'][0], [])
        # Bool
        self.check_entity(report_model['filters'][2], cs_model['filters'][2], ['meta'])
        self.check_entity(report_model['filters'][2]['values'][0], cs_model['filters'][2]['values'][0], [])
        self.check_entity(report_model['filters'][2]['values'][1], cs_model['filters'][2]['values'][1], [])
        # Lingua
        self.check_entity(report_model['lingua']['type'], cs_model['lingua'], [])
        # Specs
        self.check_entity(report_model['specs'], cs_model['specs'], ['internal'])
        # Specs - friendly
        assert len(report_model['specs']['friendly']) == len(cs_model['specs']['friendly'])
        # Specs - friendlyExt
        for i in range(3):
            self.check_entity(report_model['specs']['friendlyext'][i], cs_model['specs']['friendlyext'][i], [])
            self.check_entity(
                report_model['specs']['friendlyext'][i]['usedParamsWithValues'][0],
                cs_model['specs']['friendlyext'][i]['usedParamsWithValues'][0],
                [],
            )
            self.check_entity(
                report_model['specs']['friendlyext'][i]['usedParamsWithValues'][0]['values'],
                cs_model['specs']['friendlyext'][i]['usedParamsWithValues'][0]['values'],
                [],
            )
        # Specs - full
        self.check_entity(report_model['specs']['full'][0], cs_model['specs']['full'][0], [])
        for i in range(3):
            self.check_entity(
                report_model['specs']['full'][0]['groupSpecs'][i], cs_model['specs']['full'][0]['groupSpecs'][i], []
            )
            self.check_entity(
                report_model['specs']['full'][0]['groupSpecs'][i]['usedParams'][0],
                cs_model['specs']['full'][0]['groupSpecs'][i]['usedParams'][0],
                [],
            )
            self.check_entity(
                report_model['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0],
                cs_model['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0],
                [],
            )
            self.check_entity(
                report_model['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0]['values'],
                cs_model['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0]['values'],
                [],
            )

        # Простые поля модели
        for key in [
            'description',
            'modelCreator',
            'opinions',
            'ratingCount',
            'reviews',
            'rating',
            'preciseRating',
            'fullDescription',
        ]:
            assert key in report_model and key in cs_model
            assert isinstance(report_model[key], type(cs_model[key]))

    def test_cs_response_sku_diff(self):
        '''
        Проверяем формат ответа скю. Подробности в начале файла
        '''
        response_report = self.report.request_json(
            'place=modelinfo&pp=18&market-sku=10&rids=213&new-picture-format=1&bsformat=2&show-models-specs=msku-friendly,msku-full'
        )
        self.assertFragmentIn(
            response_report,
            {
                "total": 1,
                "results": [
                    {
                        "id": "10",
                        "titles": {"raw": "Хорошая скю"},
                        "vendor": {
                            "id": 100500,
                            "logo": {"url": "//avatars.mds.yandex.net/get-mpic/1234/vendor_logo_2/orig"},
                            "name": "Хороший производитель",
                            "website": "www.nice.ru",
                        },
                        "description": "Описание скю",
                        "categories": [
                            {
                                "id": 11,
                                "nid": 110,
                                "type": "simple",
                                "cpaType": "cpc_and_cpa",
                                "fullName": "Разные карточки (уникальное)",
                                "isLeaf": True,
                                "name": "Разные карточки",
                            }
                        ],
                        "navnodes": [
                            {
                                "id": 110,
                                "name": "Нид разных карточек",
                                "fullName": "UNIQ-NID-110",
                            }
                        ],
                        "formattedDescription": {
                            "fullHtml": "Описание скю",
                            "fullPlain": "Описание скю",
                            "shortHtml": "Описание скю",
                            "shortPlain": "Описание скю",
                        },
                        "marketSkuCreator": "market",
                        "pictures": [
                            {
                                "original": {
                                    "groupId": 1234,
                                    "height": 200,
                                    "width": 100,
                                    "key": "market_sku_pic,",
                                    "namespace": "marketpic",
                                },
                            }
                        ],
                        "product": {"id": 1},
                        "specs": {
                            "friendly": ["Карточка с вайфаем", "Длиной: 55", "Материал: железо"],
                            "friendlyext": [
                                {
                                    "type": "spec",
                                    "usedParams": [102],
                                    "usedParamsWithValues": [
                                        {"id": 102, "values": [{"isFilterable": True, "value": "1"}]}
                                    ],
                                    "value": "Карточка с вайфаем",
                                },
                                {
                                    "type": "spec",
                                    "usedParams": [101],
                                    "usedParamsWithValues": [
                                        {"id": 101, "values": [{"isFilterable": True, "value": "55"}]}
                                    ],
                                    "value": "Длиной: 55",
                                },
                                {
                                    "type": "spec",
                                    "usedParams": [100],
                                    "usedParamsWithValues": [
                                        {"id": 100, "values": [{"isFilterable": True, "value": "1"}]}
                                    ],
                                    "value": "Материал: железо",
                                },
                            ],
                            "full": [
                                {
                                    "groupName": "Технические характеристики",
                                    "groupSpecs": [
                                        {
                                            "name": "Материал",
                                            "value": "железо",
                                            "desc": "Материал parameter description",
                                            "usedParams": [{"id": 100, "name": "GLPARAM-100"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 100,
                                                    "name": "GLPARAM-100",
                                                    "values": [{"isFilterable": True, "value": "1"}],
                                                }
                                            ],
                                        },
                                        {
                                            "name": "Наличие wifi",
                                            "value": "есть",
                                            "desc": "Наличие wifi parameter description",
                                            "usedParams": [{"id": 102, "name": "GLPARAM-102"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 102,
                                                    "name": "GLPARAM-102",
                                                    "values": [{"isFilterable": True, "value": "1"}],
                                                }
                                            ],
                                        },
                                        {
                                            "name": "Размер",
                                            "value": "55",
                                            "desc": "Размер parameter description",
                                            "usedParams": [{"id": 101, "name": "GLPARAM-101"}],
                                            "usedParamsWithValues": [
                                                {
                                                    "id": 101,
                                                    "name": "GLPARAM-101",
                                                    "values": [{"isFilterable": True, "value": "55"}],
                                                }
                                            ],
                                        },
                                    ],
                                }
                            ],
                        },
                    }
                ],
            },
            allow_different_len=False,
        )
        report_sku = response_report.root['search']['results'][0]

        request_cs = {"market_skus": [10]}
        response_cs = self.content_storage.request_card_info(json.dumps(request_cs))
        cs_sku = json.loads(response_cs.content)['mskus'][0]

        # Как проверяем формат:
        # Все ключи из ответа репорта должны быть в ответе content-storage
        # За исключением тех, что в dont_check_keys

        # Категория
        self.check_entity(report_sku['categories'][0], cs_sku['categories'][0], ['entity', 'kinds', 'slug'])
        # Навнода
        self.check_entity(report_sku['navnodes'][0], cs_sku['navnodes'][0], ['entity', 'rootNavnode', 'slug'])
        # Вендор
        self.check_entity(report_sku['vendor'], cs_sku['vendor'], ['entity', 'slug'])
        # Тайтл
        self.check_entity(report_sku['titles'], cs_sku['title'], ['highlighted'])
        # Картинки (в cs только новый формат)
        self.check_entity(report_sku['pictures'][0], cs_sku['pictures'][0], ['entity', 'signatures'])
        self.check_entity(report_sku['pictures'][0]['original'], cs_sku['pictures'][0]['original'], ['entity'])
        # FormattedDescription
        self.check_entity(report_sku['formattedDescription'], cs_sku['formattedDescription'], [])
        # Specs
        self.check_entity(report_sku['specs'], cs_sku['specs'], ['internal'])
        # Specs - friendly
        assert len(report_sku['specs']['friendly']) == len(cs_sku['specs']['friendly'])
        # Specs - friendlyExt
        for i in range(3):
            self.check_entity(report_sku['specs']['friendlyext'][i], cs_sku['specs']['friendlyext'][i], [])
            self.check_entity(
                report_sku['specs']['friendlyext'][i]['usedParamsWithValues'][0],
                cs_sku['specs']['friendlyext'][i]['usedParamsWithValues'][0],
                [],
            )
            self.check_entity(
                report_sku['specs']['friendlyext'][i]['usedParamsWithValues'][0]['values'],
                cs_sku['specs']['friendlyext'][i]['usedParamsWithValues'][0]['values'],
                [],
            )
        # Specs - full
        self.check_entity(report_sku['specs']['full'][0], cs_sku['specs']['full'][0], [])
        for i in range(3):
            self.check_entity(
                report_sku['specs']['full'][0]['groupSpecs'][i], cs_sku['specs']['full'][0]['groupSpecs'][i], []
            )
            self.check_entity(
                report_sku['specs']['full'][0]['groupSpecs'][i]['usedParams'][0],
                cs_sku['specs']['full'][0]['groupSpecs'][i]['usedParams'][0],
                [],
            )
            self.check_entity(
                report_sku['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0],
                cs_sku['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0],
                [],
            )
            self.check_entity(
                report_sku['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0]['values'],
                cs_sku['specs']['full'][0]['groupSpecs'][i]['usedParamsWithValues'][0]['values'],
                [],
            )

        # Простые поля скю
        for key in ['description', 'marketSkuCreator']:
            assert key in report_sku and key in cs_sku
            assert isinstance(report_sku[key], type(cs_sku[key]))

    def test_known_thumbnails_diff(self):
        '''
        Проверям, что тумбы совпадают
        '''
        response_cs = self.content_storage.request_card_info(json.dumps({}))
        cs_thimbs = json.loads(response_cs.content)['knownThumbnails']
        assert cs_thimbs == self.get_known_thumbnails()


if __name__ == '__main__':
    main()
