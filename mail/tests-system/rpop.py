#!/usr/bin/python

import sys
import urllib
import urllib2
import time

from xml.dom import minidom

passport_url = "http://pass-test.yandex.ru/blackbox"
rpop_api_url = "http://localhost:3048/api/"

pop3_srv_addr = "pop.yandex.ru"
pop3_srv_port = "995"

test_user = "yapoptest302"

test_remote_user = "yapoptest@yandex.ru"
test_remote_pass = "poptest"

test_remote_user2 = "ya.rpopper@gmail.com"
test_remote_pass2 = "asd123qwe"


def get_suid(login):
    response = urllib2.urlopen(
        passport_url
        + "?method=userinfo&userip=127.0.0.1&login="
        + login
        + "&dbfields=subscription.suid.2"
    )
    xmlstr = response.read()
    xmldoc = minidom.parseString(xmlstr)
    itemlist = xmldoc.getElementsByTagName("dbfield")

    for s in itemlist:
        if s.attributes["id"].value == "subscription.suid.2":
            return s.childNodes[0].data


def get_mdb(login):
    response = urllib2.urlopen(
        passport_url
        + "?method=userinfo&userip=127.0.0.1&login="
        + login
        + "&dbfields=hosts.db_id.2"
    )
    xmlstr = response.read()

    xmldoc = minidom.parseString(xmlstr)
    itemlist = xmldoc.getElementsByTagName("dbfield")

    for s in itemlist:
        if s.attributes["id"].value == "hosts.db_id.2":
            return s.childNodes[0].data


def run_task(args):
    if len(args) < 2:
        print "usage: rpop.py run <login> <popid> [verbose] [processor_ng]"
        return

    suid = get_suid(args[0])
    print suid
    mdb = get_mdb(args[0])
    print mdb

    url = rpop_api_url + "run?mdb=" + mdb + "&suid=" + suid + "&popid=" + args[1] + "&synced=yes"
    if len(args) > 2 and args[2] == "verbose":
        url += "&verbose=yes"
    if len(args) > 3 and args[3] == "processor_ng":
        url += "&processor_ng=yes"

    response = urllib2.urlopen(url)
    return response.read()


def list_task(args):
    if len(args) < 1:
        print "usage: rpop.py list <login> [popid]"
        return

    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    url = rpop_api_url + "list?mdb=" + mdb + "&suid=" + suid
    if len(args) > 1:
        url += "&popid=" + args[1]

    response = urllib2.urlopen(url)
    return response.read()


def delete_task(args):
    if len(args) < 2:
        print "usage: rpop.py delete <login> <popid>"
        return

    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    response = urllib2.urlopen(
        rpop_api_url + "delete?mdb=" + mdb + "&suid=" + suid + "&popid=" + args[1]
    )
    return response.read()


def create_task(args):
    if len(args) < 3:
        print "usage: rpop.py create <my login> <remote login> <remote pass>"
        return

    login = args[0]
    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    values = {"password": args[2]}
    data = urllib.urlencode(values)
    req = urllib2.Request(
        rpop_api_url
        + "create?user="
        + login
        + "&mdb="
        + mdb
        + "&suid="
        + suid
        + "&login="
        + args[1]
        + "&email="
        + args[1]
        + "&no_delete_msgs=1&sync_abook=1&mark_archive_read=1",
        data,
    )
    response = urllib2.urlopen(req)
    return response.read()


def create_task_oauth(args):
    if len(args) < 3:
        print "usage: rpop.py create_oauth <my login> <remote login> <oauth code>"
        return

    login = args[0]
    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    values = {"social_task_id": args[2]}
    data = urllib.urlencode(values)
    req = urllib2.Request(
        rpop_api_url
        + "create?user="
        + login
        + "&mdb="
        + mdb
        + "&suid="
        + suid
        + "&login="
        + args[1]
        + "&email="
        + args[1]
        + "&no_delete_msgs=1&sync_abook=1&mark_archive_read=1&root_folder=INBOX",
        data,
    )
    response = urllib2.urlopen(req)
    return response.read()


def create_task_pop3(args):
    if len(args) < 3:
        print "usage: rpop.py create <my login> <remote login> <remote pass>"
        return

    login = args[0]
    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    values = {"password": args[2]}
    data = urllib.urlencode(values)
    req = urllib2.Request(
        rpop_api_url
        + "create?user="
        + login
        + "&mdb="
        + mdb
        + "&suid="
        + suid
        + "&login="
        + args[1]
        + "&server="
        + pop3_srv_addr
        + "&email="
        + args[1]
        + "&port="
        + pop3_srv_port
        + "&ssl=1&no_delete_msgs=1&sync_abook=1&mark_archive_read=1"
        + "&imap=0",
        data,
    )
    response = urllib2.urlopen(req)
    return response.read()


def enable_task(args):
    if len(args) < 3:
        print "usage: rpop.py enable <login> <popid> <is_on>"
        return

    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    url = (
        rpop_api_url
        + "enable?mdb="
        + mdb
        + "&suid="
        + suid
        + "&popid="
        + args[1]
        + "&is_on="
        + args[2]
    )

    response = urllib2.urlopen(url)
    return response.read()


def edit_task(args):
    if len(args) < 5:
        print "usage: rpop.py edit <login> <what> <new value> <password> <popid>"
        return

    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    url = (
        rpop_api_url
        + "edit?mdb="
        + mdb
        + "&suid="
        + suid
        + "&"
        + args[1]
        + "="
        + args[2]
        + "&popid="
        + args[4]
        + "&user="
        + args[0]
    )
    values = {"password": args[3]}
    data = urllib.urlencode(values)
    req = urllib2.Request(url, data)

    response = urllib2.urlopen(req)
    return response.read()


def check_server(args):
    if len(args) < 3:
        print "usage: rpop.py check_server <my login> <remote login> <remote password | oauth token> [oauth]"
        return

    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    url = (
        rpop_api_url
        + "check_server?mdb="
        + mdb
        + "&suid="
        + suid
        + "&login="
        + args[1]
        + "&email="
        + args[1]
        + "&popid=0"
    )

    values = {"password": args[2]}
    if len(args) > 3 and args[3] == "oauth":
        values = {"social_task_id": args[2]}

    data = urllib.urlencode(values)
    req = urllib2.Request(url, data)

    response = urllib2.urlopen(req)
    return response.read()


def test_rpop(args):
    create_res = create_task([test_user, test_remote_user, test_remote_pass])
    create_xml = minidom.parseString(create_res)
    create_item = create_xml.getElementsByTagName("create").item(0)

    popid = create_item.attributes["popid"].value

    list_res = list_task([test_user, popid])
    list_xml = minidom.parseString(list_res)
    list_item = list_xml.getElementsByTagName("rpop").item(0)

    list_is_on = list_item.attributes["is_on"].value
    if list_is_on != "1":
        print "list error"
        return

    enable_task([test_user, popid, "0"])

    list_res = list_task([test_user, popid])
    list_xml = minidom.parseString(list_res)
    list_item = list_xml.getElementsByTagName("rpop").item(0)

    list_is_on = list_item.attributes["is_on"].value
    if list_is_on != "0":
        print "enable error"
        return

    check_res = check_server([test_user, test_remote_user2, test_remote_pass2])
    check_xml = minidom.parseString(check_res)
    if check_xml.getElementsByTagName("error").length > 0:
        print "check server error"
        return

    edit_task([test_user, "login", test_remote_user2, test_remote_pass2, popid])
    edit_task([test_user, "server", "imap.gmail.com", test_remote_pass2, popid])

    list_res = list_task([test_user, popid])
    list_xml = minidom.parseString(list_res)
    list_item = list_xml.getElementsByTagName("rpop").item(0)

    if (
        list_item.attributes["login"].value != test_remote_user2
        or list_item.attributes["server"].value != "imap.gmail.com"
    ):
        print "edit error"
        return

    enable_task([test_user, popid, "1"])

    print run_task([test_user, popid])
    print "Waiting for task running"
    time.sleep(30)
    print delete_task([test_user, popid])


def status(args):
    if len(args) != 2:
        print "usage: rpop.py status <login> <popid>"
        return

    suid = get_suid(args[0])
    mdb = get_mdb(args[0])

    req = (
        rpop_api_url
        + "status?mdb="
        + mdb
        + "&suid="
        + suid
        + "&popid="
        + args[1]
        + "&user="
        + args[0]
    )
    response = urllib2.urlopen(req)
    return response.read()


def main(argv):
    if len(argv) < 1:
        print "rpop.py <operation> [args ...]"
        print "possible operations: run, create, create_oauth, create_pop3, delete, list, enable, edit, check_server, tests"
        return

    if argv[0] == "run":
        print run_task(argv[1:])
    elif argv[0] == "create":
        print create_task(argv[1:])
    elif argv[0] == "create_oauth":
        print create_task_oauth(argv[1:])
    elif argv[0] == "create_pop3":
        print create_task_pop3(argv[1:])
    elif argv[0] == "delete":
        print delete_task(argv[1:])
    elif argv[0] == "list":
        print list_task(argv[1:])
    elif argv[0] == "enable":
        print enable_task(argv[1:])
    elif argv[0] == "edit":
        print edit_task(argv[1:])
    elif argv[0] == "check_server":
        print check_server(argv[1:])
    elif argv[0] == "tests":
        print test_rpop(argv[1:])
    elif argv[0] == "status":
        print status(argv[1:])


if __name__ == "__main__":
    main(sys.argv[1:])
