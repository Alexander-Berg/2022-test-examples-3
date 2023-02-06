__author__ = 'aokhotin'
import os
import requests
import subprocess


def test_dns():
    dump = subprocess.Popen(["tcpdump", "-w", "dns.dump", "dst port 53"])
    do_requests()
    dump.terminate()
    dump.wait()
    awk = subprocess.Popen("tcpdump -r dns.dump | awk '$7 ~ /A.*/ {print $8}' | sort | uniq", shell=True,
                           stdout=subprocess.PIPE)
    out, err = awk.communicate()
    hosts = [host for host in out.split('\n') if host]
    assert set(hosts) == set()


def do_requests():
    for line in open(os.path.join(os.path.dirname(__file__), 'yandsearch.100.out')):
        method, path, _, host, _ = line.split()
        requests.get("http://localhost:8080%s" % path, headers={'Host': host})
