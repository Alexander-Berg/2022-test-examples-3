#!/usr/bin/env python
# coding: utf-8

import os
import sys
import logging


tsv_pool = "pool_full.tsv"
infos = "infos.lst"
lines = "lines.lst"
base_dir = "../../"
split_output_dir = "splitted_pool"


def abort():
    sys.exit(1)


def cleanup():
    logging.info("Cleanup...")
    os.system("rm -rf {}".format(split_output_dir))
    os.system("rm -rf {}".format(tsv_pool))
    os.system("rm -rf {} {}".format(infos, lines))
    os.system("rm -rf *.pbs")
    os.system("rm -rf *.pbs.*")
    os.system("rm -f merged.pb*")


def command(command):
    logging.debug("[shell] %s", command)
    result = os.system(command)
    if result != 0:
        logging.error("[shell] Command failed with code %s", result)
        abort()


def make_features_tsv(file_name, line_count=100):
    counter = 0
    max_offset = 5
    offset = 0
    lines = []
    for line in xrange(line_count):
        counter += 1
        offset += 1
        if offset > max_offset:
            offset = 0
            max_offset = counter % 5 + 1
        request_id = line_count * 10 + int(1 + counter / max_offset)

        rating = "0.1"
        host = "www.leningrad.spb.ru" + str(int(counter / 10))
        main_robot_url = host + "/track" + str(counter) + ".htm"
        factors = [str(1.0 / i) for i in xrange(1, 10)]
        factors_str = "\t".join(factors)
        line = (
            "{request_id}\t"
            "{rating}\t"
            "{ranking_url}\t"
            "{grouping}\t"
            "{tier}\t"
            "{shard}\t"
            "{search_type}\t"
            "{doc_id}\t"
            "{main_robot_url}\t"
            "{http_code}\t"
            "{matched_robot_url}\t"
            "{normalization_mask}\t"
            "{relevance}\t"
            "{factors}\n".format(
                request_id=request_id,
                rating=rating,
                ranking_url=main_robot_url,
                grouping=host,
                tier="WebTier0",
                shard="primus022-666-1445586" + str(500 - counter / 50),
                search_type="DEFAULT",
                doc_id=counter,
                main_robot_url=main_robot_url,
                http_code=200,
                matched_robot_url=main_robot_url,
                normalization_mask="all",
                relevance=(counter + 100 * 1000 * 1000),
                factors=factors_str,
            )
        )
        lines.append(line)

    sorted(lines)
    with open(file_name, 'w') as pool:
        for line in lines:
            pool.write(line)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    make_features_tsv(file_name=tsv_pool)

    # first of all, convert TSV pool to protopool
    command("mkdir -p {split_output_dir}".format(split_output_dir=split_output_dir))
    command(
        "{base_dir}/tsv_to_proto/tsv_to_proto_pool "
        "--input {tsv_pool} "
        "--output {split_output_dir} --uncompressed-input ".format(
            base_dir=base_dir,
            tsv_pool=tsv_pool,
            split_output_dir=split_output_dir,
        ),
    )

    command("> {}".format(infos))
    command("> {}".format(lines))

    # test SEARCH-1515 (empty lines, uncompressed output)
    command(
        "{base_dir}/merge_filter/pool_merge_filter "
        "--infos-list {infos} "
        "--lines-list {lines} "
        "--output {merged_pool} "
        "--uncompressed-input ".format(
            base_dir=base_dir,
            merged_pool="merged.pb",
            infos=infos,
            lines=lines,
        ),
    )

    # test SEARCH-1515 (empty lines, compressed output)
    command(
        "{base_dir}/merge_filter/pool_merge_filter "
        "--infos-list {infos} "
        "--lines-list {lines} "
        "--output {merged_pool} "
        "--uncompressed-input "
        "--compress-output ".format(
            base_dir=base_dir,
            merged_pool="merged.pb.gz",
            infos=infos,
            lines=lines,
        ),
    )

    # next, dump real lists
    command(
        "ls -1 {split_output_dir}/*.info > {infos}".format(
            split_output_dir=split_output_dir,
            infos=infos,
        )
    )

    command(
        "ls -1 {split_output_dir}/*.lines > {lines}".format(
            split_output_dir=split_output_dir,
            lines=lines,
        )
    )

    # uncompressed merge to uncompressed
    command(
        "{base_dir}/merge_filter/pool_merge_filter "
        "--infos-list {infos} "
        "--lines-list {lines} "
        "--output {merged_pool} "
        "--uncompressed-input ".format(
            base_dir=base_dir,
            merged_pool="merged.pb",
            infos=infos,
            lines=lines,
        ),
    )

    # uncompressed merge to compressed
    command(
        "{base_dir}/merge_filter/pool_merge_filter "
        "--infos-list {infos} "
        "--lines-list {lines} "
        "--output {merged_pool} "
        "--uncompressed-input "
        "--compress-output ".format(
            base_dir=base_dir,
            merged_pool="merged.pb.gz",
            infos=infos,
            lines=lines,
        ),
    )

    cleanup()
    logging.info("Tests passed OK")
