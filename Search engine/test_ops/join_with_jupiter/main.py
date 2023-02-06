from search.alice.snippets.lib.ops import join_with_jupiter
import vh


def main():
    path_prefix = "//home/search-functionality/kruglikov/dialog-search/" \
        "abstractive/snippet_mining/full-texts/"
    join_with_jupiter.run(
        input_table=vh.YTTable(path_prefix + "urls_dataset_v2"),
        output_table=vh.YTTable(path_prefix + "texts_oldest_fix_dataset_v2")
    )
    join_with_jupiter.run(
        input_table=vh.YTTable(path_prefix + "urls_pool_answers"),
        output_table=vh.YTTable(path_prefix + "texts_oldest_fix_pool_answers")
    )
    vh.run(label="join with jupyter")
