from multidict import MultiDict


class FuritaApi(object):
    def __init__(self, furita_component):
        self.furita = furita_component

    def ping(self):
        return self.furita.get('/ping')

    def api_list(self, uid, id=None, detailed=False, type=None, headers=None):
        params = {
            k: v
            for k, v in dict(
                db='pg',
                uid=uid,
                id=id,
                detailed=('1' if detailed else None),
                type=type,
            ).items() if v is not None
        }
        return self.furita.get('/api/list.json', params=params, headers=headers)

    def api_multilist(self, uids, master=False):
        params = dict(
            uids=','.join(uids),
            m_master=bool(master),
        )
        return self.furita.get('/api/multi_list', params=params)

    def api_edit(self, uid, name, params={}):
        default_params = dict(
            db='pg',
            letter='nospam',
            field1='from',
            field2='3',
            field3='test@test.ru',
            attachment='',
            logic='0',
            clicker='delete',
            move_folder='1',
            move_label='1',
            forward_address='test@test.ru',
            autoanswer='',
            notify_address='test@test.ru',
            order='0',
            stop='0',
            noconfirm='1',
            auth_domain='',
            confirm_domain='',
            lang='ru',
            **{'from': ''}
        )

        return self.furita.get('/api/edit.json', params={**default_params, **params, **dict(uid=uid, name=name)})

    def api_preview(self, uid, id=None, params={}):
        field_names = {'field1', 'field2', 'field3'}
        params = list({
            **dict(db='pg'),
            **{p: v for p, v in dict(params).items() if p not in field_names},
            **dict(uid=uid, id=id)
        }.items()) + [(p, v) for p, v in MultiDict(params).items() if p in field_names]

        return self.furita.get('/api/preview.json', params=params)

    def api_enable(self, uid, id, enabled):
        params = dict(
            db='pg',
            uid=uid,
            id=id,
            enabled={True: '1', False: '0'}.get(enabled)
        )
        return self.furita.get('/api/enable.json', params=params)

    def api_remove(self, uid, ids):
        params = dict(
            db='pg',
            uid=uid,
            id=ids
        )
        return self.furita.get('/api/remove.json', params=params)

    def api_order(self, uid, ids):
        params = dict(
            db='pg',
            uid=uid,
            list=(','.join(ids) if ids else None)
        )
        return self.furita.get('/api/order.json', params=params)

    def api_bwlist(self, uid, list_type="blacklist"):
        assert list_type in ['blacklist', 'whitelist']
        params = dict(
            db='pg',
            uid=uid
        )
        return self.furita.get('/api/{list_type}.json'.format(list_type=list_type), params=params)

    def api_bwlist_add(self, uid, list_type="blacklist", email=None):
        assert list_type in ['blacklist', 'whitelist']
        params = dict(
            db='pg',
            uid=uid,
            email=email
        )
        return self.furita.get('/api/{list_type}_add.json'.format(list_type=list_type), params=params)

    def api_bwlist_remove(self, uid, list_type="blacklist", emails=[]):
        assert list_type in ['blacklist', 'whitelist']
        params = dict(
            db='pg',
            uid=uid,
            email=emails
        )
        return self.furita.get('/api/{list_type}_remove.json'.format(list_type=list_type), params=params)

    def api_apply(self, uid, id, headers=None):
        params = dict(
            db='pg',
            uid=uid,
            id=id
        )
        return self.furita.get('/api/apply.json', params=params, headers=headers)

    def api_verify(self, code):
        return self.furita.get('/api/verify.json', params=dict(e=code))

    def api_domain_rules_set(self, orgid, data):
        params = dict(
            orgid=orgid
        )
        return self.furita.post('/v1/domain/rules/set', params=params, json=data)

    def api_domain_rules_get(self, orgid):
        params = dict(
            orgid=orgid
        )
        return self.furita.get('/v1/domain/rules/get', params=params)
