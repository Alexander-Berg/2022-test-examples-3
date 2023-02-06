import logging

import yt.wrapper as yt

from crypta.lib.python.yt.test_helpers import utils


logger = logging.getLogger(__name__)


class CypressNode(utils.FileSource):
    def __init__(self, cypress_path):
        self.cypress_path = cypress_path

    def __str__(self):
        return "cypress node {}".format(self.cypress_path)

    @property
    def cypress_dirname(self):
        return yt.ypath_dirname(self.cypress_path)

    @property
    def cypress_basename(self):
        _, basename = yt.ypath_split(self.cypress_path)
        return basename

    def exists_on_local(self, yt_client):
        return yt_client.exists(self.cypress_path)

    def get_attr_from_local(self, attr_name, yt_client, attr_type=None):
        return utils.get_attr_with_log(logger, attr_name, yt_client, self.cypress_path, attr_type)
