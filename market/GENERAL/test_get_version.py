#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.play.mt.env as env


class T(env.AccessPlaySuite):
    @classmethod
    def prepare(cls):
        def make_resource(beam):
            server = beam.access_agent.access_server.connect()
            server.create_publisher(name='test')
            server.create_resource(publisher_name='test', name='gold')
            server.create_version('gold', http_url='http://s3.mds.yandex.net/sandbox-469/1793794907/gold.tar.gz')
            server.stop()

        cls.access_play.configure_server = make_resource
        updater = cls.access_play.config.ResourceData.SimpleResourceUpdaters.add()
        updater.Consumer = 'play'
        updater.Resources.append('gold')

    def test_version(self):
        response = self.access_play.request_json('get_version?resource=gold')
        self.assertFragmentIn(response, ["1.0.0"])


if __name__ == '__main__':
    env.main()
