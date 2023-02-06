import core.utils.calculate_autotests_stat as calculate_autotests_stat
import requests as requests
import json
import tarfile
import os
import shutil
import sys


# Как работает скрипт
# 1. Идём в шедулер и забираем из него limit последних прогонов. Сейчас это 200 (чтоб собрать больше исторических данных)
# 2. Идём в YT смотрим какой там последний таск
# 3. Пробегаемся по завершённым прогонам, если такой прогон есть в YT, завершаем работу скрипта
# 4. Смотрим на ресурсы прогона. Ищем среди них allure-отчёт. Скачиваем его
# 5. Распаковываем allure-отчёт в свою папку
# 6. Пробегаемся по каждому кейсу. Из кейса вытягиваем информацию про оффер и шаги (всю информацию можно посмотреть в переменной res_case)
# 7. Удаляем папку с allure-отчётом
# 8. Записываем результат в файл

print(sys.version)
sandbox_token = sys.argv[1]
headers = {'Authorization': 'OAuth ' + sandbox_token}
yql_token = sys.argv[2]
yt_table = 'hahn.`home/market/users/vanina-a/demerzel/autotests_stat/cases_statuses`'
scheduler_id = '25043'
limit = '200'
output_name = sys.argv[4]
run_stat = []


def write_result(output_file_name, res):
    with open(output_file_name, 'w') as f:
        json.dump(res, f)


def get_step_names(test_steps, lst=None, ordr=None):
    if ordr is None:
        ordr = []
    if lst is None:
        lst = []
    for step in test_steps:
        lst.append(step['name'])
        for param in step['parameters']:
            if param['name'] == 'order':
                ordr.append(param['value'])
        get_step_names(step['steps'], lst=lst, ordr=ordr)
    return lst, ordr


# Идём в YT и забираем инфу о последнем запуске из YT
yt_runs = calculate_autotests_stat.get_yt_runs(yql_token, yt_table)

# Идем в шедулер и забираем инфу о последних запусках
run_urls = calculate_autotests_stat.get_runs_url(sandbox_token, scheduler_id, limit)
counter = 3

for run_url in reversed(run_urls):
    run_id = run_url.split('/')[-1]
    # Если такой ран уже есть в YT – останавливаем цикл
    if run_id in yt_runs:
        print(f'Ран {run_url} уже был обработан, запись с таким ID есть в YT.')
        continue
    counter -= 1
    if counter < 0:
        break
    # Забираем инфу о ресурсах последнего запуска
    run_response = requests.get(f'{run_url}/resources', headers=headers)
    run_resources = run_response.json()

    allure_report_id = None
    for resource in run_resources['items']:
        # Ищем среди ресурсов allure_report
        if resource['description'] != 'allure_report':
            continue
        print('=================Ран=================')
        print(f'Ран {run_url}')
        allure_report_id = resource['id']
        # Дата в ответе в формате "created": "2022-01-13T13:05:48Z"
        date_info = resource['time']['created']
        date = calculate_autotests_stat.get_time_with_moscow_hours(date_info)
        print(f'Дата {date}')
        # Берём ссылку на allure-отчёт и скачиваем его
        if resource['mds'] is not None:
            link_to_allure_report = resource['mds']['url']
        else:
            continue
        print(f'Линк {link_to_allure_report}')
        h = requests.head(link_to_allure_report, headers=headers, allow_redirects=True)
        total_size = int(h.headers.get('content-length'))
        print(total_size)
        report_response = requests.get(link_to_allure_report, headers=headers, stream=True)
        if os.path.exists(run_id):
            shutil.rmtree(run_id)
        os.mkdir(run_id)
        chunks = 0
        print('=================download started!=================')
        with open(os.path.join(run_id, 'allure_report.tar'), 'wb') as handle:
            for data in report_response.iter_content(chunk_size=1024):
                handle.write(data)
        print('=================download complete!=================')
        # Разархивируем отчёт
        with tarfile.open(os.path.join(run_id, 'allure_report.tar'), 'r') as allure_report:
            allure_report.extractall(path=f'{run_id}/')
        directory = os.path.join(run_id, 'allure_report', 'data', 'test-cases')
        # Смотрим какие есть кейсы в отчёте, разбираем их
        test_cases = os.listdir(directory)
        for test_case in test_cases:
            with open(os.path.join(directory, test_case), 'r') as case_report:
                case = json.loads(case_report.read())
                # Если это кейс для запуска прогона – скипаем
                if case['name'] == 'runSuite()':
                    continue
                # Если в кейсе нет шагов – скипаем
                if 'testStage' not in case:
                    continue
                # Заполняем инфу о ссылках на тестпалм
                links = []
                if case['links']:
                    for link in case['links']:
                        links.append(link['url'])
                testpalm_links = ', '.join(links)
                # Берём названия шагов и информацию о заказе
                print(f'testcase {test_case}')
                steps, orders = get_step_names(case['testStage']['steps'], [], [])
                # Парсим информацию о заказе
                orders_json = []
                if orders:
                    for order in orders:
                        if order != 'null':
                            try:
                                json_object = json.loads(order)
                            except ValueError as e:
                                pass
                            else:
                                orders_json.append(json_object)
                wareId = []
                orderId = 0
                if orders_json:
                    for i in orders_json[-1]['items']:
                        if i['wareMd5'] is not None:
                            wareId.append(i['wareMd5'])
                    orderId = orders_json[-1]['id']
                # Заполняем окончательную информацию
                res_case = {
                    'case_name': case['name'],
                    'date': date,
                    'duration': case['time']['duration'] / (1000 * 60),
                    'status': case['status'],
                    'case_package_name': case['fullName'],
                    'run_id': run_id,
                    'testpalm_id': testpalm_links,
                    'step_names': ' -> '.join(steps),
                    'order': orderId,
                    'wareId': ', '.join(wareId)
                }
                run_stat.append(res_case)
        # Пытаемся удалить всё, что скачали. Да, 2 раза, это не опечатка
        if os.path.exists(os.path.join(run_id, 'allure_report.tar')):
            os.remove(os.path.join(run_id, 'allure_report.tar'))
        if os.path.exists(run_id):
            shutil.rmtree(run_id, ignore_errors=True)
        if os.path.exists(os.path.join(run_id, 'allure_report.tar')):
            os.remove(os.path.join(run_id, 'allure_report.tar'))
        if os.path.exists(run_id):
            shutil.rmtree(run_id, ignore_errors=True)
write_result(output_name, run_stat)
