import vh
import vh_util.yt
from search.alice.snippets.lib.ops import translate


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/full-texts"
    for gpu_count in [1, 2, 4, 8]:
        for batch_size in [5, 10, 50, 100]:
            for client_threads in [16, 32, 64, 128]:
                stderr = translate.run(
                    input_table=vh.YTTable(prefix + "/vys100k"),
                    data=vh.data(id="10784938-4cb9-4c14-b8bc-608546fb23e7"),
                    gpu_count=gpu_count,
                    batch_size=batch_size,
                    client_threads=client_threads
                ).client_stderr
                name = f"gpu_{gpu_count}_bs_{batch_size:03d}_j_{client_threads:03d}"
                vh_util.yt.write_file(
                    stderr, vh.YTTable(prefix + "/benchmark/" + name))

    vh.run(keep_going=True)
