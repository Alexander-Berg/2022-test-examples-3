# encoding: utf-8
from __future__ import unicode_literals
header = {
    'path': '/var/log/mail/user_journal.tskv',
    'ident': 'mail',
    'offset': '3',
    'partition': '1',
    'seqno': '9921',
    'server': 'logship-dev01e.cmail.yandex.net',
    'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
    'topic': 'rt3.sas--userjournal--mail-user-journal-log',
}
data = u"""tskv	uid=00000000000022165384	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=92.243.181.166	module=mailbox_oper	suid=53830614	test-buckets=40174,0,66;41204,0,20;39459,0,8;28384,0,18;31190,0,32;32334,0,27	enabled-test-buckets=40174,0,66;41204,0,20;39459,0,8;28384,0,18;31190,0,32	yandexuidCookie=5362435001452631438	clientType=LIZA	clientVersion=13.0.602	userAgent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36	connectionId=u2709-1498110784583-35776131	requestId=820fdd546c5c844ad4971a0a86e2e994	target=message	operation=trash	date=1498118465802	unixtime=1498118465	mdb=pg	state=162129586585347716,162129586585347724	affected=2	mids=162129586585347716,162129586585347724
tskv	uid=00001130000020504564	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=37.215.2.252	module=wmi	suid=1130000037708177	yandexuidCookie=1842208291457115721	test-buckets=32334,0,88	enabled-test-buckets=32334,0,88	clientType=LIZA	clientVersion=13.0.602	userAgent=Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36	connectionId=u2709-1498118635435-70388724	requestId=2a450de1baf42f67d0e652ec30458e73	operationSystem.name=Windows 7	operationSystem.version=6.1	browser.name=Chrome	browser.version=58.0.3029	regionId=157	internetProvider=AS6697	target=mailbox	operation=reset_fresh	date=1498118641119	unixtime=1498118641	hidden=1	mdb=xdb308	affected=1

tskv	tskv_format=mail-user-journal-tskv-log	tableName=users_history	uid=0000000000049869508999999999999999999999999999999999999999999999999999999999999999999999999999999	date=1498122132067	ip=2a02:6b8:0:1a72::2ee	module=settings	target=settings	operation=settings_update	state=enable_pop=	affected=0	hidden=1	regionId=0	unixtime=1498122132
"""
expected = u"""2	u2709-1498110784583-35776131	2017-06-22	LIZA			92.243.181.166	pg	mailbox_oper	trash		162129586585347716,162129586585347724	53830614	message	2017-06-22 11:01:05	22165384
1	u2709-1498118635435-70388724	2017-06-22	LIZA	1	AS6697	37.215.2.252	xdb308	wmi	reset_fresh	157		1130000037708177	mailbox	2017-06-22 11:04:01	1130000020504564"""
