import vh
from search.alice.snippets.lib.ops import prs_dumper


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/full-texts"
    prs_dumper.run(
        input_table=vh.YTTable(prefix + "/baskette5"),
        output_table=vh.YTTable(prefix + "/baskette5_prs_texts")
    )
    vh.run(global_options=dict(yql_token="kruglikovn_yql_token"))
