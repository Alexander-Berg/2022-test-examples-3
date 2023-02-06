from search.zephyr.proto.structures import instance_pb2


def get_methods():
    """
    Creates some methods for instances
    """
    methods_a = {
        'foo': instance_pb2.Method(name='foo', input='f', output='f', timeout=1, retries=1),
        'bar': instance_pb2.Method(name='bar', input='b', output='b', timeout=1, retries=1),
    }
    methods_b = {
        'foo': instance_pb2.Method(name='foo', input='f2', output='f2', timeout=2, retries=2),
        'bar': instance_pb2.Method(name='bar', input='b', output='b', timeout=1, retries=1),
        'baz': instance_pb2.Method(name='baz', input='bz', output='bz', timeout=1, retries=1),
    }

    return methods_a, methods_b


def get_instances():
    """
    Creates some instances
    """
    methods_a, methods_b = get_methods()

    instance_a = instance_pb2.Instance(fqdn='alpha', methods=methods_a, port=11)
    instance_b = instance_pb2.Instance(fqdn='bravo', methods=methods_b, port=12)

    return instance_a, instance_b
