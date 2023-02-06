import vh
import vh_util.yt
from search.alice.snippets.lib.ops import translate


def main():
    prefix = "//home/search-functionality/kruglikov/dialog-search" \
             "/abstractive/snippet_mining/full-texts"
    for gpu_count in [1, 2, 4]:
        for cpu_count in [16, 32, 50]:
            for client_threads in [32, 64, 128]:
                stderr = translate.run(
                    input_table=vh.YTTable(prefix + "/vys100k"),
                    data=vh.data(id="10784938-4cb9-4c14-b8bc-608546fb23e7"),
                    cpu_count=cpu_count,
                    gpu_count=gpu_count,
                    client_threads=client_threads
                ).client_stderr
                name = f"gpu_{gpu_count}_cpu_{cpu_count:03d}_j_{client_threads:03d}"
                vh_util.yt.write_file(
                    stderr, vh.YTTable(prefix + "/cpu_benchmark/" + name))

    vh.run(keep_going=True)
