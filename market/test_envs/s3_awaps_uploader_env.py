# coding: utf-8
import os

from market.idx.yatf.test_envs.base_env import BaseEnv
from market.pylibrary.s3.yatf.resources.s3_bucket import S3Bucket


class S3AwapsOffersUploaderTestEnv(BaseEnv):
    def __init__(self, s3_client, s3_bucket_name, s3_awaps_uploader, **resources):
        self.s3_client = s3_client
        self.s3_bucket_name = s3_bucket_name
        self.awaps_uploader = s3_awaps_uploader

        super(S3AwapsOffersUploaderTestEnv, self).__init__(**resources)

    @property
    def s3_bucket(self):
        return self.outputs.get('s3_bucket')

    @property
    def offers_prefix(self):
        return 'offers-'

    @property
    def shop_vendors_prefix(self):
        return 'shop-vendors-'

    def execute(self, yt_stuff, generation, input_table, worker_count):
        for worker_id in range(worker_count):
            self.awaps_uploader.upload_offers(
                yt_proxy=yt_stuff.get_server(),
                yt_tokenpath='',
                tablepath=input_table,
                generation=generation,
                worker_id=worker_id,
                worker_idx=worker_id,
                workers_count=worker_count
            )

        bucket_path = '{0}/'.format(os.path.join(self.awaps_uploader.prefix, generation))

        self.outputs.update({
            's3_bucket': S3Bucket(self.s3_client, self.s3_bucket_name, bucket_path, load=True)
        })


class S3AwapsVendorsUploaderTestEnv(BaseEnv):
    def __init__(self, s3_client, s3_bucket_name, s3_awaps_uploader, **resources):
        self.s3_client = s3_client
        self.s3_bucket_name = s3_bucket_name
        self.awaps_uploader = s3_awaps_uploader

        super(S3AwapsVendorsUploaderTestEnv, self).__init__(**resources)

    @property
    def s3_bucket(self):
        return self.outputs.get('s3_bucket')

    @property
    def vendor_filename(self):
        return 'vendor'

    def execute(self, yt_stuff, generation, input_table):
        self.awaps_uploader.upload_vendors(
            yt_proxy=yt_stuff.get_server(),
            yt_tokenpath='',
            tablepath=input_table,
            generation=generation,
        )

        bucket_path = '{0}/'.format(os.path.join(self.awaps_uploader.prefix, generation))

        self.outputs.update({
            's3_bucket': S3Bucket(self.s3_client, self.s3_bucket_name, bucket_path, load=True)
        })


class S3AwapsCategoriesUploaderTestEnv(BaseEnv):
    def __init__(self, s3_client, s3_bucket_name, s3_awaps_uploader, **resources):
        self.s3_client = s3_client
        self.s3_bucket_name = s3_bucket_name
        self.awaps_uploader = s3_awaps_uploader

        super(S3AwapsCategoriesUploaderTestEnv, self).__init__(**resources)

    @property
    def s3_bucket(self):
        return self.outputs.get('s3_bucket')

    def execute(self, yt_stuff, generation, input_table):
        self.awaps_uploader.upload_categories(
            yt_proxy=yt_stuff.get_server(),
            yt_tokenpath='',
            tablepath=input_table,
            generation=generation,
        )

        bucket_path = '{0}/'.format(os.path.join(self.awaps_uploader.prefix, generation))

        self.outputs.update({
            's3_bucket': S3Bucket(self.s3_client, self.s3_bucket_name, bucket_path, load=True)
        })
