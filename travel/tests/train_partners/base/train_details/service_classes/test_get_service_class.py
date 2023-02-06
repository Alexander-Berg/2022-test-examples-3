# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date
from six import ensure_text

from common.apps.train.models import ServiceClass
from common.dynamic_settings.default import conf
from common.tester.testcase import TestCase
from common.tester.utils.replace_setting import replace_setting

from travel.library.python.dicts.trains.service_class_repository import ServiceClassRepository
from travel.proto.dicts.trains.coach_type_pb2 import TCoachType
from travel.proto.dicts.trains.service_class_pb2 import TServiceClass
from travel.rasp.train_api.pb_provider import PROTOBUF_DATA_PROVIDER
from travel.rasp.train_api.train_partners.base.train_details.service_classes import get_service_class


WHEN = date(2016, 11, 24)
PARTNER_SERVICE_CLASS_DESCRIPTION = 'Очень хороший класс. В услуги входит всё.'
OUR_SERVICE_CLASS_DESCRIPTION = 'Наше отличное описание класса из админки.'


class TrainDetailsStub(object):
    def __init__(self, start_number, brand=None, when=WHEN):
        self.start_number = start_number
        self.brand = brand
        self.when = when


class ClassDetailsStub(object):
    def __init__(self, train_details, coach_type, owner, service_class_code, service_class_description, is_firm,
                 international_service_class_code=None):
        self.train_details = train_details
        self.coach_type = coach_type
        self.owner = owner
        self.service_class_code = service_class_code
        self.is_firm = is_firm
        self.service_class_description = service_class_description
        self.international_service_class_code = international_service_class_code


class TestGetServiceClassCompartment(TestCase):
    """
    1. 2Э - ФПК
    2. 2Э - ФПК - двухэтажные
    3. 2Э - Гранд
    4. 2Э - ФПК - нефирменные
    5. 2Э - ФПК - №№ 197Б, 198Б
    6. 2Э - Гранд - №№ 197Б, 198Б
    7. 1Р - ФПК
    8. 1Р - ФПК - двухэтажные
    9. 1Р - ФПК - Стриж
    10. 1Р - ДОСС - Сапсан
    11. 2Э - ФПК - №№ 198Б, 199Б
    12. 2К - ФПК
    13. 2Э - ФПК - сидячие
    14. 3Л - ФПК - плацкарт
    15. 2С, 2Ж - ФПК - сидячие
    16. 73 - ФПК - плацкарт
    """

    def setUp(self):
        super(TestGetServiceClassCompartment, self).setUp()

        self.fpk_2e = ServiceClass.objects.create(id=1, name='ФПК. Комфорт с питанием',
                                                  code='2Э',
                                                  title_ru='Комфорт с питанием',
                                                  description_ru='Купейный вагон с питанием и дорожным набором',
                                                  coach_category='compartment',
                                                  coach_owner='ФПК, ЛДЗ')

        self.fpk_2e_two_storey = ServiceClass.objects.create(id=2, name='ФПК. Двухэатжные. Комфорт с питанием',
                                                             code='2Э',
                                                             title_ru='Комфорт с питанием',
                                                             description_ru='Купейный вагон с питанием',
                                                             coach_category='compartment',
                                                             coach_owner='ФПК',
                                                             two_storey=True)

        self.grand_2e = ServiceClass.objects.create(id=3, name='Гранд Сервис Экспресс. Комфорт с питанием и ТВ',
                                                    code='2Э',
                                                    title_ru='Комфорт с питанием и ТВ',
                                                    description_ru='Купейный вагон с питанием и телевизором',
                                                    coach_category='compartment',
                                                    coach_owner='Гранд')

        self.fpk_2e_not_firm = ServiceClass.objects.create(id=4, name='ФПК. Нефирменные. Комфорт с напитками.',
                                                           code='2Э',
                                                           title_ru='Комфорт с напитками',
                                                           description_ru='Купейный вагон с горячими напитками (чай)',
                                                           coach_category='compartment',
                                                           coach_owner='ФПК, ЛДЗ',
                                                           is_firm_coach=False)

        self.fpk_2e_197_198 = ServiceClass.objects.create(id=5, name='ФПК. Комфорт с питанием. №№197Б, 198Б',
                                                          code='2Э',
                                                          title_ru='Комфорт с питанием',
                                                          description_ru='Купейный вагон с питанием и дорожным набором',
                                                          coach_category='compartment',
                                                          coach_owner='ФПК',
                                                          train_number=r'^19[78]Б$')

        self.grand_2e_197_198 = ServiceClass.objects.create(id=6, name='Гранд. Комфорт с питанием и ноутбуком. '
                                                                       '№№197Б, 198Б',
                                                            code='2Э',
                                                            title_ru='Комфорт с питанием и ноутбуком',
                                                            description_ru='Купейный вагон с питанием и ноутбуком',
                                                            coach_category='compartment',
                                                            coach_owner='Гранд',
                                                            train_number=r'^19[78]Б$')

        self.fpk_1r = ServiceClass.objects.create(id=7, name='ФПК. Бизнес',
                                                  code='1Р',
                                                  title_ru='Бизнес',
                                                  description_ru='Вагон повышенной комфортности '
                                                                 'с улучшенной компановкой мест',
                                                  coach_category='sitting',
                                                  coach_owner='ФПК')

        self.fpk_1r_two_storey = ServiceClass.objects.create(id=8, name='ФПК. Двухэтажные. Бизнес',
                                                             code='1Р',
                                                             title_ru='Бизнес',
                                                             description_ru='Вагон повышенной комфортности '
                                                                            'с улучшенной компановкой мест',
                                                             coach_category='sitting',
                                                             coach_owner='ФПК',
                                                             two_storey=True)

        self.fpk_1r_strizh = ServiceClass.objects.create(id=9, name='ФПК. Стриж',
                                                         code='1Р',
                                                         title_ru='Бизнес',
                                                         description_ru='Вагон повышенной комфортности '
                                                                        'с улучшенной компановкой мест',
                                                         coach_category='sitting',
                                                         coach_owner='ФПК',
                                                         brand_title='Стриж')

        self.doss_1r_sapsan = ServiceClass.objects.create(id=10, name='ДОСС. Сапсан',
                                                          code='1Р',
                                                          title_ru='Бизнес',
                                                          description_ru='Вагон повышенной комфортности '
                                                                         'с улучшенной компановкой мест',
                                                          coach_category='sitting',
                                                          coach_owner='ДОСС',
                                                          brand_title='Сапсан')

        ServiceClass.objects.create(id=11, name='ФПК. Комфорт с питанием. №№198Б, 199Б', code='2Э',
                                    title_ru='Комфорт с питанием',
                                    description_ru='Купейный вагон с питанием и дорожным набором',
                                    coach_category='compartment',
                                    coach_owner='ФПК', train_number=r'^19[89]Б$')

        ServiceClass.objects.create(id=12, name='ФПК. Комфорт', code='2К',
                                    title_ru='Комфорт',
                                    description_ru='Купейный вагон кондиционером и биотуалетом',
                                    coach_category='compartment',
                                    coach_owner='ФПК')

        ServiceClass.objects.create(id=13, name='ФПК. Комфорт', code='2Э',
                                    title_ru='Комфорт',
                                    description_ru='Сидячий вагон кондиционером и биотуалетом',
                                    coach_category='sitting',
                                    coach_owner='ФПК')

        ServiceClass.objects.create(id=14, name='ФПК. Турист', code='3Л',
                                    title_ru='Турист',
                                    description_ru='Плацкартный вагон "Сделано в СССР"',
                                    coach_category='platzkarte',
                                    coach_owner='ФПК')

        self.fpk_sitting_2_class = ServiceClass.objects.create(
            id=15, name='ФПК. Сидячка 2 класс', code='2Ж ,2С',
            title_ru='2 класс',
            description_ru='Сидячий вагон кондиционером и биотуалетом',
            coach_category='sitting',
            coach_owner='ФПК')

        self.our_description_class = ServiceClass.objects.create(
            id=16, name='ФПК. Бизнес2',
            code='1Z',
            title_ru='Бизнес',
            description_ru=OUR_SERVICE_CLASS_DESCRIPTION,
            coach_category='sitting',
            coach_owner='ФПК')

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_2e_not_firm)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_two_storey(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=True)

        check_class(klass, self.fpk_2e_two_storey)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_firm_coach(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_2e)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_grand(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ГРАНД', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.grand_2e)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_number_197(self):
        train_details = TrainDetailsStub('197Б', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_2e_197_198)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_grand_number_197(self):
        train_details = TrainDetailsStub('197Б', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ГРАНД', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.grand_2e_197_198)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_number_198(self):
        train_details = TrainDetailsStub('198Б', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        assert not klass

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_tks(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ТКС', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_unmatched_class(klass, '2Э')

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_1r)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk_two_storey(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=True)

        check_class(klass, self.fpk_1r_two_storey)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk_strizh(self):
        train_details = TrainDetailsStub('201Ц', brand='СТРИЖ', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_1r_strizh)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk_orel(self):
        train_details = TrainDetailsStub('201Ц', brand='ОРЕЛ', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_1r)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_doss_sapsan(self):
        train_details = TrainDetailsStub('201Ц', brand='САПСАН', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ДОСС', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.doss_1r_sapsan)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_class_with_two_codes(self):
        train_details = TrainDetailsStub('203Ы', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '2Ж', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_sitting_2_class, expected_key='2Ж, 2С', expected_class_code='2Ж')

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', True)
    def test_class_from_partner(self):
        train_details = TrainDetailsStub('203Ы', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '2Ж', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)
        check_class(klass, self.fpk_sitting_2_class, expected_key='2Ж, 2С', expected_class_code='2Ж',
                    expected_description=PARTNER_SERVICE_CLASS_DESCRIPTION)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_class_with_our_description(self):
        train_details = TrainDetailsStub('201Ц', brand='ОРЕЛ', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Z', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.our_description_class)


class TestGetServiceClassCompartmentWithProtobufs(TestCase):
    """
    1. 2Э - ФПК
    2. 2Э - ФПК - двухэтажные
    3. 2Э - Гранд
    4. 2Э - ФПК - нефирменные
    5. 2Э - ФПК - №№ 197Б, 198Б
    6. 2Э - Гранд - №№ 197Б, 198Б
    7. 1Р - ФПК
    8. 1Р - ФПК - двухэтажные
    9. 1Р - ФПК - Стриж
    10. 1Р - ДОСС - Сапсан
    11. 2Э - ФПК - №№ 198Б, 199Б
    12. 2К - ФПК
    13. 2Э - ФПК - сидячие
    14. 3Л - ФПК - плацкарт
    15. 2С, 2Ж - ФПК - сидячие
    """

    @classmethod
    def setUpClass(cls):
        super(TestGetServiceClassCompartmentWithProtobufs, cls).setUpClass()

        cls.fpk_2e = TServiceClass(Name='ФПК. Комфорт с питанием',
                                   Code='2Э',
                                   Title='Комфорт с питанием',
                                   Description='Купейный вагон с питанием и дорожным набором',
                                   CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                   CoachOwner='ФПК, ЛДЗ')

        cls.fpk_2e_two_storey = TServiceClass(Name='ФПК. Двухэатжные. Комфорт с питанием',
                                              Code='2Э',
                                              Title='Комфорт с питанием',
                                              Description='Купейный вагон с питанием',
                                              CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                              CoachOwner='ФПК')
        cls.fpk_2e_two_storey.TwoStorey.value = True

        cls.grand_2e = TServiceClass(Name='Гранд Сервис Экспресс. Комфорт с питанием и ТВ',
                                     Code='2Э',
                                     Title='Комфорт с питанием и ТВ',
                                     Description='Купейный вагон с питанием и телевизором',
                                     CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                     CoachOwner='Гранд')

        cls.fpk_2e_not_firm = TServiceClass(Name='ФПК. Нефирменные. Комфорт с напитками.',
                                            Code='2Э',
                                            Title='Комфорт с напитками',
                                            Description='Купейный вагон с горячими напитками (чай)',
                                            CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                            CoachOwner='ФПК, ЛДЗ')
        cls.fpk_2e_not_firm.IsFirmCoach.value = False

        cls.fpk_2e_197_198 = TServiceClass(Name='ФПК. Комфорт с питанием. №№197Б, 198Б',
                                           Code='2Э',
                                           Title='Комфорт с питанием',
                                           Description='Купейный вагон с питанием и дорожным набором',
                                           CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                           CoachOwner='ФПК',
                                           TrainNumber=r'^19[78]Б$')

        cls.grand_2e_197_198 = TServiceClass(Name='Гранд. Комфорт с питанием и ноутбуком. №№197Б, 198Б',
                                             Code='2Э',
                                             Title='Комфорт с питанием и ноутбуком',
                                             Description='Купейный вагон с питанием и ноутбуком',
                                             CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                             CoachOwner='Гранд',
                                             TrainNumber=r'^19[78]Б$')

        cls.fpk_1r = TServiceClass(Name='ФПК. Бизнес',
                                   Code='1Р',
                                   Title='Бизнес',
                                   Description='Вагон повышенной комфортности с улучшенной компановкой мест',
                                   CoachCategory=TCoachType.EType.Value('SITTING'),
                                   CoachOwner='ФПК')

        cls.fpk_1r_two_storey = TServiceClass(Name='ФПК. Двухэтажные. Бизнес',
                                              Code='1Р',
                                              Title='Бизнес',
                                              Description='Вагон повышенной комфортности с улучшенной компановкой мест',
                                              CoachCategory=TCoachType.EType.Value('SITTING'),
                                              CoachOwner='ФПК')
        cls.fpk_1r_two_storey.TwoStorey.value = True

        cls.fpk_1r_strizh = TServiceClass(Name='ФПК. Стриж',
                                          Code='1Р',
                                          Title='Бизнес',
                                          Description='Вагон повышенной комфортности с улучшенной компановкой мест',
                                          CoachCategory=TCoachType.EType.Value('SITTING'),
                                          CoachOwner='ФПК',
                                          BrandTitle='Стриж')

        cls.doss_1r_sapsan = TServiceClass(Name='ДОСС. Сапсан',
                                           Code='1Р',
                                           Title='Бизнес',
                                           Description='Вагон повышенной комфортности с улучшенной компановкой мест',
                                           CoachCategory=TCoachType.EType.Value('SITTING'),
                                           CoachOwner='ДОСС',
                                           BrandTitle='Сапсан')

        cls.fpk_2e_198_199 = TServiceClass(Name='ФПК. Комфорт с питанием. №№198Б, 199Б',
                                           Code='2Э',
                                           Title='Комфорт с питанием',
                                           Description='Купейный вагон с питанием и дорожным набором',
                                           CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                           CoachOwner='ФПК',
                                           TrainNumber=r'^19[89]Б$')

        cls.fpk_2k = TServiceClass(Name='ФПК. Комфорт',
                                   Code='2К',
                                   Title='Комфорт',
                                   Description='Купейный вагон c кондиционером и биотуалетом',
                                   CoachCategory=TCoachType.EType.Value('COMPARTMENT'),
                                   CoachOwner='ФПК')

        cls.fpk_2e_sitting = TServiceClass(Name='ФПК. Комфорт',
                                           Code='2Э',
                                           Title='Комфорт',
                                           Description='Сидячий вагон c кондиционером и биотуалетом',
                                           CoachCategory=TCoachType.EType.Value('SITTING'),
                                           CoachOwner='ФПК')

        cls.fpk_3l = TServiceClass(Name='ФПК. Турист',
                                   Code='3Л',
                                   Title='Турист',
                                   Description='Плацкартный вагон "Сделано в СССР"',
                                   CoachCategory=TCoachType.EType.Value('PLATZKARTE'),
                                   CoachOwner='ФПК')

        cls.fpk_sitting_2_class = TServiceClass(Name='ФПК. Сидячка 2 класс',
                                                Code='2Ж, 2С',
                                                Title='2 класс',
                                                Description='Сидячий вагон c кондиционером и биотуалетом',
                                                CoachCategory=TCoachType.EType.Value('SITTING'),
                                                CoachOwner='ФПК')

        cls._original_setting = conf.TRAIN_BACKEND_USE_PROTOBUFS['service_class']
        conf.TRAIN_BACKEND_USE_PROTOBUFS['service_class'] = True

        cls._original_repo = PROTOBUF_DATA_PROVIDER.service_class_repo
        PROTOBUF_DATA_PROVIDER.service_class_repo = ServiceClassRepository()
        for service_proto in (
            cls.doss_1r_sapsan,
            cls.fpk_1r,
            cls.fpk_1r_strizh,
            cls.fpk_1r_two_storey,
            cls.fpk_2e,
            cls.fpk_2e_197_198,
            cls.fpk_2e_198_199,
            cls.fpk_2e_not_firm,
            cls.fpk_2e_sitting,
            cls.fpk_2e_two_storey,
            cls.fpk_2k,
            cls.fpk_3l,
            cls.fpk_sitting_2_class,
            cls.grand_2e,
            cls.grand_2e_197_198,
        ):
            PROTOBUF_DATA_PROVIDER.service_class_repo.add(service_proto.SerializeToString())

    @classmethod
    def tearDownClass(cls):
        PROTOBUF_DATA_PROVIDER.service_class_repo = cls._original_repo
        conf.TRAIN_BACKEND_USE_PROTOBUFS['service_class'] = cls._original_setting
        super(TestGetServiceClassCompartmentWithProtobufs, cls).tearDownClass()

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_2e_not_firm)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_two_storey(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=True)

        check_class(klass, self.fpk_2e_two_storey)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_firm_coach(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_2e)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_grand(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ГРАНД', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.grand_2e)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_number_197(self):
        train_details = TrainDetailsStub('197Б', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_2e_197_198)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_grand_number_197(self):
        train_details = TrainDetailsStub('197Б', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ГРАНД', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.grand_2e_197_198)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_fpk_number_198(self):
        train_details = TrainDetailsStub('198Б', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ФПК', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        assert not klass

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_2e_tks(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'compartment', 'ТКС', '2Э', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=True)
        klass = get_service_class(class_details, two_storey=False)

        check_unmatched_class(klass, '2Э')

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_1r)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk_two_storey(self):
        train_details = TrainDetailsStub('201Ц', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=True)

        check_class(klass, self.fpk_1r_two_storey)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk_strizh(self):
        train_details = TrainDetailsStub('201Ц', brand='СТРИЖ', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_1r_strizh)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_fpk_orel(self):
        train_details = TrainDetailsStub('201Ц', brand='ОРЕЛ', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_1r)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_1r_doss_sapsan(self):
        train_details = TrainDetailsStub('201Ц', brand='САПСАН', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ДОСС', '1Р', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.doss_1r_sapsan)

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', False)
    def test_class_with_two_codes(self):
        train_details = TrainDetailsStub('203Ы', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '2Ж', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)

        check_class(klass, self.fpk_sitting_2_class, expected_key='2Ж, 2С', expected_class_code='2Ж')

    @replace_setting('TRAIN_SERVICE_CLASS_FROM_PARTNER', True)
    def test_class_from_partner(self):
        train_details = TrainDetailsStub('203Ы', when=WHEN)
        class_details = ClassDetailsStub(train_details, 'sitting', 'ФПК', '2Ж', PARTNER_SERVICE_CLASS_DESCRIPTION,
                                         is_firm=False)
        klass = get_service_class(class_details, two_storey=False)
        check_class(klass, self.fpk_sitting_2_class, expected_key='2Ж, 2С', expected_class_code='2Ж',
                    expected_description=PARTNER_SERVICE_CLASS_DESCRIPTION)


def check_class(actual_class, expected_class, expected_key=None, expected_class_code=None, expected_description=None):
    if expected_class_code is None:
        expected_class_code = ensure_text(expected_class.Code if conf.TRAIN_BACKEND_USE_PROTOBUFS['service_class'] else
                                          expected_class.code)
    if expected_description is None:
        expected_description = ensure_text(expected_class.Description if conf.TRAIN_BACKEND_USE_PROTOBUFS['service_class'] else
                                           expected_class.L_description())
    assert actual_class.code == expected_class_code
    assert actual_class.key == (expected_key or expected_class_code)
    assert actual_class.title == ensure_text(expected_class.Title if conf.TRAIN_BACKEND_USE_PROTOBUFS['service_class'] else
                                             expected_class.L_title())
    assert actual_class.description == expected_description


def check_unmatched_class(actual_class, code):
    assert actual_class
    assert actual_class.key == code
    assert actual_class.code == code
    assert actual_class.title is None
    assert actual_class.description == PARTNER_SERVICE_CLASS_DESCRIPTION
