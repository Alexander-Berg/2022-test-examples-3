import vh
import vh_util.yt

from search.alice.snippets.lib.ops import translate_by_chunks


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/s2s_prod"
    voc_file = vh.data(id="4b0124bf-6644-4fc8-8494-4e6744be9215")
    model_file = vh.data(id="35a87f44-f118-4efa-8868-05bf5d6599ff")
    translate_by_chunks.run(
        input_table=vh.YTTable(prefix + "/test_translate_format"),
        voc_file_yt=yt_upload_file(voc_file),
        model_file_yt=yt_upload_file(model_file),
        chunk_size=10,
        parallel_jobs=6
    )
    vh.run(global_options=dict(yql_token="kruglikovn_yql_token"))


@vh.module(data=vh.File, path=vh.mkoutput(vh.YTTable))
def yt_upload_file(data, path):
    with vh.HardwareParams(max_ram=2048):
        vh_util.yt.write_file(data, path)
