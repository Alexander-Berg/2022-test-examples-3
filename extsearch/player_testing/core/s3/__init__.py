import boto3


class S3Client(object):
    def __init__(self, config):
        self.session = boto3.session.Session(
            aws_access_key_id=config.access_key_id,
            aws_secret_access_key=config.secret_access_key
        )
        self.client = self.session.client(
            service_name='s3',
            endpoint_url=config.endpoint_url,
            verify=False
        )
        self.bucket = config.bucket
        self.endpoint_url = config.endpoint_url

    def upload_file(self, path, key, content_type):
        self.client.upload_file(path, self.bucket, key, {'ContentType': content_type})
        return '{}/{}/{}'.format(self.endpoint_url, self.bucket, key)
