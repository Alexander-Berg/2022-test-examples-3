import random
import string


def rndstr(N=16):
    return "".join(random.choice(string.ascii_uppercase + string.digits) for _ in range(N))


def format_nullable_datetime(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%S.%fZ") if dt else None
