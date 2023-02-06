# encoding: utf-8

import re
from collections import namedtuple


Filter = namedtuple('Filter', ['id', 'value', 'disabled'])
ResponseSpecificParameters = namedtuple('ResponseSpecificParameters', ['ms', 'gta'])


class ServerError(Exception):
    pass


def _normalize(s):
    # Replace all punctuation with spaces, then collapse consecutive spaces
    return re.sub(r'\W+', ' ', s.lower())


def _fuzzy_str_equals(a, b):
    return _normalize(a) == _normalize(b)


def _fuzzy_str_contains(s, t):
    return _normalize(s) in _normalize(t)


class GeoObject(object):
    def __init__(self):
        self.name = None

    def is_company(self):
        raise NotImplementedError('is_company is not implemented')

    def is_toponym(self):
        raise NotImplementedError('is_toponym is not implemented')

    def is_collection(self):
        raise NotImplementedError('is_collection is not implemented')

    def is_goods(self):
        raise NotImplementedError('is_goods is not implemented')

    def has_category(self, category_name):
        raise NotImplementedError('has_category is not implemented')

    def has_feature(self, feature_id):
        raise NotImplementedError('has_feature is not implemented')

    def has_feature_startswith(self, feature_id_prefix):
        raise NotImplementedError('has_feature_startswith is not implemented')

    def has_matched_link(self, regex):
        raise NotImplementedError('has_matched_link is not implemented')

    def works_all_day(self):
        raise NotImplementedError('works_all_day is not implemented')

    def is_temporary_closed(self):
        raise NotImplementedError('is_temporary_closed is not implemented')

    def has_telephone_number(self, telephone_number):
        raise NotImplementedError('has_telephone_number is not implemented')

    def has_url(self, url):
        raise NotImplementedError('has_url is not implemented')

    def formatted_address(self):
        raise NotImplementedError('formatted_address is not implemented')

    def distance(self):
        raise NotImplementedError('distance is not implemented')

    def rating_score(self):
        raise NotImplementedError('rating_score is not implemented')

    def uri(self):
        raise NotImplementedError('uri is not implemented')

    def kind(self):
        raise NotImplementedError('kind is not implemented')

    def is_exact_point(self):
        raise NotImplementedError('is_exact_point is not implemented')

    def point(self):
        raise NotImplementedError('point is not implemented')

    def similar_places_count(self):
        raise NotImplementedError('similar_places_count is not implemented')

    def similar_places_with_photo_count(self):
        raise NotImplementedError('similar_places_with_photo_count is not implemented')

    def get_snippet(self, snippet_name):
        raise NotImplementedError('get_snippet is not implemented')

    def permalinks(self):
        raise NotImplementedError('company_id is not implemented')

    def get_gta(self, key):
        raise NotImplementedError('get_gta is not implemented')

    def is_injected_as_competitor(self):
        raise NotImplementedError('is_injected_as_competitor is not implemented')

    def has_serp_competitors_snippet(self):
        raise NotImplementedError('has_serp_competitors_snippet is not implemented')

    def has_org_competitors_snippet(self):
        raise NotImplementedError('has_org_competitors_snippet is not implemented')

    def is_advert(self):
        raise NotImplementedError('is_advert is not implemented')

    def is_boosted_advert(self):
        raise NotImplementedError('is_boosted_advert is not implemented')

    def get_goods(self):
        raise NotImplementedError('get_goods is not implemented')

    def check_name_equals(self, *expected_names):
        assert any(
            _fuzzy_str_equals(self.name, name) for name in expected_names
        ), 'GeoObject name "{}" is not {}'.format(self.name, ', '.join('"{}"'.format(name) for name in expected_names))

    def check_name_contains(self, substr):
        assert _fuzzy_str_contains(substr, self.name), 'GeoObject name "{}" does not contain "{}"'.format(
            self.name, substr
        )


class SearchResult(object):
    @classmethod
    def get_specific_params(cls):
        raise NotImplementedError('get_specific_params is not implemented')

    def is_non_empty(self):
        raise NotImplementedError('is_non_empty is not implemented')

    def is_business_result(self):
        raise NotImplementedError('is_business_result is not implemented')

    def is_geocoder_result(self):
        raise NotImplementedError('is_geocoder_result is not implemented')

    def has_filters(self):
        raise NotImplementedError('has_filters is not implemented')

    def filter_by_id(self, filter_id):
        raise NotImplementedError('filter_by_id is not implemented')

    def get_enum_filter_ids(self, filter_id):
        raise NotImplementedError('get_enum_filter_ids is not implemented')

    def context(self):
        raise NotImplementedError('context is not implemented')

    def bounded_by(self):
        raise NotImplementedError('bounded_by is not implemented')


class GeoObjectListResult(SearchResult):
    def __init__(self):
        self.geo_objects = []

    @property
    def doc_count(self):
        return len(self.geo_objects)

    @property
    def first_doc(self):
        assert self.doc_count > 0, 'nothing found, the response is empty'
        return self.geo_objects[0]

    def check_contains_item(self, expected_name):
        names = ''.join('\n\t"{}"'.format(it.name) for it in self.geo_objects)
        assert any(
            _fuzzy_str_equals(o.name, expected_name) for o in self.geo_objects
        ), 'GeoObject "{}" was not found in {}'.format(expected_name, names)
