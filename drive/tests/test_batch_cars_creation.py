import copy
import datetime

import pandas
import pytz

from django.test import TransactionTestCase

from cars.carsharing.core.registry_manager import CarRegistryManager
from cars.carsharing.models import Car, CarModel, CarDocument
from cars.carsharing.models.car_document import CarDocumentAssignment
from cars.users.factories import UserFactory


class CarsBulkUploadTestCase(TransactionTestCase):

    def setUp(self):
        super().setUp()
        audi_a3 = CarModel(code='audi_a3', name='Audi A3', manufacturer='Audi')
        audi_q3 = CarModel(code='audi_q3', name='Audi Q3', manufacturer='Audi')
        bmw_520i = CarModel(code='bmw_520i', name='BMW 520i', manufacturer='BMW')
        genesis_g70 = CarModel(code='genesis_g70', name='Genesis G70', manufacturer='Genesis')
        kia_rio_xline = CarModel(code='kia_rio_xline', name='KIA Rio X-Line', manufacturer='KIA')
        kia_rio = CarModel(code='kia_rio', name='KIA Rio', manufacturer='KIA')
        mercedes_e200 = CarModel(code='mercedes_e200', name='Mercedes E200', manufacturer='Mercedes')
        porsche_carrera = CarModel(code='porsche_carrera', name='Porsche 911 Carrera 4S', manufacturer='Porsche')
        porsche_macan = CarModel(code='porsche_macan', name='Porsche Macan 2.0', manufacturer='Porsche')
        renault_kaptur = CarModel(
            code='renault_kaptur',
            name='Renault Kaptur',
            manufacturer='Renault'
        )
        skoda_octavia = CarModel(code='skoda_octavia', name='Skoda Octavia', manufacturer='Skoda')
        skoda_rapid = CarModel(code='skoda_rapid', name='Skoda Rapid', manufacturer='Skoda')
        petrol_tanker = CarModel(code='petrol_tanker', name='Бензовозик', manufacturer='Бензовозик')
        vw_polo = CarModel(code='vw_polo', name='Volkswagen Polo', manufacturer='Volkswagen')
        lada_largus = CarModel(code='petrol_tanker_2', name='Lada Largus', manufacturer='Lada')
        citroen_berlingo = CarModel(code='petrol_tanker_3', name='Citroen Berlingo', manufacturer='Citroen')
        citroen_jumpy = CarModel(code='citroen_jumpy', name='Citroen Jumpy', manufacturer='Citroen')
        volvo_xc60 = CarModel(code='volvo_xc60', name='Volvo XC60', manufacturer='Volvo')

        audi_a3.save()
        audi_q3.save()
        bmw_520i.save()
        genesis_g70.save()
        kia_rio.save()
        kia_rio_xline.save()
        mercedes_e200.save()
        porsche_carrera.save()
        porsche_macan.save()
        renault_kaptur.save()
        skoda_octavia.save()
        skoda_rapid.save()
        petrol_tanker.save()
        vw_polo.save()
        lada_largus.save()
        citroen_berlingo.save()
        citroen_jumpy.save()
        volvo_xc60.save()

        self.user = UserFactory.create()
        self.user.save()

    def test_bulk_upload_cars(self):
        cars = STUB_DATAFRAME_DATA

        manager = CarRegistryManager(self.user)
        manager.bulk_add_and_update_cars_from_df(cars)

        self.assertEqual(CarDocument.objects.count(), 5)
        self.assertEqual(Car.objects.count(), 5)

        car_to_check = Car.objects.get(vin='Z94C251ABJR008242')
        self.assertEqual(car_to_check.registration_id, 7760409210)
        self.assertEqual(car_to_check.model.code, 'kia_rio_xline')
        self.assertEqual(car_to_check.registration_date, datetime.datetime(year=2018, month=4, day=3, tzinfo=pytz.UTC))
        self.assertEqual(car_to_check.fuel_card_number, '782555000000273708')
        self.assertEqual(car_to_check.vin, 'Z94C251ABJR008242')
        self.assertEqual(car_to_check.imei, 866710035869384)

        docs = CarDocumentAssignment.objects.filter(car__vin='Z94C251ABJR008242')
        self.assertEqual(docs.count(), 1)
        doc = docs.first().document
        self.assertEqual(doc.type, 'car_registry_document')
        cr_doc = doc.get_impl()

        self.assertEqual(cr_doc.pts_number, '78 ОУ 289494')
        self.assertEqual(cr_doc.antitheft_system, 'Иммобилайзер штатный')
        self.assertEqual(cr_doc.mark_by_pts, 'KIA Rio')
        self.assertEqual(cr_doc.imei, 866710035869384)

        manager.bulk_add_and_update_cars_from_df(cars)
        self.assertEqual(CarDocument.objects.count(), 5)
        self.assertEqual(Car.objects.count(), 5)

        new_dataframe_list = copy.deepcopy(STUB_DATAFRAME_LIST)
        new_dataframe_list[0]['СТС'] = 123456
        manager.bulk_add_and_update_cars_from_df(pandas.DataFrame(new_dataframe_list))
        self.assertEqual(CarDocument.objects.count(), 6)
        self.assertEqual(Car.objects.count(), 5)

        docs = CarDocumentAssignment.objects.filter(car__vin='Z94C251ABJR008242')
        self.assertEqual(docs.count(), 2)
        self.assertEqual(Car.objects.get(vin='Z94C251ABJR008242').registration_id, 123456)

STUB_DATAFRAME_LIST = [
  {
    '№ПТС': '78 ОУ 289494',
    'Марка': 'Kia',
    'Дата регистрации': pandas.Timestamp('2018-04-03 00:00:00'),
    'Договор №': 'LO-054/2017-876',
    'ВЕГА': '"866710035869384"',
    'ОСАГО дата': pandas.Timestamp('2018-03-30 00:00:00'),
    'Дата ДС': pandas.np.nan,
    'дата начала парковочного разрешения': pandas.NaT,
    'Топливная карта': 273708,
    'VIN': 'Z94C251ABJR008242',
    'СТС': 7760409210,
    'Количество ключей': 1,
    'ПТС.Модель': 'KIA Rio  ',
    'Модель': 'Rio X-line',
    'ОСАГО №': 'ХХХ0032916858',
    'ГРЗ': 'Н102ЕА799',
    'КАСКО': '001AG18-0203',
    'Год выпуска': 2018,
    'Противоугонная система': 'Иммобилайзер штатный',
    'Дата подачи документов в дептранс': pandas.Timestamp('2018-04-05 00:00:00'),
    'Дата передачи': pandas.np.nan
  },
  {
    '№ПТС': '78 ОУ 293805',
    'Марка': 'Kia',
    'Дата регистрации': pandas.Timestamp('2018-04-01 00:00:00'),
    'Договор №': 'LO-054/2017-816',
    'ВЕГА': '"866710035767430"',
    'ОСАГО дата': pandas.Timestamp('2018-03-30 00:00:00'),
    'Дата ДС': pandas.np.nan,
    'дата начала парковочного разрешения': pandas.Timestamp('2018-04-11 00:00:00'),
    'Топливная карта': 270837,
    'VIN': 'Z94C251ABJR008936',
    'СТС': 7758677729,
    'Количество ключей': 1,
    'ПТС.Модель': 'KIA Rio ',
    'Модель': 'Rio X-line',
    'ОСАГО №': 'ХХХ0032905431',
    'ГРЗ': 'Н109ЕА799',
    'КАСКО': '001AG18-0203',
    'Год выпуска': 2018,
    'Противоугонная система': 'Иммобилайзер штатный',
    'Дата подачи документов в дептранс': pandas.Timestamp('2018-04-04 00:00:00'),
    'Дата передачи': pandas.np.nan
  },
  {
    '№ПТС': '78 ОУ 292707',
    'Марка': 'Kia',
    'Дата регистрации': pandas.Timestamp('2018-04-01 00:00:00'),
    'Договор №': 'LO-054/2017-820',
    'ВЕГА': '"866710035762936"',
    'ОСАГО дата': pandas.Timestamp('2018-03-30 00:00:00'),
    'Дата ДС': pandas.np.nan,
    'дата начала парковочного разрешения': pandas.Timestamp('2018-04-11 00:00:00'),
    'Топливная карта': 201204,
    'VIN': 'Z94C251ABJR008839',
    'СТС': 7758677723,
    'Количество ключей': 1,
    'ПТС.Модель': 'KIA Rio ',
    'Модель': 'Rio X-line',
    'ОСАГО №': 'ХХХ0032905449',
    'ГРЗ': 'Н170ЕА799',
    'КАСКО': '001AG18-0203',
    'Год выпуска': 2018,
    'Противоугонная система': 'Иммобилайзер штатный',
    'Дата подачи документов в дептранс': pandas.Timestamp('2018-04-04 00:00:00'),
    'Дата передачи': pandas.np.nan
  },
  {
    '№ПТС': '78 ОУ 323770',
    'Марка': 'Kia',
    'Дата регистрации': pandas.Timestamp('2018-04-02 00:00:00'),
    'Договор №': 'LO-054/2017-771',
    'ВЕГА': '"866710035882247"',
    'ОСАГО дата': pandas.Timestamp('2018-03-30 00:00:00'),
    'Дата ДС': pandas.np.nan,
    'дата начала парковочного разрешения': pandas.Timestamp('2018-04-11 00:00:00'),
    'Топливная карта': 201303,
    'VIN': 'Z94C251ABJR007753',
    'СТС': 7758677640,
    'Количество ключей': 1,
    'ПТС.Модель': 'KIA Rio ',
    'Модель': 'Rio X-line',
    'ОСАГО №': 'ХХХ0032896631',
    'ГРЗ': 'Н234ЕА799',
    'КАСКО': '001AG18-0203',
    'Год выпуска': 2018,
    'Противоугонная система': 'Иммобилайзер штатный',
    'Дата подачи документов в дептранс': pandas.Timestamp('2018-04-04 00:00:00'),
    'Дата передачи': pandas.np.nan
  },
  {
    '№ПТС': '78 ОУ 323736',
    'Марка': 'Kia',
    'Дата регистрации': pandas.Timestamp('2018-04-02 00:00:00'),
    'Договор №': 'LO-054/2017-906',
    'ВЕГА': '"866710035759122"',
    'ОСАГО дата': pandas.Timestamp('2018-03-30 00:00:00'),
    'Дата ДС': pandas.np.nan,
    'дата начала парковочного разрешения': pandas.Timestamp('2018-04-11 00:00:00'),
    'Топливная карта': 273518,
    'VIN': 'Z94C251ABJR007593',
    'СТС': 7758677905,
    'Количество ключей': 1,
    'ПТС.МОДЕЛЬ': 'KIA Rio ',
    'Модель': 'Rio X-line',
    'ОСАГО №': 'ХХХ0032917273',
    'ГРЗ': 'Н244ЕА799',
    'КАСКО': '001AG18-0203',
    'Год выпуска': 2018,
    'Противоугонная система': 'Иммобилайзер штатный',
    'Дата подачи документов в дептранс': pandas.Timestamp('2018-04-04 00:00:00'),
    'Дата передачи': pandas.np.nan
  }
]

STUB_DATAFRAME_DATA = pandas.DataFrame(STUB_DATAFRAME_LIST)
