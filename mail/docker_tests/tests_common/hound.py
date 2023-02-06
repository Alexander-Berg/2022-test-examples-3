import json
import backoff


from ora2pg.tools import http


class MalformedHoundResponse(RuntimeError):
    pass

# https://wiki.yandex-team.ru/users/jkennedy/ywmiapi/


def get_field_from_response(field, resp):
    try:
        return resp[field]
    except KeyError:
        raise MalformedHoundResponse(
            ("Response doesn't have '%s' field: " % field) + json.dumps(resp))


hound_retries = backoff.on_exception(backoff.expo,
                                     MalformedHoundResponse,
                                     max_tries=5)

class Hound(object):
    def __init__(self, location):
        self.location = location

    def _hound_request(self, method, uid, **args):
        args['uid'] = uid
        with http.request(
            url=http.url_join(
                host=self.location,
                method=method,
                args=args),
            do_retries=True,
            timeout=10,
        ) as fd:
            return json.load(fd)


    def folders(self, uid):
        resp = self._hound_request('folders', uid)
        return get_field_from_response('folders', resp)


    def labels(self, uid):
        resp = self._hound_request('labels', uid)
        return get_field_from_response('labels', resp)

    @hound_retries
    def messages_by_message_id(self, uid, msgid, fid):
        resp = self._hound_request(
            'messages_by_message_id',
            uid,
            msgid=msgid,
            fid=fid
        )
        return get_field_from_response('mids', resp)


    @hound_retries
    def _hound_envelopes_request(self, method, uid, **kwargs):
        resp = self._hound_request(
            method,
            uid,
            **kwargs
        )
        return get_field_from_response('envelopes', resp)

    def messages_by_folder(self, uid, fid, first, count):
        return self._hound_envelopes_request(
            'messages_by_folder',
            uid,
            fid=fid, first=first, count=count
        )

    def filter_search(self, uid, mids):
        return self._hound_envelopes_request(
            'filter_search',
            uid,
            mids=','.join(mids)
        )

    def nearest_messages(self, uid, mid, deviation):
        return self._hound_envelopes_request(
            'nearest_messages',
            uid,
            mid=mid, deviation=deviation
        )


def folders(location, uid):
    return Hound(location).folders(uid)


def messages_by_folder(location, uid, fid, first, count):
    return Hound(location).messages_by_folder(
        uid=uid,
        fid=fid,
        first=first,
        count=count)
