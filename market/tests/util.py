import gzip
import hashlib
import os


def touch(filepath, content=None):
    dirname = os.path.dirname(filepath)
    if not os.path.exists(dirname):
        os.makedirs(dirname)
    open_func = open
    if filepath.endswith('.gz'):
        open_func = gzip.open
    with open_func(filepath, 'w') as obj:
        if content:
            obj.write(content)


def calc_md5(content):
    hasher = hashlib.md5()
    hasher.update(content)
    return hasher.hexdigest()
