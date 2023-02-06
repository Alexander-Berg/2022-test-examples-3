# coding: utf-8

import unittest

from sqlalchemy import Column, Integer, String

from components_app.component.db import Db


class TestDb(unittest.TestCase):
    def setUp(self):
        self.db = Db()
        self.db.load_config(dict(source="sqlite:///:memory:"))

    def tearDown(self):
        self.db.stop()

    def test_model(self):
        class Model(self.db.base_model):
            __tablename__ = 'test_table'
            id = Column(Integer, primary_key=True, autoincrement=True)
            name = Column(String)

        self.db.start()

        with self.db.session as session:
            obj = Model()
            obj.name = 'test_name'
            session.add(obj)
            session.commit()

        with self.db.session as session:
            count = session.query(Model).count()
            self.assertEqual(count, 1)

    def test_in_out_session(self):
        class Model(self.db.base_model):
            __tablename__ = 'test_table'
            id = Column(Integer, primary_key=True, autoincrement=True)

        self.db.start()

        with self.db.session as session:
            obj = Model()
            session.add(obj)

        with self.db.session as session:
            count = session.query(Model).count()
            self.assertEqual(count, 0)
