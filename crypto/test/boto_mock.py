import gzip
import os
import tempfile


GZ_SUFFIX = "_gz"


class Key(object):
    def __init__(self, name, data):
        self.name = name
        self.data = data

    def read(self):
        return self.data

    def get_contents_to_file(self, fo):
        fo.write(self.data)


class Bucket(object):
    def __init__(self, keys):
        self.keys = keys

    def __iter__(self):
        return (key for key in self.keys)

    def __contains__(self, name):
        return any(key.name == name for key in self.keys)

    def list(self):
        return self.keys

    def get_key(self, name):
        for key in self.keys:
            if key.name == name:
                return key

    def delete_keys(self, names):
        for name in names:
            key = self.get_key(name)
            if key is not None:
                self.keys.remove(key)


def get_bucket_from_dir(dirpath):
    keys = []
    for filename in os.listdir(dirpath):
        with open(os.path.join(dirpath, filename)) as f:
            data = f.read()
        if filename.endswith(GZ_SUFFIX):
            filename = filename[:-len(GZ_SUFFIX)] + ".gz"
            _, tmp = tempfile.mkstemp()
            with gzip.GzipFile(tmp, "w") as g:
                g.write(data)
            with open(tmp) as f:
                data = f.read()
        keys.append(Key(filename, data))
    return Bucket(keys)
