#!/usr/bin/env python

from __future__ import print_function
from os import listdir
import argparse
import sys
import json

def main():
        parser = argparse.ArgumentParser(description = "ice performance test script")
        parser.add_argument("--dump-metrics-dir", dest = "dump_dir", required = True)
        args = parser.parse_args(sys.argv[1:])

        perf_report = {'Tests': []}
        perffile = open("{0}/{1}".format(args.dump_dir, 'acceptance-performance.value.link'))
        for line in perffile:
            (perfpercent, taskid) = line.split()
            perf = perfpercent.replace('%', '')
            perf_report['Tests'].append(
                {
                    'taskid': taskid,
                    'performance_percent': perf
                })

        output_file = open("{0}/{1}".format(args.dump_dir, 'perf.report.json'), 'w')
        output_file.write(json.dumps(perf_report))
        output_file.close()

if __name__ == "__main__":
        main()
