# -*- coding: utf-8 -*-
import datetime
import unittest

import mock

from sqlalchemy import (Table, Column, MetaData,
                        String, BigInteger, PrimaryKeyConstraint)

from mpfs.dao.base import BaseDAOItem, ValuesTemplateGenerator
from mpfs.dao.fields import StringField, UidField
from mpfs.dao.spec_converter import convert_spec_to_sql, SpecAST, AndOperation, OrOperation
from mpfs.dao.query_converter import MongoQueryConverter

metadata = MetaData()

users_table = Table(
    'users', metadata,
    Column('user_id', BigInteger, nullable=False),
    Column('user_name', String, nullable=True),
    Column('address', String, nullable=True),

    PrimaryKeyConstraint('user_id', name='pk_users'),
)


class UsersDAOItem(BaseDAOItem):
    postgres_table_obj = users_table

    uid = UidField(mongo_path='uid', pg_path=users_table.c.user_id)
    name = StringField(mongo_path='data.name', pg_path=users_table.c.user_name)
    address = StringField(mongo_path='data.address', pg_path=users_table.c.address, default_value='No home')


class FindToSelectConverterTestCase(unittest.TestCase):
    def test_simple_find(self):
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql()
        assert query == 'SELECT user_id,user_name,address FROM users'
        assert params == {}

    def test_empty_spec_find(self):
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql({})
        assert query == 'SELECT user_id,user_name,address FROM users'
        assert params == {}

    def test_find_by_uid(self):
        uid = '99996671'
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql(
            {'uid': uid}
        )
        assert query == 'SELECT user_id,user_name,address FROM users WHERE user_id=:param0'
        assert params['param0'] == int(uid)

    def test_find_with_fields(self):
        uid = '99996671'
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql(
            {'uid': uid},
            ['data.address', 'uid']
        )
        assert query == 'SELECT address,user_id FROM users WHERE user_id=:param0'
        assert params['param0'] == int(uid)

        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql(
            {'uid': uid},
            {'data.address': 1, 'uid': 0}
        )
        assert query == 'SELECT address FROM users WHERE user_id=:param0'
        assert params['param0'] == int(uid)

    def test_find_with_limit_and_skip(self):
        name = 'Barack Obama'
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql(
            {'data.name': name},
            limit=5,
            skip=1
        )
        assert query == 'SELECT user_id,user_name,address FROM users WHERE user_name=:param0 LIMIT 5 OFFSET 1'
        assert params['param0'] == name

    def test_find_with_sort(self):
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql(
            {},
            sort=[('data.address', 1)]
        )
        assert query == 'SELECT user_id,user_name,address FROM users ORDER BY address ASC'

    def test_find_complex(self):
        uid = '99996671'
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql(
            {'uid': uid}, ['uid', 'data.address'],
            sort=[('uid', 1), ('data.address', -1)],
            limit=2,
            skip=50
        )
        assert query == 'SELECT user_id,address FROM users WHERE user_id=:param0 ORDER BY user_id ASC, address DESC LIMIT 2 OFFSET 50'

    def test_find_with_in_list(self):
        uids = ['1234567890', '0987654321']
        name = 'John Smith'
        query, params = MongoQueryConverter(UsersDAOItem).find_to_sql(
            {'uid': {'$in': uids}, 'data.name': name}
        )
        assert query == 'SELECT user_id,user_name,address FROM users WHERE (user_name=:param0 AND user_id IN (:param1,:param2))'
        assert params['param0'] == name
        assert params['param1'] == int(uids[0])
        assert params['param2'] == int(uids[1])


class TreeBuilderTestCase(unittest.TestCase):
    @staticmethod
    def convert_key(key):
        return key

    @staticmethod
    def convert_key_value(key, value):
        return key, value

    def test_spec_to_tree_conversion(self):
        spec = {
            '$or': [
                {
                    '$and': [
                        {'field1': {'$exists': 1}},
                        {'field2': {'$lte': 256}},
                        {'field3': 5}
                    ]
                },
                {'field4': {'$gt': 6}},
                {'field5': 1}
            ],
            'field6': {'$ne': 42}
        }

        tree = SpecAST.build_tree_from_spec(spec)
        assert isinstance(tree.root, AndOperation)
        assert len(tree.root.children) == 2
        assert isinstance(tree.root.children[0], OrOperation)

    def test_spec_to_sql_conversion(self):
        spec = {
            '$or': [
                {
                    '$and': [
                        {'field1': {'$exists': 1}},
                        {'field2': {'$lte': 256}},
                        {'field3': 5}
                    ]
                },
                {'field4': {'$gt': 6}},
                {'field5': 1}
            ],
            'field6': {'$ne': 42},
            'field7': {'$in': [1, 2, 3]}
        }

        sql, params = convert_spec_to_sql(spec, self.convert_key, self.convert_key_value)
        assert sql == '(((field1 IS NOT NULL AND field2<=:param0 AND field3=:param1) OR field4>:param2 OR field5=:param3) AND field6 IS DISTINCT FROM :param4 AND field7 IN (:param5,:param6,:param7))'
        assert params['param0'] == 256
        assert params['param1'] == 5
        assert params['param2'] == 6
        assert params['param3'] == 1
        assert params['param4'] == 42
        assert params['param5'] == 1
        assert params['param6'] == 2
        assert params['param7'] == 3

    def test_spec_to_sql_conversion_2(self):
        spec = {
            '$or': [
                {
                    'field1': {'$exists': 1},
                    'field2': {'$lte': 256},
                    'field3': 5
                },
                {'field4': {'$gt': 6}},
                {'field5': 1}
            ],
            'field6': {'$ne': 42},
            'field7': {'$in': [1, 2, 3]}
        }

        sql, params = convert_spec_to_sql(spec, self.convert_key, self.convert_key_value)
        assert sql == '(((field2<=:param0 AND field3=:param1 AND field1 IS NOT NULL) OR field4>:param2 OR field5=:param3) AND field6 IS DISTINCT FROM :param4 AND field7 IN (:param5,:param6,:param7))'
        assert params['param0'] == 256
        assert params['param1'] == 5
        assert params['param2'] == 6
        assert params['param3'] == 1
        assert params['param4'] == 42
        assert params['param5'] == 1
        assert params['param6'] == 2
        assert params['param7'] == 3

    def test_spec_to_sql_conversion_3(self):
        spec = {
            'field1': {'$gte': 5, '$lt': 10}
        }

        sql, params = convert_spec_to_sql(spec, self.convert_key, self.convert_key_value)
        assert sql == '(field1>=:param0 AND field1<:param1)'
        assert params['param0'] == 5
        assert params['param1'] == 10

    def test_spec_to_sql_conversion_for_in_with_none(self):
        spec = {
            'field1': {'$in': [1, 2, 3, None, 4]}
        }

        sql, params = convert_spec_to_sql(spec, self.convert_key, self.convert_key_value)
        assert sql == '(field1 IS NULL OR field1 IN (:param0,:param1,:param2,:param3))'
        assert params['param0'] == 1
        assert params['param1'] == 2
        assert params['param2'] == 3
        assert params['param3'] == 4

    def test_spec_to_sql_conversion_for_in_without_none(self):
        spec = {
            'field1': {'$in': [1, 2, 3, 4]}
        }

        sql, params = convert_spec_to_sql(spec, self.convert_key, self.convert_key_value)
        assert sql == 'field1 IN (:param0,:param1,:param2,:param3)'
        assert params['param0'] == 1
        assert params['param1'] == 2
        assert params['param2'] == 3
        assert params['param3'] == 4

    def test_spec_values_conversion(self):
        def convert_key(key):
            convert_map = {
                'uid': 'user_id',
                'data.ctime': 'date_created'
            }
            return convert_map[key]

        def convert_value_for_key(key, value):
            if key == 'uid':
                return convert_key(key), int(value)
            elif key == 'data.ctime':
                date = datetime.datetime.fromtimestamp(value) - datetime.timedelta(hours=3)
                return convert_key(key), date.strftime('%Y-%m-%d %H:%M:%S+03')

        spec = {
            'uid': '123456',
            'data.ctime': {'$lte': 1481046625, '$gt': 1481046620},
        }

        sql, params = convert_spec_to_sql(spec, convert_key, convert_value_for_key)
        assert sql == '((date_created<=:param0 AND date_created>:param1) AND user_id=:param2)'
        assert len(params) == 3
        assert params['param0'] == '2016-12-06 17:50:25+03'
        assert params['param1'] == '2016-12-06 17:50:20+03'
        assert params['param2'] == 123456


class ValuesTemplateGeneratorTestCase(unittest.TestCase):

    def test_base(self):
        template_generator = ValuesTemplateGenerator(['column1', 'column2', 'column3'])

        data = [
            {'column1': 1, 'column2': 2, 'column3': 3},
            {'column1': 4, 'column2': 5, 'column3': 6},
            {'column1': 7, 'column2': 8, 'column3': 9},
        ]
        template = template_generator.get_values_tmpl(len(data))

        assert template == '(:column1_0, :column2_0, :column3_0), ' \
                           '(:column1_1, :column2_1, :column3_1), ' \
                           '(:column1_2, :column2_2, :column3_2)'

        values = template_generator.get_values_for_tmpl(data)

        assert values == {
            'column1_0': 1, 'column2_0': 2, 'column3_0': 3,
            'column1_1': 4, 'column2_1': 5, 'column3_1': 6,
            'column1_2': 7, 'column2_2': 8, 'column3_2': 9,
        }

    def test_template_caching(self):
        template_generator = ValuesTemplateGenerator(['column1', 'column2', 'column3'], expected_values_count=3)

        orig_get_values_row_template = template_generator.get_values_row_template

        def side_effect(*args, **kwargs):
            return orig_get_values_row_template(*args, **kwargs)

        with mock.patch.object(ValuesTemplateGenerator, 'get_values_row_template', side_effect=side_effect) as m:
            template = template_generator.get_values_tmpl(3)
            assert template == '(:column1_0, :column2_0, :column3_0), ' \
                               '(:column1_1, :column2_1, :column3_1), ' \
                               '(:column1_2, :column2_2, :column3_2)'
            m.assert_not_called()

        with mock.patch.object(ValuesTemplateGenerator, 'get_values_row_template', side_effect=side_effect) as m:
            template = template_generator.get_values_tmpl(2)
            assert template == '(:column1_0, :column2_0, :column3_0), ' \
                               '(:column1_1, :column2_1, :column3_1)'
            assert m.call_count == 2


class InsertConverterTestCase(unittest.TestCase):
    def test_simple_insert(self):
        query, values = MongoQueryConverter(UsersDAOItem).insert_to_sql(
            {'uid': '123456', 'data': {'name': 'James Bond'}},
            manipulate=False
        )
        assert query == 'INSERT INTO users (user_id,user_name,address) VALUES (:value0,:value1,:value2)'
        assert values == {'value0': 123456, 'value1': 'James Bond', 'value2': 'No home'}

    def test_bulk_insert(self):
        query, values = MongoQueryConverter(UsersDAOItem).insert_to_sql(
            [
                {'uid': '123456', 'data': {'name': 'James Bond'}},
                {'uid': '123457', 'data': {'name': 'James Bond', 'address': 'UK, London, MI-6'}},
                {'uid': '123458', 'data': {'name': 'James Bond', 'address': 'UK, London, Universal Exports inc.'}},
                {'uid': '123459', 'data': {'name': 'James Bond', 'address': 'UK, Scotland, Skyfall Lodge'}},
            ],
            manipulate=False
        )
        assert query == 'INSERT INTO users (user_id,user_name,address) VALUES (:value0_0,:value0_1,:value0_2),' \
                        '(:value1_0,:value1_1,:value1_2),(:value2_0,:value2_1,:value2_2),' \
                        '(:value3_0,:value3_1,:value3_2)'
        assert len(values) == 12
        assert values == {
            'value0_0': 123456, 'value0_1': 'James Bond', 'value0_2': 'No home',
            'value1_0': 123457, 'value1_1': 'James Bond', 'value1_2': 'UK, London, MI-6',
            'value2_0': 123458, 'value2_1': 'James Bond', 'value2_2': 'UK, London, Universal Exports inc.',
            'value3_0': 123459, 'value3_1': 'James Bond', 'value3_2': 'UK, Scotland, Skyfall Lodge'}

    def test_bulk_insert_with_manipulate(self):
        query, values = MongoQueryConverter(UsersDAOItem).insert_to_sql(
            [
                {'uid': '123456', 'data': {'name': 'James Bond'}},
                {'uid': '123457', 'data': {'name': 'James Bond', 'address': 'UK, London, MI-6'}},
                {'uid': '123458', 'data': {'name': 'James Bond', 'address': 'UK, London, Universal Exports inc.'}},
                {'uid': '123459', 'data': {'name': 'James Bond', 'address': 'UK, Scotland, Skyfall Lodge'}},
            ],
            manipulate=True
        )
        assert query == 'INSERT INTO users (user_id,user_name,address) VALUES (:value0_0,:value0_1,:value0_2),' \
                        '(:value1_0,:value1_1,:value1_2),(:value2_0,:value2_1,:value2_2),' \
                        '(:value3_0,:value3_1,:value3_2) RETURNING user_id'
        assert len(values) == 12
        assert values == {
            'value0_0': 123456, 'value0_1': 'James Bond', 'value0_2': 'No home',
            'value1_0': 123457, 'value1_1': 'James Bond', 'value1_2': 'UK, London, MI-6',
            'value2_0': 123458, 'value2_1': 'James Bond', 'value2_2': 'UK, London, Universal Exports inc.',
            'value3_0': 123459, 'value3_1': 'James Bond', 'value3_2': 'UK, Scotland, Skyfall Lodge'}


class RemoveToDeleteConverterTestCase(unittest.TestCase):
    def test_simple_remove(self):
        query, values = MongoQueryConverter(UsersDAOItem).remove_to_sql(
            {'uid': '123456'}
        )
        assert query == 'DELETE FROM users WHERE user_id=:param0'
        assert len(values) == 1
        assert values == {'param0': 123456}

    def test_complex_remove(self):
        query, values = MongoQueryConverter(UsersDAOItem).remove_to_sql(
            {'$or': [{'uid': '123456'}, {'uid': '123457'}]}
        )
        assert query == 'DELETE FROM users WHERE (user_id=:param0 OR user_id=:param1)'
        assert len(values) == 2
        assert values == {'param0': 123456, 'param1': 123457}

    def test_remove_all_collection(self):
        query, values = MongoQueryConverter(UsersDAOItem).remove_to_sql()
        assert query == 'DELETE FROM users'
        assert len(values) == 0

    def test_remove_only_first_item(self):
        query, values = MongoQueryConverter(UsersDAOItem).remove_to_sql(
            {'$or': [{'uid': '123456'}, {'uid': '123457'}]},
            multi=False
        )
        assert query == 'DELETE FROM users WHERE user_id IN (SELECT user_id FROM users WHERE (user_id=:param0 OR user_id=:param1) LIMIT 1)'
        assert len(values) == 2
        assert values == {'param0': 123456, 'param1': 123457}


class UpdateConverterTestCase(unittest.TestCase):
    def test_update_with_set(self):
        query, values = MongoQueryConverter(UsersDAOItem).update_to_sql(
            {'uid': '123456'},
            {'$set': {'data.name': 'Bond'}}
        )
        assert query == 'UPDATE users SET user_name=:value0 WHERE user_id IN (SELECT user_id FROM users WHERE' \
                        ' user_id=:param0 LIMIT 1)'
        assert len(values) == 2
        assert values['value0'] == 'Bond'
        assert values['param0'] == 123456

    def test_update_whole_row(self):
        query, values = MongoQueryConverter(UsersDAOItem).update_to_sql(
            {'uid': '123456'},
            {'uid': '123457', 'data': {'name': 'Bond'}}
        )
        assert query == 'UPDATE users SET user_id=:value0,user_name=:value1,address=:value2 ' \
                        'WHERE user_id IN (SELECT user_id FROM users WHERE user_id=:param0 LIMIT 1)'
        assert len(values) == 4
        assert values['value0'] == 123457
        assert values['value1'] == 'Bond'
        assert values['value2'] == 'No home'
        assert values['param0'] == 123456

    def test_update_with_set_multiple(self):
        query, values = MongoQueryConverter(UsersDAOItem).update_to_sql(
            {'uid': '123456'},
            {'$set': {'data.name': 'Bond'}},
            multi=True
        )
        assert query == 'UPDATE users SET user_name=:value0 WHERE user_id=:param0'
        assert len(values) == 2
        assert values['value0'] == 'Bond'
        assert values['param0'] == 123456

    def test_update_whole_row_multiple(self):
        query, values = MongoQueryConverter(UsersDAOItem).update_to_sql(
            {'uid': '123456'},
            {'uid': '123457', 'data': {'name': 'Bond'}},
            multi=True
        )
        assert query == 'UPDATE users SET user_id=:value0,user_name=:value1,address=:value2 WHERE user_id=:param0'
        assert len(values) == 4
        assert values['value0'] == 123457
        assert values['value1'] == 'Bond'
        assert values['value2'] == 'No home'
        assert values['param0'] == 123456

    def test_update_with_upsert(self):
        query, values = MongoQueryConverter(UsersDAOItem).update_to_sql(
            {'uid': '123456'},
            {'uid': '123457', 'data': {'name': 'Bond'}},
            multi=True,
            upsert=True
        )
        assert query == 'WITH found AS (UPDATE users SET user_id=:item0,user_name=:item1,address=:item2 WHERE ' \
                        'user_id=:param0 RETURNING 1), ins_res AS (INSERT INTO users (user_id,user_name,address) ' \
                        '(SELECT :value0,:value1,:value2 WHERE NOT EXISTS (SELECT * FROM found))) ' \
                        'SELECT * FROM found'
        assert len(values) == 7
        assert values['value0'] == 123457
        assert values['value1'] == 'Bond'
        assert values['value2'] == 'No home'
        assert values['param0'] == 123456
        assert values['item0'] == 123457
        assert values['item1'] == 'Bond'
        assert values['item2'] == 'No home'

    def test_update_with_upsert_and_set(self):
        query, values = MongoQueryConverter(UsersDAOItem).update_to_sql(
            {'uid': '123456'},
            {'$set': {'data': {'name': 'Bond'}}},
            multi=True,
            upsert=True
        )
        assert query == 'WITH found AS (UPDATE users SET user_name=:item0,address=:item1 WHERE ' \
                        'user_id=:param0 RETURNING 1), ins_res AS (INSERT INTO users (user_id,user_name,address) ' \
                        '(SELECT :value0,:value1,:value2 WHERE NOT EXISTS (SELECT * FROM found))) ' \
                        'SELECT * FROM found'
        assert len(values) == 6
        assert values['value0'] == 123456
        assert values['value1'] == 'Bond'
        assert values['value2'] == 'No home'
        assert values['param0'] == 123456
        assert values['item0'] == 'Bond'
        assert values['item1'] == 'No home'

    def test_update_without_upsert_and_set(self):
        query, values = MongoQueryConverter(UsersDAOItem).update_to_sql(
            {'uid': '123456'},
            {'$set': {'data.name': 'Bond', 'data.address': 'UK, London'}},
            multi=True,
            upsert=False
        )
        assert query == 'UPDATE users SET user_name=:value0,address=:value1 WHERE user_id=:param0'
        assert len(values) == 3
        assert values['value0'] == 'Bond'
        assert values['value1'] == 'UK, London'
        assert values['param0'] == 123456
