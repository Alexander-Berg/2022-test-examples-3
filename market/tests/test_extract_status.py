# coding=utf-8
import datetime
import string

import pytest
import requests

from lib.blueprints.cubes_arrival import get_extract_status, OK_CODE, CRIT_CODE, WARN_CODE
import logging
from freezegun import freeze_time

log = logging.getLogger()

TABLEAU_JOBS_TEMPLATE = '{{"pagination":{{"pageNumber":"1","pageSize":"30","totalAvailable":"105"}},"backgroundJobs":{{"backgroundJob":[{}]}}}}'
# createdAt используется только у пендингов, не хочу забивать пространство лишними подстановками
SUCCESS_JOB_TEMPLATE = '{{' \
                       '"id":"1b691784-5150-430d-8a21-089ad6d91063",' \
                       '"status":"Success",' \
                       '"createdAt":"2020-05-24T20:30:13Z",' \
                       '"startedAt":"{started_at}",' \
                       '"endedAt":"{ended_at}",' \
                       '"priority":"50",' \
                       '"jobType":"refresh_extracts"' \
                       '}}'
IN_PROGRESS_JOB_TEMPLATE = '{{' \
                           '"id":"6d5a0ffb-4a8e-46bd-ad00-182ab83bb012",' \
                           '"status":"InProgress",' \
                           '"createdAt":"2020-05-24T20:26:05Z",' \
                           '"startedAt":"{started_at}",' \
                           '"priority":"0",' \
                           '"jobType":"refresh_extracts"}}'
PENDING_JOB_TEMPLATE = '{{' \
                       '"id":"c06a9ff1-855b-4b44-8a65-c7509fb10fe6",' \
                       '"status":"Pending",' \
                       '"createdAt":"{created_at}",' \
                       '"priority":"50",' \
                       '"jobType":"refresh_extracts"' \
                       '}}'
FAILED_JOB_TEMPLATE = '{{' \
                      '"id":"d2a959c6-e26d-418e-8cf0-b86b4c5862d7",' \
                      '"status":"Failed",' \
                      '"createdAt":"2020-06-02T05:42:43Z",' \
                      '"startedAt":"{started_at}",' \
                      '"endedAt":"{ended_at}",' \
                      '"priority":"0",' \
                      '"jobType":"refresh_extracts"' \
                      '}}'


@pytest.fixture
def mock_get_secrets(mocker):
    mocker.patch('lib.blueprints.cubes_arrival.get_secrets', return_value={"tablo_user": None, "tablo_password": None})


@pytest.fixture
def mock_get_secrets_in_extract_info(mocker):
    mocker.patch('lib.extract_info.get_secrets', return_value={"tablo_user": None, "tablo_password": None})


@pytest.fixture
def mock_tableau_signin(mocker):
    mocker.patch('lib.extract_info.signin', return_value=(None, 'hypothetical_tableau_url_prefix'))


@pytest.fixture
def mock_request_get_tablo(mocker, tableau_jobs):
    tableau_response = requests.Response()
    tableau_response.status_code = 200
    tableau_response._content = tableau_jobs
    mocker.patch('lib.extract_info.request_get_tablo', return_value=tableau_response)


@freeze_time("2012-05-25 00:00:00")
@pytest.mark.usefixtures('mock_get_secrets', 'mock_tableau_signin', 'mock_request_get_tablo',
                         'mock_get_secrets_in_extract_info')
@pytest.mark.parametrize("test_case, estimated_start_time, tableau_jobs, expected_status, extract_time",
                         [
                             # Внимание, АПИ табло отдает время в UTC таймзоне, а оповещать надо все же в нашей
                             # Поэтому время в шаблоне и на выходе везде на три часа расходится

                             # Запуск без доп. аргументов, поэтому просто последний не-pending статус "success" - и все ок
                             (
                                 # test_case
                                 'successful_extract',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(SUCCESS_JOB_TEMPLATE.format(
                                     started_at='2020-05-24T20:52:53Z',
                                     ended_at='2020-05-24T20:53:03Z')),
                                 # expected_status
                                 OK_CODE,
                                 # extract_time
                                 None
                             ),
                             # Указываем ожидаемое время начала, поэтому последний не-pending статус должен быть "success"
                             # И время старта этого экстракта должно быть достаточно свежим (оно 23:52 24го мая)
                             (
                                 # test_case
                                 'successful_extract_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 20, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(SUCCESS_JOB_TEMPLATE.format(
                                     started_at='2020-05-24T20:52:53Z',
                                     ended_at='2020-05-24T20:53:03Z')),
                                 # expected_status
                                 OK_CODE,
                                 # extract_time
                                 None
                             ),
                             # Указываем ожидаемое время начала, поэтому последний не-pending статус должен быть "success"
                             # И время старта этого экстракта должно быть достаточно свежим (а оно 23:52 24го мая)
                             # Пенлингов тут нет -> crit, потому что актуальный экстракт даже в очереди не стоит
                             (
                                 # test_case
                                 'successful_extract_not_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 55, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(SUCCESS_JOB_TEMPLATE.format(
                                     started_at='2020-05-24T20:52:53Z',
                                     ended_at='2020-05-24T20:53:03Z')),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Стоит в пендинге, последний экстракт до него - успех
                             (
                                 # test_case
                                 'pending_after_success',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(
                                     string.join(
                                         [PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T20:52:53Z'),
                                          SUCCESS_JOB_TEMPLATE.format(started_at='2020-05-24T10:52:53Z',
                                                                      ended_at='2020-05-24T11:53:03Z')
                                          ], ', ')
                                 ),
                                 # expected_status
                                 OK_CODE,
                                 # extract_time
                                 None
                             ),
                             # Стоит в пендинге, последний экстракт до него - успех
                             # Но передаем ожидаемое время старта -> warn, потому что мы ждем свежий экстракт
                             (
                                 # test_case
                                 'pending_after_success_not_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 55, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(
                                     string.join([
                                         PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T20:52:53Z'),
                                         SUCCESS_JOB_TEMPLATE.format(started_at='2020-05-24T10:52:53Z',
                                                                     ended_at='2020-05-24T11:53:03Z')
                                     ], ', ')
                                 ),
                                 # expected_status
                                 WARN_CODE,
                                 # extract_time
                                 None
                             ),
                             # Стоит в пендинге, последний экстракт до него - фэйл
                             (
                                 # test_case
                                 'pending_after_fail',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(string.join(
                                     [PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T20:52:53Z'),
                                      FAILED_JOB_TEMPLATE.format(started_at='2020-05-24T10:52:53Z',
                                                                 ended_at='2020-05-24T11:53:03Z')
                                      ], ', ')),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Стоит в пендинге, последний экстракт до него - фэйл
                             # Но мы ждем свежий и нам пофиг на фэйл
                             (
                                 # test_case
                                 'pending_after_fail_not_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 55, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(string.join(
                                     [PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T20:52:53Z'),
                                      FAILED_JOB_TEMPLATE.format(started_at='2020-05-24T10:52:53Z',
                                                                 ended_at='2020-05-24T11:53:03Z')
                                      ], ', ')),
                                 # expected_status
                                 WARN_CODE,
                                 # extract_time
                                 None
                             ),
                             # Стоит в пендинге, а уже запустился экстракт на старых данных
                             (
                                 # test_case
                                 'pending_after_inprogress_not_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 55, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(string.join(
                                     [PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T20:52:53Z'),
                                      IN_PROGRESS_JOB_TEMPLATE.format(started_at='2020-05-24T10:52:53Z')
                                      ], ', ')),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Стоит в пендинге, бежит экстракт на свежих данных
                             # Странно, но бежит себе свежий - и ладно
                             (
                                 # test_case
                                 'pending_after_inprogress_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 25, 1, 35, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(string.join(
                                     [PENDING_JOB_TEMPLATE.format(created_at='2020-05-25T00:00:53Z'),
                                      IN_PROGRESS_JOB_TEMPLATE.format(started_at='2020-05-24T23:00:00Z')
                                      ], ', ')),
                                 # expected_status
                                 OK_CODE,
                                 # extract_time
                                 '05:20'
                             ),
                             # Стоит в пендинге, бежит экстракт, свежесть данных не волнует
                             (
                                 # test_case
                                 'pending_after_inprogress',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(string.join(
                                     [PENDING_JOB_TEMPLATE.format(created_at='2020-05-25T00:00:53Z'),
                                      IN_PROGRESS_JOB_TEMPLATE.format(started_at='2020-05-24T23:00:00Z')
                                      ], ', ')),
                                 # expected_status
                                 OK_CODE,
                                 # extract_time
                                 '05:20'
                             ),
                             # Упал
                             (
                                 # test_case
                                 'failed_extract',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(FAILED_JOB_TEMPLATE.format(
                                     started_at='2020-05-24T20:52:53Z',
                                     ended_at='2020-05-24T20:53:03Z')),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Актуальный упал
                             (
                                 # test_case
                                 'failed_extract',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 0, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(FAILED_JOB_TEMPLATE.format(
                                     started_at='2020-05-24T20:52:53Z',
                                     ended_at='2020-05-24T20:53:03Z')),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Неактуальный упал, но нового тоже не видно
                             (
                                 # test_case
                                 'failed_extract',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 22, 0, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(FAILED_JOB_TEMPLATE.format(
                                     started_at='2020-05-24T20:52:53Z',
                                     ended_at='2020-05-24T20:53:03Z')),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Запустился, бежит, верим, что все будет ок, extract_time - прогноз по файлу graph_ds_extracts
                             (
                                 # test_case
                                 'inprogress',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(IN_PROGRESS_JOB_TEMPLATE.format(
                                     started_at='2020-05-24T21:00:00Z')),
                                 # expected_status
                                 OK_CODE,
                                 # extract_time
                                 '03:20'
                             ),
                             # Запустился, бежит, верим, что все будет ок
                             # extract_time - прогноз по файлу graph_ds_extracts
                             # С ожидаемым временем начала (вписывается)
                             (
                                 # test_case
                                 'inprogress_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 0, 0, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(
                                     IN_PROGRESS_JOB_TEMPLATE.format(started_at='2020-05-24T20:52:53Z')),
                                 # expected_status
                                 OK_CODE,
                                 # extract_time
                                 '03:12'
                             ),

                             # Запустился, бежит, верим, что все будет ок
                             # С ожидаемым временем начала (не вписывается)
                             (
                                 # test_case
                                 'inprogress_not_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 55, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(
                                     IN_PROGRESS_JOB_TEMPLATE.format(started_at='2020-05-24T20:52:53Z')),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Упал, но уже обратно встал в очередь, есть надежда, но лучше присматривать
                             (
                                 # test_case
                                 'pending_after_fail_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 25, 1, 40, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(
                                     string.join(
                                         [
                                             PENDING_JOB_TEMPLATE.format(created_at='2020-05-25T00:00:53Z'),
                                             FAILED_JOB_TEMPLATE.format(started_at='2020-05-24T23:00:00Z',
                                                                        ended_at='2020-05-24T23:01:00Z')
                                         ], ', ')
                                 ),
                                 # expected_status
                                 WARN_CODE,
                                 # extract_time
                                 None
                             ),
                             # Одни пендинги на заданном промежутке
                             (
                                 # test_case
                                 'only_pendings',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(
                                     string.join([
                                         PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T21:52:53Z'),
                                         PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T20:52:53Z'),
                                         PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T19:52:53Z')
                                     ], ', ')
                                 ),
                                 # expected_status
                                 WARN_CODE,
                                 # extract_time
                                 None
                             ),
                             # Одни пендинги на заданном промежутке
                             # С ожидаемым временем старта
                             (
                                 # test_case
                                 'only_pendings_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 23, 0, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(
                                     string.join([
                                         PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T21:52:53Z'),
                                         PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T20:52:53Z'),
                                         PENDING_JOB_TEMPLATE.format(created_at='2020-05-24T19:52:53Z')
                                     ], ', ')
                                 ),
                                 # expected_status
                                 WARN_CODE,
                                 # extract_time
                                 None
                             ),
                             # Нет таких тасок (экстракт на заданном промежутке времени не создавался даже)
                             (
                                 # test_case
                                 'no_extracts_found',
                                 # estimated_start_time
                                 None,
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(''),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),
                             # Нет таких тасок (экстракт на заданном промежутке времени не создавался даже)
                             # Для порядка с ожидаемым времнем тоже
                             (
                                 # test_case
                                 'no_extracts_found_actual',
                                 # estimated_start_time
                                 datetime.datetime(2020, 5, 24, 0, 0, 0),
                                 # tableau_jobs
                                 TABLEAU_JOBS_TEMPLATE.format(''),
                                 # expected_status
                                 CRIT_CODE,
                                 # extract_time
                                 None
                             ),

                         ]
                         )
def test_get_extract_status(test_case, estimated_start_time, tableau_jobs, expected_status, extract_time):
    # Последние два аргумента используюся только для получения списка джобов, а он тут ручной
    response = get_extract_status(unicode('OOS', "utf-8"), estimated_start_time, None, None)
    log.debug(response)
    # Текст не сверяю, потому что мы задолбаемся поддерживать это в актуальном виде.
    assert response.status_code == expected_status
    assert response.forecasted_finish_time == extract_time
