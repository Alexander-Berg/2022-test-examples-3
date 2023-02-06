import os
import json
import time
import argparse
import datetime

parser = argparse.ArgumentParser()
parser.add_argument('path', help='Directory with pre build rules')
parser.add_argument('-s', '--shard', help='Shard name')
args = parser.parse_args()

task = os.environ.get('SANDBOX_TASK')
arcadia_revision = os.environ.get('ARCADIA_REVISION')
robots_revision = os.environ.get('ROBOTS_REVISION')
shard_name = args.shard
now = datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')

with open(os.path.join(args.path, "version.info"), "wb") as fd:
    json.dump({
        "RobotsRevision": int(robots_revision) if robots_revision else -1,
        "Revision": int(arcadia_revision) if arcadia_revision else -1,
        "Task": int(task) if task else -1,
        "GenerationTime": now,
        "ShardName": shard_name or 'Unknown',
    }, fd, indent=2)

with open(os.path.join(args.path, 'version.pb.txt'), 'w') as f:
    if task:
        f.write('Task: %s\n' % task)
    if arcadia_revision:
        f.write('Revision: %s\n' % arcadia_revision)
    if shard_name:
        f.write('ShardName: "%s"\n' % shard_name)
    f.write('GenerationTime: "%s"\n' % now)

with open(os.path.join(args.path, "timestamp.txt"), "wb") as fd:
    json.dump(time.time(), fd, indent=2)
