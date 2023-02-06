# -*- coding: utf-8 -*-
# заготовка для заглушек для сервисов для нагрузочного тестирования
# сейчас есть: торги БК, паспорт (oauth)
# как запускать: python fake_services_app.py
# требует python-flask 0.10.* (на более высоких версиях другой интерфейс и работать не будет)
# как запустить uwsgi (требует uwsgi-core, uwsgi-plugin-python)
# FAKE_BSRANK_DELAY=1 uwsgi --strict --plugins python,http --http 127.0.0.1:5000 --module fake_services_app:app --master --workers 10

from flask import Flask, request
import os
import re
import sys
import time

app = Flask(__name__)

def generate_data_for_phrase(id, PriorityID, PhraseID):
    return """{},542,{},{},13900000,300000,0,0,0
29473,46438,88241,3,0,0,0,36016
18200000:36700000,14200000:22700000,12900000:12900000,12900000:12900000
10300000:19800000,7000000:17700000,6400000:14200000,6200000:6200000
11: 300000 1028, 900000 26860, 1500000 112202, 2000000 229713, 2600000 402810, 3200000 580993, 3800000 737921, 4400000 850527, 4900000 912781, 5500000 957249, 6100000 980103
5889166:5889166,10194823:19800000,12900000:12900000,16900331:34422088
300000 400000 500000 600000 1000000 1100000 1300000 1600000 1900000 2000000 2300000 3400000 3700000 4500000""".format(id, PriorityID, PhraseID)

# пока считаем, что ходит только api с oauth и не проверяем параметры
@app.route("/blackbox", methods=['GET'])
def blackbox():
    return """<?xml version="1.0" encoding="UTF-8"?>
<doc>
<OAuth>
<uid>311514137</uid>
<token_id>338364459</token_id>
<device_id></device_id>
<device_name></device_name>
<scope>direct:api</scope>
<ctime>2016-10-28 14:06:45</ctime>
<issue_time>2016-10-28 14:06:45</issue_time>
<expire_time>2018-07-07 16:42:11</expire_time>
<is_ttl_refreshable>1</is_ttl_refreshable>
<client_id>ae99016820074f809e5c268e564bebad</client_id>
<client_name>direct api test</client_name>
<client_icon></client_icon>
<client_homepage></client_homepage>
<client_ctime>2011-08-05 00:00:00</client_ctime>
<client_is_yandex>0</client_is_yandex>
<xtoken_id></xtoken_id>
<meta></meta>
</OAuth>
<uid hosted="0">311514137</uid>
<login>yukaba-super-2</login>
<have_password>1</have_password>
<have_hint>1</have_hint>
<karma confirmed="0">0</karma>
<karma_status>0</karma_status>
<regname>yukaba-super-2</regname>
<display_name>
<name>yukaba-super-2</name>
<avatar>
<default>0/0-0</default>
<empty>1</empty>
</avatar>
</display_name>
<dbfield id="accounts.login.uid">yukaba-super-2</dbfield>
<status id="0">VALID</status>
<error>OK</error>
<connection_id>t:338364459</connection_id>
</doc>
"""

# описание торгов: https://wiki.yandex-team.ru/direct/auction/
# работает не для всех запросов, например для такого не работает (в тестовой базе)
# {"method":"get","params":{"SelectionCriteria":{"CampaignIds":["11956691"]},"Page":{"Limit":"10000","Offset":"0"},"FieldNames":["KeywordId","Bid","AuctionBids","AdGroupId"]}}
@app.route("/rank/24", methods=['GET'])
def rank24():
    targets = request.args.getlist('target')
    response=""
    for i, t in enumerate(targets):
        id, OrderID, market_price, text, rest = t.split(',', 4)
        PriorityID, PhraseID = 0, 0
        if rest.startswith('s:'):
            pass
        else:
            if rest.startswith('p:'):
                rest = rest[2:]
            sys.stderr.write(rest + "\n")
            PriorityID, PhraseID = rest.split(',')
        response += generate_data_for_phrase(i + 1, PriorityID, PhraseID) + '\n'
    if 'FAKE_BSRANK_DELAY' in os.environ:
        time.sleep(float(os.environ['FAKE_BSRANK_DELAY']))
    return response

if __name__ == '__main__':
    app.run(debug=True)
