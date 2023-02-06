from req_base import DataFile
import json
import re
from copy import deepcopy
import itertools


class Saas(DataFile):
    def __init__(self):
        super(Saas, self).__init__('')
        self.__data = {}
        self.attr = {
            'Key': 'key',
            'Value': 'value'
        }

        self.doc = {
            'Document': [
                {
                    'ArchiveInfo': {
                        'Title': 'title',
                        'GtaRelatedAttribute': [
                        ]
                    }
                }
            ]
        }
        self.group = {
            'TotalDocCount': [
                1, 1, 1
            ],
            'Grouping': [
                {
                    'Group': [
                    ]
                }
            ]
        }
        self.empty_answer = {
            'TotalDocCount': [
                0, 0, 0
            ]
        }

    def add_attr(self, doc, attr):
        doc['Document'][0]['ArchiveInfo']['GtaRelatedAttribute'].append(attr)

    def __json_answer(self, answer):
        return answer

    def add_json(self, method, params, answer):
        key = self._calc_key(method, params)
        self.__data[key] = answer

    def find_type(self, base_key):
        type_expr = re.search("[.#&]type=([^&#]+)", base_key)  # find all type args
        return type_expr.group(1) if type_expr else ''

    def shadow_key(self, key):
        return key.replace('%', '%25').replace('#', '%23').replace('=', '%3D').replace(',', '%2C')

    def add_base_key(self, base_keys, docs, service='market_cms_kv_strg', search_by_types={}):
        params = [('service', service),
                  ('hr', 'json'), ('sp_meta_search', 'multi_proxy'),
                  ('sgkps', '2'), ('gta', '_Body')]
        data_keys = []

        types_in_keys = [self.find_type(base_key) for base_key in base_keys]
        all_keys = set(types_in_keys)
        if all_keys and all_keys.issubset(search_by_types):
            types = list(all_keys)
            params.append(('key_name', 'type_key'))
            for type in itertools.permutations(types):
                params.append(('text', self.shadow_key(','.join(type))))
                data_keys.append(self._calc_key('', params))
                del params[-1]
        else:
            params.append(('key_name', 'base_key'))
            for keylist in itertools.permutations(base_keys):
                params.append(('text', self.shadow_key(','.join([key for key in keylist]))))
                data_keys.append(self._calc_key('', params))
                del params[-1]

        if not docs:
            group = deepcopy(self.empty_answer.copy())
        else:
            group = deepcopy(self.group.copy())
            for doc in docs:
                doc_dict = deepcopy(self.doc.copy())
                attr = deepcopy(self.attr.copy())
                attr['Key'] = '_Body'
                attr['Value'] = doc[1]
                self.add_attr(doc_dict, attr)
                doc_dict['Document'][0]['ArchiveInfo']['Title'] = doc[0]
                group['Grouping'][0]['Group'].append(doc_dict)
                group['TotalDocCount'][0] += 1

        for key in data_keys:
            self.__data[key] = json.dumps(group)

    def add_final_ans(self, values, is_xml=False, service='market_cms_kv_strg'):
        params = [('service', service), ('hr', 'json'),
                  ('sp_meta_search', 'multi_proxy'), ('sgkps', '1'), ('gta', '_Body')]

        if isinstance(values, dict):
            list_of_keys = values.keys()
            list_of_values = values
        else:
            list_of_keys = str(values).split(',')
            if is_xml:
                list_of_values = {key : '<page id="{}"/>'.format(key) for key in list_of_keys}
            else:
                list_of_values = {key : json.dumps({"page": int(key)}) for key in list_of_keys}

        data_keys = []
        for numlist in itertools.permutations(list_of_keys):
            params.append(('text', self.shadow_key(','.join([key for key in numlist]))))
            data_keys.append(self._calc_key('', params))
            del params[-1]

        group = deepcopy(self.group)
        # we reverse array to test that templator still can restore it by indices order
        for el in list_of_keys:
            doc = deepcopy(self.doc.copy())
            doc['Document'][0]['ArchiveInfo']['Title'] = el
            attr = deepcopy(self.attr.copy())
            attr['Key'] = '_Body'
            attr['Value'] = list_of_values.get(el)
            self.add_attr(doc, attr)
            group['Grouping'][0]['Group'].append(doc)

        for key in data_keys:
            self.__data[key] = json.dumps(group)
        group['TotalDocCount'][0] += 1

    @property
    def data(self):
        return self.__data
