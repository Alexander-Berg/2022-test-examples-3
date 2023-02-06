# -*- coding: utf-8 -*-
"""
Usage:
    $./manage.py run_path scripts/push_test.py --help
"""
from __future__ import unicode_literals

import json
import logging
import sys

import click
from django.conf import settings

from avia.lib.pusher import Pusher
from avia.lib.pusher.tag import PusherTag
from avia.v1.model.device import Device
from avia.v1.model.user import User


log = logging.getLogger('')
log.addHandler(logging.StreamHandler(sys.stdout))
log.setLevel(logging.INFO)


class JsonData(click.ParamType):
    name = 'jsondata'

    def convert(self, value, param, ctx):
        try:
            return json.loads(value)
        except KeyError as e:
            self.fail(str(e), param, ctx)


class Platform(click.ParamType):
    name = 'platform'

    def convert(self, value, param, ctx):
        if value not in settings.PUSHER_PLATFORMS:
            self.fail('Available platforms: %r' % (settings.PUSHER_PLATFORMS,), param, ctx)

        return value


@click.command()
@click.option('-v', '--verbose', is_flag=True)
@click.option('--transport', required=True)
@click.option('--uuid', required=True)
@click.option('--push-token', required=True)
@click.option('--platform', required=True, type=Platform())
@click.option('--data', type=JsonData())
@click.option('--message', required=True)
@click.option('--tag', default=PusherTag.Undefined)
def main(verbose, transport, uuid, push_token, platform, data, message, tag):
    if verbose:
        log.setLevel(logging.DEBUG)

    device = Device(
        uuid=uuid,
        transport=transport,
        push_token=push_token,
        platform=platform,
        user=User()
    )

    pusher = Pusher()

    pusher.delete_by_uuid(device.uuid)
    pusher.add(device)
    pusher.push_many(devices=[device], data=data or {}, message=message, push_tag=tag)


if __name__ == '__main__':
    main()
