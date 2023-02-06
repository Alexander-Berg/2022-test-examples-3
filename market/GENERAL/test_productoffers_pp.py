#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import MnPlace, Offer
from core.testcase import TestCase, main
from core.matcher import Contains


class Pp:
    TOUCH_MODELCARD_PROPERTIES_TOP = 601
    TOUCH_MODELCARD_FEEDBACK_TOP = 602
    TOUCH_MODELCARD_QUESTIONS_TOP = 604
    TOUCH_MODELCARD_OFFERS_USED = 614
    TOUCH_MODELCARD_PROPERTIES_DEFAULTOFFER = 636
    TOUCH_MODELCARD_QUESTIONS_DEFAULTOFFER = 639
    TOUCH_MODELCARD_FEEDBACK_DEFAULTOFFER = 648

    TURBOAPP_MODELCARD_PROPERTIES_TOP = 3601
    TURBOAPP_MODELCARD_FEEDBACK_TOP = 3602
    TURBOAPP_MODELCARD_QUESTIONS_TOP = 3604
    TURBOAPP_MODELCARD_OFFERS_USED = 3614
    TURBOAPP_MODELCARD_PROPERTIES_DEFAULTOFFER = 3636
    TURBOAPP_MODELCARD_QUESTIONS_DEFAULTOFFER = 3639
    TURBOAPP_MODELCARD_FEEDBACK_DEFAULTOFFER = 3648


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(hyperid=100, fesh=1, price=10000, bid=10, ts=101001),  # default offer
            Offer(hyperid=100, fesh=2, price=10000, bid=50, ts=101002),  # top-1
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101001).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101002).respond(0.01)

    def request(self, pp):
        req = "place=productoffers&hyperid=100&offers-set=default,list&show-urls=external&pp={}".format(pp)
        return self.report.request_json(req)

    def test_pp_for_properties_on_touch(self):
        self.request(Pp.TOUCH_MODELCARD_PROPERTIES_TOP)

        self.show_log_tskv.expect(shop_id=2, pp=Pp.TOUCH_MODELCARD_PROPERTIES_TOP)
        self.show_log_tskv.expect(shop_id=1, pp=Pp.TOUCH_MODELCARD_PROPERTIES_DEFAULTOFFER)

        self.click_log.expect(shop_id=2, pp=Pp.TOUCH_MODELCARD_PROPERTIES_TOP)
        self.click_log.expect(shop_id=1, pp=Pp.TOUCH_MODELCARD_PROPERTIES_DEFAULTOFFER)

    def test_pp_for_feedback_on_touch(self):
        self.request(Pp.TOUCH_MODELCARD_FEEDBACK_TOP)

        self.show_log_tskv.expect(shop_id=2, pp=Pp.TOUCH_MODELCARD_FEEDBACK_TOP)
        self.show_log_tskv.expect(shop_id=1, pp=Pp.TOUCH_MODELCARD_FEEDBACK_DEFAULTOFFER)

        self.click_log.expect(shop_id=2, pp=Pp.TOUCH_MODELCARD_FEEDBACK_TOP)
        self.click_log.expect(shop_id=1, pp=Pp.TOUCH_MODELCARD_FEEDBACK_DEFAULTOFFER)

    def test_pp_for_properties_on_touch_2(self):
        self.request(Pp.TOUCH_MODELCARD_QUESTIONS_TOP)

        self.show_log_tskv.expect(shop_id=2, pp=Pp.TOUCH_MODELCARD_QUESTIONS_TOP)
        self.show_log_tskv.expect(shop_id=1, pp=Pp.TOUCH_MODELCARD_QUESTIONS_DEFAULTOFFER)

        self.click_log.expect(shop_id=2, pp=Pp.TOUCH_MODELCARD_QUESTIONS_TOP)
        self.click_log.expect(shop_id=1, pp=Pp.TOUCH_MODELCARD_QUESTIONS_DEFAULTOFFER)

    def test_pp_for_properties_on_turboapp(self):
        self.request(Pp.TURBOAPP_MODELCARD_PROPERTIES_TOP)

        self.show_log_tskv.expect(shop_id=2, pp=Pp.TURBOAPP_MODELCARD_PROPERTIES_TOP)
        self.show_log_tskv.expect(shop_id=1, pp=Pp.TURBOAPP_MODELCARD_PROPERTIES_DEFAULTOFFER)

        self.click_log.expect(shop_id=2, pp=Pp.TURBOAPP_MODELCARD_PROPERTIES_TOP)
        self.click_log.expect(shop_id=1, pp=Pp.TURBOAPP_MODELCARD_PROPERTIES_DEFAULTOFFER)

    def test_pp_for_feedback_on_turboapp(self):
        self.request(Pp.TURBOAPP_MODELCARD_FEEDBACK_TOP)

        self.show_log_tskv.expect(shop_id=2, pp=Pp.TURBOAPP_MODELCARD_FEEDBACK_TOP)
        self.show_log_tskv.expect(shop_id=1, pp=Pp.TURBOAPP_MODELCARD_FEEDBACK_DEFAULTOFFER)

        self.click_log.expect(shop_id=2, pp=Pp.TURBOAPP_MODELCARD_FEEDBACK_TOP)
        self.click_log.expect(shop_id=1, pp=Pp.TURBOAPP_MODELCARD_FEEDBACK_DEFAULTOFFER)

    def test_pp_for_properties_on_turboapp_2(self):
        self.request(Pp.TURBOAPP_MODELCARD_QUESTIONS_TOP)

        self.show_log_tskv.expect(shop_id=2, pp=Pp.TURBOAPP_MODELCARD_QUESTIONS_TOP)
        self.show_log_tskv.expect(shop_id=1, pp=Pp.TURBOAPP_MODELCARD_QUESTIONS_DEFAULTOFFER)

        self.click_log.expect(shop_id=2, pp=Pp.TURBOAPP_MODELCARD_QUESTIONS_TOP)
        self.click_log.expect(shop_id=1, pp=Pp.TURBOAPP_MODELCARD_QUESTIONS_DEFAULTOFFER)

    def test_pp_for_cutprice_on_turboapp(self):
        """
        Для запросов из турбоапа pp должен меняться на TURBOAPP_MODELCARD_OFFERS_USED,
        в трассировке должна быть запись вида 'Perform additional request: ...&pp=3614...'
        """
        pp = "&pp={}".format(Pp.TURBOAPP_MODELCARD_OFFERS_USED)

        req = "place=productoffers&hyperid=100&show-cutprice=1&good-state=cutprice&touch=1&pp={}&debug=1&is-turbo-app=1".format(
            Pp.TOUCH_MODELCARD_OFFERS_USED
        )
        response = self.report.request_json(req)
        self.assertFragmentIn(response, {"debug": {"report": {"logicTrace": [Contains(pp)]}}})

        req = "place=productoffers&hyperid=100&show-cutprice=1&good-state=cutprice&touch=1&pp={}&debug=1".format(
            Pp.TOUCH_MODELCARD_OFFERS_USED
        )
        response = self.report.request_json(req)
        self.assertNotIn(pp, response.text)


if __name__ == '__main__':
    main()
