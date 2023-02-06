# encoding: utf-8

from .geo_utils import Point
from .base_response_wrapper import GeoObject, GeoObjectListResult, Filter, ResponseSpecificParameters
from .pb_utils import extension, metadata_extension, source_extension, has_source_extension, has_metadata_extension
from .pb_utils import PbExtensionError
from . import url_utils
from . import metactx

import yandex.maps.proto.common2.response_pb2 as pb_response
import yandex.maps.proto.search.business_pb2 as pb_business
import yandex.maps.proto.search.business_rating_pb2 as pb_rating
import yandex.maps.proto.search.collection_pb2 as pb_collection
import yandex.maps.proto.search.geocoder_pb2 as pb_geocoder
import yandex.maps.proto.search.experimental_pb2 as pb_experimental
import yandex.maps.proto.search.goods_metadata_pb2 as pb_goods
import yandex.maps.proto.search.kind_pb2 as pb_kind
import yandex.maps.proto.search.precision_pb2 as pb_precision
import yandex.maps.proto.search.search_internal_pb2 as pb_search_internal
import yandex.maps.proto.search.search_pb2 as pb_search
import yandex.maps.proto.search.related_places_1x_pb2 as pb_related_places
import yandex.maps.proto.search.related_adverts_1x_pb2 as pb_related_adverts
import yandex.maps.proto.uri.uri_pb2 as pb_uri

import re
import logging


class PbGeoObject(GeoObject):
    def __init__(self, message):
        self.message = message
        self.name = self.message.name
        self.description = self.message.description
        self.experimental_items = _experimental_metadata_to_dict(self.message, pb_experimental.GEO_OBJECT_METADATA)

    def has_metadata(self, handle):
        return has_metadata_extension(self.message, handle)

    def is_company(self):
        return self.has_metadata(pb_business.GEO_OBJECT_METADATA)

    def is_toponym(self):
        return self.has_metadata(pb_geocoder.GEO_OBJECT_METADATA)

    def is_collection(self):
        return self.has_metadata(pb_collection.COLLECTION_METADATA)

    def is_goods(self):
        return self.has_metadata(pb_goods.GOODS_METADATA)

    def get_goods(self):
        return metadata_extension(self.message, pb_goods.GOODS_METADATA)

    def get_gta(self, key):
        return self.experimental_items.get(key)

    def has_category(self, category):
        return any(c.name == category for c in self._biz_metadata().category)

    def has_feature(self, feature_id):
        return any(f.id == feature_id for f in self._biz_metadata().feature)

    def has_feature_startswith(self, feature_regex):
        return any(f.id.startswith(feature_regex) for f in self._biz_metadata().feature)

    def has_matched_link(self, regex):
        rgx = re.compile(r'%s' % regex)
        return any(rgx.match(link.link.href) for link in self._biz_metadata().link)

    def works_all_day(self):
        return self._biz_metadata().open_hours.hours[0].time_range[0].all_day

    def is_temporary_closed(self):
        return self._biz_metadata().closed == pb_business.Closed.TEMPORARY

    def has_telephone_number(self, telephone_number):
        return any(number.formatted == telephone_number for number in self._biz_metadata().phone)

    def has_url(self, url):
        normalized_url = url_utils.normalize_url(url)
        return any(
            url_utils.normalize_url(u.link.href) == normalized_url
            for u in self._biz_metadata().link
            if u.type == u.SELF
        )

    def formatted_address(self):
        try:
            return self._geo_metadata().address.formatted_address
        except:
            return self._biz_metadata().address.formatted_address

    def distance(self):
        if self._biz_metadata().distance:
            return self._biz_metadata().distance.value
        return None

    def rating_score(self):
        try:
            if self._rating_metadata().score:
                return int(self._rating_metadata().score)
        except Exception:
            pass
        return 0

    def uri(self):
        return metadata_extension(self.message, pb_uri.GEO_OBJECT_METADATA).uri[0].uri

    def kind(self):
        logging.info('pb_kind.__dict__ = %s' % pb_kind.__dict__)
        return pb_kind.Kind.Name(self._geo_metadata().address.component[-1].kind[-1]).lower()

    def is_exact_point(self):
        return self._biz_metadata().geocode_result.house_precision == pb_precision.EXACT

    def point(self):
        pb_point = self.message.geometry[0].point
        return Point(pb_point.lon, pb_point.lat)

    def similar_places_count(self):
        return len(self._related_places_metadata().similar_places)

    def similar_places_with_photo_count(self):
        return sum(bool(p.photo_url_template) for p in self._related_places_metadata().similar_places)

    def has_advert(self):
        return self._biz_metadata().HasField('advert')

    def permalinks(self):
        return self.experimental_items.get('cluster_permalinks', '').split(',')

    def is_injected_as_competitor(self):
        return self.experimental_items.get('advert:experimental_type') == 'inject_by_rubric_at_bottom'

    def has_serp_competitors_snippet(self):
        has_metadata = has_metadata_extension(self.message, pb_related_adverts.GEO_OBJECT_RELATED_ADVERTS_SNIPPET)
        return has_metadata and len(self._related_adverts_metadata().nearby_on_map) > 0

    def has_org_competitors_snippet(self):
        return len(self._related_adverts_metadata().nearby_on_card) > 0

    def is_advert(self):
        return self.experimental_items.get('advert') == '1'

    def is_boosted_advert(self):
        return self.experimental_items.get('boosted_advert') == '1'

    def _rating_metadata(self):
        return metadata_extension(self.message, pb_rating.GEO_OBJECT_METADATA)

    def _biz_metadata(self):
        '''
        Returns yandex.maps.proto.search.search.business.GeoObjectMetadata
        '''
        return metadata_extension(self.message, pb_business.GEO_OBJECT_METADATA)

    def _geo_metadata(self):
        '''
        Returns yandex.maps.proto.search.search.geocoder.GeoObjectMetadata
        '''
        return metadata_extension(self.message, pb_geocoder.GEO_OBJECT_METADATA)

    def _related_places_metadata(self):
        return metadata_extension(self.message, pb_related_places.GEO_OBJECT_METADATA)

    def _related_adverts_metadata(self):
        return metadata_extension(self.message, pb_related_adverts.GEO_OBJECT_RELATED_ADVERTS_SNIPPET)


class PbSearchResult(GeoObjectListResult):
    @classmethod
    def get_specific_params(cls):
        return ResponseSpecificParameters(ms='pb', gta=['cluster_permalinks'])

    def __init__(self, serialized_message):
        self.message = pb_response.Response()
        self.message.ParseFromString(serialized_message)
        err_list = []
        assert self.message.IsInitialized(err_list), 'Message is not initialized\n%s' % err_list
        self.geo_objects = [PbGeoObject(obj) for obj in self.message.reply.geo_object]
        self.experimental_items = _experimental_metadata_to_dict(self.message.reply, pb_experimental.RESPONSE_METADATA)

    def is_non_empty(self):
        return (
            len(self.geo_objects) > 0
            and self._response_metadata().found > 0
            and self._response_metadata().HasField('bounded_by')
        )

    def is_business_result(self):
        return has_source_extension(self._response_metadata(), pb_business.RESPONSE_METADATA)

    def is_geocoder_result(self):
        return has_source_extension(self._response_metadata(), pb_geocoder.RESPONSE_METADATA)

    def has_filters(self):
        return len(self._biz_metadata().filter) > 0

    def filter_by_id(self, filter_id):
        value_id = None
        if filter_id.find(':') != -1:
            filter_id, value_id = filter_id.split(':', 1)
        for filter_ in self._biz_metadata().filter:
            if filter_.id != filter_id:
                continue
            if value_id is None:
                return Filter(filter_.id, filter_.boolean_filter.value[0].selected, filter_.disabled)
            for enum_value in filter_.enum_filter.value:
                if enum_value.value.id == value_id:
                    return Filter(filter_.id, enum_value.selected, filter_.disabled)
        return None

    def get_enum_filter_ids(self, filter_id):
        enum_filter = next(it for it in self._biz_metadata().filter if it.id == filter_id)
        return {it.value.id for it in enum_filter.enum_filter.value}

    def context(self):
        '''
        Returns base64-encoded string
        '''
        return extension(self._response_metadata(), pb_search_internal.SEARCH_RESPONSE_INFO).context

    def commands(self):
        return extension(self._response_metadata(), pb_search_internal.SEARCH_RESPONSE_INFO).commands

    def bounded_by(self):
        bbox = self._response_metadata().bounded_by
        return {
            'll': '{0:f},{1:f}'.format(
                (bbox.lower_corner.lon + bbox.upper_corner.lon) / 2, (bbox.lower_corner.lat + bbox.upper_corner.lat) / 2
            ),
            'spn': '{0:f},{1:f}'.format(
                (bbox.upper_corner.lon - bbox.lower_corner.lon), (bbox.upper_corner.lat - bbox.lower_corner.lat)
            ),
        }

    def _response_metadata(self):
        '''
        Returns yandex.maps.proto.search.search.SearchResponseMetadata message
        '''
        return metadata_extension(self.message.reply, pb_search.SEARCH_RESPONSE_METADATA)

    def _biz_metadata(self):
        return source_extension(self._response_metadata(), pb_business.RESPONSE_METADATA)

    def experimental_metadata(self):
        if has_metadata_extension(self.message.reply, pb_experimental.RESPONSE_METADATA):
            return metadata_extension(self.message.reply, pb_experimental.RESPONSE_METADATA)
        return None

    def unpack_context(self):
        '''
        Returns TGeoReaskContextProto message
        '''
        return metactx.decode(self.context())


def _experimental_metadata_to_dict(geo_object, handle):
    try:
        exp_metadata = metadata_extension(geo_object, handle)
    except PbExtensionError:  # no such metadata
        return {}

    res = {}
    for item in exp_metadata.experimental_storage.item:
        res[item.key] = item.value
    return res
