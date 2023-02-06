import os
import yaml
import shutil

from copy import deepcopy
from io import StringIO

from yatest.common import execute, binary_path, source_path

_cache = {}


def get_conf_path(name):
    return os.path.join(binary_path("mail/nwsmtp/config"), 'nwsmtp.yml-' + name + '-production')


def read_conf(path):
    cfg_dumper = binary_path("mail/yplatform/config-dumper/config-dumper")

    key = os.path.basename(path)
    if key not in _cache:
        dumper = execute([cfg_dumper, path, "-yml"], close_fds=True, wait=True)
        raw_config = dumper.std_out.decode("utf-8")
        _cache[key] = raw_config

    obj = yaml.safe_load(StringIO(_cache[key]))
    normalize(obj)
    return obj


def write_conf(obj, path):
    obj = deepcopy(obj)
    denormalize(obj)
    with open(path, "w+") as fd:
        yaml.dump(obj, fd)


def copy_conf(src_path, dst_dir, back):
    with open(src_path, "r", encoding="utf-8") as fd:
        obj = yaml.safe_load(fd)
    new_path = os.path.join(dst_dir, back + '.yml')
    with open(new_path, "w+", encoding="utf-8") as fd:
        yaml.dump(obj, fd)
    return new_path


def find_files(src):
    *_, files = next(os.walk(src))
    return {fname: os.path.join(src, fname) for fname in files}


def copy_files(conf, dst):
    nwsmtp_etc = source_path("mail/nwsmtp/package/deploy/etc/nwsmtp")
    dsn_conf = source_path("mail/library/dsn/conf")

    files = find_files(nwsmtp_etc)
    files.update(find_files(dsn_conf))

    for v in conf.values():
        if v in files:
            shutil.copyfile(files[v], os.path.join(dst, v))

    # rc_*_limits.conf
    for k, v in files.items():
        if k.startswith("rc_") and k.endswith("_limits.conf"):
            shutil.copyfile(v, os.path.join(dst, k))


def normalize(d):
    """ Replace [{"_name": .., }, ..] with {dict["_name"]: dict, ..}.

    :type d: dict
    """
    for k, v in d.items():
        if isinstance(v, list) and v \
                and isinstance(v[0], dict) and "_name" in v[0]:
            d[k] = {}
            for i in v:
                if i["_name"] in d[k]:
                    raise RuntimeError("Duplicate node found: {[_name]}".format(i))
                d[k][i["_name"]] = i
            normalize(d[k])
        elif isinstance(v, dict):
            normalize(v)


def denormalize(d):
    """ Do opposite of normalize.

    :type d: dict
    """
    for k, v in d.items():
        if isinstance(v, dict):
            next_v = next(iter(v.values()), {})
            if isinstance(next_v, dict) and "_name" in next_v:
                d[k] = [i for i in d[k].values()]
                for i in d[k]:
                    denormalize(i)
            else:
                denormalize(v)
