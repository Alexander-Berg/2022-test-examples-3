import json
from subprocess import check_output

import requests


GET_RESOURCES_URL = 'https://sandbox.yandex-team.ru/api/v1.0/resource?limit=1&attrs={{"svn_revision":%20"{svn_revision}"}}&type=MARKET_REPORT'


def get_report_info_by_revision(svn_revision):
    response = requests.get(GET_RESOURCES_URL.format(svn_revision=svn_revision))
    response.raise_for_status()
    decoded_data = json.loads(response.content.decode())
    items = decoded_data['items']
    if items:
        return items[0]['skynet_id'], items[0]['file_name']


def get_report_bins(skynet_id, dir_to_extract, file_name):
    print('>>> temp dir for downloading report: ' + dir_to_extract)
    check_output(['sky', 'get', '-w', skynet_id], cwd=dir_to_extract)
    print('>>> {} has been downloaded'.format(file_name))
    check_output(['tar', '-xvzf', file_name], cwd=dir_to_extract)
    check_output(['rm', file_name], cwd=dir_to_extract)
    print('>>> {} has been extracted to {} and removed'.format(file_name, dir_to_extract))
