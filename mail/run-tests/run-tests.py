import yt.wrapper as yt

if __name__ == "__main__":
    yt.config["proxy"]["url"] = "hahn.yt.yandex.net"
    input_table = "//home/cocainum/xrater/appmetrica-log"
    output_table = '//home/cocainum/xrater/results1'

    batch_size = yt.row_count(input_table)

    yt.run_map(
        'bash run-script.sh',
        local_files=["testopithecus.zip", "run-script.sh"],
        input_format='json',
        output_format='json',
        memory_limit=4 * yt.common.GB,
        source_table=input_table,
        destination_table=output_table,
        spec={
            'mapper': {
                'layer_paths': [
                    '//home/cocainum/xrater/porto/node-packages-global/node_layer.tar.gz',
                    '//home/cocainum/xrater/porto/node-packages-global/porto_layer_search_ubuntu_trusty_subagent_yt-2019-07-18-17.42.49.tar.gz',
                ],
                'job_time_limit': 5 * 60 * 1000
            },
            'scheduling_tag_filter': 'porto',
            'job_count': 1,
            'max_failed_job_count': 1
        }
    )
