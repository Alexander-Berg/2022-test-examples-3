#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    FormalizedParam,
    GLParam,
    GLType,
    HyperCategory,
    MnPlace,
    Offer,
    RedirectWhiteListRecord,
    Suggestion,
)
from core.matcher import NotEmpty, NoKey, Absent, Contains, LikeUrl

import six

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse


# Test queries that result in multiple requests to report under the hood
# (using speller, fuzzy search, etc.).
class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        hid = {100,200}
        """
        # в этих тестах вся суть на тонкостях нахождения офферов при опечатках
        # пантера находит все как бы вы ни ошибались
        cls.settings.enable_panther = False

        cls.index.offers += [
            Offer(title='Щетка хозяйственная для чистки батареи FLORETTA Micro cheap', ts=1, price=1000, hid=123),
            Offer(title='Щетка хозяйственная для чистки батареи FLORETTA Micro expensive', ts=2, price=2000, hid=123),
            Offer(title='Губка хозяйственная для чистки батареи FLORETTA Micro num1', hid=300),
            Offer(title='Губка хозяйственная для чистки батареи FLORETTA Micro num2', hid=300),
            Offer(title='Губка хозяйственная для чистки батареи FLORETTA Micro num3', hid=300),
            Offer(title='Губка хозяйственная для чистки батареи FLORETTA Micro num4', hid=301),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.2)

        cls.speller.on_default_request().respond(originalText=None, fixedText=None)

        cls.speller.on_request(text='мытьё').return_code(503)

        cls.speller.on_request(text='хозяйственные щотки для мытья батарей').respond(
            originalText='хозяйственные щ<fix>о</fix>тки для мытья батарей',
            fixedText='хозяйственные щ<fix>е</fix>тки для мытья батарей',
        )

        #
        cls.index.offers += [
            Offer(title='Переходник Apple Lightning to Micro USB Adapter (MD820)', hid=1),
        ]

        cls.index.offers += [
            Offer(
                title='молоко',
                shop_category_path='категория 1\\категория 2\\категория 3',
                shop_category_path_ids='1\\2\\3',
                original_sku='milk123',
            ),
            Offer(title='колбаса', original_sku='sausage'),
        ]

        cls.speller.on_request(text='колбоса').respond(
            originalText='колб<fix>о</fix>са',
            fixedText='колб<fix>а</fix>са',
            reliability=10000,
        )

        cls.speller.on_request(text='mcro usb to lighting').respond(
            originalText='<fix>mc</fix>ro usb to ligh<fix>ti</fix>ng',
            fixedText='m<fix>i</fix>cro usb to light<fix>n</fix>ing',
        )

        # test_speller_and_before_search_dummy_redirects data
        cls.suggester.on_request(part='mobile phones').respond(
            suggestions=[Suggestion(part='mobile phones', url='/search?hid=123&suggest=1')]
        )
        cls.suggester.on_default_request().respond()

        cls.index.hypertree += [HyperCategory(hid=123)]

        cls.index.hypertree += [HyperCategory(hid=299, children=[HyperCategory(hid=300), HyperCategory(hid=301)])]

        cls.speller.on_request(text='mabile phones').respond(
            originalText='mabile phones', fixedText='m<fix>o</fix>bile phones'
        )

        # test_speller_and_before_search_redirects_with_text data
        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='scissors', url='/search?hid=145&suggest=1'),
        ]

        cls.index.hypertree += [HyperCategory(hid=145)]

        cls.speller.on_request(text='scisssors').respond(originalText='scisssors', fixedText='sci<fix>ss</fix>ors')

        # test_speller_and_parametric_search data
        cls.index.hypertree += [HyperCategory(hid=167)]

        cls.index.gltypes += [
            GLType(param_id=1, hid=167, gltype=GLType.NUMERIC),
        ]

        cls.index.offers += [
            Offer(
                title="фейерверки 30 залпов",
                hid=167,
                glparams=[
                    GLParam(param_id=1, value=30),
                ],
            ),
            Offer(
                title="фейерверки разгони бабулек 30 залпами",
                hid=167,
                glparams=[
                    GLParam(param_id=1, value=30),
                ],
            ),
        ]

        cls.speller.on_request(text='феерверки 30 залпов').respond(
            originalText='феерверки 30 залпов', fixedText='фе<fix>й</fix>ерверки 30 залпов', reliability=10000
        )

        cls.speller.on_request(text='фейерверки 30 залов').respond(
            originalText='фейерверки 30 залов', fixedText='фейерверки 30 зал<fix>п</fix>ов', reliability=5000
        )

        # https://st.yandex-team.ru/MARKETOUT-14238
        # "Надежно" исправляем "опечатку" так, что выдача по исправленному запросу становится пустой.
        # Должен произойти перезапрос без исправления, т.е. с исходным запросом.
        # В результате такое исправление опечатки должно никак не повлиять на выдачу.
        cls.speller.on_request(text='фейерверки 30 залпов').respond(
            originalText='фейерверки 30 залпов', fixedText='фейерверки 30 залп<fix>ик</fix>ов', reliability=100500
        )

        cls.formalizer.on_request(hid=167, query="фейерверки 30 залпов").respond(
            formalized_params=[
                FormalizedParam(
                    param_id=1, value=30, is_numeric=True, value_positions=(11, 13), param_positions=(14, 20)
                )
            ]
        )

        cls.formalizer.on_default_request().respond()

    def test_fuzzy_search(self):
        """
        MARKETOUT-10917
        Проверяем, что кворум работает всегда, кроме запросов с пустым &text.
        При этом на выдачу не отдаётся "isFuzzySearch": False
        """

        # 1. Офферы находятся по точному соответствию

        response = self.report.request_json('place=prime&text=щетки+для+батарей')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro expensive"}},
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro cheap"}},
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentIn(response, {"isFuzzySearch": Absent()}, preserve_order=True)

        # 2. Офферы находятся по неточному соответствию

        response = self.report.request_json('place=prime&text=хозяйственные+щетки+для+мытья+батарей')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro expensive"}},
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro cheap"}},
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentIn(response, {"isFuzzySearch": Absent()}, preserve_order=True)

        # 3. Всё то же самое при сортировке по цене

        response = self.report.request_json('place=prime&text=хозяйственные+щетки+для+мытья+батарей&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro cheap"}},
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro expensive"}},
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentIn(response, {"isFuzzySearch": Absent()}, preserve_order=True)

        # 4. Офферы не находятся даже с кворумом

        response = self.report.request_json('place=prime&text=хозяйственные+щетки+для+мытья+батарей+и+слонов')
        self.assertFragmentNotIn(response, {"entity": "offer"}, preserve_order=True)

        # 5. В аксесс-логе всегда кворум

        self.access_log.expect(
            redirect_info='', test_ids='', query_corrected_by_speller='', fuzzy_search_used='1'
        ).times(4)

        # 6. На запросах без текста кворума нет
        response = self.report.request_json('place=prime&hid=167')
        self.assertFragmentIn(response, {"isFuzzySearch": Absent()}, preserve_order=True)

        self.access_log.expect(redirect_info='', test_ids='', query_corrected_by_speller='', fuzzy_search_used='0')

    def test_shiny_speller_request(self):
        '''Проверяем какой запрос отправляется в spellchecker
        запрос содержит текст запроса пользователя
        параметр lang=ru,en,*
        и пр.
        '''
        self.report.request_json('place=prime&text=mcro+usb+to+lightin')

        self.external_services_trace_log.expect(
            target_module='Speller',
            query_params='/misspell/check?fix=1&lang=ru%2Cen%2C*&options=65&srv=market&text=mcro+usb+to+lightin',
        )

    def test_speller_no_fix(self):
        '''spellchecker не вернул результата - ошибка не была пофикшена - ничего не найдено'''
        response = self.report.request_json('place=prime&text=mcro+usb+to+lightin')
        self.assertFragmentNotIn(response, {"entity": "offer"}, preserve_order=True)

        self.access_log.expect(redirect_info='', test_ids='', query_corrected_by_speller='', fuzzy_search_used='1')

    def test_speller_has_fix(self):
        '''По запросу [mcro usb to lighting] ничего не найдено
        После получения пустого серпа был сделан перезапрос
        с исправленным текстом [micro usb to lighting]
        '''

        response = self.report.request_json('place=prime&text=mcro+usb+to+lighting')
        self.assertEqual(1, response.count({"entity": "offer"}, preserve_order=True))
        self.assertFragmentIn(
            response,
            {
                "spellchecker": {
                    "probablyTypo": False,
                    "old": "mcro usb to lighting",
                    "new": {
                        "raw": "micro usb to lightning",
                        "highlighted": [
                            {"value": "m"},
                            {"value": "i", "highlight": True},
                            {"value": "cro usb to light"},
                            {"value": "n", "highlight": True},
                            {"value": "ing"},
                        ],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(redirect_info='', test_ids='', query_corrected_by_speller=u'micro usb to lightning')

    def test_speller_has_fix_with_etc_results(self):
        '''По запросу [mcro usb to lighting] в категории 1 не находится оффера
        но находится виджет 128 usb adapters
        проверяем что при этом будет исправлена ошибка
        '''
        # проверяем не смотря на то что results не пуст - происходит перезапрос с исправлением опечатки
        response = self.report.request_json(
            'place=prime&text=mcro+usb+to+lighting&hid=1&additional_entities=articles,collections'
        )
        self.assertEqual(1, response.count({"entity": "offer"}))
        self.assertFragmentIn(
            response,
            {
                "spellchecker": {
                    "probablyTypo": False,
                    "old": "mcro usb to lighting",
                    "new": {"raw": "micro usb to lightning"},
                }
            },
        )

    def test_speller_and_fuzzy(self):
        response = self.report.request_json('place=prime&text=хозяйственные+щотки+для+мытья+батарей')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro expensive"}},
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro cheap"}},
                ]
            },
            preserve_order=True,
        )
        speller_response = {
            "spellchecker": {
                "old": u"хозяйственные щотки для мытья батарей",
                "new": {
                    "raw": u"хозяйственные щетки для мытья батарей",
                    "highlighted": [
                        {"value": u"хозяйственные щ"},
                        {"value": u"е", "highlight": True},
                        {"value": u"тки для мытья батарей"},
                    ],
                },
            }
        }
        self.assertFragmentIn(response, speller_response, preserve_order=True)

        response = self.report.request_json('place=prime&text=хозяйственные+щотки+для+мытья+батарей&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro cheap"}},
                    {"titles": {"raw": "Щетка хозяйственная для чистки батареи FLORETTA Micro expensive"}},
                ]
            },
            preserve_order=True,
        )
        self.assertFragmentIn(response, speller_response, preserve_order=True)

        self.access_log.expect(
            redirect_info='',
            test_ids='',
            query_corrected_by_speller='хозяйственные щетки для мытья батарей',
            fuzzy_search_used='1',
        ).times(2)

    def test_speller_fails(self):
        self.report.request_json('place=prime&text=мытьё')
        self.error_log.not_expect('HTTP/1.1 503 Service Unavailable')
        self.external_services_log.expect(service='speller', http_code=503).once()

    def test_speller_and_redirects(self):
        response = self.report.request_json('place=prime&text=mcro+usb+to+lighting&cvredirect=1')

        fixed_text = "micro usb to lightning"
        self.assertFragmentIn(
            response,
            {"redirect": {"params": {"hid": ["1"], "rs": [NotEmpty()], "text": [fixed_text]}, "target": "search"}},
            preserve_order=True,
        )
        rs = response.root['redirect']['params']['rs'][0]

        self.access_log.expect(
            redirect_info=NotEmpty(),
            test_ids='',
            query_corrected_by_speller='micro usb to lightning',
            fuzzy_search_used='1',
        )

        response = self.report.request_json('place=prime&text={fixed_text}&rs={rs}'.format(**locals()))
        self.assertEqual(1, response.count({"entity": "offer"}, preserve_order=True))
        self.assertFragmentIn(
            response,
            {
                "spellchecker": {
                    "old": "mcro usb to lighting",
                    "new": {
                        "raw": "micro usb to lightning",
                        "highlighted": [
                            {"value": "m"},
                            {"value": "i", "highlight": True},
                            {"value": "cro usb to light"},
                            {"value": "n", "highlight": True},
                            {"value": "ing"},
                        ],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(
            redirect_info='', test_ids='', query_corrected_by_speller='micro usb to lightning', fuzzy_search_used='1'
        )

    def test_blender(self):
        response = self.report.request_json('place=prime&text=mcro+usb+to+lighting&cvredirect=1')

        fixed_text = "micro usb to lightning"
        self.assertFragmentIn(
            response,
            {"redirect": {"params": {"hid": ["1"], "rs": [NotEmpty()], "text": [fixed_text]}, "target": "search"}},
            preserve_order=True,
        )
        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json('place=blender&text={fixed_text}&rs={rs}'.format(**locals()))
        self.assertEqual(1, response.count({"entity": "offer"}, preserve_order=True))
        self.assertFragmentIn(
            response,
            {
                "spellchecker": {
                    "old": "mcro usb to lighting",
                    "new": {
                        "raw": "micro usb to lightning",
                        "highlighted": [
                            {"value": "m"},
                            {"value": "i", "highlight": True},
                            {"value": "cro usb to light"},
                            {"value": "n", "highlight": True},
                            {"value": "ing"},
                        ],
                    },
                }
            },
            preserve_order=True,
        )

    def test_speller_and_before_search_dummy_redirects(self):
        response = self.report.request_json('place=prime&text=mabile+phones&cvredirect=1&pp=18')

        fixed_text = "mobile phones"
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "url": LikeUrl.of("/search?hid=123&suggest=1&was_redir=1&suggest_text={0}".format(fixed_text)),
                    "params": NoKey("params"),
                }
            },
            preserve_order=True,
        )
        rs = urlparse.parse_qs(response.root['redirect']['url'])['rs'][0]

        self.access_log.expect(
            redirect_info=NotEmpty(), query_corrected_by_speller='mobile phones', fuzzy_search_used='1'
        )

        response = self.report.request_json('place=prime&text={fixed_text}&rs={rs}'.format(**locals()))
        self.assertFragmentIn(
            response,
            {
                "spellchecker": {
                    "old": "mabile phones",
                    "new": {
                        "raw": "mobile phones",
                        "highlighted": [{"value": "m"}, {"value": "o", "highlight": True}, {"value": "bile phones"}],
                    },
                }
            },
            preserve_order=True,
        )

        self.access_log.expect(
            redirect_info='', test_ids='', query_corrected_by_speller='mobile phones', fuzzy_search_used='1'
        )

    def test_speller_and_before_search_redirects_with_text(self):

        fixed_text = "scissors"
        # запрос с фронта белого маркета по белому списку редиректов получит редирект по сырому урлу
        response = self.report.request_json('place=prime&text=scisssors&cvredirect=1&pp=18&debug=da')
        self.assertFragmentIn(
            response,
            {
                'redirect': {
                    'url': LikeUrl.of(
                        '/search?hid=145&suggest=1&was_redir=1&rt=10&suggest_text={0}'.format(fixed_text)
                    ),
                    "params": NoKey("params"),
                }
            },
        )

        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'TryRequestAndApplySpeller(): We have spelling result with low reliability, so we will search by original query [scisssors]'
                    ),
                    Contains('The report state is [no_reask_speller: true\n]'),
                    Contains(
                        'TryRequestAndApplySpeller(): Original query [scisssors] makes empty serp, will re-run with fiexed query: [scissors]'
                    ),
                ]
            },
            preserve_order=True,
        )

        # запрос с АПИ получит non-dummy редирект
        response = self.report.request_json('place=prime&text=scisssors&cvredirect=1&pp=18&non-dummy-redirects=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "rs": [NotEmpty()],
                        "rt": ["10"],
                        "hid": ["145"],
                        "suggest": ["1"],
                        "suggest_text": [fixed_text],
                    },
                    "target": "search",
                }
            },
            preserve_order=True,
        )

        rs = response.root['redirect']['params']['rs'][0]
        self.access_log.expect(redirect_info=NotEmpty(), query_corrected_by_speller='scissors')

        response = self.report.request_json('place=prime&hid=145&suggest_text=scissors&rs={rs}'.format(**locals()))
        self.assertFragmentIn(
            response,
            {
                "spellchecker": {
                    "old": "scisssors",
                    "new": {
                        "raw": "scissors",
                        "highlighted": [{"value": "sci"}, {"value": "ss", "highlight": True}, {"value": "ors"}],
                    },
                }
            },
            preserve_order=True,
        )

        # после редиректа по белому списку мы получаем бестекстовый поиск в категории
        self.access_log.expect(redirect_info='', query_corrected_by_speller='scissors')

    def test_speller_and_parametric_search(self):
        '''Проверяем как работают друг с другом spellcheker и параметрические редиректы
        Учитываем, что spellchecker может отработать как до получения пустых серпов (при high_reliability)
        так и после - когда мы уже сделали запрос на базовый и получили пустую выдачу

        Проверяем что в любом случае параметрический редирект случается (и он происходит после исправления запроса)
        '''

        fixed_text = 'фейерверки 30 залпов'
        glfilters = ["1:30,30"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        hid = "167"

        def check(text, probablyTypo, cgi=''):

            # делаем запрос с cvredirect=1 - проверяем что полученный редирект - параметрический с исправленным текстом
            response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text) + cgi)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "search",
                        "params": {
                            "was_redir": ["1"],
                            "rs": [NotEmpty()],
                            "hid": [hid],
                            "text": [fixed_text],
                            "glfilter": glfilters,
                        },
                    }
                },
            )

            # применяем rs из редиректа к последующему запросу
            rs = response.root['redirect']['params']['rs'][0]
            response = self.report.request_json(
                'place=prime&text={}&hid={}&rs={}&{}'.format(fixed_text, hid, rs, glfilters_query)
            )

            # проверяем что в результатах находятся два документа соответствующие параметрическому запросу и исправленному тексту
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "isFuzzySearch": Absent(),
                        "isParametricSearch": True,
                        "results": [
                            {"titles": {"raw": "фейерверки 30 залпов"}},
                            {"titles": {"raw": "фейерверки разгони бабулек 30 залпами"}},
                        ],
                    },
                    "filters": [{"id": "1", "isParametric": True}],
                },
            )

            # [феЙерверки 30 залпов]
            hilighted_i = [{"value": "фе"}, {"value": "й", "highlight": True}, {"value": "ерверки 30 залпов"}]
            # [фейерверки 30 залПов]
            hilighted_p = [{"value": "фейерверки 30 зал"}, {"value": "п", "highlight": True}, {"value": "ов"}]

            if probablyTypo is None:
                # проверяем что в выдаче нет spellchecker'а
                self.assertFragmentNotIn(response, {"spellchecker": {}})
            else:
                # проверяем что в выдаче spellchecker указано
                # probablyType=1 если исправление опечатки произошло до совершения поиска (опечатка с высокой вероятностью)
                # probablyTypo=0 если исправление опечатки происходит после совершения поиска и получения пустого серпа
                # также в выдаче spellchecker указан исправленный текст и правильная подсветка исправленного символа
                hilighted = hilighted_i if 'феерверки' in text else hilighted_p
                self.assertFragmentIn(
                    response,
                    {
                        "spellchecker": {
                            "probablyTypo": probablyTypo,
                            "old": text,
                            "new": {"raw": fixed_text, "highlighted": hilighted},
                        }
                    },
                    preserve_order=True,
                )

            # проверяем что запрос исправлен и в нем подсвечено сочетание [30 залпов] (сработал параметрический)
            self.assertFragmentIn(
                response,
                {
                    "query": {
                        "highlighted": [
                            {"value": "фейерверки ", "highlight": NoKey("highlight")},
                            {"value": "30", "highlight": True},
                            {"value": " ", "highlight": NoKey("highlight")},
                            {"value": "залпов", "highlight": True},
                        ]
                    }
                },
                preserve_order=True,
            )

        # исправление опечатки происходит до запроса на базовые, т.к. reliability = 10000 >= 10000
        check('феерверки 30 залпов', probablyTypo=True)

        # исправление опечатки происходит после пустых серпов, т.к. reliability = 5000 < 10000
        check('фейерверки 30 залов', probablyTypo=False)

        # на запрос c "надежным" исправлением опечатки, дающим пустой серп,
        # происходит перезапрос без исправления "опечатки"
        # https://st.yandex-team.ru/MARKETOUT-14238
        check('фейерверки 30 залпов', probablyTypo=None)

    def test_no_reask(self):
        """Проверяем что при noreask=1 мы не выполняем запрос в spellchecker и не исправляем опечатку
        noreask выставляется если пользователь нажал на [Искать по неисправленному запросу]
        """
        response = self.report.request_json('place=prime&text=фейерверки 30 залов&cvredirect=1&noreask=1&debug=da')
        self.assertTrue('misspell/check?' not in response.text)
        self.assertFragmentNotIn(response, {"spellchecker": NotEmpty()})
        self.assertFragmentIn(response, {'search': {'total': 0, 'results': []}}, allow_different_len=False)

    @classmethod
    def prepare_rerequest_without_speller_on_empty_high_reliable_spelled_serps(cls):
        # Исправляем опечатку с высокой степенью надежности
        cls.speller.on_request(text='вода').respond(
            originalText='вода', fixedText='вод<fix>к</fix>а', reliability=100500
        )

        # Создаем оффер на неисправленный запрос
        cls.index.offers += [Offer(title="вода")]

    def test_rerequest_without_speller_on_empty_high_reliable_spelled_serps(self):
        """Если получили пустой серп при исправлении опечатки с высокой надежностью,
        делаем перезапрос без опечаточника.

        https://st.yandex-team.ru/MARKETOUT-14238
        """

        # Для исправленного запроса "водка" нет подоходящего оффера, поэтому без
        # перезапроса с исходным текстом отдали бы пустой серп.
        # Проверяем, что нашелся оффер для неисправленного запроса
        response = self.report.request_json('place=prime&text=вода')
        self.assertFragmentIn(
            response, {"search": {"isFuzzySearch": Absent(), "results": [{"titles": {"raw": "вода"}}]}}
        )

        # Также проверяем, что не осталось артефактов от исправления запроса
        self.assertFragmentNotIn(response, {"spellchecker": {}})

    def test_shiny_no_spellcheck_if_empty_query(self):
        """Проверяем что если запрос пустой, то запрос в spellchecker не отправляется"""

        self.report.request_json('place=prime&text=not+dropped+text')

        self.report.request_json('place=prime&hid=300')

        self.external_services_trace_log.expect(target_module='Speller', query_params=Contains('text=not+dropped+text'))
        self.external_services_trace_log.expect(target_module='Speller').once()

    @classmethod
    def prepare_no_error_on_timeout(cls):
        cls.speller.on_request(text='timeouted query').return_code(418)
        cls.speller.on_request(text='server error query').return_code(500)

    def test_no_error_on_timeout(self):
        """
        Проверяем отсутствие ошибки при таймауте
        """
        self.report.request_json('place=prime&text=timeouted+query')
        self.report.request_json('place=prime&text=server+error+query')


if __name__ == '__main__':
    main()
