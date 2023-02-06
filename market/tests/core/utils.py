import os, datetime


def ensure_path(path):
    if not os.path.exists(path):
        os.makedirs(path)


def ensure_file_directory(file_path):
    ensure_path(os.path.dirname(file_path))


def force_create_directory(path):
    import shutil
    shutil.rmtree(path, ignore_errors=True)
    os.makedirs(path)


def get_datetime(date):
    return date if isinstance(date, datetime.datetime) else datetime.datetime.strptime(date, "%Y-%m-%d")

def date_generator(begin, end):
    date = get_datetime(begin)
    end_date = get_datetime(end)
    while date != end_date:
        yield date.strftime("%Y-%m-%d")
        date += datetime.timedelta(days=1)


def dict_contains(full, part):
    diff = part.viewitems() - full.viewitems()
    return len(diff) == 0


def list_contains(full, part):
    for item in full:
        if dict_contains(item, part):
            return True
    return False


def contains(full, part):
    if isinstance(full, dict):
        return dict_contains(full, part)
    elif isinstance(full, list):
        return list_contains(full, part)
    raise RuntimeError('Unknown class in contains()')

