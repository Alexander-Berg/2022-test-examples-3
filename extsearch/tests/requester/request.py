import re


class Locale(object):
    ru_RU = 'ru_RU'
    en_US = 'en_US'


class Span(object):
    Moscow = {'ll': '37.623634,55.760407', 'spn': '0.5,0.3'}
    Bashkortostan = {'ll': '56,54.75', 'spn': '0.05,0.05'}
    SaintPetersburg = {'ll': '30.247684,59.943244', 'spn': '0.05,0.05'}
    SeaOfMarmara = {'ll': '29.003679,40.978020', 'spn': '0.05,0.05'}
    Nemiga = {'ll': '27.550419,53.902904', 'spn': '0.05,0.05'}
    Minsk = {'ll': '27.727360,53.882845', 'spn': '0.70,0.26'}
    Taganskaya = {'ll': '37.655030,55.740044', 'spn': '0.007037,0.002733'}
    Voronezh = {'ll': '39.200,51.661', 'spn': '0.646,0.418'}


REGION_MAP = {
    'Москва': Span.Moscow,
    'Башкортостан': Span.Bashkortostan,
    'Санкт-Петербург': Span.SaintPetersburg,
    'Мраморное море': Span.SeaOfMarmara,
    'Немига': Span.Nemiga,
    'Минск': Span.Minsk,
    'Таганская': Span.Taganskaya,
    'Воронеж': Span.Voronezh,
}


class Request(object):
    def __init__(self):
        self.params = {}

    def set_locale(self, lang):
        self.params['lang'] = lang
        return self

    def set_results(self, n):
        self.params['results'] = n
        return self

    def set_type(self, types):
        self.params['type'] = types
        return self

    # what to find

    def set_text(self, text):
        self.params['text'] = text
        return self

    def set_url(self, url):
        self.params['text'] = url
        self.params['mode'] = 'url'
        return self

    def set_reverse_mode_request(self, ll):
        self.params['ll'] = ll
        self.params['mode'] = 'reverse'
        return self

    def set_telephone_number(self, telephone_number):
        rgx = re.compile(r'[\+7\(\)\-\s]')
        self.params['search_phone'] = rgx.sub('', telephone_number)
        return self

    # snippets

    def ask_snippet(self, *argv):
        self.params.setdefault('add_snippet', []).extend(argv)
        self.params.setdefault('gta', []).extend(argv)
        return self

    def set_middle_snippets_oid(self, oid):
        self.params['middle_snippets_oid'] = oid
        return self

    # location

    def set_span(self, ll_spn):
        self.params.update(ll_spn)
        return self

    def set_span_by_name(self, name):
        self.params.update(REGION_MAP[name])
        return self

    def set_ll(self, ll):
        self.params['ll'] = ll
        return self

    def set_ull(self, ull):
        self.params['ull'] = ull
        return self

    def set_ull_to_ll(self):
        self.params['ull'] = self.params['ll']
        return self

    def set_region(self, region_id):
        self.params['lr'] = region_id
        return self

    # advert

    def set_advert_page(self, page_id):
        self.params['advert_page_id'] = page_id
        return self

    def set_maxadv(self, limit):
        self.params['maxadv'] = limit
        return self

    # oids and uris

    def set_business_oid(self, *business_oid):
        self.params['business_oid'] = list(business_oid)
        return self

    def set_uri(self, *uri):
        self.params['uri'] = list(uri)
        return self

    # sort

    def set_sort(self, sort):
        self.params['sort'] = sort
        return self

    def set_sort_origin(self, ll):
        self.params['sort_origin'] = ll
        return self

    # context (sequence of requests)

    def set_context(self, context):
        self.params['context'] = context
        return self

    def clear_context(self):
        self.params.pop('context', None)
        return self

    # general (try to avoid if possible)

    def ask_gta(self, *argv):
        self.params.setdefault('gta', []).extend(argv)
        return self

    def add_rearr(self, rearr):
        self.params.setdefault('rearr', []).append(rearr)
        return self

    def add_param(self, key, value):
        self.params.setdefault(key, []).append(value)
        return self

    # etc.

    def add_business_filter(self, f):
        filters = self.params.get('business_filter')
        if filters is not None:
            filters += '~' + f
        else:
            filters = f
        self.params['business_filter'] = filters
        return self

    def enable_layer_detection(self):
        self.params['distinguish_layers'] = 1
        return self

    def set_hardcoded_passport_uid(self):
        self.params['passport_uid'] = '162072925'
        return self

    def set_maps_platform(self, platform):
        self.params['maps_platform'] = platform
        return self
