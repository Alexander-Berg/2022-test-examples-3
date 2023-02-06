from .api_schema import video_info_schema
from .model import VideoInfo, ModerationInfoField, ModerationStatus


def test_serialize():
    data = VideoInfo(
        id=1,
        service="qwe",
        service_id="1",
        moderation_status=ModerationStatus.success,
        moderation_info={
            "topic": ModerationInfoField(
                value="Кошки",
                source="final",
                status=ModerationStatus.success,
            ),
        },
        transcoder_status="",
        transcoder_info=None,
        deleted=False,
        create_time="today"
    )
    json = video_info_schema.dump(data)
    print(json)
    assert not json[1]
    res = video_info_schema.load(json[0])
    assert not res[1]
    assert res[0] == data


test_serialize()
