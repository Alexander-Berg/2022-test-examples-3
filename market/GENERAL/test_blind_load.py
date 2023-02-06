#!/usr/bin/env python
# -*- coding: utf-8 -*-

if __name__ == '__main__':
    import __classic_import    # noqa
else:
    from . import __classic_import    # noqa
import market.access.agent.mt.env as env


class T(env.AgentSuite):
    NodeId = None

    @classmethod
    def connect(cls):
        return {
            'access_server': cls.access_agent.access_server
        }

    @classmethod
    def prepare(cls):
        cls.access_agent.config.Downloader.Dry = True
        cls.access_agent.config.Installer.Dry = True
        cls.access_agent.config.Consumer.BlindLoadInterval.seconds = 100500

    def test_blind_load(self):
        self.access_server.create_publisher('dwarf')
        self.access_server.create_resource('gold', publisher_name='dwarf')
        self.access_server.create_version(resource_name='gold', rbtorrent='link-to-file')

        session = self.access_agent.create_session()
        resource = self.access_agent.install_resource_sync(session.id, 'gold')
        self.access_agent.commit_resource(session.id, 'gold', resource.load[0].spec.version.number)
        version_number = resource.load[0].spec.version.number

        self.access_server.stop_server()

        session = self.access_agent.create_session()
        resource = self.access_agent.install_resource_sync(session.id, 'gold')
        self.assertEqual(resource.load[0].spec.version.number, version_number)


if __name__ == '__main__':
    env.main()
