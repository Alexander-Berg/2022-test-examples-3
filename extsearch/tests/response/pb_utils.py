# encoding: utf-8


class PbExtensionError(Exception):
    pass


def extension(message, handle):
    if message.HasExtension(handle):
        return message.Extensions[handle]
    raise PbExtensionError("Requested extension {} not found in message".format(handle.full_name))


def has_metadata_extension(geo_object, handle):
    return any(md.HasExtension(handle) for md in geo_object.metadata)


def metadata_extension(geo_object, handle):
    for metadata in geo_object.metadata:
        if metadata.HasExtension(handle):
            return metadata.Extensions[handle]
    raise PbExtensionError("Requested extension {} not found in geo object".format(handle.full_name))


def has_source_extension(search_metadata, handle):
    return any(s.HasExtension(handle) for s in search_metadata.source)


def source_extension(search_metadata, handle):
    for source in search_metadata.source:
        if source.HasExtension(handle):
            return source.Extensions[handle]
    raise PbExtensionError("Requested extension {} not found in source".format(handle.full_name))
