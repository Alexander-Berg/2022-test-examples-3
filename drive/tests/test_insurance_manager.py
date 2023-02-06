import copy
import decimal

import pandas
from django.test import TransactionTestCase

from cars.carsharing.core.insurance_manager import CarInsuranceManager
from cars.carsharing.models import Car, CarModel, CarInsurance
from cars.carsharing.models.car_document import CarDocument, CarDocumentAssignment
from cars.users.factories import UserFactory


class CarsInsuranceUploadTestCase(TransactionTestCase):

    def setUp(self):
        super().setUp()
        kia_rio_xline = CarModel(code='kia_rio_xline', name='KIA Rio X-Line', manufacturer='KIA')
        kia_rio = CarModel(code='kia_rio', name='KIA Rio', manufacturer='KIA')
        renault_kaptur = CarModel(
            code='renault_kaptur',
            name='Renault Kaptur',
            manufacturer='Renault'
        )
        kia_rio.save()
        kia_rio_xline.save()
        renault_kaptur.save()

        for row in STUB_DATAFRAME_LIST:
            c = Car(
                model=kia_rio,
                vin=row['VIN'],
            )
            c.save()

        self.user = UserFactory.create()
        self.user.save()

    def test_upload(self):
        self.assertEqual(CarInsurance.objects.count(), 0)
        manager = CarInsuranceManager(operator_user=self.user)
        manager.bulk_add_insurance_from_df(STUB_DATAFRAME_DATA)
        self.assertEqual(CarInsurance.objects.count(), 5)

        # try to add the same, ensure nothing is changed
        manager.bulk_add_insurance_from_df(STUB_DATAFRAME_DATA)
        self.assertEqual(CarInsurance.objects.count(), 5)

        # alter one record, ensure that it was added
        new_dataframe_list = copy.deepcopy(STUB_DATAFRAME_LIST)
        new_dataframe_list[0]['AgreementNum'] += '_T'
        new_df = pandas.DataFrame(new_dataframe_list)
        manager.bulk_add_insurance_from_df(new_df)
        self.assertEqual(CarInsurance.objects.count(), 6)
        self.assertEqual(CarDocument.objects.count(), 6)
        self.assertEqual(CarDocumentAssignment.objects.count(), 6)
        self.assertEqual(
            Car.objects.get(vin='Z94C251ABJR002363').get_insurance().agreement_partner_number,
            '001AG18-0029'
        )
        self.assertEqual(
            Car.objects.get(vin='Z94C251ABJR002363').get_insurance().base_cost,
            decimal.Decimal('0.35'),
        )
        self.assertEqual(
            Car.objects.get(vin='Z94C251ABJR002363').get_insurance().per_minute_cost,
            decimal.Decimal('0.50'),
        )
        self.assertEqual(
            Car.objects.get(vin='Z94C251ABJR002363').get_insurance().agreement_partner_number,
            '001AG18-0029'
        )


STUB_DATAFRAME_LIST = [
  {
    '№ПТС': '78  ОТ  832195',
    'Дата начала страхования': pandas.Timestamp('2018-01-19 00:00:00'),
    'Сумма первого страхового взноса ': 7500,
    'VIN': 'Z94C251ABJR002363',
    'Страховая премия угон/тоталь': 1000,
    'Противоугонная система': 'Иммобилайзер (штатный)',
    'AgreementPartnerNum': '001AG18-0029 ',
    'Страховая премия': 8500,
    'ПТС.Модель': 'KIA Rio',
    'Количество ключей': 1,
    '№': 1,
    'ГРЗ': 'В416ВН799',
    'AgreementNum': '001AT-18/19990',
    'Дата окончания страхования': pandas.Timestamp('2019-01-18 00:00:00'),
    'Поминутное': 0.5,
    'Год.вып.': 2017,
    'Страховая сумма': 713037.5,
    'марка, модель, год выпуска, VIN, ПТС, Гос. рег. Знак, противоугонная система, количество ключей': 'KIA Rio, 2017гв, Z94C251ABJR002363, 78  ОТ  832195, В416ВН799, Иммобилайзер (штатный), 1 ключ'
  },
  {
    '№ПТС': '78 ОТ 832232',
    'Дата начала страхования': pandas.Timestamp('2018-01-19 00:00:00'),
    'Сумма первого страхового взноса ': 7500,
    'VIN': 'Z94C251ABJR002492',
    'Страховая премия угон/тоталь': 1000,
    'Противоугонная система': 'Иммобилайзер (штатный)',
    'AgreementPartnerNum': '001AG18-0029 ',
    'Страховая премия': 8500,
    'ПТС.Модель': 'KIA Rio',
    'Количество ключей': 1,
    '№': 2,
    'ГРЗ': 'В153ВН799',
    'AgreementNum': '001AT-18/19991',
    'Дата окончания страхования': pandas.Timestamp('2019-01-18 00:00:00'),
    'Поминутное': 0.5,
    'Год.вып.': 2017,
    'Страховая сумма': 713037.5,
    'марка, модель, год выпуска, VIN, ПТС, Гос. рег. Знак, противоугонная система, количество ключей': 'KIA Rio, 2017гв, Z94C251ABJR002492, 78 ОТ 832232, В153ВН799, Иммобилайзер (штатный), 1 ключ'
  },
  {
    '№ПТС': '78 ОТ 833820',
    'Дата начала страхования': pandas.Timestamp('2018-01-19 00:00:00'),
    'Сумма первого страхового взноса ': 7500,
    'VIN': 'Z94C251ABJR002574',
    'Страховая премия угон/тоталь': 1000,
    'Противоугонная система': 'Иммобилайзер (штатный)',
    'AgreementPartnerNum': '001AG18-0029 ',
    'Страховая премия': 8500,
    'ПТС.Модель': 'KIA Rio',
    'Количество ключей': 1,
    '№': 3,
    'ГРЗ': 'В252ВН799',
    'AgreementNum': '001AT-18/19992',
    'Дата окончания страхования': pandas.Timestamp('2019-01-18 00:00:00'),
    'Поминутное': 0.5,
    'Год.вып.': 2017,
    'Страховая сумма': 713037.5,
    'марка, модель, год выпуска, VIN, ПТС, Гос. рег. Знак, противоугонная система, количество ключей': 'KIA Rio, 2017гв, Z94C251ABJR002574, 78 ОТ 833820, В252ВН799, Иммобилайзер (штатный), 1 ключ'
  },
  {
    '№ПТС': '78  ОТ  832285',
    'Дата начала страхования': pandas.Timestamp('2018-01-19 00:00:00'),
    'Сумма первого страхового взноса ': 7500,
    'VIN': 'Z94C251ABJR002617',
    'Страховая премия угон/тоталь': 1000,
    'Противоугонная система': 'Иммобилайзер (штатный)',
    'AgreementPartnerNum': '001AG18-0029 ',
    'Страховая премия': 8500,
    'ПТС.Модель': 'KIA Rio',
    'Количество ключей': 1,
    '№': 4,
    'ГРЗ': 'В393ВН799',
    'AgreementNum': '001AT-18/19993',
    'Дата окончания страхования': pandas.Timestamp('2019-01-18 00:00:00'),
    'Поминутное': 0.5,
    'Год.вып.': 2017,
    'Страховая сумма': 713037.5,
    'марка, модель, год выпуска, VIN, ПТС, Гос. рег. Знак, противоугонная система, количество ключей': 'KIA Rio, 2017гв, Z94C251ABJR002617, 78  ОТ  832285, В393ВН799, Иммобилайзер (штатный), 1 ключ'
  },
  {
    '№ПТС': '78  ОТ  833848',
    'Дата начала страхования': pandas.Timestamp('2018-01-19 00:00:00'),
    'Сумма первого страхового взноса ': 7500,
    'VIN': 'Z94C251ABJR002667',
    'Страховая премия угон/тоталь': 1000,
    'Противоугонная система': 'Иммобилайзер (штатный)',
    'AgreementPartnerNum': '001AG18-0029 ',
    'Страховая премия': 8500,
    'ПТС.Модель': 'KIA Rio',
    'Количество ключей': 1,
    '№': 5,
    'ГРЗ': 'В380ВН799',
    'AgreementNum': '001AT-18/19994',
    'Дата окончания страхования': pandas.Timestamp('2019-01-18 00:00:00'),
    'Поминутное': 0.5,
    'Год.вып.': 2017,
    'Страховая сумма': 713037.5,
    'марка, модель, год выпуска, VIN, ПТС, Гос. рег. Знак, противоугонная система, количество ключей': 'KIA Rio, 2017гв, Z94C251ABJR002667, 78  ОТ  833848, В380ВН799, Иммобилайзер (штатный), 1 ключ'
  }
]

STUB_DATAFRAME_DATA = pandas.DataFrame(STUB_DATAFRAME_LIST)
