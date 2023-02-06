# coding: utf-8

from market.pylibrary.saas.kv_client import KVGetter
from market.idx.yatf.test_envs.base_env import BaseEnv

from saas.rtyserver_test.local_saas import LocalSaas


class SaasEnv(BaseEnv, LocalSaas):
    def __init__(self, saas_service_configs=None, cluster_config=None, config_patch=None, prefixed=None, **resources):
        BaseEnv.__init__(self, **resources)
        self._patch = config_patch if config_patch is not None else {}
        if prefixed is not None:
            self._patch['Server.IsPrefixedIndex'] = [prefixed]
        LocalSaas.__init__(self, saas_service_configs, cluster_config, cwd=self.output_dir, config_patch=self._patch)
        self._kv_client = None
        self._prefixed = prefixed

    def __enter__(self):
        BaseEnv.__enter__(self)
        self.start()
        return self

    def __exit__(self, *args):
        BaseEnv.__exit__(self, args)
        self.stop()

    @property
    def description(self):
        return 'saas'

    @property
    def host(self):
        return 'localhost'

    @property
    def kv_client(self):
        if not self._kv_client:
            self._kv_client = KVGetter(self.host, self.search_port, self.service_name, retries_count=3, retry_timeout=5)
        return self._kv_client

    @property
    def prefixed(self):
        return self._prefixed

    @property
    def config_patch(self):
        return self._patch
