#!/usr/bin/env python
# -*- coding: utf-8 -*-

import datetime
import time

from core.testcase import TestCase


def create_offer(hyperid, waremd5=None, price=None, fesh=None):
    offer = {"entity": "offer", "model": {"id": hyperid}}
    if waremd5 is not None:
        offer["wareId"] = waremd5
    if price is not None:
        offer["prices"] = {"value": str(price)}
    if fesh is not None:
        offer["shop"] = {"id": fesh}
    return offer


def create_product(hyperid, product_type):
    return {"entity": "product", "type": product_type, "id": hyperid}


def create_model(hyperid):
    return create_product(hyperid=hyperid, product_type="model")


def create_model_with_default_offer(model_id, waremd5=None, price=None, fesh=None, model_factory=None):
    m = model_factory(model_id) if model_factory is not None else create_model(model_id)
    m["offers"] = {"items": [create_offer(model_id, waremd5, price, fesh)]}
    return m


def to_list(obj):
    return obj if isinstance(obj, (list, tuple, xrange)) else [obj]


def make_fragment(ids, is_offer=None, f=None):
    if is_offer is None and f is None:
        raise Exception("either is_office or f must be set")
    if is_offer is not None:
        f = create_offer if is_offer else create_model
    return map(f, to_list(ids))


def make_models_fragment(ids):
    """
    Возвращает простой фрагмент для модельной выдачи
    """
    return make_fragment(ids, is_offer=False)


def get_timestamp(year, month, day):
    dt = datetime.date(year, month, day)
    return int(time.mktime(dt.timetuple()))


class SimpleTestCase(TestCase):
    """
    Базовый класс с реализацией некоторых вспомогательных методов
    """

    def assertItemsBlockIn(self, response, ids, is_offer=None, preserve_order=False, item_factory=None):
        items = make_fragment(ids, is_offer, item_factory)
        self.assertFragmentIn(response, {"search": {"results": items}}, preserve_order=preserve_order)

    def assertItemsBlockNotIn(self, response, ids, is_offer=None, item_factory=None):
        items = make_fragment(ids, is_offer, item_factory)
        self.assertFragmentNotIn(response, {"search": {"results": items}})

    def assertItemsIn(self, response, ids, is_offer=None, item_factory=None):
        for i in to_list(ids):
            self.assertFragmentIn(response, {"search": {"results": make_fragment(i, is_offer, item_factory)}})

    def assertItemsNotIn(self, response, ids, is_offer=None, item_factory=None):
        for i in to_list(ids):
            self.assertFragmentNotIn(response, {"search": {"results": make_fragment(i, is_offer, item_factory)}})

    def assertOnlyItemsIn(self, response, ids, all_ids, is_offer=None, preserve_order=False, item_factory=None):
        ids = to_list(ids)
        all_ids = to_list(all_ids)
        self.assertItemsBlockIn(response, ids, is_offer, preserve_order, item_factory)
        self.assertFragmentIn(response, {"search": {"total": len(ids)}})
        other_ids = set(all_ids) - set(ids)
        if len(other_ids) > 0:
            self.assertItemsNotIn(response, list(other_ids), is_offer, item_factory)

    def assertItemsInResponse(self, query, ids, is_offer=None, item_factory=None):
        self.assertItemsIn(self.report.request_json(query), ids, is_offer, item_factory)

    def assertItemsBlockInResponse(self, query, ids, is_offer=None, preserve_order=False, item_factory=None):
        self.assertItemsBlockIn(self.report.request_json(query), ids, is_offer, preserve_order, item_factory)

    def assertItemsNotInResponse(self, query, ids, is_offer=None, item_factory=None):
        self.assertItemsNotIn(self.report.request_json(query), ids, is_offer, item_factory)

    def assertOnlyItemsInResponse(self, query, ids, all_ids, is_offer=None, preserve_order=False, item_factory=None):
        response = self.report.request_json(query)
        self.assertOnlyItemsIn(response, ids, all_ids, is_offer, preserve_order, item_factory)

    def assertResponseIsEmpty(self, query, total=0):
        response = self.report.request_json(query)
        self.assertFragmentIn(response, {"search": {"total": total, "results": []}})

    # models

    def assertNoModelsIn(self, response):
        self.assertFragmentNotIn(response, {"entity": "product"})

    def assertNoModelsInResponse(self, query):
        self.assertNoModelsIn(self.report.request_json(query))

    def assertModelsBlockIn(self, response, ids, preserve_order=False):
        self.assertItemsBlockIn(response, ids, is_offer=False, preserve_order=preserve_order)

    def assertModelsBlockNotIn(self, response, ids):
        self.assertItemsBlockNotIn(response, ids, is_offer=False)

    def assertModelsIn(self, response, ids):
        self.assertItemsIn(response, ids, is_offer=False)

    def assertModelsNotIn(self, response, ids):
        self.assertItemsNotIn(response, ids, is_offer=False)

    def assertOnlyModelsIn(self, response, ids, all_ids, preserve_order=False):
        self.assertOnlyItemsIn(response, ids, all_ids, is_offer=False, preserve_order=preserve_order)

    def assertModelsInResponse(self, query, ids):
        self.assertItemsInResponse(query, ids, is_offer=False)

    def assertModelsBlockInResponse(self, query, ids, preserve_order=False):
        self.assertItemsBlockInResponse(query, ids, is_offer=False, preserve_order=preserve_order)

    def assertModelsNotInResponse(self, query, ids):
        self.assertItemsNotInResponse(query, ids, is_offer=False)

    def assertOnlyModelsInResponse(self, query, ids, all_ids, preserve_order=False):
        self.assertOnlyItemsInResponse(query, ids, all_ids, is_offer=False, preserve_order=preserve_order)

    # offers

    def assertNoOffersIn(self, response):
        self.assertFragmentNotIn(response, {"results": [{"entity": "offer"}]})

    def assertNoOffersInResponse(self, query):
        self.assertNoOffersIn(self.report.request_json(query))

    def assertOffersBlockIn(self, response, model_ids, preserve_order=False):
        self.assertItemsBlockIn(response, model_ids, is_offer=True, preserve_order=preserve_order)

    def assertOffersBlockNotIn(self, response, model_ids):
        self.assertItemsBlockNotIn(response, model_ids, is_offer=True)

    def assertOffersIn(self, response, model_ids):
        self.assertItemsIn(response, model_ids, is_offer=True)

    def assertOffersNotIn(self, response, model_ids):
        self.assertItemsNotIn(response, model_ids, is_offer=True)

    def assertOnlyOffersIn(self, response, model_ids, all_model_ids, preserve_order=False):
        self.assertOnlyItemsIn(response, model_ids, all_model_ids, is_offer=True, preserve_order=preserve_order)

    def assertOffersInResponse(self, query, model_ids):
        self.assertItemsInResponse(query, model_ids, is_offer=True)

    def assertOffersBlockInResponse(self, query, model_ids, preserve_order=False):
        self.assertItemsBlockInResponse(query, model_ids, is_offer=True, preserve_order=preserve_order)

    def assertOffersNotInResponse(self, query, model_ids):
        self.assertItemsNotInResponse(query, model_ids, is_offer=True)

    def assertOnlyOffersInResponse(self, query, model_ids, all_model_ids, preserve_order=False):
        self.assertOnlyItemsInResponse(query, model_ids, all_model_ids, is_offer=True, preserve_order=preserve_order)

    def assertPagingSupportedFor(self, base_query, base_output_fragment):
        """
        Тестирование пэйджинга по некоторой выдаче с фрагментом base_output_fragment
        1. Одна первая страница покрывает всё
        2. Одна первая страница и ей не хватает данных
        3. Одна последняя страница и ей не хватает данных
        4. Страница в середине коллекции
        5. Последняя полная страница
        """

        total = len(base_output_fragment)
        self.assertGreater(total, 1)

        # numdoc=total
        response = self.report.request_json(base_query + '&numdoc={numdoc}&page=1'.format(numdoc=total))
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment}},
            allow_different_len=False,
            preserve_order=False,
        )
        # page out of range
        numdoc = total + 1
        response = self.report.request_json(base_query + '&numdoc={numdoc}&page=1'.format(numdoc=numdoc))
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment}},
            allow_different_len=False,
            preserve_order=False,
        )
        # incomplete
        numdoc = total - 1
        response = self.report.request_json(base_query + '&numdoc={numdoc}&page=2'.format(numdoc=numdoc))
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment[numdoc:]}},
            allow_different_len=False,
            preserve_order=False,
        )
        # regular single
        response = self.report.request_json(base_query + '&numdoc=1&page=2')
        self.assertFragmentIn(
            response,
            {"search": {"total": total, "results": base_output_fragment[1:2]}},
            allow_different_len=False,
            preserve_order=False,
        )

        cnt = 4
        if total > 3:
            # regular
            cnt += 1
            response = self.report.request_json(base_query + '&numdoc=2&page=2')
            self.assertFragmentIn(
                response,
                {"search": {"total": total, "results": base_output_fragment[2:4]}},
                allow_different_len=False,
                preserve_order=False,
            )
        # last
        if total % 2 == 0:
            cnt += 1
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

        self.access_log.expect(total_renderable=str(total)).times(cnt)

    def assertPagingSupportedForModels(self, base_query, ids):
        return self.assertPagingSupportedFor(base_query, make_models_fragment(ids))

    def assertPagingSupportedForOffers(self, base_query, model_ids):
        return self.assertPagingSupportedFor(base_query, make_fragment(model_ids, is_offer=True))
