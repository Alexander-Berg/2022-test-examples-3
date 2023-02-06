import os


class EnvironmentContextManager(object):
    def __init__(self, env_vars):
        self.new_vars = env_vars

    def __enter__(self):
        self.old_vars = {key: os.environ.get(key) for key in self.new_vars}
        os.environ.update(self.new_vars)

    def __exit__(self, exc_type, exc_value, traceback):
        for key, old_val in self.old_vars.items():
            if old_val is not None:
                os.environ[key] = old_val
            else:
                del os.environ[key]
