from core.utils.internal.yql_clickhouse_worker import YqlClickhouseWorker
from operator import itemgetter
import json
import sys
from datetime import datetime


sandbox_token = sys.argv[1]
yql_token = sys.argv[2]
output_name = sys.argv[4]


def get_yt_data():
    yql_client = YqlClickhouseWorker(yql_token)
    query = """USE marketclickhouse;
    SELECT
        date,
        timestamp,
        pipeline_id,
        pipeline_release_id,
        pipeline_jobs_id,
        pipeline_jobs_title,
        pipeline_job_launches_job_id as job_id,
        pipeline_job_launches_launch_id as launch_id,
        pipeline_job_launches_exclusive_execution_time_no_holidays_seconds as exclusive_execution_time_seconds,
        pipeline_job_launches_execution_time_no_holidays_seconds as execution_time_seconds,
        pipeline_job_launches_waiting_for_relaunch_no_holidays_seconds as waiting_for_relaunch_seconds,
        pipeline_job_launches_waiting_for_launch_no_holidays_seconds as waiting_for_launch_seconds
    FROM market.tsum_code_metrics
    WHERE date >= today() - 180 AND pipeline_id in
        ['LGW','LMS','MDB-Auto','capacity-storage-pipeline','deliveryTracker_release', 'logistics-lom', 'lrm-release'];
    """
    return yql_client.run_query(query)


def get_indexes(lst, el):
    return [i for i in range(len(lst)) if lst[i] == el]


def get_job_ids(pipeline):
    pipe_meta = {
        'pipeline_release_id': pipeline['pipeline_release_id'],
        'jobs': []
    }
    jobs = []
    if pipeline['pipeline_id'] == 'LGW':
        jobs = [
            'Пропуск автотестов',
            'Создание заказа Dropship через СЦ',
            'Создание заказа ФФ в партнёрский ПВЗ',
            'Создание Express заказа',
            'Создание Dropoff заказа'
        ]
        pipe_meta['component'] = 'LGW'
    elif pipeline['pipeline_id'] == 'LMS':
        jobs = [
            'Пропуск автотестов',
            'Создание заказа Dropship через СЦ',
            'Создание заказа ФФ в партнёрский ПВЗ',
            'Создание Express заказа',
            'Создание Dropoff заказа'
        ]
        pipe_meta['component'] = 'LMS'
    elif pipeline['pipeline_id'] == 'MDB-Auto':
        jobs = [
            'Пропуск всех тестов',
            'Создание заказа Dropship через СЦ',
            'Создание заказа ФФ в партнёрский ПВЗ',
            'Создание Express заказа',
            'Создание Dropoff заказа',
            'Создани заказа DBS в ПВЗ'
        ]
        pipe_meta['component'] = 'MDB'
    elif pipeline['pipeline_id'] == 'capacity-storage-pipeline':
        jobs = [
            'Пропустить автотесты',
            'Создание заказа Dropship через СЦ',
            'Создание заказа ФФ в партнёрский ПВЗ',
            'Создание Express заказа',
            'Создание Dropoff заказа'
        ]
        pipe_meta['component'] = 'CS'
    elif pipeline['pipeline_id'] == 'deliveryTracker_release':
        jobs = [
            'Пропуск автотестов',
            'Создание заказа Dropship через СЦ',
            'Создание заказа ФФ в партнёрский ПВЗ',
            'Создание Express заказа',
            'Создание Dropoff заказа'
        ]
        pipe_meta['component'] = 'Tracker'
    elif pipeline['pipeline_id'] == 'logistics-lom':
        jobs = [
            'Пропуск автотестов',
            'Создание заказа Dropship через СЦ',
            'Создание заказа ФФ в партнёрский ПВЗ',
            'Создание Express заказа',
            'Создание Dropoff заказа',
            'Создание заказа DBS в ПВЗ',
            'Проверка синхронизации данных в LMS и Redis LOM'
        ]
        pipe_meta['component'] = 'LOM'
    elif pipeline['pipeline_id'] == 'lrm-release':
        jobs = [
            'Пропустить автотесты',
            'FashionTest',
            'FBY Client Return',
            'FBS Client Return'
        ]
        pipe_meta['component'] = 'LRM'

    for job in jobs:
        if job not in pipeline['pipeline_jobs_title']:
            continue
        pipe_meta['jobs'].append({
            'job_name': job,
            'job_id': pipeline['pipeline_jobs_id'][pipeline['pipeline_jobs_title'].index(job)]
        })
    return pipe_meta


def get_jobs_max_waiting_time(jobs, pipeline):
    times = []
    skip_time = 0
    for job in jobs:
        if job['job_id'] not in pipeline['job_id']:
            continue
        time = 0
        for i in get_indexes(pipeline['job_id'], job['job_id']):
            time += pipeline['execution_time_seconds'][i] + pipeline['waiting_for_relaunch_seconds'][i] + pipeline['waiting_for_launch_seconds'][i]
        if job['job_name'] == 'Пропуск автотестов' or job['job_name'] == 'Пропуск всех тестов' or job['job_name'] == 'Пропустить автотесты':
            skip_time = time
        else:
            times.append(time)
    return max(times, default=0) + skip_time


def contains_pipe_id(_pipe_id, _pipelines):
    flag = 0
    for pipe in _pipelines:
        if _pipe_id in pipe['pipeline_release_id']:
            flag = 1
    return flag


def write_result(output_file_name, res):
    with open(output_file_name, 'w') as f:
        json.dump(res, f)


query_df = get_yt_data()
pipelines = []
for idx, row in query_df.iterrows():
    # Скипаем дубли
    if contains_pipe_id(row['pipeline_release_id'], pipelines):
        continue
    # Если в названии пайплайна есть /n, то он воспринимается как отдельный элемент в массиве. Убираем его
    if ' что запустилось' in row['pipeline_jobs_title']:
        row['pipeline_jobs_title'].remove(' что запустилось')
    # Если в названии пайплайна есть /n, то он воспринимается как отдельный элемент в массиве. Убираем его
    if ' вошедшие в релиз' in row['pipeline_jobs_title']:
        row['pipeline_jobs_title'].remove(' вошедшие в релиз')
    # Записываем в мету всё, что знаем о пайплайне
    pipeline_meta = get_job_ids(row)
    # Вычисляем максимальное время ожидания для тестовых кубиков в пайплайне
    pipeline_meta['time'] = get_jobs_max_waiting_time(pipeline_meta['jobs'], row)
    final_data = {
        'date': datetime.fromtimestamp(row['timestamp'], tz=None).strftime("%Y-%m-%d %H:%M:%S"),
        'pipeline_release_id': pipeline_meta['pipeline_release_id'],
        'component': pipeline_meta['component'],
        'execution_time': pipeline_meta['time']
    }
    pipelines.append(final_data)

write_result(output_name, sorted(pipelines, key=itemgetter('pipeline_release_id')))
