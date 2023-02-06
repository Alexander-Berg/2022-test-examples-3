import base64
import struct

from extsearch.geo.kernel.meta.proto.context_pb2 import TGeoReaskContextProto


def decode(context_str):
    ctx = TGeoReaskContextProto()
    blob = base64.b64decode(context_str)

    fmt = '<i'
    (version,) = struct.unpack_from(fmt, blob)
    if version != 100:
        raise ValueError('Unsupported context version {}'.format(version))

    ctx.ParseFromString(blob[struct.calcsize(fmt) :])
    return ctx
