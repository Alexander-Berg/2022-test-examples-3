#!/usr/bin/python3

import yaml
import sys
import subprocess
import re

script = "./upload_pandora.sh 2>&1"
conf = "tank.yaml"
tmpl = "tank.tmpl"
proxy_url = "https://proxy.sandbox.yandex-team.ru/"

def parse_link(link):
    return link.split('/')[-1]

def help():
    return """
Usage: ./resources.py <command>
where command is
help - for print this help
print - for print resources configs and binary
update - for update resources configs and binary
"""

def display_config():
    with open(conf) as f:
        data = yaml.safe_load(f)

    pandora_root = data['pandora']
    resources_raw = pandora_root['resources']
    pandora_cmd = pandora_root['pandora_cmd']

    resources = {}
    for resource in resources_raw:
        resources[resource['dst']] = parse_link(resource['src'])

        print("resources:")
        print("=" * 40)
        print(resources)
        print("=" * 40)
        print("pandora_cmd: {}".format(parse_link(pandora_cmd)))

def do_upload():
    p = subprocess.run(script.split(' '), capture_output=True)
    return parse_output(p.stdout.decode("utf-8"))
    
def parse_output(raw):    
    key = "-"
    ptrn = re.compile("upload (.*)")
    result = {}
    
    for line in raw.split('\n'):
        r = ptrn.match(line)
        if not r is None:
            key = r.group(1)
        else:
            result[key] = line
            key = "-"

    del result["-"]
            
    return result
            
    
def update_yaml(values):
    with open(tmpl, 'rt') as f:
        template = f.read()

    resources = {}
    
    for key, value in values.items():
        if re.match('.*\.json$', key):
            resources['{{resource_{}}}'.format(key)] = value

    resources['{resource_app/app}'] = values['app/app']

    result = template
    for k, v in resources.items():
        result = result.replace(k, v) 

    with open(conf, 'wt') as f:
        f.write(result)
            
def update_config():
    update_yaml(do_upload())
    

if len (sys.argv) < 2:
    print(help())
    sys.exit(1)
    
command = sys.argv[1]

if command == "print":
    display_config()
elif command == "update":
    update_config()
else:
    print(help())
