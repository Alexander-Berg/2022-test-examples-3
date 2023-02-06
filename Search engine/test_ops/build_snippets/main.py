import vh
from dict.mt.make.libs.common import VhRunner
from search.alice.snippets.lib.ops import build_snippets


def main():
    data_prefix = "//home/search-functionality/kruglikov/dialog-search" \
                  "/abstractive/snippet_mining/full-texts"
    exp_prefix = "//home/search-functionality/kruglikov/dialog-search" \
                 "/exp/snippets/01_simple_top_cut_1000"
    with VhRunner("nirvana") as runner:
        build_snippets.run(
            input_table=vh.YTTable(
                data_prefix + "/baskette5_factsnips_and_texts"
            ),
            voc_path=exp_prefix + "/voc",
            model_paths=[
                exp_prefix + "/model/" + model_file for model_file in [
                    "model-876000.npz",
                    "model-874000.npz",
                    "model-872000.npz",
                    "model-870000.npz",
                    "model-868000.npz",
                ]
            ],
            config_path=exp_prefix + "/config.json",
            output_table=vh.YTTable(data_prefix + "/baskette5_s2s_snippets")
        )
        runner.run()
