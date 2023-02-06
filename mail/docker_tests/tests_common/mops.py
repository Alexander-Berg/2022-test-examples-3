# coding: utf-8
import logging
import requests
import backoff

log = logging.getLogger(__name__)


def lst2param(lst):
    return ','.join(map(str, lst))


def user_params(user):
    return {
        'uid': user.uid,
        'suid': user.suid,
        'mdb': user.mdb,
    }


# https://wiki.yandex-team.ru/pochta/backend/mops/http-interface/

class Mops(object):
    def __init__(self, location):
        self.location = location

    @backoff.on_exception(backoff.expo,
                          requests.HTTPError,
                          max_tries=3)
    def _do(self, method, **params):
        r = requests.post(
            self.location + '/' + method,
            params,
        )
        log.info('[%s] mops response is %s', r.url, r.text)
        r.raise_for_status()
        return r.json()

    def stat(self, uid):
        return self._do('stat', uid=uid)

    def mark(self, user, mids, status):
        return self._do(
            'mark',
            status=status,
            mids=lst2param(mids),
            **user_params(user)
        )

    def label(self, user, mids, lids):
        return self._do(
            'label',
            mids=lst2param(mids),
            lids=lst2param(lids),
            **user_params(user)
        )

    def labels_create(self, user, name):
        return self._do(
            'labels/create',
            name=name,
            **user_params(user)
        )

    def folders_update_position(self, user, fid, prev_fid):
        return self._do(
            'folders/update_position',
            fid=fid,
            prev_fid=prev_fid,
            **user_params(user)
        )
