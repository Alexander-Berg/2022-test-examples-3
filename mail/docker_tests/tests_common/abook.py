import json


from ora2pg.tools import http


class Abook(object):
    def __init__(self, location):
        self.location = location

    def colabook_addp(self, uid, first_name=None, middle_name=None, last_name=None, tel_list=None, descr=None, mail_addr=None):
        return self._post_request(
            method='compat/colabook_addp',
            args=filter_not_none(dict(
                uid=uid,
                first_name=first_name,
                middle_name=middle_name,
                last_name=last_name,
                tel_list=tel_list,
                descr=descr,
                mail_addr=mail_addr,
            )),
        )

    def search_contacts(self, uid):
        return self._get_request(
            method='v1/searchContacts.jsonx',
            args=dict(uid=uid),
        )

    def _post_request(self, method, args):
        return self._request(method=method, args=args, post=True)

    def _get_request(self, method, args):
        return self._request(method=method, args=args, post=False)

    def _request(self, method, args, post):
        with http.request(
            url=http.url_join(
                host=self.location,
                method=method,
                args=args,
            ),
            do_retries=True,
            timeout=10,
            data='' if post else None
        ) as fd:
            return json.load(fd)


def filter_not_none(dict_value):
    return {k: v for k, v in dict_value.iteritems() if v is not None}
