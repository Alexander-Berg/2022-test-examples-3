# encoding: UTF-8

import unittest

import enum
import marshmallow as ma
import sqlalchemy as sa
import sqlalchemy.ext.declarative.api as decl
import sqlalchemy.orm as orm
from hamcrest import *
from mock import mock

from appcore.data.schema import ModelSchemaOpts, ModelSchema, EnumField


class SampleModel(object):
    __tablename__ = 'sample_model'

    id = sa.Column(
        sa.Integer,
        sa.Sequence(__tablename__ + '_id_seq'),
        primary_key=True,
    )
    name = sa.Column(sa.String)
    type = sa.Column(sa.Integer)

    def __init__(self, id=None, name=None, type=None):
        self.id = id
        self.name = name
        self.type = type


class SampleMeta:
    model = SampleModel


class ModelSchemaOptsTestCase(unittest.TestCase):
    def test_has_model_attrbute(self):
        opts = ModelSchemaOpts(SampleMeta)

        assert_that(
            opts,
            has_properties(
                model=equal_to(SampleModel),
            ),
        )


class SampleModelSchema(ModelSchema):
    class Meta:
        model = SampleModel

    id = ma.fields.Integer(required=True, allow_none=True)
    name = ma.fields.String(required=True)
    type = ma.fields.Integer(required=True)


class ModelSchemaTestCase(unittest.TestCase):
    def setUp(self):
        try:
            # noinspection PyUnresolvedReferences
            md = SampleModel.metadata
        except AttributeError:
            md = sa.MetaData()
            reg = {}
            decl.instrument_declarative(SampleModel, reg, md)

        self.md = md

        self.engine = sa.create_engine('sqlite://')
        self.md.create_all(bind=self.engine)

        self.session = orm.sessionmaker(bind=self.engine)()

    def _do_test(
            self,
            dump_schema,
            load_schema,
            sample_model,
            dumped_entries,
            loaded_properties,
    ):
        result, errors = dump_schema.dump(sample_model)

        assert_that(
            errors,
            empty(),
        )

        assert_that(
            result,
            all_of(
                instance_of(dict),
                has_entries(dumped_entries),
            )
        )

        result, errors = load_schema.load(result)

        assert_that(
            errors,
            empty(),
        )

        assert_that(
            result,
            all_of(
                instance_of(SampleModel),
                has_properties(loaded_properties),
            )
        )

    def test_simple(self):
        sample_model = SampleModel(
            name='hello',
            type=10,
        )

        sample_properties = dict(
            id=is_(None),
            name=equal_to('hello'),
            type=equal_to(10)
        )

        self._do_test(
            SampleModelSchema(),
            SampleModelSchema(),
            sample_model=sample_model,
            dumped_entries=sample_properties,
            loaded_properties=sample_properties,
        )

    def test_with_session(self):
        sample_model = SampleModel(
            name='hello',
            type=10,
        )
        self.session.add(sample_model)

        sample_properties = dict(
            id=is_not(None),
            name=equal_to('hello'),
            type=equal_to(10)
        )

        self._do_test(
            SampleModelSchema(),
            SampleModelSchema(),
            sample_model=sample_model,
            dumped_entries=sample_properties,
            loaded_properties=sample_properties,
        )

    def test_with_provider(self):
        sample_model = SampleModel(
            id=100500,
            name='hello',
            type=10,
        )

        sample_data = dict(
            id=100500,
            name='hello world',
        )

        provider = mock.Mock(return_value=sample_model)

        schema = SampleModelSchema(
            partial=True,
            provider=provider,
        )

        result, errors = schema.load(sample_data)

        assert_that(
            errors,
            empty(),
        )

        assert_that(
            result,
            all_of(
                instance_of(SampleModel),
                has_properties(
                    id=100500,
                    name='hello world',
                    type=10,
                )
            )
        )

        provider.assert_called_once_with(100500)


class SampleEnum(int, enum.Enum):
    A = 1
    B = 2
    C = 3


class SampleObj(object):
    def __init__(self, value):
        self.value = value


class EnumFieldTestCase(unittest.TestCase):
    def _do_serialize(self, value, enum_cls, *args, **kwargs):
        field = EnumField(enum_cls, *args, **kwargs)
        obj = SampleObj(value)
        return field.serialize('value', obj)

    def _do_deserialize(self, value, enum_cls, *args, **kwargs):
        field = EnumField(enum_cls, *args, **kwargs)
        return field.deserialize(value, 'value', {'value': value})

    def test_serialize(self):
        result = self._do_serialize(SampleEnum.B, SampleEnum)
        assert_that(
            result,
            equal_to(SampleEnum.B.value),
        )

    def test_serialize_by_name(self):
        result = self._do_serialize(SampleEnum.B, SampleEnum, by_name=True)
        assert_that(
            result,
            equal_to('B'),
        )

    def test_serialize_raises(self):
        assert_that(
            calling(self._do_serialize).with_args(100500, SampleEnum),
            raises(
                ma.ValidationError,
                'Must be one of: ',
            )
        )

    def test_deserialize(self):
        result = self._do_deserialize(SampleEnum.B.value, SampleEnum)
        assert_that(
            result,
            equal_to(SampleEnum.B),
        )

    def test_deserialize_by_name(self):
        result = self._do_deserialize(
            SampleEnum.B.name,
            SampleEnum,
            by_name=True,
        )
        assert_that(
            result,
            equal_to(SampleEnum.B),
        )

    def test_deserialize_raises(self):
        assert_that(
            calling(self._do_deserialize).with_args(100500, SampleEnum),
            raises(
                ma.ValidationError,
                'Must be one of: ',
            )
        )

        assert_that(
            calling(self._do_deserialize).with_args(
                'D',
                SampleEnum,
                by_name=True,
            ),
            raises(
                ma.ValidationError,
                'Must be one of: ',
            )
        )