import vh
from search.alice.snippets.lib.ops import get_factsnips_and_texts


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/full-texts"
    get_factsnips_and_texts.run(
        input_table=vh.YTTable(prefix + "/baskette5"),
    )
    vh.run(global_options=dict(yql_token="kruglikovn_yql_token"))
