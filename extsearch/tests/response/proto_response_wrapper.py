# encoding: utf-8

from .base_response_wrapper import GeoObject, GeoObjectListResult, Filter, ResponseSpecificParameters, ServerError
from .geo_utils import point_from_string
from . import url_utils

import base64
import json
import itertools
import search.idl.meta_pb2 as pb_meta
import re
import xml.etree.ElementTree as ET

from extsearch.geo.kernel.pbreport.intermediate.proto import metadatacollection_pb2
from yandex.maps.proto.common2 import metadata_pb2
from yandex.maps.proto.search import business_pb2, geocoder_pb2, kind_pb2, precision_pb2
from yandex.maps.proto.uri import uri_pb2


def get_texts(et_nodes):
    return map(lambda el: el.text, et_nodes)


def get_attrs(et_nodes, attr_name):
    return map(lambda el: el.get(attr_name), et_nodes)


def proto_pairs_to_dict(pairs):
    d = {}
    for p in pairs:
        d.setdefault(p.Key.decode('utf-8'), []).append(p.Value.decode('utf-8'))
    return d


class ProtoGeoObject(GeoObject):
    def _parse_experimental(self, xml):
        if len(xml) == 0:
            return
        for storage in xml[0]:
            for p in storage.findall('Item'):
                key = p.find('key').text
                value = p.find('value').text
                try:
                    js = json.loads(value)
                    self.json_snippets[key] = js
                except:
                    self.other_snippets[key] = value

    def _parse_snippets(self, passages):
        self.xml_snippets = {}
        self.json_snippets = {}
        self.other_snippets = {}
        for p in itertools.chain(map(lambda s: s.decode('utf-8'), passages), self.gta.get('snippets', [])):
            try:
                xml = ET.fromstring(re.sub(' xmlns="[^"]+"', '', p))
                self.xml_snippets.setdefault(xml.tag, []).append(xml)
            except:
                try:
                    js = json.loads(p)
                    self.json_snippets[js.keys()[0]] = js
                except:
                    raise Exception('Failed to parse passage [%s]' % p)

    def _parse_binary_metadata(self, data):
        self.binary_data = {}

        geosearch_data = metadatacollection_pb2.TMetadataCollection()
        geosearch_data.ParseFromString(data)
        for item in geosearch_data.Data:
            metadata = metadata_pb2.Metadata()
            metadata.ParseFromString(item)
            if metadata.HasExtension(business_pb2.GEO_OBJECT_METADATA):
                self.binary_data['business'] = metadata.Extensions[business_pb2.GEO_OBJECT_METADATA]
            if metadata.HasExtension(geocoder_pb2.GEO_OBJECT_METADATA):
                self.binary_data['geocoder'] = metadata.Extensions[geocoder_pb2.GEO_OBJECT_METADATA]
            if metadata.HasExtension(uri_pb2.GEO_OBJECT_METADATA):
                self.binary_data['uri'] = metadata.Extensions[uri_pb2.GEO_OBJECT_METADATA]

    def _get_xml_snippet(self, snip_name):
        return self.xml_snippets[snip_name][0]

    def _get_company_meta_data(self):
        return self.binary_data['business']

    def _get_geocoder_meta_data(self):
        return self.binary_data['geocoder']

    def __init__(self, group):
        self.group = group
        self.doc = group.Document[0]
        self.arch_info = self.doc.ArchiveInfo
        self.name = self.arch_info.Title.decode('utf-8')
        self.description = self.arch_info.Headline.decode('utf-8')
        self.gta = proto_pairs_to_dict(self.arch_info.GtaRelatedAttribute)
        self._parse_snippets(self.arch_info.Passage)
        self._parse_experimental(self.xml_snippets.get('ExperimentalMetaData', []))
        self._parse_binary_metadata(self.doc.BinaryData.GeosearchDocMetadata)

    def is_company(self):
        return 'business' in self.binary_data

    def is_toponym(self):
        return 'geocoder' in self.binary_data

    def has_category(self, category_name):
        return any(c.name == category_name for c in self._get_company_meta_data().category)

    def has_feature(self, feature_id):
        return any(f.id == feature_id for f in self._get_company_meta_data().feature)

    def has_feature_startswith(self, feature_id_prefix):
        return any(feature.id.startswith(feature_id_prefix) for feature in self._get_company_meta_data().feature)

    def has_matched_link(self, regex):
        rgx = re.compile(r'%s' % regex)
        return any(rgx.match(link.link.href) for link in self._get_company_meta_data().link)

    def works_all_day(self):
        return self._get_company_meta_data().open_hours.hours[0].time_range[0].all_day

    def is_temporary_closed(self):
        company = self._get_company_meta_data()
        return company.HasField('closed') and company.closed == business_pb2.Closed.TEMPORARY

    def has_telephone_number(self, telephone_number):
        return any(number.formatted == telephone_number for number in self._get_company_meta_data().phone)

    def has_url(self, url):
        normalized_url = url_utils.normalize_url(url)
        return any(
            url_utils.normalize_url(u.link.href) == normalized_url
            for u in self._get_company_meta_data().link
            if u.type == u.SELF
        )

    def formatted_address(self):
        try:
            return self._get_geocoder_meta_data().address.formatted_address
        except:
            return self._get_company_meta_data().address.formatted_address

    def kind(self):
        last_component = self._get_geocoder_meta_data().address.component[-1]
        kind = last_component.kind[-1]
        return kind_pb2.Kind.Name(kind).lower()

    def distance(self):
        if self._get_company_meta_data().distance:
            return self._get_company_meta_data().distance.value
        return None

    def rating_score(self):
        el = self._get_xml_snippet('BusinessRating').find('score')
        if el is not None:
            return int(float(el.text))
        return None

    def uri(self):
        try:
            return self.binary_data['uri'].uri[0].uri
        except:
            return self._get_xml_snippet('URIMetaData').find('URI/uri').text

    def is_exact_point(self):
        return self._get_company_meta_data().geocode_result.house_precision == precision_pb2.EXACT

    def point(self):
        return point_from_string(self.gta['ll'][0])

    def _get_related_places_count(self, name):
        return len(list(self._get_xml_snippet('RelatedPlaces').find(name)))

    def _get_related_places_with_photo_count(self, name):
        return len(list(self._get_xml_snippet('RelatedPlaces').findall('%s/Company/photoUrlTemplate' % name)))

    def similar_places_count(self):
        return self._get_related_places_count('SimilarPlaces')

    def similar_places_with_photo_count(self):
        return self._get_related_places_with_photo_count('SimilarPlaces')

    def get_snippet(self, snippet_name):
        return (
            self.gta.get(snippet_name)
            or self.xml_snippets.get(snippet_name)
            or self.json_snippets.get(snippet_name)
            or self.other_snippets.get(snippet_name)
        )

    def get_gta(self, gta_name):
        return self.gta[gta_name][0]

    def permalinks(self):
        return self.gta['cluster_permalinks'][0].split(',')

    def is_injected_as_competitor(self):
        return self.get_snippet('advert:experimental_type') == 'inject_by_rubric_at_bottom'

    def is_advert(self):
        return self.gta['advert'][0] == '1'

    def is_boosted_advert(self):
        values = self.gta.get('boosted_advert', [])
        return values and values[0] == '1'


def _check_ms_proto_error(report):
    yxNULL_RESULT = 15
    if report.ErrorInfo.GotError == pb_meta.TErrorInfo.YES and report.ErrorInfo.Code != yxNULL_RESULT:
        raise ServerError(
            'NMetaProtocol::TReport contains error {}: {}'.format(report.ErrorInfo.Code, report.ErrorInfo.Text)
        )


class ProtoSearchResult(GeoObjectListResult):
    @classmethod
    def get_specific_params(cls):
        return ResponseSpecificParameters(ms='proto', gta=['snippets', 'll', 'spn', 'cluster_permalinks'])

    def __init__(self, serialized_message):
        self.message = pb_meta.TReport()
        self.message.ParseFromString(serialized_message)
        err_list = []
        assert self.message.IsInitialized(err_list), 'Message is not initialized\n%s' % err_list
        _check_ms_proto_error(self.message)

        self.geo_objects = (
            [ProtoGeoObject(group) for group in self.message.Grouping[0].Group] if len(self.message.Grouping) else []
        )
        self.searcher_props = proto_pairs_to_dict(self.message.SearcherProp)

    def _get_searcher_prop(self, prop, default=None):
        return self.searcher_props.get(prop, [default])[0]

    def _get_response_metadata(self):
        resp_metadata = self._get_searcher_prop('metadata')
        if resp_metadata:
            try:
                return ET.fromstring(re.sub(' xmlns="[^"]+"', '', resp_metadata))
            except:
                pass
        return None

    def _get_binary_response_metadata(self):
        serialized = self._get_searcher_prop('binary_response_metadata')
        if serialized:
            metadata = metadata_pb2.Metadata()
            metadata.ParseFromString(base64.b64decode(serialized))
            return metadata
        return None

    def _get_filters(self):
        metadata = self._get_response_metadata()
        if metadata:
            return metadata.find('BusinessSearchResponse/Filters')
        return None

    def is_non_empty(self):
        return len(self.geo_objects) > 0 and self.message.Grouping[0].NumDocs[0] > 0 and self.bounded_by()

    def is_business_result(self):
        metadata = self._get_response_metadata()
        if metadata:
            return metadata.find('BusinessSearchResponse') is not None
        return False

    def is_geocoder_result(self):
        metadata = self._get_binary_response_metadata()
        if metadata and metadata.HasExtension(geocoder_pb2.RESPONSE_METADATA):
            return True
        return False

    def has_filters(self):
        return self._get_filters() is not None

    def filter_by_id(self, filter_id):
        filter_query = '*[@id="{}"]'.format(filter_id)
        selected_query = '*[@id="{}"]/value/[.="1"]'.format(filter_id)

        if filter_id.find(':') != -1:
            filter_id, value_id = filter_id.split(':', 1)
            filter_query = '*[@id="{}"]/enum[@id="{}"]'.format(filter_id, value_id)
            selected_query = '*[@id="{}"]/value/[.="{}"]'.format(filter_id, value_id)

        filters_el = self._get_filters()
        filter_el = filters_el.find(filter_query)
        if filter_el is not None:
            return Filter(filter_id, filters_el.find(selected_query) is not None, filter_el.get('disabled', False))
        return None

    def context(self):
        return self._get_searcher_prop('drag_context')

    def bounded_by(self):
        lower_left = point_from_string(self._get_searcher_prop('response_lower_corner', ''), ' ')
        upper_right = point_from_string(self._get_searcher_prop('response_upper_corner', ''), ' ')
        if lower_left and upper_right:
            return {
                'll': self._get_searcher_prop('response_ll', ''),
                'spn': ','.join(map(str, [upper_right.lon - lower_left.lon, upper_right.lat - lower_left.lat])),
            }
        return None
