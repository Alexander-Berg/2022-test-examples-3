from dataclasses import dataclass, field
from datetime import datetime

import pytest
import sqlalchemy as sa
from marshmallow import fields
from sqlalchemy import func
from sqlalchemy.dialects.postgresql import JSONB

from sendr_aiopg import BaseMapperCRUD, CRUDQueries, Entity, StorageAnnotatedMeta, StorageBase, StorageContextBase
from sendr_aiopg.data_mapper import SelectableDataMapper, TableDataDumper
from sendr_aiopg.storage.entity import JSONBEntity
from sendr_utils import utcnow
from sendr_utils.schemas.base import BaseSchema

metadata = sa.MetaData(schema='sendr_qtools')

t_merchants = sa.Table(
    'merchants', metadata,
    sa.Column('uid', sa.BigInteger(), primary_key=True, nullable=False),
    sa.Column('name', sa.Text(), nullable=False),
    sa.Column('created', sa.DateTime(timezone=True), default=func.now(), nullable=False),
)


@dataclass
class Merchant(Entity):
    uid: int
    name: str
    created: datetime = field(default_factory=utcnow)


@dataclass
class AnotherMerchant(Entity):
    uid: int
    name: str
    created: datetime = field(default_factory=utcnow)


class MerchantDataMapper(SelectableDataMapper):
    entity_class = Merchant
    selectable = t_merchants


class MerchantDataDumper(TableDataDumper):
    entity_class = Merchant
    table = t_merchants


class MerchantMapper(BaseMapperCRUD[Merchant]):
    model = Merchant

    _builder = CRUDQueries(
        base=t_merchants,
        id_fields=('uid',),
        mapper_cls=MerchantDataMapper,
        dumper_cls=MerchantDataDumper,
    )


t_example_jsonb = sa.Table(
    'example_jsonb', metadata,
    sa.Column('id', sa.BigInteger(), nullable=False),
    sa.Column('data', JSONB(), nullable=False),
)


class ExampleJSONBEntitySchema(BaseSchema):
    a = fields.Integer()
    b = fields.DateTime()


@dataclass
class ExampleJSONBEntity(JSONBEntity):
    schema = ExampleJSONBEntitySchema

    a: int
    b: datetime


@dataclass
class ExampleJSONB(Entity):
    id: int
    data: ExampleJSONBEntity


class ExampleJSONBDataMapper(SelectableDataMapper):
    entity_class = ExampleJSONB
    selectable = t_example_jsonb

    def map_data(self, data):
        return ExampleJSONBEntity.from_jsonb(data)


class ExampleJSONBDataDumper(TableDataDumper):
    entity_class = ExampleJSONB
    table = t_example_jsonb

    def dump_data(self, data):
        return data.to_jsonb()


class ExampleJSONBMapper(BaseMapperCRUD[Merchant]):
    model = Merchant

    _builder = CRUDQueries(
        base=t_example_jsonb,
        id_fields=('id',),
        mapper_cls=ExampleJSONBDataMapper,
        dumper_cls=ExampleJSONBDataDumper,
    )


class Storage(StorageBase, metaclass=StorageAnnotatedMeta):
    merchant: MerchantMapper
    example_jsonb: ExampleJSONBMapper


class StorageContext(StorageContextBase):
    STORAGE_CLS = Storage


@pytest.fixture
async def merchant(storage, randn, rands):
    return await storage.merchant.create(Merchant(uid=randn(), name=rands()))
