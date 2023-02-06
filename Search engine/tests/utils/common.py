import os

from search.zephyr.proto.structures import instance_pb2


def get_search_path():
    cur_path = os.getcwd()

    while cur_path != '/':
        split_path = os.path.split(cur_path)
        if split_path[1].lower() == 'search':
            break
        cur_path = split_path[0]
    if cur_path == '/':
        raise ValueError('The script was not launched from the arcadia directory')
    return cur_path


def remove_zephyr_instances(instances: instance_pb2.InstanceList):
    return instance_pb2.InstanceList(objects=[instance for instance in instances.objects if instance.project != 'zephyr'])
