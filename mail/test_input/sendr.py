# encoding: utf-8
# TestCase mail statistics
header_delivery = {'path': '/var/log/yandex/sendr/delivery.log',
                   'ident': 'sender',
                   'offset': '3',
                   'partition': '0',
                   'seqno': '9921',
                   'server': 'logship-dev01e.cmail.yandex.net',
                   'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
                   'topic': 'rt3.sas--sender--sender-delivery-log',
                   }
header_click = {'path': '/var/log/yandex/sendr/click.tskv.log',
                'ident': 'sender',
                'offset': '3',
                'partition': '0',
                'seqno': '9921',
                'server': 'logship-dev01e.cmail.yandex.net',
                'sourceid': 'base64:eL5wm2zzRya7j7ZHsKS2Zg',
                'topic': 'rt3.sas--sender--sender-click-log',
                }

data_delivery = """
tskv\ttskv_format=sendr-delivery-log\tunixtime=1488315756\tfull_timestamp=2017-03-01 00:02:36.537686+0300\tpid=30521\taccount=realty\tcampaign_id=932\tchannel=email\tcontext={"host":"delivery1h.cmail.yandex.net","to_email":"test@example.com","task_id":"3b912c15-3e6e-4cc6-b8e6-d0f9be0eeae6","context":{}}\tletter_code=A\tletter_id=1053\tmessage-id=<20170301000235.383894.22193.8617.30521@delivery1h.cmail.yandex.net>\trecepient=test@example.com\tresults={"smtp":{"response":{"text":"2.0.0 Ok: queued on yaback1m.mail.yandex.net as 1488315756-sWkbtbIiW7-2ZsqHQg2","code":250}}}\tstatus=0\tfor_testing=True
tskv\ttskv_format=sendr-delivery-log\tunixtime=1488315756\tfull_timestamp=2017-03-01 00:02:36.537686+0300\tpid=30521\taccount=realty\tcampaign_id=932\tchannel=email\tcontext={"host":"delivery1h.cmail.yandex.net","to_email":"test@example.com","task_id":"3b912c15-3e6e-4cc6-b8e6-d0f9be0eeae6","context":{}}\tletter_code=A\tletter_id=1053\tmessage-id=<20170301000235.383894.22193.8617.30521@delivery1h.cmail.yandex.net>\trecepient=test@example.com\tresults={"smtp":{"response":{"text":"2.0.0 Ok: queued on yaback1m.mail.yandex.net as 1488315756-sWkbtbIiW7-2ZsqHQg2","code":250}}}\tstatus=0\tfor_testing=False
tskv\ttskv_format=sendr-delivery-log\tunixtime=1488315756\tfull_timestamp=2017-03-01 00:02:36.537686+0300\tpid=30521\taccount=\tcampaign_id=\tchannel=email\tcontext={"host":"delivery1h.cmail.yandex.net","to_email":"test@example.com","task_id":"3b912c15-3e6e-4cc6-b8e6-d0f9be0eeae6","context":{}}\tletter_code=\tletter_id=1053\tmessage-id=\trecepient=test@example.com\tresults=\tstatus=999\tfor_testing=False
"""

expected_delivery = (
    'tskv\tdate=2017-03-01\tchannel=email\trecepient=test@example.com\tevent_date=2017-03-01 00:02:36\taccount=realty\tcampaign=932\tletter=1053\tletter_code=A\tmessage_id=<20170301000235.383894.22193.8617.30521@delivery1h.cmail.yandex.net>\tstatus=0\ttest_letter=1',
    'tskv\tdate=2017-03-01\tchannel=email\trecepient=test@example.com\tevent_date=2017-03-01 00:02:36\taccount=realty\tcampaign=932\tletter=1053\tletter_code=A\tmessage_id=<20170301000235.383894.22193.8617.30521@delivery1h.cmail.yandex.net>\tstatus=0\ttest_letter=0',
    'tskv\tdate=2017-03-01\tchannel=email\trecepient=test@example.com\tevent_date=2017-03-01 00:02:36\tletter=1053\tstatus=999\ttest_letter=0',
)


data_click = """
tskv\ttskv_format=sendr-click-log\tunixtime=1488488425\tfull_timestamp=2017-03-03 00:00:25.492464+0300\tpid=17002\tblackbox={"session": false}\tcampaign_id=7885\taccount=sendr\tcookies={}\temail=test@example.com\tevent=px\tparsed=True\tts=1488488425.492459\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.2 Safari/537.36\tuser_ip=188.93.56.121\tfor_testing=True\tletter_id=1053
tskv\ttskv_format=sendr-click-log\tunixtime=1488488425\tfull_timestamp=2017-03-03 00:00:25.492464+0300\tpid=17002\tblackbox={"session": false}\tcampaign_id=7885\taccount=sendr\tcookies={}\temail=test@example.com\tevent=px\tparsed=True\tts=1488488425.492459\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.2 Safari/537.36\tuser_ip=188.93.56.121\tfor_testing=False\tletter_id=1053
tskv\ttskv_format=sendr-click-log\tunixtime=1488488472\tfull_timestamp=2017-03-03 00:01:12.032066+0300\tpid=20149\tallowed=True\tblackbox={"session": false}\tcampaign_id=7885\tcookies=\temail=test@example.com\tevent=click\tparsed=True\tts=1488488472.032060\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0\tuser_ip=178.35.5.168\tfor_testing=False\tletter_id=1053\tlink_number=1
tskv\ttskv_format=sendr-click-log\tunixtime=1488488472\tfull_timestamp=2017-03-03 00:01:12.032066+0300\tpid=20149\tallowed=True\tblackbox={"session": false}\tcampaign_id=7885\tcookies=\temail=test@example.com\tevent=click\tparsed=True\tts=1488488472.032060\tuser_agent=Mozilla/5.0 (Windows NT 6.1; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0\tuser_ip=178.35.5.168\tfor_testing=False\tletter_id=1053\tlink_id=some_link\tlink_url=https://ya.ru
tskv\ttskv_format=sendr-click-log\tunixtime=1488488472\tfull_timestamp=2017-03-03 00:01:12.032066+0300\tblackbox={"session": false}\tcampaign_id=7885\tcookies=\temail=test@example.com\tevent=unsub\tevent_type=account\tparsed=True\tts=1488488472.032060\tfor_testing=False\tletter_id=1053
tskv\ttskv_format=sendr-click-log\tunixtime=1488488472\tfull_timestamp=2017-03-03 00:01:12.032066+0300\tcampaign_id=7885\temail=test@example.com\tevent=bounce\tevent_type=permanent\tts=1488488472.032060\tfor_testing=False\tmessage_id=test
"""

expected_click = (
    'tskv\tdate=2017-03-03\tchannel=email\trecepient=test@example.com\tevent=px\tevent_date=2017-03-03 00:00:25\tcampaign=7885\taccount=sendr\tletter=1053\ttest_letter=1',
    'tskv\tdate=2017-03-03\tchannel=email\trecepient=test@example.com\tevent=px\tevent_date=2017-03-03 00:00:25\tcampaign=7885\taccount=sendr\tletter=1053\ttest_letter=0',
    'tskv\tdate=2017-03-03\tchannel=email\trecepient=test@example.com\tevent=click\tevent_date=2017-03-03 00:01:12\tcampaign=7885\tletter=1053\tlink_id=1\ttest_letter=0',
    'tskv\tdate=2017-03-03\tchannel=email\trecepient=test@example.com\tevent=click\tevent_date=2017-03-03 00:01:12\tcampaign=7885\tletter=1053\tlink_id=some_link\tlink_url=https://ya.ru\ttest_letter=0',
    'tskv\tdate=2017-03-03\tchannel=email\trecepient=test@example.com\tevent=unsub\tevent_type=account\tevent_date=2017-03-03 00:01:12\tcampaign=7885\tletter=1053\ttest_letter=0',
    'tskv\tdate=2017-03-03\tchannel=email\trecepient=test@example.com\tevent=bounce\tevent_type=permanent\tevent_date=2017-03-03 00:01:12\tcampaign=7885\ttest_letter=0\tmessage_id=test',
)
