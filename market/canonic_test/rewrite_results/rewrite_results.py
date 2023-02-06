# By default yt token read from os.environ['YT_TOKEN']
import logging
import yt.wrapper as yt


def copy_tables(input_tables, destination_dir, cluster, prefix_to_remove="//home/market/production"):
    yt.config["proxy"]["url"] = cluster

    for table in input_tables:
        if table == '':
            continue
        destination_path = destination_dir + table.replace(prefix_to_remove, "", 1)
        yt.create("table", destination_path, recursive=True, ignore_existing=True)
        yt.copy(table, destination_path, force=True)
        yt.link(destination_path, '/'.join(destination_path.split('/')[:-1]) + "/latest", ignore_existing=True)
        logging.info("Copy table\n from {0} to {1}".format(table, destination_path))


def remove_tables(tables, cluster):
    yt.config["proxy"]["url"] = cluster

    for table in tables:
        if table == '':
            continue
        yt.remove(table)
        logging.info("Remove table {0}".format(table))
