import extsearch.ymusic.quality.nirvana_ops.music_admin as music_admin_nirvana_ops
import extsearch.ymusic.quality.nirvana_ops.music_admin.util as utils
import extsearch.ymusic.pylibs.music_api.admin as music_admin_api

import vh
import json
import unittest.mock as mock
import yatest.common as yc


class MusicAdminApiStub(music_admin_api.MusicAdminClient):

    def __init__(self, token_file):
        self.experiment_data = {
            "values": [
                {
                    "serializedValue": "{\"title\":\"control\",\"rankingInfo\":{"
                                       "\"formula\":\"default\",\"prunCount\":600,\""
                                       "fastFormula\":\"tc-7d-parody-v2-small\",\"fastCount\":50,"
                                       "\"useAnnIndex\":true,\"useLingboost\":true}}"
                },
                {
                    "serializedValue": "{\"title\":\"default\",\"rankingInfo\":{"
                                       "\"formula\":\"default\",\"prunCount\":600,"
                                       "\"fastFormula\":\"tc-7d-parody-v2-small\",\"fastCount\":50,"
                                       "\"useAnnIndex\":true,\"useLingboost\":true}}"
                },
                {
                    "serializedValue": "{\"title\":\"ymusic-stove-lb-736536\",\"rankingInfo\":{"
                                       "\"formula\":\"ymusic-stove-lb-736536\",\"prunCount\":600,"
                                       "\"fastFormula\":\"tc-7d-parody-v2-small\",\"fastCount\":50,"
                                       "\"useAnnIndex\":true,\"useLingboost\":true}}"
                },
            ],
        }
        super().__init__(token_file)

    def get_experiment(self, experiment_name):
        assert experiment_name == 'musicSearchRanking'
        return self.experiment_data

    def update_experiment(self, experiment_name, experiment_data):
        assert experiment_name == 'musicSearchRanking'
        self.experiment_data = experiment_data
        return experiment_data


def test__add_experiment_values():
    exp_data = run_add_experiment_values_graph_locally()
    assert_new_values_added(exp_data)


def run_add_experiment_values_graph_locally():
    music_admin_stub = MusicAdminApiStub(
        yc.source_path('extsearch/ymusic/quality/nirvana_ops/tests/data/fake_api_token.txt')
    )
    music_admin_api.create_client = mock.Mock(return_value=music_admin_stub)
    with vh.Graph() as g_test_ceg, vh.cwd('add_experiment_values'):
        exp_result = music_admin_nirvana_ops.add_experiment_values(
            'test_secret_oauth_token',
            'qa',
            yc.source_path('extsearch/ymusic/quality/nirvana_ops/tests/data/formulas.json'),
        )
    keeper = vh.run_async(
        graph=g_test_ceg,
        backend=vh.LocalBackend(),
        secrets={'test_secret_oauth_token': '123'},
    )
    exp_data = keeper.download(exp_result)
    with open(exp_data) as f:
        return json.load(f)


def assert_new_values_added(exp_data):
    assert len(exp_data['values']) == 5
    exp_values = utils.parse_experiment_values(exp_data)
    assert 'test-formula-1' in exp_values
    assert 'test-formula-2' in exp_values
    assert exp_values['test-formula-1']['formula'] == 'test-formula-1'
    assert exp_values['test-formula-2']['formula'] == 'test-formula-2'
