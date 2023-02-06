# -*- encoding: utf-8 -*-
from travel.avia.travelers.application import models, schemas
from travel.avia.travelers.application.validations.bonus_card_types import BonusCardType
from travel.avia.travelers.application.validations.document_types import DocumentType

from travel.avia.travelers.tests.custom_faker import faker
from travel.avia.travelers.tests.utils import update_params, random_enum


class Factory(object):
    @staticmethod
    def traveler(**kwargs):
        return models.Traveler().fill(_traveler(kwargs))

    @staticmethod
    def passenger(**kwargs):
        return models.Passenger().fill(_passenger(kwargs))

    @staticmethod
    def document(**kwargs):
        return models.Document().fill(_document(kwargs))

    @staticmethod
    def bonus_card(**kwargs):
        return models.BonusCard().fill(_bonus_card(kwargs))


class DictFactory(object):
    @staticmethod
    def traveler(**kwargs):
        return _traveler(kwargs)

    @staticmethod
    def passenger(**kwargs):
        return _passenger(kwargs)

    @staticmethod
    def document(**kwargs):
        return _document(kwargs)

    @staticmethod
    def bonus_card(**kwargs):
        return _bonus_card(kwargs)

    @staticmethod
    def combine_passengers(**kwargs):
        return _combine_passengers(kwargs)


@update_params
def _traveler():
    return dict(
        agree=faker.pybool(),
        phone=faker.phone_number(),
        phone_additional=faker.phone_number(),
        email=faker.email(),
        created_at=faker.date_time(),
        updated_at=faker.date_time(),
    )


@update_params
def _passenger():
    return dict(
        id=faker.uuid4(),
        title=faker.pystr(),
        gender=random_enum(schemas.Gender),
        birth_date=faker.date_time(),
        phone=faker.pystr(),
        phone_additional=faker.pystr(),
        itn=faker.random_number(digits=12, fix_len=True),
        email=faker.email(),
        created_at=faker.date_time(),
        updated_at=faker.date_time(),
        documents=[],
        bonus_cards=[],
    )


@update_params
def _document():
    return dict(
        id=faker.uuid4(),
        passenger_id=faker.uuid4(),
        type=random_enum(DocumentType),
        title=faker.pystr(),
        number=faker.pystr(),
        first_name=faker.pystr(),
        middle_name=faker.pystr(),
        last_name=faker.pystr(),
        first_name_en=faker.pystr(),
        middle_name_en=faker.pystr(),
        last_name_en=faker.pystr(),
        issue_date=faker.date_object(),
        expiration_date=faker.date_object(),
        citizenship=faker.pyint(),
        created_at=faker.date_time(),
        updated_at=faker.date_time(),
    )


@update_params
def _bonus_card():
    return dict(
        id=faker.uuid4(),
        passenger_id=faker.uuid4(),
        type=random_enum(BonusCardType),
        title=faker.pystr(),
        number=faker.pystr(),
        company_id=faker.pyint(),
        created_at=faker.date_time(),
        updated_at=faker.date_time(),
    )


@update_params
def _combine_passengers():
    return dict(
        title=faker.pystr(),
        gender=random_enum(schemas.Gender),
        birth_date=faker.date_time(),
        phone=faker.phone_number(),
        phone_additional=faker.phone_number(),
        email=faker.email(),
        passengers=[faker.pystr()],
    )
