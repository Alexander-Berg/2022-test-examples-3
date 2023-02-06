import vh
from market.dynamic_pricing.pricing.library.utils import as_pretty_string
from market.dynamic_pricing.pricing.library.nirvana.operations import NirvanaOperations


class CanonicTestOperation(object):
    def __init__(self, yt_token, cluster):
        self.yt_token = yt_token
        self.operation = vh.op(id=NirvanaOperations.RUN_EXECUTABLE)
        self.attrs = {
            "cluster": cluster,
            "key-columns": "{key_columns}",
            "actual-table": "{actual_table}",
            "expected-table": "{expected_table}",
            "diff-path": "{diff_path}",
            "exception-on-diff": "true"
        }

    def add_to_pipeline(self, actual_table, expected_table, diff_path, key_columns, skip_columns=None, after=None):
        if after is None:
            after = []
        if skip_columns:
            self.attrs["skip-columns"] = skip_columns
        attrs_str = as_pretty_string(self.attrs).format(
            actual_table=actual_table,
            expected_table=expected_table,
            diff_path=diff_path,
            key_columns=key_columns
        )

        return self.operation(
            yt_token=self.yt_token,
            args=vh.OptionExpr(attrs_str),
            tar_path='tables_diff',
            sandbox_resource_type='MARKET_CANONIC_TEST_APP',
            sandbox_resource_attrs='{"env_type": "production"}',
            max_ram=500,
            _name='canonic_test',
            _after=after
        )
