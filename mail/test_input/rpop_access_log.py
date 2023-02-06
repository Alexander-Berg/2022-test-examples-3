header = {'server': 'logship-dev01e.cmail.yandex.net',
          'path': '/var/log/yrpop/access.log'}

data = """tskv	tskv_format=yrpop-access	timestamp=18/Apr/2015:00:01:43	vhost=rpop.mail.yandex.net	ip=5.45.198.64	method=POST	request=/api/check_server?popid=1&login=nvr1255%40mail.ru&server=imap.mail.ru&port=993&ssl=1&imap=1&suid=611745524&mdb=mdb190&user=nvr20	protocol=HTTP/1.1	status=200	service=	suid=611745524	action=check_server	session=h1LLP55SaqM1	request_time=0.056
tskv	tskv_format=yrpop-access	timestamp=18/Apr/2015:00:02:37	vhost=rpop.mail.yandex.net	ip=5.45.198.64	method=POST	request=/api/check_server?popid=1&login=nvr1255%40mail.ru&server=imap.mail.ru&port=993&ssl=1&imap=1&suid=611745524&mdb=mdb190&user=nvr20	protocol=HTTP/1.1	status=200	service=	suid=611745524	action=check_server	session=a2LclLISaKo1	request_time=0.084
"""

expected = {
    'rpop_suid_sessions': [['00000000000611745524_18446742644405048196',
                            {'cf:action': 'check_server',
                             'cf:ip': '5.45.198.64',
                             'cf:method': 'POST',
                             'cf:protocol': 'HTTP/1.1',
                             'cf:request': '/api/check_server?popid=1&login=nvr1255%40mail.ru&server=imap.mail.ru&port=993&ssl=1&imap=1&suid=611745524&mdb=mdb190&user=nvr20',
                             'cf:request_time': '0.056',
                             'cf:service': '',
                             'cf:session': 'h1LLP55SaqM1',
                             'cf:status': '200',
                             'cf:suid': '611745524',
                             'cf:timestamp': '18/Apr/2015:00:01:43',
                             'cf:vhost': 'rpop.mail.yandex.net'}],
                           ['00000000000611745524_18446742644404994196',
                            {'cf:action': 'check_server',
                             'cf:ip': '5.45.198.64',
                             'cf:method': 'POST',
                             'cf:protocol': 'HTTP/1.1',
                             'cf:request': '/api/check_server?popid=1&login=nvr1255%40mail.ru&server=imap.mail.ru&port=993&ssl=1&imap=1&suid=611745524&mdb=mdb190&user=nvr20',
                             'cf:request_time': '0.084',
                             'cf:service': '',
                             'cf:session': 'a2LclLISaKo1',
                             'cf:status': '200',
                             'cf:suid': '611745524',
                             'cf:timestamp': '18/Apr/2015:00:02:37',
                             'cf:vhost': 'rpop.mail.yandex.net'}]]}
