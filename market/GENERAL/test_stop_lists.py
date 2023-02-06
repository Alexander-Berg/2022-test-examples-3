#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import NavCategory, Offer
from core.testcase import TestCase, main
from core.matcher import NotEmpty, Contains
from core.types.autogen import Const


class T(TestCase):
    @classmethod
    def prepare_final_stop_list(cls):
        """
        Добавляем стоп-слова, которые будут вырезаться,
        если остались только они в запросе (при наличии hid).
        """
        cls.index.navtree += [
            NavCategory(hid=Const.TIRES_HID, nid=23456),
        ]

        cls.reqwizard.on_default_request().respond()

        # реквизард отвечает WholeQueryToRemove=1 если запрос целиком состоит из стоп-слов
        cls.reqwizard.on_request("для").respond(remove_query=True)
        cls.reqwizard.on_request("от").respond(remove_query=True)

        cls.index.offers += [Offer(title='шина от хонда', hid=Const.TIRES_HID)]

    def test_final_stop_list(self):
        """
        Проверяем, что стоп-слово не вырезается, если не задан hid.
        И вырезается, если hid задан.
        """

        # только стоп-слово с hid
        for stop_word in ['для', 'от']:
            response = self.report.request_json('place=prime&text={}&hid=90490&debug=da'.format(stop_word))
            self.assertFragmentIn(response, {'results': [NotEmpty()]})
            self.assertFragmentIn(response, {'reqwizardText': Contains('hyper_categ_id:"90490"')})

        # только стоп-слово без hid
        response = self.report.request_json('place=prime&text=для&debug=da')
        self.assertFragmentNotIn(response, {'results': [NotEmpty()]})
        self.assertFragmentIn(response, {'reqwizardText': Contains('для::')})

        # только стоп-слово без hid, но по нему находятся документы
        response = self.report.request_json('place=prime&text=от&debug=da')
        self.assertFragmentIn(response, {'results': [NotEmpty()]})
        self.assertFragmentIn(response, {'reqwizardText': Contains('от::')})


if __name__ == '__main__':
    main()
