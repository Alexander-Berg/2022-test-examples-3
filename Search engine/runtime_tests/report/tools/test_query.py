#!/usr/bin/env python
# coding: utf-8

import requests
import sys

q = (
    'https://hamster.yandex.ru/search/site/?encoding=&text=test+%D0%BF%D1%80%D0%B8%D0%B2%D1%91%D0%81+%E1%B8%B7%C5%AB%E1%B9%AD%C3%B1%C4%81+kamp%C3%BCs+giri%C5%9F+%D8%B4%D8%A4%D8%AB+kas%C4%B1mpa%C5%9F%C5%9Fa+AR%C5%9E%C4%B0VLER%C4%B0%C4%9EQ%C3%87+%E2%98%BC%F0%9F%90%BC%F0%9F%90%B1%F0%9F%90%B2%F0%9F%92%A9%F0%9F%87%B7%F0%9F%87%BA&within=&clid=&htmlcss=1.x&web=0&constraintid=&from_month=&l10n=ru&html=1&from_year=&to_day=&surl=&from_day=&to_year=&callback=jQuery18305933488425875815_1423233158581&to_month=&date=&_=1427105835503&searchid=2124370&p=&topdoc=https%3A%2F%2Fkeaz.ru%2Fhelp%2Fsearch%3Fsearchid%3D2124370%26text%3D%25D1%2588%25D0%25B8%25D1%2580%25D0%25BE%25D0%25BA%25D0%25B8%25D0%25B9%26web%3D0&updatehash=true&tld=ru'
    # '&timeout=5000000'  # affects nothing, seems to mvel@
)

q = (
    'http://kozunov-report-dev.sas.yp-c.yandex.net/search/site/?encoding=&text=test+%D0%BF%D1%80%D0%B8%D0%B2%D1%91%D0%81+%E1%B8%B7%C5%AB%E1%B9%AD%C3%B1%C4%81+kamp%C3%BCs+giri%C5%9F+%D8%B4%D8%A4%D8%AB+kas%C4%B1mpa%C5%9F%C5%9Fa+AR%C5%9E%C4%B0VLER%C4%B0%C4%9EQ%C3%87+%E2%98%BC%F0%9F%90%BC%F0%9F%90%B1%F0%9F%90%B2%F0%9F%92%A9%F0%9F%87%B7%F0%9F%87%BA&within=&clid=&htmlcss=1.x&web=0&constraintid=&from_month=&l10n=ru&html=1&from_year=&to_day=&surl=&from_day=&to_year=&callback=jQuery18305933488425875815_1423233158581&to_month=&date=&_=1427105835503&searchid=2124370&p=&topdoc=https%3A%2F%2Fkeaz.ru%2Fhelp%2Fsearch%3Fsearchid%3D2124370%26text%3D%25D1%2588%25D0%25B8%25D1%2580%25D0%25BE%25D0%25BA%25D0%25B8%25D0%25B9%26web%3D0&updatehash=true&tld=ru'
    # '&timeout=5000000'  # affects nothing, seems to mvel@
)


headers = """Accept-Language: ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3
X-Yandex-Internal-Flags: eyJzcmNyd3IiOiB7IldJWkFSRCI6ICI6OjEwMDAiLCAiQVBQX0hPU1RfV0VCIjogIjo6MTAwMCJ9LCAidmFsaWRhdGVfdGVtcGxhdGVzX3BlcmMiOiAwfQ==
Host: yandex.ru
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
X-Yandex-Internal-Request: 0
Connection: close
Cookie: yp=; Session_id=noauth:1427381588
X-Forwarded-For-Y: 8.8.8.8
X-Yandex-Test-UUID: db6b5be0-2f7b-11e9-ba0d-bccbaeea4204
X-Yandex-HTTPS: yes
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2"""

headers = """Accept-Language: ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3
Host: yandex.ru
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
NNNNX-Yandex-Internal-Request: 0
Connection: close
NNNNNCookie: yp=; Session_id=noauth:1427381588
NNNNX-Forwarded-For-Y: 8.8.8.8
X-Yandex-Test-UUID: db6b5be0-2f7b-11e9-ba0d-bccbaeea4204
X-Yandex-HTTPS: yes
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2"""


headers = headers.split('\n')
headers_dict = {}
for h in headers:
    key, value = h.split(':', 1)
    headers_dict[key] = value.strip()

curl_query = "curl -v -v '{}' ".format(q)
for h in headers:
    curl_query += '-H "{}" '.format(h)

print curl_query
sys.exit(0)

r = requests.get(q, headers=headers_dict, verify=False, stream=True)
raw = r.raw
result = ""
with open('response.txt', 'wb') as f:
    c = raw.read()
    f.write(c)
    print len(c)

# print r.text

for h in r.headers:
    print h, r.headers[h]
