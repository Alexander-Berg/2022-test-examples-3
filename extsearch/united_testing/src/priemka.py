import extsearch.images.tools.offline_diff.united_testing.src.utils.ammo as ammo
import extsearch.images.tools.offline_diff.united_testing.src.utils.build as build
import extsearch.images.tools.offline_diff.united_testing.src.utils.shoot as shoot
import extsearch.images.tools.offline_diff.united_testing.src.utils.diff as diff
import os
import json
from argparse import ArgumentParser


def parse_args(argv):
    parser = ArgumentParser()
    parser.add_argument('-a', '--ammo', action='store_true')
    parser.add_argument('-b', '--build', action='store_true')
    parser.add_argument('-s', '--shoot', action='store_true')
    parser.add_argument('-d', '--diff', action='store_true')
    parser.add_argument('-c', '--config', default='config.json')
    parser.add_argument('-t', '--token', default='')
    return parser.parse_args(argv)


def start(argv):
    args = parse_args(argv)
    cfg_file = open(args.config, 'r')
    cfg = json.load(cfg_file)
    cfg_file.close()
    for mode in ['baseline', 'experiment']:
        cfg['{}_graphs'.format(mode)] = os.path.expanduser(cfg['{}_graphs'.format(mode)])
    if args.token:
        cfg['token'] = args.token

    do_all = not(args.ammo or args.build or args.shoot or args.diff)
    if args.ammo or do_all:
        ammo.build_ammo(cfg)
    if args.build or do_all:
        build.build_backends_and_bundle(cfg)
    if args.shoot or do_all:
        shoot.shoot(cfg)
    if args.diff or do_all:
        diff.diff_results(cfg)
