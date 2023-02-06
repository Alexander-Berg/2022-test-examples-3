import mock

from crypta.lib.python.swagger import swagger
from crypta.profile.runners.export_profiles.lib.export import upload_audience_segments_to_api


def test_get_crypta_id_segments(local_yt, patch_config, date, mock_crypta_api):
    task = upload_audience_segments_to_api.PrepareAudienceSegments(date=date, data_source='yandexuid')

    with mock.patch("crypta.profile.runners.export_profiles.lib.export.upload_audience_segments_to_api.get_api",
                    return_value=swagger(mock_crypta_api.url_prefix + "/swagger.json", "token")):
        return task.get_crypta_id_segments()
