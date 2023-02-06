import vh
from search.alice.snippets.lib.ops import canonize


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/s2s_prod"
    canonize.run(
        input_table=vh.YTTable(prefix + "/test_gemini_original"),
        output_table=vh.YTTable(prefix + "/test_gemini_result")
    )
    vh.run(global_options=dict(yql_token="kruglikovn_yql_token"))
