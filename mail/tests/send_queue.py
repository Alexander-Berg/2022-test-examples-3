#!/usr/bin/python
# -*- encoding: utf-8 -*-

import sys
import urllib
import urllib2
import httplib
import json

def split_with_offsets(line):
    words = line.split()
    index = line.index
    offsets = []
    append = offsets.append
    running_offset = 0
    for word in words:
        word_offset = index(word, running_offset)
        word_len = len(word)
        previous_offset = running_offset
        running_offset = word_offset + word_len
        append((word, previous_offset, running_offset))
    return offsets

def read_header(fp):
    line = fp.next()[:-1]
    items = split_with_offsets(line)
    length = len(line)

    return (items, length)

def read_values(fp, items, line_len, send_count):
    envelopes = []
    try:
        while True:
            line = fp.next()[:-1]
            envelope = {}
            line += ' '*(line_len - len(line))

            for (name, offset, end) in items:
                envelope[name] = line[offset:end].strip()

            #if int(envelope['OPERATION_SIZE']) > 1:
            envelopes.append(envelope)
            if len(envelopes) >= send_count:
                break

    except StopIteration:
        pass

    return envelopes

def make_request(request):
    method = request.get_method()
    url = request.get_full_url()
    host = request.get_host()
    data = request.get_data()
    print "outgoing request: %s %s " % (method, url)
    print 'data:', data
    print request.has_data()

    conn = httplib.HTTPConnection(host)
    if method == 'POST':
        conn.request(method, url, data, headers=request.headers)
    else:
        conn.request(method, url, headers=request.headers)

    response = conn.getresponse()

    status = response.status
    message = response.read()
    print '[response]'
    print response.status, message
    print '\n'.join([ key + ': ' + value for (key, value) in response.getheaders()])

    conn.close()

if len(sys.argv) not in [3, 4]:
    print 'usage: send_queue.py <operations_dump_file> <requests_to_send_count> [<item_number_to_send>]'
    sys.exit(1)

operations_file = sys.argv[1]
send_count = int(sys.argv[2])
if len(sys.argv) == 4:
    send_item = send_count - 1
    send_count = int(sys.argv[3])
else:
    send_item = -1

fp = open(operations_file)
(items, line_len) = read_header(fp)

envelopes = read_values(fp, items, line_len, send_count + send_item + 1)

# curl -d '{"ACTION_TYPE": "1", "OPERATION_SIZE": "1", "USEFUL_NEW_MESSAGES": "0", "MID": "2170000000022176816", "UNAME": "31268316", "PART": "3", "FLAGS": "119144468", "FID": "2170001130000005787", "LCN": "92027", "OPERATION_ID": "6026603", "SESSION_KEY": "", "FRESH_COUNT": "14"}' 'http://hampers.yandex.ru:31080/notify' -i ; echo
if envelopes:
    print "ready, let's send it!"
    queue_ids = {}
    for i in range(0, min(send_count + send_item, len(envelopes))):
        if send_item > -1 and i < send_item:
            continue
        try:
            current = envelopes[i]
            shard_id = int(current["UNAME"]) % 65536
            if not shard_id in queue_ids:
                queue_ids[shard_id] = send_item + 1
            queue_ids[shard_id] += 1
            current['UID'] = current["UNAME"]
            headers_ = {'ZooQueueId' : str(queue_ids[shard_id]),
                       'ZooShardId' : str(shard_id)}
            print headers_
            req = urllib2.Request(
                    url = 'http://hampers.yandex.ru:31080/notify',
                    data = json.dumps(current),
                    headers = headers_
                )

            make_request(req)

        except urllib2.URLError, er:
            print 'send error:', er
            print er.headers
