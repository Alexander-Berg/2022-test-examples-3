def deserialize(type_name, obj):
    if isinstance(obj, dict):
        attrs = {}
        for k, v in obj.items():
            attrs[k] = deserialize(k, v)
        clazz = type(type_name, (), attrs)
        new_obj = clazz()
    else:
        new_obj = obj

    return new_obj


class ApiResult(object):
    def __init__(self, response):
        self.response = response

    def result(self):
        return self.response


def api_result(obj_type):
    def wrapper(func):
        def result(*args, **kwargs):
            api_obj = deserialize(obj_type.DESCRIPTOR.name, func(*args, **kwargs))
            return ApiResult(api_obj)
        return result
    return wrapper
