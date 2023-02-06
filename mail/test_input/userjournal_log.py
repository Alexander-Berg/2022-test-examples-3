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
data = u"""tskv	tskv_format=mail-user-journal-log	tableName=users_history	uid=00000000000043156031	module=yserver_imap	ip=::ffff:46.20.69.13	target=message	operation=label	date=1498121719487	unixtime=1498121719	mdb=xdb04	state=mids=162411061562063039:=32	affected=1	mids=162411061562063039	lids=32	
 tskv	tskv_format=mail-user-journal-log	timestamp=2017-06-22 11:58:02	timezone=+0300	target=message	operation=юникод	mid=162411061562056413widgetType=onelink	widgetSubType=onelink	origin=list-widget	client=LIZA	module=jsintegration	clientVer=13.0.602	uid=104762072	date=1498121882121	unixtime=1498121882	yandexuid=2563301631497470978	userIp=213.87.148.57	userAgent=Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.109 Safari/537.36	requestId=a9bda4a9c54dda9a5399a65c3b425744	uuid=u2709-1498121879466-98802572	orgId=users_history	status=ok	testBuckets=28236,0,43;44619,0,31;41204,0,43;40425,0,37;39459,0,21;28383,0,16;31190,0,93;32334,0,14	enabledTestBuckets=28236,0,43;44619,0,31;41204,0,43;40425,0,37;39459,0,21;28383,0,16;31190,0,93	testIds=28236,44619,41204,40425,39459,28383,31190
 tskv	tskv_format=mail-user-journal-log	timestamp=2017-06-22 11:58:02	timezone=+0300	target=message	operation=юникод	mid=162411061562056413widgetType=onelink	widgetSubType=onelink	origin=list-widget	client=LIZA	module=jsintegration	clientVer=13.0.602		date=1498121882121	unixtime=1498121882	yandexuid=2563301631497470978	userIp=213.87.148.57	userAgent=Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.109 Safari/537.36	requestId=a9bda4a9c54dda9a5399a65c3b425744	uuid=u2709-1498121879466-98802572	orgId=users_history	status=ok	testBuckets=28236,0,43;44619,0,31;41204,0,43;40425,0,37;39459,0,21;28383,0,16;31190,0,93;32334,0,14	enabledTestBuckets=28236,0,43;44619,0,31;41204,0,43;40425,0,37;39459,0,21;28383,0,16;31190,0,93	testIds=28236,44619,41204,40425,39459,28383,31190

"""
expected = u"""1		2017-06-22				::ffff:46.20.69.13	xdb04	yserver_imap	label		mids=162411061562063039:=32		message	2017-06-22 11:55:19	43156031
		2017-06-22						jsintegration	юникод				message	2017-06-22 11:58:02	104762072"""
