import vh
from search.alice.snippets.lib.ops import translate


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/full-texts"
    translate.run(
        input_table=vh.YTTable(prefix + "/vys1000"),
        voc_file=vh.data(id="4b0124bf-6644-4fc8-8494-4e6744be9215"),
        model_file=vh.data(id="35a87f44-f118-4efa-8868-05bf5d6599ff"),
        gpu_count=1,
        output_table=vh.YTTable(prefix + "/vys1000_translated")
    )
    vh.run()
