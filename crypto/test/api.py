import json

import flask
from library.python import resource
from library.python.protobuf.json import proto2json

from crypta.lib.python.test_utils import api_mock
from crypta.profile.lib.socdem_helpers import socdem_groups
from crypta.profile.services.upload_direct_exports_tanker_names_to_yt import lib
from crypta.profile.services.upload_direct_exports_tanker_names_to_yt.bin.test.proto import (
    segment_pb2,
    translations_pb2,
)

CHILDREN_INTERESTS_GROUP = 'group-4bd86371'
NON_SOCDEM_GROUP = 'group-2b5ad5c0'
SOCDEM_GROUP = 'group-8fecf38e'


def get_translations(key, en, ru):
    return (key, translations_pb2.Translations(
        tankerKey=key,
        en=translations_pb2.Translation(text=en),
        ru=translations_pb2.Translation(text=ru),
    ))


def load_keys():
    return dict(
        get_translations(tanker_key, en, ru)
        for tanker_key, en, ru in (
            ('crypta_interest_diapers_name', 'Diapers', 'Подгузники'),
            ('crypta_interest_diapers_description', 'Bought diapers', 'Покупали подгузники'),
            ('crypta_restaurants_name', 'Restaurants', 'Рестораны'),
            ('crypta_restaurants_description', 'Visited restaurants', 'Посещали рестораны'),
            ('crypta_things_name', 'Things', 'Вещи'),
            ('crypta_things_description', 'Used thing', 'Использовали вещи'),
        )
    )


def load_segments():
    return [
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=601,
                        segmentId=362,
                        tags=['direct'],
                    ),
                    segment_pb2.Segment.Export(
                        keywordId=602,
                        segmentId=362,
                        tags=['direct'],
                    )
                ]
            ),
            id='segment-diapers',
            tankerDescriptionKey='crypta_interest_diapers_description',
            tankerNameKey='crypta_interest_diapers_name',
            parentId=CHILDREN_INTERESTS_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=1001,
                        tags=['direct'],
                    ),
                ]
            ),
            id='segment-things',
            tankerDescriptionKey='crypta_things_description',
            tankerNameKey='crypta_things_name',
            parentId=NON_SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=1000,
                        tags=['direct'],
                    ),
                ]
            ),
            id='segment-restaurants',
            tankerDescriptionKey='crypta_restaurants_description',
            tankerNameKey='crypta_restaurants_name',
            parentId=NON_SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=1234,
                        tags=['ca_type:good_type'],
                    ),
                ]
            ),
            id='segment-not-in-direct',
            tankerDescriptionKey='crypta_restaurants_description',
            tankerNameKey='crypta_restaurants_name',
            parentId=NON_SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=1765,
                        tags=['direct'],
                    ),
                ]
            ),
            id='segment-with-no-tanker-keys',
            tankerDescriptionKey='',
            tankerNameKey='',
            parentId=NON_SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=1765,
                        tags=['direct'],
                    ),
                ]
            ),
            id='segment-in-socdem-group',
            tankerDescriptionKey='crypta_restaurants_description',
            tankerNameKey='crypta_restaurants_name',
            parentId=SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=2000,
                        tags=['direct', 'ca_type:good_type'],
                    ),
                ]
            ),
            id='segment-good-type',
            tankerDescriptionKey='crypta_things_name',
            tankerNameKey='crypta_things_description',
            parentId=NON_SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=2001,
                        tags=['direct', 'ca_type:fake_type'],
                    ),
                ]
            ),
            id='segment-fake-type',
            tankerDescriptionKey='crypta_things_name',
            tankerNameKey='crypta_things_description',
            parentId=NON_SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=2002,
                        tags=['direct', 'ca_type:good_type'],
                    ),
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=2003,
                        tags=['direct', 'ca_type:good_type2'],
                    ),
                ]
            ),
            id='segment-several-good-types',
            tankerDescriptionKey='crypta_things_name',
            tankerNameKey='crypta_things_description',
            parentId=NON_SOCDEM_GROUP,
        ),
        segment_pb2.Segment(
            exports=segment_pb2.Segment.Exports(
                exports=[
                    segment_pb2.Segment.Export(
                        keywordId=547,
                        segmentId=2004,
                        tags=['direct', 'ca_type:good_type'],
                    ),
                ]
            ),
            id='segment-fake-tanker-keys',
            tankerDescriptionKey='crypta_fake_name',
            tankerNameKey='crypta_fake_description',
            parentId=NON_SOCDEM_GROUP,
        ),
    ]


def load_parents():
    return {
        'segment-diapers': ['root'],
        'segment-restaurants': ['root', lib.ORGVISITS_GROUP],
    }


def load_tree():
    return segment_pb2.SegmentNode(
        id=socdem_groups.ROOT_GROUP,
        children=[segment_pb2.SegmentNode(
            id=socdem_groups.CRYPTA_ROOT_GROUP,
            children=[segment_pb2.SegmentNode(
                id=socdem_groups.SOCDEM_ROOT_GROUP,
                children=[segment_pb2.SegmentNode(
                    id=SOCDEM_GROUP,
                    children=[],
                )],
            )],
        )],
    )


class MockCryptaApi(api_mock.MockCryptaApiBase):
    def __init__(self):
        super(MockCryptaApi, self).__init__(resource.find('/swagger.json'))

        self.not_found_response = {
            "message": "Not found",
            "requestId": "request-00000000-0000-0000-0000-000000000000"
        }

        self.keys = {
            key: json.loads(proto2json.proto2json(translations))
            for key, translations in load_keys().items()
        }
        self.segments = [
            json.loads(proto2json.proto2json(
                segment,
                config=proto2json.Proto2JsonConfig(missing_single_key_mode=proto2json.MissingKeyMode.MissingKeyDefault),
            ))
            for segment in load_segments()
        ]
        self.parents = load_parents()
        self.tree = json.loads(proto2json.proto2json(
            load_tree(),
            config=proto2json.Proto2JsonConfig(missing_repeated_key_mode=proto2json.MissingKeyMode.MissingKeyDefault),
        ))

        @self.app.errorhandler(404)
        def page_not_found(error):
            return flask.jsonify(self.not_found_response), 404

        @self.app.route('/lab/segment')
        def get_all_segments():
            return flask.jsonify(self.segments)

        @self.app.route('/lab/segment/groups_tree')
        def get_segment_groups_tree():
            return flask.jsonify(self.tree)

        @self.app.route('/lab/segment/parents')
        def get_parents_per_segment():
            return flask.jsonify(self.parents)

        @self.app.route('/lab/segment/tanker/<key>')
        def get_translations_by_key(key):
            assert key, '"/lab/segment/tanker" request instead of "/lab/segment/tanker/<key>"'

            translation = self.keys.get(key)
            if translation is not None:
                return flask.jsonify(self.keys[key])
            else:
                flask.abort(404)
