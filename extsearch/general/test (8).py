import re


def parse_hostlist(hostlist_path):
    hosts = {}

    with open(hostlist_path, 'r') as f:
        for line in f:
            line = ' '.join(line.split())
            if not line or line.startswith('#'):
                continue
            m_with_clusters = re.match(r'^(?P<name>[\w_]+):{{localhost}}(?P<clusters>(?:(?: \w+..\w+)|(?: \w+))+)$', line)
            m_simple = re.match(r'^(?P<name>[\w_]+) {{localhost}}$', line)
            if m_with_clusters is not None:
                name = m_with_clusters.group('name')
                clusters_str = m_with_clusters.group('clusters')
                clusters = []
                for part in clusters_str.split():
                    m_range = re.match(r'^(?P<from>\w+)\.\.(?P<to>\w+)$', part)
                    m_exact = re.match(r'^(?P<exact>\w+)$', part)
                    if m_range is not None:
                        clusters.append({
                            'from': m_range.group('from'),
                            'to': m_range.group('to'),
                        })
                    elif m_exact is not None:
                        clusters.append(m_exact.group('exact'))
                    else:
                        raise Exception('Failed to parse clusters part: {}'.format(part))
            elif m_simple is not None:
                name = m_simple.group('name')
                clusters = None
            else:
                raise Exception('Failed to parse line in hostlist: {}'.format(line))
            if name in hosts:
                raise Exception('Duplicate name in hostlist: {}'.format(name))
            hosts[name] = {
                'clusters': clusters,
            }

    return hosts


def parse_target_types(target_types_path):
    target_types = {}

    with open(target_types_path, 'r') as f:
        for line in f:
            line = ' '.join(line.split())
            if not line or line.startswith('#'):
                continue
            m_with_clusters = re.match(r'^(?P<type>[\w_]+) = !hostlist:(?P<name>[\w_]+) !hostlist:(?P=name):clusters', line)
            m_simple = re.match(r'^(?P<type>[\w_]+) = !hostlist:(?P<name>[\w_]+)', line)  # subpattern of m_with_clusters
            if m_with_clusters is not None:
                type_ = m_with_clusters.group('type')
                name = m_with_clusters.group('name')
                clustered = True
            elif m_simple is not None:
                type_ = m_simple.group('type')
                name = m_simple.group('name')
                clustered = False
            else:
                raise Exception('Failed to parse line in target types: {}'.format(line))
            if type_ in target_types:
                raise Exception('Duplicate type in target types: {}'.format(type_))
            target_types[type_] = {
                'name': name,
                'clustered': clustered,
            }

    return target_types


def parse_targets(targets_path):
    targets = {}

    with open(targets_path, 'r') as f:
        line_prefix = ''
        for line in f:
            line = ' '.join(line.split())
            if not line or line.startswith('#'):
                continue
            if line.endswith('\\'):
                line_prefix += line
                continue
            line = line_prefix + line
            line_prefix = ''
            m = re.match(r'^(?P<type>[\w]+) +(?P<target>[\w.]+):', line)
            if m is not None:
                type_ = m.group('type')
                target = m.group('target')
            else:
                raise Exception('Failed to parse line in targets: {}'.format(line))
            if target in targets:
                raise Exception('Duplicate target in targets: {}'.format(target))
            targets[target] = {
                'type': type_,
                'pos': len(targets),
            }
        if line_prefix:
            raise Exception('Finished with non-empty line prefix: {}'.format(line_prefix))

    return targets


def parse_targets_with_args(hostlist_path, target_types_path, targets_path):
    hosts = parse_hostlist(hostlist_path)
    target_types = parse_target_types(target_types_path)
    targets = parse_targets(targets_path)

    targets_list = [(key, value) for key, value in targets.items()]
    targets_sorted = sorted(targets_list, key=lambda x: x[1]['pos'])

    targets_with_args = []
    for target_name, value in targets_sorted:
        target_type = target_types[value['type']]
        if target_type['clustered']:
            clusters = hosts[target_type['name']]['clusters']
            clst = clusters[0]
            if isinstance(clst, dict):
                targets_with_args.append([target_name, clst['from']])
            else:
                targets_with_args.append([target_name, clst])
        else:
            targets_with_args.append([target_name])

    return targets_with_args
