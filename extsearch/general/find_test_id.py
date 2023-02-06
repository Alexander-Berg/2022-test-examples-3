import sys
import json
import urlparse

debug_info_file = sys.argv[1]
requests_file = sys.argv[2]
sub_requests_file = sys.argv[3]
test_id = sys.argv[4]
ts_from = int(sys.argv[5])
ts_to = int(sys.argv[6])

for line in open(sub_requests_file):
    fields = line.strip().split('\t')
    parsed_url = urlparse.urlparse(fields[0])
    url_parts = dict(d.split('=', 1) for d in parsed_url.query.split('&') if '=' in d)
    if "test-ids" in url_parts and test_id in url_parts["test-ids"]:
        ts = int(url_parts["reqid"][:10])
        if ts >= ts_from and ts < ts_to:
            fields += [parsed_url.netloc, parsed_url.path, url_parts["reqid"]]
            print '\t'.join(fields)

