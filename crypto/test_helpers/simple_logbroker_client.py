import logging

from kikimr.public.sdk.python.persqueue import grpc_pq_streaming_api as pq_api


logger = logging.getLogger()

TIMEOUT = 10


class SimpleLogbrokerClient(object):
    def __init__(self, config):
        self.config = config

    def start(self):
        self.api = pq_api.PQStreamingAPI(self.config.host, self.config.port)
        self.api.start().result(timeout=TIMEOUT)

    def stop(self):
        self.api.stop()

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args):
        self.stop()

    def create_producer(self):
        configurator = pq_api.ProducerConfigurator(self.config.topic, self.config.source_id)
        producer = self.api.create_producer(configurator)
        result = producer.start().result(timeout=TIMEOUT)
        assert result.HasField("init")
        return producer

    def create_consumer(self):
        configurator = pq_api.ConsumerConfigurator(self.config.topic, self.config.consumer)
        consumer = self.api.create_consumer(configurator)
        result = consumer.start().result(timeout=TIMEOUT)
        assert result.HasField("init")
        return consumer
