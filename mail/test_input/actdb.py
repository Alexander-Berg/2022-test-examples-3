# Test case for actdb.

header = {
    'path': '/var/log/mops/user_journal.tskv',
    'ident': 'userjournal',
    'offset': '3',
    'partition': '0',
    'seqno': '9921',
    'server': 'logship-dev01e.cmail.yandex.net',
    'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
    'topic': 'rt3.sas--userjournal--raw',
}

tskv_log = """tskv	uid=00000000000466242936	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=185.13.112.129	module=wmi	suid=987910468yandexuidCookie=1808792171495900736	test-buckets=44619,0,64;38033,0,83;43778,0,7;46919,0,84;39459,0,93	enabled-test-buckets=44619,0,64;38033,0,83;43778,0,7;46919,0,84;39459,0,93	clientType=LIZA	clientVersion=13.3.358	userAgent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36	connectionId=u2709-1504472448618-68883156	requestId=1ba00e17a61e10a2c99aac4e2c3f73aa	operationSystem.name=Windows 10	operationSystem.version=10.0	browser.name=Chrome	browser.version=60.0.3112	regionId=1	internetProvider=AS29069	target=mailbox	operation=reset_fresh	date=1504472971139	unixtime=1504472971	hidden=1	mdb=xdb312	affected=1
tskv	uid=00000000000501481128	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=212.22.86.13	module=wmi	suid=1017836814	yandexuidCookie=5212144261465036477	test-buckets=48127,0,33;38033,0,90;43778,0,56;46919,0,40;39459,0,84	enabled-test-buckets=48127,0,33;38033,0,90;43778,0,56;46919,0,40;39459,0,84	clientType=LIZA	clientVersion=13.3.358iuserAgent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36	connectionId=u2709-1504465843453-46394327	requestId=c5516374d046bc0de3a139fa16e20400	operationSystem.name=Windows 7operationSystem.version=6.1	browser.name=Chrome	browser.version=60.0.3112	regionId=11007	internetProvider=AS204054	target=mailbox	operation=reset_fresh	date=1504472973016	unixtime=1504472973	hidden=1	mdb=xdb314	affected=1
tskv	uid=00000000000219899014	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=31.173.87.144	module=mobile	suid=683648964clientType=mob-app-unknown	userAgent=ru.yandex.mail/349.409 (iPhone8,4; iOS 10.3.3)	connectionId=97b11dc96ff8f362eae3c5fcb56a6ec5	requestId=e21d27cbf81b2f74f17a0c64152268f0	operationSystem.version=10.3.3	browser.name=Unknown	regionId=213	internetProvider=AS25159	target=mailbox	operation=reset_fresh	date=1504472974754	unixtime=1504472974	hidden=1	mdb=xdb17	affected=1
tskv	uid=00000000000501481128	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=212.22.86.13	module=wmi	suid=1017836814	yandexuidCookie=5212144261465036477	test-buckets=48127,0,33;38033,0,90;43778,0,56;46919,0,40;39459,0,84	enabled-test-buckets=48127,0,33;38033,0,90;43778,0,56;46919,0,40;39459,0,84	clientType=LIZA	clientVersion=13.3.358userAgent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36	connectionId=u2709-1504465843453-46394327	requestId=bd1bd7f2321eb907c22c14784e9fb4d4	operationSystem.name=Windows 7operationSystem.version=6.1	browser.name=Chrome	browser.version=60.0.3112	regionId=11007	internetProvider=AS204054	target=mailbox	operation=reset_fresh	date=1505472973009	unixtime=1504472975	hidden=1	mdb=xdb314	affected=1
tskv	uid=00000000000020003987	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=46.242.14.12	module=wmi	suid=48269635	yandexuidCookie=1131249961244059442	test-buckets=35428,0,4;38033,0,12;43778,0,51;46919,0,4;39459,0,58	enabled-test-buckets=35428,0,4;38033,0,12;43778,0,51;46919,0,4;39459,0,58	clientType=LIZA	clientVersion=13.3.358userAgent=Mozilla/5.0 (Windows NT 5.1; rv:52.0) Gecko/20100101 Firefox/52.0	connectionId=u2709-1504426470786-43407878	requestId=2c5886e33c3332e81038a588340f72f9	operationSystem.name=Windows XP	operationSystem.version=5.1	browser.name=Firefox	browser.version=52.0	regionId=213	internetProvider=AS42610	target=mailbox	operation=reset_fresh	date=1504472975075	unixtime=1504472975	hidden=1	mdb=xdb11	affected=1
tskv	uid=00000000000321079085	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=195.93.151.235	module=wmi	suid=862421594yandexuidCookie=4479252321491286146	clientType=TOUCH	clientVersion=4.16.412	connectionId=iface-1504472977401-84550019	requestId=d2926703a8824396e9c651404b466df7	operationSystem.name=Android Lollipop	operationSystem.version=5.1.1	browser.name=YandexSearch	browser.version=6.53	regionId=98611	internetProvider=AS44254	target=mailbox	operation=reset_fresh	date=1504472978139	unixtime=1504472978	hidden=1	mdb=xdb313	affected=1
tskv	uid=00000000000008419139	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=89.223.57.232	module=wmi	suid=18868452	yandexuidCookie=6399381671483032478	test-buckets=48127,0,88,38033,0,24,43778,0,24,46920,0,91,39459,0,37	enabled-test-buckets=48127,0,88,38033,0,24,43778,0,24,46920,0,91,39459,0,37	clientType=LIZA	clientVersion=13.3.358	userAgent=Mozilla/5.0 (Windows NT 10.0; WOW64; rv:55.0) Gecko/20100101 Firefox/55.0	connectionId=LIZA-61656356-1504472272564	requestId=9519622497d186111104875b8740d653	operationSystem.name=Windows 10	operationSystem.version=10.0	browser.name=Firefox	browser.version=55.0	regionId=2	internetProvider=AS42668	target=mailbox	operation=reset_fresh	date=1505472973491	unixtime=1504472978	hidden=1	mdb=xdb10	affected=1
tskv	uid=00000000000331874432	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=154.130.232.88	suid=872039695yandexuidCookie=3035104741436247373	clientType=TOUCH	clientVersion=4.16.412	userAgent=Mozilla/5.0 (Linux; Android 4.1.2; C1905 Build/15.1.C.2.8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.95 Mobile Safari/537.36	connectionId=iface-1504472816068-11643719	requestId=8c2d35b059aba475edd953c32a0433e2	operationSystem.name=Android Jelly Bean	operationSystem.version=4.1.2	browser.name=ChromeMobile	browser.version=48.0.2564	regionId=1056	internetProvider=AS37069	target=mailbox	operation=reset_fresh	date=1504472980384	unixtime=1504472980	hidden=1	mdb=xdb303	affected=1
tskv	uid=00000000000031464462	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=37.215.171.254	module=wmi	suid=1130000031555421	yandexuidCookie=9269030461373018484	test-buckets=44651,0,52	enabled-test-buckets=44651,0,52	clientType=LIZA	clientVersion=13.3.358	userAgent=Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 YaBrowser/17.7.1.791 Yowser/2.5 Safari/537.36	connectionId=u2709-1504458017140-78458437	requestId=61d033114b380b56f52ae23b4386b268	operationSystem.name=Windows 10	operationSystem.version=10.0	browser.name=YandexBrowser	browser.version=17.7.1.791	regionId=157	internetProvider=AS6697	target=mailbox	operation=reset_fresh	date=1504472980834	unixtime=1504472980	hidden=1	mdb=xdb22	affected=1
tskv	uid=00000000000031464462	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=31.129.208.42	module=wmi	suid=78392776	yandexuidCookie=3647651761450295153	test-buckets=44618,0,34,38033,0,31,43778,0,13,46920,0,30,39459,0,45,40424,0,3	enabled-test-buckets=44618,0,34,38033,0,31,43778,0,13,46920,0,30,39459,0,45,40424,0,3	clientType=LIZAclientVersion=13.3.358	userAgent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.2.0.3539 Safari/537.36	connectionId=LIZA-04692168-1504472978867	requestId=64cb0757500d8cb5a48e066bf53dabaa	operationSystem.name=Windows 7	operationSystem.version=6.1	browser.name=YandexBrowser	browser.version=16.2.0.3539	regionId=21645	internetProvider=AS47286	target=mailbox	operation=reset_fresh	date=1504472981625	unixtime=1504472981	hidden=1	mdb=xdb20	affected=1
tskv	uid=00000000000331874432	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=154.130.232.88	module=settings	suid=872039695yandexuidCookie=3035104741436247373	clientType=TOUCH	clientVersion=4.16.412	userAgent=Mozilla/5.0 (Linux; Android 4.1.2; C1905 Build/15.1.C.2.8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.95 Mobile Safari/537.36	connectionId=iface-1504472816068-11643719	requestId=5cbba7c35cfb1c131df95c967295befa	operationSystem.name=Android Jelly Bean	operationSystem.version=4.1.2	browser.name=ChromeMobile	browser.version=48.0.2564	regionId=1056	internetProvider=AS37069	target=mailbox	operation=reset_fresh	date=1504472982313	unixtime=1504472982	hidden=1	mdb=xdb303	affected=1
tskv	uid=00001130000012162170	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=31.23.170.44	module=fastsrv	suid=1130000028825468	yandexuidCookie=5522433211447318905	test-buckets=44618,0,42,38033,0,56,43778,0,70,46920,0,40,39459,0,45	enabled-test-buckets=44618,0,42,38033,0,56,46920,0,40	clientType=LIZA	clientVersion=13.3.358	userAgent=Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36 OPR/47.0.2631.71	connectionId=LIZA-19935063-1504472982463	requestId=744a9df190b5037df2712336b7d6918a	operationSystem.name=Windows 7operationSystem.version=6.1	browser.name=Opera	browser.version=47.0.2631.71	regionId=39	internetProvider=AS21479	target=mailbox	operation=reset_fresh	date=1504472985819	unixtime=1504472985	hidden=1	mdb=xdb303	affected=1
tskv	uid=00000000000136774475	tskv_format=mail-user-journal-tskv-log	tableName=users_history	ip=109.252.29.70	module=mobile	suid=402537538yandexuidCookie=2157802251501364215	test-buckets=38033,0,29,43778,0,76,46920,0,91,39459,0,80	enabled-test-buckets=38033,0,29,43778,0,76,46920,0,91,39459,0,80	clientType=LIZA	clientVersion=13.3.358	userAgent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36	connectionId=LIZA-87650481-1504435999155	requestId=e4c27de1e384461ff8783205e617b97f	operationSystem.name=Windows 10	operationSystem.version=10.0	browser.name=Chrome	browser.version=60.0.3112	regionId=213	internetProvider=AS25513	target=mailbox	operation=reset_fresh	date=1504472988167	unixtime=1504472988	hidden=1	mdb=xdb26	affected=1
"""

expected_data_accumulator = {
    '2017-09-04': [{
        'last_dt':
        '2017-09-04',
        'user_agent':
        u'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36',
        'module':
        u'wmi',
        'uid':
        466242936
    }, {
        'last_dt': '2017-09-04',
        'user_agent': '\\N',
        'module': u'wmi',
        'uid': 501481128
    }, {
        'last_dt':
        '2017-09-04',
        'user_agent':
        u'ru.yandex.mail/349.409 (iPhone8,4; iOS 10.3.3)',
        'module':
        u'mobile',
        'uid':
        219899014
    }, {
        'last_dt': '2017-09-04',
        'user_agent': '\\N',
        'module': u'wmi',
        'uid': 20003987
    }, {
        'last_dt': '2017-09-04',
        'user_agent': '\\N',
        'module': u'wmi',
        'uid': 321079085
    }, {
        'last_dt':
        '2017-09-04',
        'user_agent':
        u'Mozilla/5.0 (Linux; Android 4.1.2; C1905 Build/15.1.C.2.8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.95 Mobile Safari/537.36',
        'module':
        '\\N',
        'uid':
        331874432
    }, {
        'last_dt':
        '2017-09-04',
        'user_agent':
        u'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 YaBrowser/17.7.1.791 Yowser/2.5 Safari/537.36',
        'module':
        u'wmi',
        'uid':
        31464462
    }, {
        'last_dt':
        '2017-09-04',
        'user_agent':
        u'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.2.0.3539 Safari/537.36',
        'module':
        u'wmi',
        'uid':
        31464462
    }, {
        'last_dt':
        '2017-09-04',
        'user_agent':
        u'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36',
        'module':
        u'mobile',
        'uid':
        136774475
    }],
    '2017-09-15': [{
        'last_dt': '2017-09-15',
        'user_agent': '\\N',
        'module': u'wmi',
        'uid': 501481128
    }, {
        'last_dt':
        '2017-09-15',
        'user_agent':
        u'Mozilla/5.0 (Windows NT 10.0; WOW64; rv:55.0) Gecko/20100101 Firefox/55.0',
        'module':
        u'wmi',
        'uid':
        8419139
    }]
}

expected_sql = [('cursor', (), {}), ('execute', (
    'SELECT pg_is_in_recovery();',
), {}), ('fetchone', (), {}), ('mogrify', (
    "INSERT INTO history.user_agent_activity VALUES %s, %s ON CONFLICT (uid, user_agent) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_agent_activity.last_dt != '2017-09-04'",
    ((466242936,
      u'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36',
      '2017-09-04'),
     (219899014, u'ru.yandex.mail/349.409 (iPhone8,4; iOS 10.3.3)',
      '2017-09-04'))
), {}), ('execute', (((
    "INSERT INTO history.user_agent_activity VALUES %s, %s ON CONFLICT (uid, user_agent) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_agent_activity.last_dt != '2017-09-04'",
    ((466242936,
      u'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36',
      '2017-09-04'),
     (219899014, u'ru.yandex.mail/349.409 (iPhone8,4; iOS 10.3.3)',
      '2017-09-04'))
), {}), ), {}), ('mogrify', (
    "INSERT INTO history.user_activity VALUES %s, %s, %s, %s, %s ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_activity.last_dt != '2017-09-04'",
    ((321079085, u'wmi', '2017-09-04'), (466242936, u'wmi', '2017-09-04'),
     (501481128, u'wmi', '2017-09-04'), (219899014, u'mobile', '2017-09-04'),
     (20003987, u'wmi', '2017-09-04'))
), {}), ('execute', (((
    "INSERT INTO history.user_activity VALUES %s, %s, %s, %s, %s ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_activity.last_dt != '2017-09-04'",
    ((321079085, u'wmi', '2017-09-04'), (466242936, u'wmi', '2017-09-04'),
     (501481128, u'wmi', '2017-09-04'), (219899014, u'mobile', '2017-09-04'),
     (20003987, u'wmi', '2017-09-04'))
), {}), ), {}), ('mogrify', (
    "INSERT INTO history.user_agent_activity VALUES %s, %s, %s, %s ON CONFLICT (uid, user_agent) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_agent_activity.last_dt != '2017-09-04'",
    ((136774475,
      u'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36',
      '2017-09-04'),
     (331874432,
      u'Mozilla/5.0 (Linux; Android 4.1.2; C1905 Build/15.1.C.2.8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.95 Mobile Safari/537.36',
      '2017-09-04'),
     (31464462,
      u'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 YaBrowser/17.7.1.791 Yowser/2.5 Safari/537.36',
      '2017-09-04'),
     (31464462,
      u'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.2.0.3539 Safari/537.36',
      '2017-09-04'))
), {}), ('execute', (((
    "INSERT INTO history.user_agent_activity VALUES %s, %s, %s, %s ON CONFLICT (uid, user_agent) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_agent_activity.last_dt != '2017-09-04'",
    ((136774475,
      u'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36',
      '2017-09-04'),
     (331874432,
      u'Mozilla/5.0 (Linux; Android 4.1.2; C1905 Build/15.1.C.2.8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.95 Mobile Safari/537.36',
      '2017-09-04'),
     (31464462,
      u'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 YaBrowser/17.7.1.791 Yowser/2.5 Safari/537.36',
      '2017-09-04'),
     (31464462,
      u'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.2.0.3539 Safari/537.36',
      '2017-09-04'))
), {}), ), {}), ('mogrify', (
    "INSERT INTO history.user_activity VALUES %s, %s ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_activity.last_dt != '2017-09-04'",
    ((136774475, u'mobile', '2017-09-04'), (31464462, u'wmi', '2017-09-04'))
), {}), ('execute', (((
    "INSERT INTO history.user_activity VALUES %s, %s ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2017-09-04' WHERE history.user_activity.last_dt != '2017-09-04'",
    ((136774475, u'mobile', '2017-09-04'), (31464462, u'wmi', '2017-09-04'))
), {}), ), {}), ('commit', (), {}), ('mogrify', (
    "INSERT INTO history.user_agent_activity VALUES %s ON CONFLICT (uid, user_agent) DO UPDATE SET last_dt = '2017-09-15' WHERE history.user_agent_activity.last_dt != '2017-09-15'",
    ((8419139,
      u'Mozilla/5.0 (Windows NT 10.0; WOW64; rv:55.0) Gecko/20100101 Firefox/55.0',
      '2017-09-15'), )
), {}), ('execute', (((
    "INSERT INTO history.user_agent_activity VALUES %s ON CONFLICT (uid, user_agent) DO UPDATE SET last_dt = '2017-09-15' WHERE history.user_agent_activity.last_dt != '2017-09-15'",
    ((8419139,
      u'Mozilla/5.0 (Windows NT 10.0; WOW64; rv:55.0) Gecko/20100101 Firefox/55.0',
      '2017-09-15'), )
), {}), ), {}), ('mogrify', (
    "INSERT INTO history.user_activity VALUES %s, %s ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2017-09-15' WHERE history.user_activity.last_dt != '2017-09-15'",
    ((501481128, u'wmi', '2017-09-15'), (8419139, u'wmi', '2017-09-15'))
), {}), ('execute', (((
    "INSERT INTO history.user_activity VALUES %s, %s ON CONFLICT (uid, module) DO UPDATE SET last_dt = '2017-09-15' WHERE history.user_activity.last_dt != '2017-09-15'",
    ((501481128, u'wmi', '2017-09-15'), (8419139, u'wmi', '2017-09-15'))
), {}), ), {}), ('commit', (), {})]
