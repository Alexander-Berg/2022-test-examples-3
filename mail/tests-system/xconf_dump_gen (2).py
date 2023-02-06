#!/usr/bin/python
from json import load
from msgpack import packb
from sys import argv, exit
from collections import defaultdict

import sys
import certificates

# Config types
SERVICE = 0
MOBILE = 1
SEND_TOKEN = 2
LISTEN_TOKEN = 3

def get(json, key, default):
  if key in json:
    return json[key]
  return default

if len(argv) != 2:
  print("Input json filename is missing")
  exit(1)

with open(argv[1], "r") as f:
  data_json = load(f)

service_list = data_json['services'] if 'services' in data_json else {}
app_list = data_json['apps'] if 'apps' in data_json else {}
xconf_cache = {}

for service in service_list:
  data = service_list[service]
  # Create xconf service record. From service.h:
  # (name, owner_id, owner_prefix, description, is_passport, oauth_scopes, is_stream, stream_count, queued_delivery_by_default)
  owner_id = get(data, 'owner_id', '200')
  owner_prefix = get(data, 'owner_prefix', 'test_')
  service_data = (service, owner_prefix, owner_id, get(data, 'description', ''), get(data, 'is_passport', False),
    get(data, 'oauth_scopes', []), get(data, 'is_stream', False), get(data, 'stream_count', 0), get(data, 'revoked', False),
    get(data, 'auth_disabled', False), get(data, 'queued_delivery_by_default', True))
  # tvm_publishers
  tvm_publishers = defaultdict(list)
  for env, tvm_apps in get(data, 'tvm_publishers', {}).iteritems():
    for tvm_app in tvm_apps:
      tvm_publishers[env].append((get(tvm_app, 'id', 0), get(tvm_app, 'name', ''), get(tvm_app, 'suspended', False), ))
  # tvm_subscribers
  tvm_subscribers = defaultdict(list)
  for env, tvm_apps in get(data, 'tvm_subscribers', {}).iteritems():
    for tvm_app in tvm_apps:
      tvm_subscribers[env].append((get(tvm_app, 'id', 0), get(tvm_app, 'name', ''), get(tvm_app, 'suspended', False), ))
  service_data += (tvm_publishers, tvm_subscribers, )
  xconf_cache[(SERVICE, '', service)] = (packb(service_data), '', service,
    owner_prefix + owner_id, len(xconf_cache) + 1, SERVICE)
  # Create xconf send token records.
  token_list = get(data, 'send_tokens', [])
  for token in token_list:
    # From service.h: ((service, name))
    token_data = (service, token['name'])
    rec_name = '%s:%s' % token_data
    token_data = (token_data + (get(token, 'revoked', False),),)
    xconf_cache[(SEND_TOKEN, get(token, 'environment', 'sandbox'), rec_name)] = (packb(token_data), token['token'], rec_name,
      service, len(xconf_cache) + 1, SEND_TOKEN, get(token, 'environment', 'sandbox'))
  # Create xconf listen token records.
  token_list = get(data, 'listen_tokens', [])
  for token in token_list:
    # From service.h: ((service, name), client, allowed_services)
    token_data = (service, token['name'])
    rec_name = '%s:%s' % token_data
    token_data = (token_data + (get(token, 'revoked', False),),) + (get(token, 'client', token_data[1]), get(token, 'services', []))
    xconf_cache[(LISTEN_TOKEN, get(token, 'environment', 'sandbox'), rec_name)] = (packb(token_data), token['token'], rec_name,
      service, len(xconf_cache) + 1, LISTEN_TOKEN, get(token, 'environment', 'sandbox'))

for platform in app_list:
  for app_record in app_list[platform]:
    app = app_record['name']
    secret = get(app_record, 'secret', '')
    backup = get(app_record, 'backup', '')
    expires = get(app_record, 'expires', 0)
    service = get(app_record, 'service', '')
    environment = get(app_record, 'environment', 0)
    updated_at = get(app_record, 'updated_at', 0)
    owner = 'user_owned' if not service else 'xivaservice:' + service
    if secret.startswith('cert:'):
      secret = getattr(certificates, secret[5:])
    if backup.startswith('cert:'):
      backup = getattr(certificates, backup[5:])
    rec_name = '%s:%s' % (platform, app)
    # from types.h: (bb_client_id, xiva_service, platform, app_name, ttl, secret_key)
    app_data = ('id' if not service else '', service, platform, app, 0, secret, expires, backup, environment, updated_at)
    xconf_cache[(MOBILE, '', rec_name)] = (packb(app_data), '', rec_name, owner, len(xconf_cache) + 1, MOBILE)

print packb(xconf_cache)
