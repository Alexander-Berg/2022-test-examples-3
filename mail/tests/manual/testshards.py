import sys

mapname = sys.argv[1]

f = open(mapname, 'r')

maplist = f.readlines()

shardset = set()
for el in maplist:
    if not el.strip(): continue
    try:
        shard = el.split(" ")[1].split(",")[3].split(":")[1]
        shardset.add(shard)
    except Exception as e:
        print el
        print e

shards = set()
for e in shardset:
    for shard in range(int(e.split("-")[0]), int(e.split("-")[1]) + 1):
        shards.add(shard)

if len(shards) == 65534:
    print "shard count verified"
else:
    sys.exit(1)