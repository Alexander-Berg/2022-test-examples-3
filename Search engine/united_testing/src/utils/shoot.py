import lib
import os
import json
from daemon import BaseDaemon


class FrozenServant(BaseDaemon):
    def __init__(self, args):
        super(FrozenServant, self).__init__()
        self.name = 'Frozen Servant'
        self.bin_path = args['frozen_servant_binary']
        self.port = args['frozen_servant_port']
        self.sources = args['sources_list']

    def clear_logs(self):
        pass

    def get_cmd(self):
        cmd = [self.bin_path, '-p', self.port]
        if self.sources:
            cmd.extend(['-s', self.sources])
        return cmd


class AppHost(BaseDaemon):
    def __init__(self, args, mode):
        super(AppHost, self).__init__()
        self.name = 'AppHost'
        self.bin_path = args['app_host_binary']
        self.port = args['app_host_port']
        self.mode = mode
        self.args = args
        self.build_config()

    def clear_logs(self):
        try:
            os.remove(lib.get_apphost_log(self.args, self.mode) + '-' + str(self.port))
        except:
            pass

    def get_cmd(self):
        cmd = [self.bin_path, '--no-mlock', '-p', self.port]
        cmd.extend(['--config', lib.get_apphost_config(self.args, self.mode)])
        return cmd

    def build_config(self):
        config_path = lib.get_apphost_config(self.args, self.mode)
        with open(self.args['template'], 'r') as template:
            with open(config_path, 'w') as config:
                graphs = lib.get_graphs_dir(self.args, self.mode)
                cfg = json.load(template)
                cfg['conf_dir'] = os.path.join(graphs, 'patched_graphs')
                cfg['fallback_conf_dir'] = os.path.join(graphs, 'graphs')
                cfg['backends_path'] = os.path.join(graphs, 'patched_backends.json')
                cfg['fallback_backends_path'] = os.path.join(graphs, 'backends.json')
                cfg['port'] = self.args['app_host_port']
                cfg['log'] = lib.get_apphost_log(self.args, self.mode)
                json.dump(cfg, config)


def patch_graphs(**kwargs):
    cmd = [
        kwargs['patch_graphs_binary'],
        '--path', kwargs['graphs'],
    ]
    for flag in ['patch_timeouts', 'use_frozen']:
        if kwargs.get(flag, False):
            cmd.extend(['--{}'.format(flag.replace('_', '-'))])
    if kwargs.get('frozen_servant_port', ''):
        cmd.extend(['--frozen-servant-port', kwargs['frozen_servant_port']])
    if kwargs.get('sources_list', ''):
        cmd.extend(['--sources-list', kwargs['sources_list']])
    lib.call(cmd)


def raw_save_list(full):
    return ",".join(map(lambda x: x.split(':')[0], full.split(',')))


def convert_report_results(src_folder, dst):
    with open(dst, 'w') as out:
        sorted_names = sorted(os.listdir(src_folder))
        for name in sorted_names:
            with open(os.path.join(src_folder, name), 'r') as inp:
                print 'decoding', os.path.join(src_folder, name)
                val = json.load(inp, strict=False)
                out.write(name)
                out.write('\t')
                val.pop('eventlog', None)
                json.dump(val, out, sort_keys=True)
                out.write('\n')


def analyze_results(args, mode):
    cmd = '{bin} --output-path {mode} {logpath}-{p}'.format(
        bin=args['evlog_grep_binary'],
        mode=args[mode + '_dir'],
        logpath=lib.get_apphost_log(args, mode),
        p=args['app_host_port']
    )
    if args['sources_list']:
        cmd += ' --sources-list {}'.format(args['sources_list'])
    if len(args['compare_graphs']) > 0:
        cmd += ' --save-graph ' + raw_save_list(args['compare_graphs'])
    if len(args['compare_requests']) > 0:
        cmd += ' --save-request ' + raw_save_list(args['compare_requests'])
    if len(args['compare_responses']) > 0:
        cmd += ' --save-response ' + raw_save_list(args['compare_responses'])
    lib.verified_call(cmd)


def do_shoot(args, mode):
    cmd = '{} -j {} -n {} -q {}'.format(
        args['fast_shooter_binary'],
        args['shoot_threads'],
        args['count'],
        lib.get_requests(args, mode)
    )
    if args['report_diff_binary']:
        cmd += ' -o {}'.format(os.path.join(args[mode + '_dir'], 'report.responses'))
    lib.verified_call(cmd)


def shoot(args):
    patch_graphs(
        patch_timeouts=True,
        graphs=lib.get_graphs_dir(args, 'baseline'),
        sources_list=args['sources_list'],
        patch_graphs_binary=args['patch_graphs_binary']
    )

    with AppHost(args, 'baseline'):
        do_shoot(args, 'baseline')

    if 'shoot_only' in args and args['shoot_only']:
        return

    analyze_results(args, 'baseline')

    patch_graphs(
        use_frozen=True,
        frozen_servant_port=args['frozen_servant_port'],
        graphs=lib.get_graphs_dir(args, 'experiment'),
        patch_graphs_binary=args['patch_graphs_binary'],
        sources_list=args['sources_list']
    )

    with open('responses.tab', 'w') as responses:
        for src in args['sources_list'].split(','):
            if src:
                with open('{}/{}.responses'.format(args['baseline_dir'], src), 'r') as res:
                    responses.write(res.read())

    with FrozenServant(args):
        with AppHost(args, 'experiment'):
            do_shoot(args, 'experiment')

    analyze_results(args, 'experiment')
