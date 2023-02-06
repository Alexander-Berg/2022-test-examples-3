#!/usr/bin/python3

import sys
import json
import yaml


def main(args):
    filters = args[1:]

    pods = json.load(sys.stdin)
    result = []

    for pod in pods:
        pod = pod[0]
        filtered = {}
        for filter in filters:
            node = filtered
            pod_node = pod
            tokens = filter.split('/')
            for token in tokens[:-1]:
                if not token:
                    continue
                if token not in pod_node:
                    break
                if token not in node:
                    node[token] = {}
                node = node[token]
                pod_node = pod_node[token]
            else:
                node[tokens[-1]] = pod_node[tokens[-1]]
        result.append(filtered)

    yaml.dump(result, sys.stdout)


main(sys.argv)
