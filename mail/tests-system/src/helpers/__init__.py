from time import sleep
import base64


def retry(tries=20, delay=0.5):
    def retry_impl(fn):
        def wrapper(*args, **kwargs):
            i = 0
            while True:
                i += 1
                try:
                    return fn(*args, **kwargs)
                except:
                    if i < tries:
                        sleep(delay)
                    else:
                        raise

        return wrapper

    return retry_impl


def decode_global_collector_id(id):
    decoded = base64.b64decode(id).decode()
    return decoded.split(":")
