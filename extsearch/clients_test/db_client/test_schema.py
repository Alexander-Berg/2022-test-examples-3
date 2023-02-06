from extsearch.video.ugc.sqs_moderation.clients.db_client.api_schema import (
    ModerationInfoFieldSchema, VideoMetaSchema, VideoResourceDictSchema
)
from extsearch.video.ugc.sqs_moderation.clients.db_client.model import (
    ModerationStatus, ModerationInfoField, VideoResourceDict
)


class TestModerationField(object):
    def test_parse(self):
        data = {
            "status": "success",
            "value": [1, 2, 3],
            "original_value": [1, 2, 3],
            "verdicts": {
                "xxx": "fail"
            },
            "effective_verdicts": {
                "yyyy": "error"
            }
        }
        expected = ModerationInfoField(
            status=ModerationStatus.success,
            value=[1, 2, 3],
            original_value=[1, 2, 3],
            verdicts={
                "xxx": ModerationStatus.fail
            },
            effective_verdicts={
                "yyyy": ModerationStatus.error
            }
        )
        schema = ModerationInfoFieldSchema()
        res, err = schema.load(data)
        assert not err
        assert res == expected
        res, err = schema.dump(res)
        assert not err
        assert res == data

    def test_parse_none(self):
        data = {
            "status": "success",
            "value": [1, 2, 3],
            "original_value": [1, 2, 3],
            "verdicts": None,
            "effective_verdicts": None
        }
        expected = ModerationInfoField(
            status=ModerationStatus.success,
            value=[1, 2, 3],
            original_value=[1, 2, 3],
            verdicts=None,
            effective_verdicts=None,
        )
        schema = ModerationInfoFieldSchema()
        res, err = schema.load(data)
        assert not err
        assert res == expected
        res, err = schema.dump(res)
        assert not err
        assert res == data


class TestVideoMeta(object):
    def test_parse_moderation_info_none(self):
        data = {
            'id': 1,
            'channel_id': 1,
            'video_file_id': None,
            'video_stream_id': None,
            'title': None,
            'thumbnail': None,
            'description': None,
            'moderation_info': None,
            'moderation_status': None,
            'deleted': False,
            'update_time': '2021-10-21T01:35:00+03:00',
            'release_date': '2021-10-21T01:35:00+03:00',
            'privacy': None,
        }
        schema = VideoMetaSchema()
        res, err = schema.load(data)
        assert not err
        assert res.moderation_info == {}


class TestVideoResource(object):
    def test_parse(self):
        data = {
            "resources": {
                "name1": "value1",
                "name2": "value2",
            },
        }
        expected = VideoResourceDict(
            resources={
                "name1": "value1",
                "name2": "value2",
            },
        )
        schema = VideoResourceDictSchema()
        res, err = schema.load(data)
        assert not err
        assert res == expected
        res, err = schema.dump(res)
        assert not err
        assert res == data
