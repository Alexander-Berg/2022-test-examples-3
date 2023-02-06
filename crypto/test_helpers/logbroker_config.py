class LogbrokerConfig(object):
    def __init__(self, host, port, topic):
        self.host = host
        self.port = port
        self.dc_name = "dc1"
        self.topic = topic
        self.source_id = b"test_src"
        self.consumer = "test_client"

    @property
    def full_topic(self):
        return "rt3.{}--{}".format(self.dc_name, self.topic)
