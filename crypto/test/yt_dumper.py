import logging
import os

logger = logging.getLogger("test_logger")


class YtCypressDumper(object):

    def __init__(self, yt_client, base_local_dir):
        super(YtCypressDumper, self).__init__()

        self.yt_client = yt_client
        self.base_local_dir = base_local_dir

    def dump_json(self, yt_path):
        logger.info("Starting YT directory dump:\n%s -> %s", yt_path, self.base_local_dir)

        if not self.yt_client.exists(yt_path):
            raise Exception("%s doesn't exist" % yt_path)

        self._dump_json(yt_path)

        logger.info("YT directory dump finished: :\n%s -> %s", yt_path, self.base_local_dir)

    def _dump_json(self, yt_path):
        if not yt_path.startswith('//'):
            raise Exception("YT path must start with //")

        relative_local_path = yt_path[2:]  # cut leading //
        abs_local_path = os.path.join(self.base_local_dir, relative_local_path)

        node_type = self.yt_client.get_attribute(yt_path, 'type')

        if node_type == 'map_node':
            if not os.path.exists(abs_local_path):
                os.makedirs(abs_local_path)

            logger.info("Dumping dir: %s", str(yt_path))

            for node in self.yt_client.list(yt_path):
                child_yt_path = os.path.join(yt_path, node)
                self._dump_json(child_yt_path)

        elif node_type == 'table':
            logger.info("Dumping file: %s", str(yt_path))
            with open(abs_local_path, 'w') as f:
                recs = self.yt_client.read_table(yt_path, format='json', raw=True)
                f.writelines(recs)
