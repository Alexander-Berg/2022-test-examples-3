#!/usr/bin/python

import pymongo

db = pymongo.Connection('localhost:27018').mpfs

queues = ['queue', 'queue_index', 'queue_photoslice', 'queue_minor']

for queue in queues:
    print "{}_total {}".format(queue, db.command("collstats", queue)['count'])
    for jtype in db[queue].distinct('type'):
        print "{} {}".format(jtype, db[queue].find({'type': jtype}).count())

