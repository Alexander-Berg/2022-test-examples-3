# -*- encoding: utf-8 -*-
from faker import Faker
from faker.providers.phone_number import Provider


class PhoneProvider(Provider):
    formats = (
        '#' * 11,
        '+' + '#' * 11,
    )


faker = Faker()
faker.add_provider(PhoneProvider)
