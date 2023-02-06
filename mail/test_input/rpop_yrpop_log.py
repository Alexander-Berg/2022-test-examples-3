header = {'server': 'logship-dev01e.cmail.yandex.net',
          'path': '/var/log/yrpop/yrpop.log'}

data = """tskv	tskv_format=yrpop-yrpop	session=h1LLP55SaqM1	action=check_server	status=error	reason=connect error	desc=connect error
tskv	tskv_format=yrpop-yrpop	session=a2LclLISaKo1	action=check_server	status=error	reason=login error	desc=login error
"""

expected = {
    'rpop_sessions': [['h1LLP55SaqM1',
                       {'cf:action': 'check_server',
                        'cf:desc': 'connect error',
                        'cf:reason': 'connect error',
                        'cf:session': 'h1LLP55SaqM1',
                        'cf:status': 'error'}],
                      ['a2LclLISaKo1',
                       {'cf:action': 'check_server',
                        'cf:desc': 'login error',
                        'cf:reason': 'login error',
                        'cf:session': 'a2LclLISaKo1',
                        'cf:status': 'error'}]]
}
