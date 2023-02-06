# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from factory import Faker, DictFactory, Sequence
from factory.fuzzy import FuzzyChoice

from travel.rasp.bus.api.connectors.entities.endpoint import Endpoint


class EndpointFactory(DictFactory):
    supplier_id = Sequence(lambda n: 'point_id_{}'.format(n))
    type = FuzzyChoice(Endpoint.Type.enum)
    title = Faker('city')
    description = Faker('address')
    latitude = Faker('pyfloat', positive=False, min_value=-90, max_value=90)
    longitude = Faker('pyfloat', positive=False, min_value=-90, max_value=90)
    country = Faker('country')
    city_id = Faker('hexify', text='^'*5)
    country_code = Faker('country_code')
    city_title = Faker('city')
    region = Faker('lexify', text='Region_??')
    region_code = Faker('lexify', text='Region_code_??')
    district = Faker('lexify', text='District_??')
    extra_info = Faker('lexify', text='extra_info_??')
    timezone_info = Faker('lexify', text='Europe/???????')
