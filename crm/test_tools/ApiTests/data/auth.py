import requests

secretId = 'ver-01fa086bqsgncrp37jffhqmh1j' #версия секрета где лежит OAuth для Space API
host = 'https://vault-api.passport.yandex.net'

def getSecrets(yav_token):
    url = host + '/1/versions/' + secretId
    auth = {"Authorization":0}
    auth['Authorization'] = 'OAuth ' + yav_token
    newSecret = requests.get(url, headers=auth, verify=False)
    newSecret = newSecret.json()
    token = newSecret['version']['value'][0]['value']
    return token
