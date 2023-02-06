#!/usr/bin/env /usr/bin/python
# coding:utf8
import requests
import json
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--post", type=str, help="Post shoot comment")
parser.add_argument("--ticket", type=str, help="Ticket number for comment")
parser.add_argument("--regress", type=str, help="Regression comment")
args = parser.parse_args()
Ticket = args.ticket
PostShoot = args.post
Regress = args.regress
Comment = ''
Footer = 'https://wiki.yandex-team.ru/Load/Ocenka-strelby/'
Header = u'Сравнение'
good_result = u'успешно пройдено' 
bad_result = u'не пройдено'
regress_result = Regress.split("\n")[-1]
regress_body = "\n".join(Regress.split("\n")[:-1])
if Ticket != None:
#
    with open("/var/bmpt/lunapark/oauth_startrek.token", "r") as oauth:
        benderAuth = oauth.readline().replace("\n","")
#
    with open(PostShoot, "r") as ps:
        for line in ps:
            Comment += line
    Comment += "<{%s\n#|\n||%s||\n|#\n%s}>\n%s"%(Header.encode('utf-8'), 
                regress_body.replace("\n","||\n||").replace("passed","!!(green)passed!!").replace("failed","!!(red)failed!!").replace("improved","!!(yellow)improved!!"),
                Footer.encode('utf-8'), regress_result.replace("passed","**!!(green)" + good_result.encode('utf-8') + "!!**").replace("failed","**!!(red)" + bad_result.encode('utf-8') + "!!**"))
    Headers = {"Authorization": "OAuth %s"%(benderAuth), "Content-Type":"application/json"} 
    URL = "https://st-api.yandex-team.ru"
    DATA = {"text":Comment}
    tickets = Ticket.split(" ")
    for ticket in tickets:
        URI = "/v2/issues/{!s}/comments".format(ticket)
        try:
            session = requests.Session()
            result = session.post(url=URL+URI, headers=Headers, data=json.dumps(DATA), verify=False)
            print(result.json())
        except Exception as ex:
            print(ex)
        finally:
            session.close()
else:
    print("Unknown ticket")
