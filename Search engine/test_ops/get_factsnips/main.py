import vh
from search.alice.snippets.lib.ops import get_factsnips
from search.alice.snippets.lib.util import append_serp


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/full-texts"
    get_factsnips.run(
        input_table=vh.YTTable(prefix + "/baskette5"),
    )
    append_serp(
        input_table=vh.YTTable(prefix + "/baskette5"),
    )
    vh.run(global_options=dict(yql_token="kruglikovn_yql_token"))
