import json

g = json.loads(open('ah.json').read())
print g["sources"]

sources = []

for sn in g["sources"]:
    # print sn
    data = g["sources"][sn]

    if "handler" not in data:
        pass
        # print json.dumps(data, indent=4)
    else:
        if 'search/app-host' in data["handler"]:
            sources.append(sn)

