#!/usr/bin/env python
# -*- coding: utf-8 -*-

if __name__ == '__main__':
    import __classic_import    # noqa
else:
    from . import __classic_import    # noqa
import market.access.agent.mt.env as env
from market.access.server.proto.consumer_pb2 import TConsumer
from market.pylibrary.lite.matcher import Capture, CaptureTo, NoKey
import market.access.agent.proto.session_pb2 as session_pb2
import socketserver
import os
from six.moves import _thread
from six.moves import BaseHTTPServer
import six
import hashlib

TResourceOptions = TConsumer.TOptions.TResource


class T(env.AgentSuite):
    NodeId = None
    Httpd = None

    @classmethod
    def connect(cls):
        return {
            'access_server': cls.access_agent.access_server
        }

    @classmethod
    def prepare(cls):
        cls.NodeId = cls.access_agent.config.Identity.NodeId
        cls.Httpd = socketserver.TCPServer(("", 0), VersionsHandler)
        _thread.start_new_thread(cls.start_httpd_server, ())

    @classmethod
    def start_httpd_server(cls):
        cls.Httpd.serve_forever()

    def create_resource_version(self, resource, version):
        filename_base = "{}_{}.bin".format(resource , version)
        file_body = os.urandom(1024)
        VersionsHandler.set_resource(filename_base, file_body)
        _, port = self.Httpd.server_address
        url = 'http://localhost:{}/{}'.format(port, filename_base)
        return url, calc_md5(file_body)

    def test_simple_watch_install(self):
        resource_url, resource_hash = self.create_resource_version("gold", "1.0.0")
        session = self.access_agent.create_session('xomiak')

        watch_op = self.access_agent.watch_resource(session.id, 'gold')
        self.assertFalse(watch_op.done)

        op = self.access_agent.get_operation(watch_op.name)
        self.assertFalse(op.done)
        self.assertEqual(watch_op.name, op.name)

        self.access_server.create_publisher('dwarf')
        self.access_server.create_resource('gold', publisher_name='dwarf')
        self.access_server.create_version(resource_name='gold', http_url=resource_url)

        self.access_agent.wait_done(watch_op)

        install_op = self.access_agent.install_resource(session.id, 'gold')
        self.access_agent.wait_done(install_op)

        # test session re-creation
        session = self.access_agent.create_session('xomiak')
        install_op = self.access_agent.install_resource_sync(session.id, 'gold')

        # verify installed file
        install_dir = install_op.load[0].install_path
        installed_files = os.listdir(install_dir)
        self.assertEqual(len(installed_files), 1)
        self.assertEqual(resource_hash, calc_md5_file(os.path.join(install_dir, installed_files[0])))

        # test load error
        self.access_agent.commit_resource(session.id, 'gold', '1.0.0', 'bad file')

        while True:
            response = self.access_server.get_consumer_node(self.NodeId, 'xomiak')
            load = Capture()
            if not response.contains({'in_load': {'1.0.0': CaptureTo(load)}}):
                continue

            if isinstance(load.value, NoKey):
                continue

            self.assertEqual(load.value['done'], True)
            self.assertEqual(load.value['error'], 'bad file')
            break

    def test_full_cycle(self):
        self.access_server.create_publisher('orc')
        self.access_server.create_resource('bronze', publisher_name='orc')
        self.access_server.create_consumer('elf')
        self.access_server.update_consumer(
            name='elf',
            options={
                'resource': {'bronze': TResourceOptions(retention={'download_count': {'value': 2}})}
            }
        )

        url1, _ = self.create_resource_version('bronze', '1.0.0')
        self.access_server.create_version(resource_name='bronze', http_url=url1)
        session = self.access_agent.create_session('elf')
        self.access_agent.watch_resource_sync(session.id, 'bronze')
        self.access_agent.install_resource_sync(session.id, 'bronze')
        self.access_agent.commit_resource(session.id, 'bronze', '1.0.0')

        url2, _ = self.create_resource_version('bronze', '2.0.0')
        self.access_server.create_version(resource_name='bronze', http_url=url2)
        self.access_agent.watch_resource_sync(session.id, 'bronze')
        self.access_agent.commit_resource(session.id, 'bronze', '1.0.0', unload=True)
        self.access_agent.install_resource_sync(session.id, 'bronze')
        self.access_agent.commit_resource(session.id, 'bronze', '2.0.0')

        url3, _ = self.create_resource_version('bronze', '3.0.0')
        self.access_server.create_version(resource_name='bronze', http_url=url3)
        self.access_agent.watch_resource_sync(session.id, 'bronze')
        self.access_agent.commit_resource(session.id, 'bronze', '2.0.0', unload=True)
        self.access_agent.install_resource_sync(session.id, 'bronze')
        self.access_agent.commit_resource(session.id, 'bronze', '3.0.0')

    def test_update_after_fail_version(self):
        SHORT_TIMEOUT_SEC = 5
        LONG_TIMEOUT_SEC = 300

        self.access_server.create_publisher('orc2')
        self.access_server.create_resource('silver', publisher_name='orc2')
        self.access_server.create_consumer('elf2')
        self.access_server.update_consumer(
            name='elf2',
            options={
                'resource': {'silver': TResourceOptions(retention={'download_count': {'value': 2}})}
            }
        )
        url1, _ = self.create_resource_version('silver', '1.0.0')
        self.access_server.create_version(resource_name='silver', http_url=url1)
        session = self.access_agent.create_session('elf2')
        self.access_agent.watch_resource_sync(session.id, 'silver')
        self.access_agent.install_resource_sync(session.id, 'silver')
        self.access_agent.commit_resource(session.id, 'silver', '1.0.0')

        self.access_server.create_version(resource_name='silver', http_url=url1+"wrong_url")
        self.access_agent.watch_resource_sync(session.id, 'silver', SHORT_TIMEOUT_SEC)

        self.access_agent.commit_resource(session.id, 'silver', '1.0.0', unload=True)
        self.access_agent.install_resource_sync(session.id, 'silver', SHORT_TIMEOUT_SEC)

        url3, _ = self.create_resource_version('silver', '3.0.0')
        self.access_server.create_version(resource_name='silver', http_url=url3)

        watch_op = self.access_agent.watch_resource_sync(session.id, 'silver', LONG_TIMEOUT_SEC)
        self.assertEqual(watch_op.status, session_pb2.DONE)

        self.access_agent.commit_resource(session.id, 'silver', '2.0.0', unload=True)
        install_op = self.access_agent.install_resource_sync(session.id, 'silver', LONG_TIMEOUT_SEC)
        self.assertEqual(install_op.status, session_pb2.DONE)
        self.access_agent.commit_resource(session.id, 'silver', '3.0.0')

    def test_dependent_watch_install(self):
        resource_url, resource_hash = self.create_resource_version("diamond", "1.0.0")
        session = self.access_agent.create_session('xomiak_dep')

        watch_op = self.access_agent.watch_resource(session.id, 'diamond')
        self.assertFalse(watch_op.done)

        op = self.access_agent.get_operation(watch_op.name)
        self.assertFalse(op.done)
        self.assertEqual(watch_op.name, op.name)

        self.access_server.create_publisher('dwarf_dep')
        self.access_server.create_resource('diamond', publisher_name='dwarf_dep')
        self.access_server.create_resource('diamond_dep', publisher_name='dwarf_dep')
        self.access_server.create_version(resource_name='diamond_dep', http_url="fake_url")
        self.access_server.create_version(resource_name='diamond', http_url=resource_url, dependencies=[('diamond_dep', '1.0.0')])

        self.access_agent.wait_done(watch_op)

        install_op = self.access_agent.install_resource(session.id, 'diamond')
        self.access_agent.wait_done(install_op)

        # test session re-creation
        session = self.access_agent.create_session('xomiak_dep')
        install_op = self.access_agent.install_resource_sync(session.id, 'diamond')

        # verify installed file
        install_dir = install_op.load[0].install_path
        installed_files = os.listdir(install_dir)
        self.assertEqual(len(installed_files), 1)
        self.assertEqual(resource_hash, calc_md5_file(os.path.join(install_dir, installed_files[0])))
        deps = install_op.load[0].dependency
        self.assertEqual(len(deps), 1)
        self.assertEqual(deps[0].resource_name, "diamond_dep")
        self.assertEqual(deps[0].number, "1.0.0")

    def test_stats(self):
        response = self.access_agent.request_json('/stat')
        self.assertFragmentIn(response, [
            "replica=3;shard=2;shiny_component=server;Market_NAccessAgent_IAgentService_CommitResource_client_errors_dmmm",
            0
        ])


class VersionsHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    resources = dict()

    @classmethod
    def set_resource(cls, name, body):
        cls.resources["/"+name] = body

    def do_GET(self):
        body = self.resources.get(self.path)
        if body is None:
            self.send_response(404)
            self.end_headers()
            return

        self.send_response(200)
        self.end_headers()
        self.wfile.write(body)


def calc_md5(content):
    md5_hash = hashlib.md5()
    md5_hash.update(six.ensure_binary(content))
    return md5_hash.hexdigest()


def calc_md5_file(filename):
    with open(filename, 'rb') as file:
        return calc_md5(file.read())


if __name__ == '__main__':
    env.main()
