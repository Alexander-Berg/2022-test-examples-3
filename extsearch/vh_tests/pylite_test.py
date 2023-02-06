import vh

from extsearch.audio.tools.vh_tools import pylite_operations as pylite
from extsearch.audio.tools.vh_tools import operations as op
from extsearch.audio.tools.vh_tools import common
import config


def test_tranformer():

    @pylite.transformer(
        a=pylite.PytInput('text'),
        b=pylite.PytInput('text'),
        out=pylite.Output1('text'),
    )
    def test_function(a, b):
        for i in a:
            yield i
        for i in b:
            yield i

    with vh.Graph() as g:
        result = test_function(
            op.text_output(input='Hello'),
            op.text_output(input='World')
        )['output_text']

    keeper = vh.run_async(
        g,
        workflow_guid=config.workflow_guid,
        quota=config.quota
    )
    keeper.get_future(result).wait()
    data = keeper.download(result)
    with open(data) as f:
        assert f.read() == "Hello\nWorld\n"


def test_mr_map():
    @pylite.mr_mapper(yt_token=config.yt_token, rec=pylite.TableRow())
    def test_mapper(rec):
        rec['column'] = 'value'
        yield rec

    with vh.Graph() as g:

        vh_common = common.Common(yt_token=config.yt_token, yql_token=config.yql_token, mr_account='tmp', cluster='hahn')

        table = vh_common.mr_write_json(
            op.json_output(input='{"column": "old_value"}'),
            dst=config.temp_table
        )
        result_table = test_mapper(table)
        result = vh_common.mr_read_tsv(table=result_table, columns=['column'])['tsv']

    keeper = vh.run_async(
        g,
        workflow_guid=config.workflow_guid,
        quota=config.quota
    )
    keeper.get_future(result).wait()
    data = keeper.download(result)
    with open(data) as f:
        assert f.read() == "value\n"
