import vh

from extsearch.audio.tools.vh_tools import operations as op
from extsearch.audio.tools.vh_tools import common
import config


def test_mr_write_json():

    with vh.Graph() as g:
        vh_common = common.Common(yt_token=config.yt_token, yql_token=config.yql_token, mr_account='tmp', cluster='hahn')
        new_table = vh_common.mr_write_json(
            op.json_output(input='{"Json": "Output"}'),
            dst=config.temp_table
        )

        result = op.mr_read_tsv(table=new_table, columns=["Json"], yt_token=config.yt_token)['tsv']

    keeper = vh.run_async(
        g,
        workflow_guid=config.workflow_guid,
        quota=config.quota
    )
    keeper.get_future(result).wait()
    data = keeper.download(result)
    with open(data) as f:
        assert f.read() == "Output\n"
