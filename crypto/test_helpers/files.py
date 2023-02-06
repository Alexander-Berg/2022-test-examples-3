import logging

from crypta.lib.python.yt.test_helpers import cypress


logger = logging.getLogger(__name__)


class OnWrite:
    def __init__(self, attributes=None, compressed=False):
        self.attributes = attributes or {}
        self.compressed = compressed


class YtFile(cypress.CypressNode):
    def __init__(self, file_path, cypress_path, on_write=None):
        super(YtFile, self).__init__(cypress_path)
        self.file_path = file_path
        self.on_write = on_write or OnWrite()

    def __str__(self):
        return "Yt file {} corresponding to local file {}".format(self.cypress_path, self.file_path)

    def create_on_local(self, yt_client):
        logger.info("Create %s with attributes %s", self, self.on_write.attributes)
        yt_client.create("file", path=self.cypress_path, recursive=True, attributes=self.on_write.attributes)

    def write_to_local(self, yt_client):
        self.create_on_local(yt_client)

        logger.info("Write %s", self)
        with open(self.file_path, "rb") as input_stream:
            yt_client.write_file(self.cypress_path, input_stream, is_stream_compressed=self.on_write.compressed)

    def read_from_local(self, yt_client):
        logger.info("Read %s", self)
        with open(self.file_path, "wb") as f:
            for chunk in yt_client.read_file(self.cypress_path).chunk_iter():
                f.write(chunk)
