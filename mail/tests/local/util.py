import json
from mail.notsolitesrv.tests.integration.lib.util.user import User


def create_user(context, login, orgid=0):
    email = '{login}@yandex.ru'.format(login=login)
    response = context.fbb.register(email, orgid=orgid)
    assert response.status_code == 200

    data = json.loads(response.text)
    assert data.get('status') == 'ok'

    user = User(email=email)
    user.uid = data.get('uid')
    user.org_id = str(orgid)
    return user


def check_message_stored(context, uid, mid=None, revision=1):
    response = context.hound_api.get(
        '/v2/changes',
         params={'revision': revision, 'max_count': 10, 'uid': uid},
         headers={'X-Ya-Service-Ticket': context.hound.service_ticket}
    )
    assert response.status_code == 200
    changes = json.loads(response.text)['changes']
    if mid:
        assert len(changes) == 1, response.text
        assert changes[0]['type'] == 'store'
        assert mid == changes[0]['value'][0]['rfcId'], response.text
        return changes[0]['revision']
    else:
        assert len(changes) == 0
