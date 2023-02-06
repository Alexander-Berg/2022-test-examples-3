from mail.devpack.lib.config_master import generate_config
from yatest.common import binary_path, runtime


class TestEnv(object):
    def __init__(self, pm, top_comp_cls, devpack_root=None, config=None):
        self.pm = pm
        self.config = config
        self.top_comp_cls = top_comp_cls
        self.devpack_root = devpack_root or "devpack"

    def get_config(self):
        if not self.config:
            gp = lambda port: self.pm.get_port(port)
            self.config = generate_config(gp, self.devpack_root, top_comp_cls=self.top_comp_cls)
        return self.config

    def get_arcadia_bin(self, path):
        return binary_path(path)

    def log_stdout(self):
        return False

    def get_java_path(self):
        return runtime.java_path()
