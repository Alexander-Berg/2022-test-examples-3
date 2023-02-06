# coding=utf-8
import core.utils.calculate_autotests_stat as calculate_autotests_stat
import requests as requests
import sys
import json

# Пример, как это работает:
# 1. Идем в шедулер и получаем из него последний запущенный таск -
# https://sandbox.yandex-team.ru/api/v1.0/scheduler/703882?limit=1 - таск 1253522891
# Идем в YT проверяем, что это новый таск (отличается от последнего в таблице)
# 2. Идем в ресурсы этого таска. Убеждаемся, что таск завершен. Среди ресурсов находим allure report.
# https://sandbox.yandex-team.ru/api/v1.0/task/1253522891/resources
# 3. Узнаем id этого ресурса - 2687075904. Из ресурса забираем json-отчет (подставляем id в ссылку)
# https://proxy.sandbox.yandex-team.ru/2902297804/data/suites.json
# 4. Забираем отчет и обрабатываем его


sandbox_token = sys.argv[1]
headers = {'Authorization': 'OAuth ' + sandbox_token}
yql_token = sys.argv[2]
yt_table = 'hahn.`home/market/users/vanina-a/demerzel/autotests_pipline_stat/ya_make`'
scheduler_id = '703882'
limit = '100'
output_name = sys.argv[4]
run_stat = []


def write_result(output_file_name, res):
    with open(output_file_name, 'w') as f:
        json.dump(res, f)


# Идем в шедулер и забираем инфу о последних запусках
run_urls = calculate_autotests_stat.get_runs_url(sandbox_token, scheduler_id, limit)

# Идём в YT и забираем инфу о последнем запуске из YT
last_run_yt = calculate_autotests_stat.get_last_yt_id(yql_token, yt_table)

for run_url in run_urls:
    run_id = run_url.split('/')[-1]
    # Если такой ран уже есть в YT – останавливаем цикл
    if last_run_yt == run_id:
        print('Ран {} уже был обработан, запись с таким ID есть в YT.'.format(run_url))
        break
    # Забираем инфу о ресурсах последнего запуска
    run_response = requests.get(run_url + '/resources', headers=headers)
    run_resources = json.loads(run_response.text)

    allure_report_id = None
    for item in run_resources['items']:
        if item['description'] != 'allure_report':
            continue
        print('Ран {}'.format(run_url))
        allure_report_id = item['id']
        # Дата в ответе в формате "created": "2022-01-13T13:05:48Z"
        date_info = item['time']['created']
        date = calculate_autotests_stat.get_time_with_moscow_hours(date_info)
        link_to_report_json = 'https://proxy.sandbox.yandex-team.ru/{}/data/suites.json'.format(allure_report_id)
        report_json_response = requests.get(link_to_report_json, headers=headers)
        report_json = json.loads(report_json_response.text)
        stats = calculate_autotests_stat.process_suites_data(report_json)
        stats['run_id'] = run_id
        stats['date'] = date
        print(stats)
        run_stat.append(stats)

write_result(output_name, run_stat)
