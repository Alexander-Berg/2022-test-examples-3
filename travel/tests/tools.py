import travel.hotels.proto.geocounter_service.geocounter_service_pb2 as geocounter_service_pb2
import travel.hotels.proto.hotel_filters.filters_pb2 as filters_pb2


class Bbox:
    def __init__(self, lower_left_lat, lower_left_lon, upper_right_lat, upper_right_lon):
        self.lower_left_lat = lower_left_lat
        self.lower_left_lon = lower_left_lon
        self.upper_right_lat = upper_right_lat
        self.upper_right_lon = upper_right_lon


class Bboxes:
    WHOLE_WORLD = Bbox(-90, -180, 90, 180)


class Filter:
    def __init__(self, business_id, values):
        self.business_id = business_id
        self.values = values


def prepare_request(bbox, initial_filter_groups=[]):
    return geocounter_service_pb2.TGetCountsRequest(
        LowerLeftLat=bbox.lower_left_lat,
        LowerLeftLon=bbox.lower_left_lon,
        UpperRightLat=bbox.upper_right_lat,
        UpperRightLon=bbox.upper_right_lon,
        CheckInDate="2010-01-01",
        CheckOutDate="2010-01-02",
        Ages="88,88",
        InitialFilterGroups=[
            geocounter_service_pb2.TGetCountsRequest.TInitialFilterGroup(
                UniqueId=group_id,
                Filters=[
                    filters_pb2.THotelFilter(
                        UniqueId=filter_id,
                        FeatureId=filter.business_id,
                        ListValue=filters_pb2.THotelFilter.TListValue(
                            Value=filter.values
                        )
                    ) for filter_id, filter in filter_group
                ]
            ) for group_id, filter_group in initial_filter_groups
        ],
        AdditionalFilters=[
            geocounter_service_pb2.TGetCountsRequest.TAdditionalFilter(
                Type='Or',
                GroupId='star',
                Filter=filters_pb2.THotelFilter(
                    UniqueId=unique_id,
                    FeatureId=business_id,
                    ListValue=filters_pb2.THotelFilter.TListValue(
                        Value=values
                    )
                )
            ) for unique_id, business_id, values in [
                ('stars-1', 'star', ['one']),
                ('stars-2', 'star', ['two']),
                ('stars-3', 'star', ['three']),
                ('stars-4', 'star', ['four']),
                ('stars-5', 'star', ['five'])
            ]
        ])
