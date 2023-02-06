import requests
import data.auth

host = "https://tcrm-myt.yandex-team.ru/api/v0/issue/"
yav_token = '' #токен OAuth от yav
token = data.auth.getSecrets(yav_token)
auth = {"Authorization":0}
auth['Authorization'] = 'OAuth ' +token
headers = auth