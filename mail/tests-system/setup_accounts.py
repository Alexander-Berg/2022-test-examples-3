#!/usr/bin/python
# coding=utf-8

import imaplib
import json
import string
import random
import datetime
import requests
from email.mime.text import MIMEText
from pyparsing import nestedExpr


def gen_str(size, with_newline=False):
    source = string.ascii_letters + string.digits
    if with_newline:
        source = source + '\n'
    return ''.join([random.choice(source) for x in range(size)])


def gen_date(start=None):
    if start is None:
        start = datetime.datetime(2015, 1, 1)

    end = datetime.datetime(2019, 1, 1)
    delta = end - start
    random_day = random.randrange(delta.days if delta.days > 0 else 100)
    random_second = ((random_day + 1) * 24 * 60 * 60) + random.randrange(24 * 60 * 60)
    res = start + datetime.timedelta(seconds=random_second)
    return res


def gen_message(date, to, lease_size=1000, body=None):
    msg_size = random.randint(lease_size+1, lease_size+10000)
    msg = MIMEText(body if body is not None else gen_str(msg_size, with_newline=True), _charset="utf-8")
    msg['Subject'] = gen_str(50)
    msg['From'] = gen_str(10)+'@ya.ru'
    msg['To'] = to
    msg['Message-Id'] = gen_str(100)
    msg['Date'] = date.strftime("%a, %d %b %Y %H:%M:%S +0300")
    msg['Content-Type'] = 'text/html; charset=utf-8'
    return msg.as_string()


def clear_mailbox(imap):
    resp = imap.list()
    assert resp[0] == 'OK', resp

    folders = resp[1]
    folder_names = []
    for folder in folders:
        parsed = nestedExpr().parseString("(" + folder + ")").asList()[0]
        folder_names.append(parsed[2])

    folder_names.sort(reverse = True )

    for name in folder_names:
        resp = imap.delete(name)

        if resp[0] != 'OK':
            try:
                resp = imap.select(name)
                assert resp[0] == 'OK', resp

                if resp[1][0] != '0':
                    resp = imap.store('1:'+resp[1][0], '+FLAGS', '\\Deleted')
                    assert resp[0] == 'OK', resp

                    resp = imap.expunge()
                    assert resp[0] == 'OK', resp
            except Exception as e:
                print "Clear problem on " + name
                print e


def append_letters(imap, descr, to):
    flags = ['\\Seen', '\\Deleted', '\\Draft', '\\Answered', '\\Flagged']
    if 'count' not in descr:
        descr['count'] = 5

    if 'randomize_date' not in descr:
        descr['randomize_date'] = True

    if 'randomize_to' not in descr:
        descr['randomize_to'] = False

    if 'sort_by_size' not in descr:
        descr['sort_by_size'] = False

    if 'text' not in descr:
        descr['text'] = None

    fixed_date = None
    if not descr['randomize_date']:
        fixed_date = gen_date()

    last_date = None
    last_size = 1000
    for i in range(descr['count']):
        date = fixed_date if fixed_date is not None else gen_date(last_date)
        last_date = date

        if descr['randomize_to']:
            msg = gen_message(date, gen_str(10)+'@ya.ru', last_size, descr['text'])
        else:
            msg = gen_message(date, to, last_size, descr['text'])

        if descr['sort_by_size']:
            last_size = len(msg)

        flag = random.choice(flags)
        str_date = date.strftime('"%d-%b-%Y %H:%M:%S +0300"')
        print "Appending message with flag=" + flag + ", date=" + str_date
        resp = imap.append('INBOX', flag, str_date, msg)


accounts_file = 'src/imap-tests/src/main/resources/accounts-testing.json'
accounts = json.load(open(accounts_file, 'r'))
search_accounts = {
    'SearchBasicKeysAllTest': {},
    'SearchByDateBeforeTest': {},
    'SearchByDateOnTest': {'randomize_date': False},
    'SearchByDateSentTest': {},
    'SearchByDateSinceTest': {},
    'SearchByDateTest': {'randomize_date': False},
    'SearchByDateWrongFormatTest': {},
    'SearchByDifferentCharTest': {},
    'SearchByFieldFromTest': {},
    'SearchByFieldSubjectTest': {},
    'SearchByFieldTextTest': {},
    'SearchByFieldToTest': { 'randomize_to': True },
    'SearchByFlagsUidTest': {},
    'SearchByKeysWithQuoteTest': {},
    'SearchBySizeCrossingTest': {},
    'SearchBySizeDifferentDataTest': {},
    'SearchBySizeLargerThenTest': {'count': 1},
    'SearchBySizeSmallerThenTest': {'count': 1},
    'SearchHeaderTest': {},
    'SearchInterestingCasesTest': {'count': 2, 'text': u'привет'.encode('utf-8')},
    'SearchKeywordTest': {'count': 10},
    'SearchOrCombinationTest': {'count': 2, 'randomize_date': False, 'sort_by_size': True}
}

accounts_to_disable_autoexpunge = [
    'CloseSpamAndDeleted',
    'CloseSystemFolders',
    'CloseUserFolders',
    'ExpungeUserFolders',
    'StoreDifferentFlags',
    'UnselectUserFolders'
]

for name, auth_data in accounts.iteritems():
    if name not in search_accounts:
        continue

    print "Processing user", name
    imap = imaplib.IMAP4_SSL('imap-tst.mail.yandex.net', 993)
    imap.capability()
    try:
        imap.login(auth_data["login"], auth_data["pwd"])

        clear_mailbox(imap)
        append_letters(imap, search_accounts[name] if name in search_accounts else {}, auth_data["login"]+'@ya.ru')
    except Exception as e:
        print "Failure on " + name + " (" + auth_data["login"] + "):"
        print e

for name in accounts_to_disable_autoexpunge:
    login = accounts[name]['login']
    req = requests.get(
        'http://pass-test.yandex.ru/blackbox?method=userinfo&userip=127.0.0.1&login={login}&format=json'.format(
            login=login))
    bb_res = json.loads(req.text)
    uid = bb_res['users'][0]['id']
    req = requests.post('http://settings-test.mail.yandex.net/update_params?uid={uid}'.format(uid=uid),
        data = {'disable_imap_autoexpunge':'true'})
    print 'Disbale auto-expunge for login={login}: {res}'.format(login=login, res=req.text)
