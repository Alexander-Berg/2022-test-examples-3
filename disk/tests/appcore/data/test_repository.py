# encoding: UTF-8
import contextlib
import unittest

import sqlalchemy as sa
import sqlalchemy.ext.declarative.api as decl
import sqlalchemy.orm as orm
from hamcrest import *

from appcore.data.model import Pageable
from appcore.data.repository import Repository


class Item(object):
    __tablename__ = 'items'

    id = sa.Column(sa.Integer, autoincrement=True, primary_key=True)
    name = sa.Column(sa.String, nullable=False)

    def __init__(self, id=None, name=None):
        self.id = id
        self.name = name


class RepositoryTestCase(unittest.TestCase):
    def setUp(self):
        try:
            self.metadata = Item.metadata
        except AttributeError:
            self.metadata = sa.MetaData()
            decl.instrument_declarative(Item, {}, self.metadata)

        self.engine = sa.create_engine('sqlite://')
        self.session_factory = orm.scoped_session(orm.sessionmaker(self.engine))
        self.repository = Repository(Item, self.session_factory)

        self.metadata.create_all(self.engine)
        items = []
        with self.session_scope() as session:
            for i in xrange(100):
                item = session.merge(Item(name='item%03d' % (99 - i)))
                items.append(item)

            session.flush()
            self.ids = [item.id for item in items]

    @contextlib.contextmanager
    def session_scope(self):
        session = self.session_factory()
        try:
            yield session
            session.commit()
        finally:
            session.close()

    def test_model(self):
        assert_that(self.repository.model, equal_to(Item))

    def test_session(self):
        assert_that(self.repository.session, instance_of(orm.Session))

    def test_query(self):
        assert_that(self.repository.query, instance_of(orm.Query))

    def test_find_iter(self):
        with self.session_scope():
            for i, item in enumerate(self.repository.find_iter(sort=Item.id)):
                assert_that(item.id, equal_to(self.ids[i]))

    def test_find_paged(self):
        with self.session_scope():
            page = self.repository.find_paged(
                Pageable(2, 10),
                sort=Item.id,
            )
            assert_that(
                page,
                has_properties(
                    offset=2,
                    size=10,
                    total=100,
                    items=has_length(10),
                )
            )
            assert_that(page.items[0], has_properties(id=3))

    def test_find_one(self):
        with self.session_scope():
            item = self.repository.find_one(Item.name == 'item000')
            assert_that(
                item,
                all_of(
                    is_not(None),
                    has_properties(id=self.ids[-1]),
                ),
            )

    def test_find_first(self):
        with self.session_scope():
            item = self.repository.find_first(
                Item.name.like('item%'),
                Item.name.desc(),
            )
            assert_that(
                item,
                all_of(
                    is_not(None),
                    has_properties(id=self.ids[0]),
                ),
            )

        with self.session_scope():
            item = self.repository.find_first(Item.name == 'non existent')
            assert_that(item, is_(None))

    def test_get(self):
        with self.session_scope():
            item = self.repository.get(self.ids[0])
            assert_that(
                item,
                all_of(
                    is_not(None),
                    has_properties(id=self.ids[0]),
                ),
            )

        with self.session_scope():
            item = self.repository.get(100500)
            assert_that(item, is_(None))

    def test_save(self):
        new_name = 'unexpectedly new name'

        with self.session_scope():
            item = self.repository.get(self.ids[0])
            item.name = new_name
            saved = self.repository.save(item)
            assert_that(saved.name, equal_to(new_name))

        with self.session_scope():
            item = self.repository.get(self.ids[0])
            assert_that(
                item,
                has_properties(name=new_name),
            )

    def test_delete(self):
        with self.session_scope():
            item = self.repository.get(self.ids[0])
            self.repository.delete(item)

        with self.session_scope():
            item = self.repository.get(self.ids[0])
            assert_that(item, is_(None))
