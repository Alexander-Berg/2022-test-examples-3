from dataclasses import fields

from extsearch.video.ugc.sqs_moderation.clients.db_client.resources import CmntData


def test_get_data(cmnt_data_getter, cmnt_data, valid_cmnt_res, no_data_cmnt_res):
    resources = [
        valid_cmnt_res,
        no_data_cmnt_res,
    ]
    res = cmnt_data_getter.get_cmnt_data(resources)
    data_fields = [field.name for field in fields(res.__class__)]
    for key in data_fields:
        assert getattr(res, key) == cmnt_data.get(key), 'Wrong parsing data'


def test_no_data(cmnt_data_getter, no_data_cmnt_res):
    res = cmnt_data_getter.get_cmnt_data([no_data_cmnt_res])
    assert res is None, 'Resources have no data result must be None'


def test_wrong_cmnt_data_data(cmnt_data_getter, wrong_cmnt_res):
    res = cmnt_data_getter.get_cmnt_data([wrong_cmnt_res])
    assert isinstance(res, CmntData)
    assert res.with_cmnt is True
    assert res.cmnt_id is None
