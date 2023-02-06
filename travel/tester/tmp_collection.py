# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import uuid

from travel.rasp.library.python.common23.db.mongo.mongo import database
from travel.rasp.library.python.common23.utils.code_utils import ContextManagerAsDecorator


class TmpCollection(ContextManagerAsDecorator):
    """
    Создает временную коллекцию. Можно использовать как декоратор функции либо как context manager.

    with TmpCollection('my_coll') as coll:
        coll.insert({'foo': 42})
    # после with коллекции больше нет
    """

    def __init__(self, coll_name=None, db=database):
        self.db = db
        self.coll_name = coll_name or uuid.uuid4().hex

    def __enter__(self):
        return getattr(self.db, self.coll_name)

    def __exit__(self, exc_type, exc_val, exc_tb):
        getattr(self.db, self.coll_name).drop()


tmp_collection = TmpCollection
