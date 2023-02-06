# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
import mock

from travel.rasp.library.python.common23.db.migration_version import check_migration_version
from common.tester.factories import create_deluxe_train
from common.apps.im_logs.models import ImTrainCarType

from travel.rasp.admin.scripts.schedule.tis_train.check_deluxe_trains import get_deluxe_trains_errors


pytestmark = pytest.mark.dbuser


def test_get_deluxe_trains_errors():
    if check_migration_version(11):
        create_deluxe_train(
            numbers='101/201', wagon_types='11/12',
            title='Неправильный номер', im_title='Неправильный номер ИМ',
            need_check=True, need_reverse_check=True, min_im_records_count=10
        )
        create_deluxe_train(
            numbers='301/302/402', wagon_types='31',
            title='Лишний номер без проверки', im_title='Лишний номер без проверки ИМ',
            need_check=True, need_reverse_check=False, min_im_records_count=10
        )
        create_deluxe_train(
            numbers='401', wagon_types='41',
            title='Недостает номера без проверки', im_title='Недостает номера без проверки ИМ',
            need_check=False, need_reverse_check=True, min_im_records_count=10
        )
        create_deluxe_train(
            numbers='601', wagon_types='51',
            title='Не задано имя', im_title='',
            need_check=True, need_reverse_check=True, min_im_records_count=10
        )

        im_types = [
            ImTrainCarType(
                train_name='Неправильный номер ИМ',
                train_number='101',
                car_sub_type='11',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Неправильный номер ИМ',
                train_number='101',
                car_sub_type='12',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Неправильный номер ИМ',
                train_number='102',
                car_sub_type='11',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Неправильный номер ИМ',
                train_number='102',
                car_sub_type='12',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Какой-то',
                train_number='201',
                car_sub_type='21',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Какой-то',
                train_number='201',
                car_sub_type='22',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Неправильный номер ИМ',
                train_number='999',
                car_sub_type='11',
                im_records_count=2
            ),
            ImTrainCarType(
                train_name='Какой-то',
                train_number='101',
                car_sub_type='21',
                im_records_count=2
            ),

            ImTrainCarType(
                train_name='Лишний номер без проверки ИМ',
                train_number='301',
                car_sub_type='31',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Лишний номер без проверки ИМ',
                train_number='302',
                car_sub_type='31',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Недостает номера без проверки ИМ',
                train_number='402',
                car_sub_type='41',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Недостает номера без проверки ИМ',
                train_number='401',
                car_sub_type='41',
                im_records_count=20
            ),

            ImTrainCarType(
                train_name='Не задано имя ИМ',
                train_number='501',
                car_sub_type='51',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Не задано имя ИМ',
                train_number='601',
                car_sub_type='61',
                im_records_count=20
            ),

            ImTrainCarType(
                train_name='Не задано имя ИМ',
                train_number='',
                car_sub_type='51',
                im_records_count=20
            ),
            ImTrainCarType(
                train_name='Не задано имя ИМ',
                train_number='501',
                car_sub_type='',
                im_records_count=20
            )
        ]

        with mock.patch(
            'travel.rasp.admin.scripts.schedule.tis_train.check_deluxe_trains.get_im_train_car_types',
            return_value=im_types
        ):
            errors_message=get_deluxe_trains_errors()

            assert 'Поезд 102 ошибочно отсутствует в списке "Неправильный номер".' in errors_message
            assert '("Неправильный номер ИМ", "11")' in errors_message
            assert '("Неправильный номер ИМ", "12")' in errors_message

            assert 'Поезд 201 ошибочно присутствует в списке "Неправильный номер".' in errors_message
            assert '("Какой-то", "21")' in errors_message
            assert '("Какой-то", "22")' in errors_message

            assert 'Поезд 999' not in errors_message
            assert 'в списке "Какой-то"' not in errors_message

            assert 'Поезд 301' not in errors_message
            assert 'Поезд 302' not in errors_message
            assert 'Поезд 401' not in errors_message
            assert 'Поезд 402' not in errors_message
            assert 'в списке "Лишний номер без проверки"' not in errors_message
            assert 'в списке "Недостает номера без проверки"' not in errors_message

            assert 'Поезд 501 ошибочно отсутствует в списке "Не задано имя". ' \
                   'Неправильные вагоны: ("Не задано имя ИМ", "51")' in errors_message
            assert 'Поезд 601 ошибочно присутствует в списке "Не задано имя". ' \
                   'Неправильные вагоны: ("Не задано имя ИМ", "61")' in errors_message
