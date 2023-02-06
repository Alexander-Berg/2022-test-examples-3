from time import sleep


def retry(tries=3, delay=0.5):
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
