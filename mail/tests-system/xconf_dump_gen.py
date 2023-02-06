#!/usr/bin/python
from json import load
from msgpack import packb
from sys import argv, exit

# Config type
SERVICE = 0

def get(json, key, default):
  if key in json:
    return json[key]
  return default

if len(argv) != 2:
  print("Input json filename is missing")
  exit(1)

with open(argv[1], "r") as f:
  service_list = load(f)

xconf_cache = {}

for service in service_list:
  data = service_list[service]
  # Create xconf service record. From service.h:
  # (name, owner_uid, owner_prefix, description, is_passport, oauth_scopes, is_stream, stream_count)
  owner_uid = get(data, 'owner_uid', '200')
  owner_prefix = get(data, 'owner_prefix', 'test_')
  service_data = (service, owner_prefix, owner_uid, get(data, 'description', ''), get(data, 'is_passport', False),
    get(data, 'oauth_scopes', []), get(data, 'is_stream', False), get(data, 'stream_count', 0), get(data, 'revoked', False))
  xconf_cache[(SERVICE, '', service)] = (packb(service_data), '', service,
    owner_prefix + owner_uid, len(xconf_cache) + 1, SERVICE, '')

print packb(xconf_cache)
