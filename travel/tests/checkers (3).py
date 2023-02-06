#!/usr/bin/env python
# encoding: utf-8

import urlparse
import base64
import random
import string


from travel.hotels.proto2 import label_pb2
from google.protobuf.descriptor import FieldDescriptor


def check_location(resp, host, label_param='label', path='/', token=None, params=None, https=True):
    assert resp.status_code == 302
    loc = resp.headers['Location']
    print("Location: %s" % loc)
    o = urlparse.urlsplit(loc)
    assert o.scheme == 'https' if https else 'http'
    assert o.netloc == host
    assert o.path == path
    qp = urlparse.parse_qs(o.query)
    if label_param is not None:
        label_hash = qp.get(label_param)[0]
        assert label_hash  # Label is hash, and cannot be properly checked
        assert len(label_hash) > 10
    else:
        label_hash = None
    if token is None:
        assert 'token' not in qp
    else:
        assert qp['token'][0] == token
    if params is not None:
        for k, v in params.iteritems():
            assert qp[k][0] == v
    return label_hash


def check_pricecheck_reqs(reqs, expected_offer_ids):
    assert len(reqs) == len(expected_offer_ids)
    for r, oid in zip(reqs, expected_offer_ids):
        assert r.OfferId == oid


def check_no_reqans_records(redir_app):
    assert not redir_app.read_reqans_new_records()


def decode_proto_label(proto_str):
    proto_str = str(proto_str)  # No unicode!
    missing_padding = len(proto_str) % 4
    if missing_padding:
        proto_str += '=' * (4 - missing_padding)
    l = label_pb2.TLabel()
    l.ParseFromString(base64.urlsafe_b64decode(proto_str))
    return l


def to_string(v):
    if isinstance(v, bool):
        return 'true' if v else 'false'
    return str(v)


def check_reqans_record(redir_app, target_url, label_hash, to_check):
    act_recs = redir_app.read_reqans_new_records()
    assert len(act_recs) == 1
    rec = act_recs[0]
    assert rec['TargetUrl'] == target_url
    assert rec['Label'] == label_hash
    pb_label = decode_proto_label(rec['Proto'])
    for f in label_pb2.TLabel.DESCRIPTOR.fields:
        try:
            if not f.name:  # Means deprecated field
                continue
            act_val_from_map = rec['FieldsMap'][f.name]
            act_val_from_proto = getattr(pb_label, f.name)
            if f.label == f.LABEL_REPEATED:
                assert act_val_from_map == ','.join(to_string(v) for v in act_val_from_proto)
            else:
                assert act_val_from_map == to_string(act_val_from_proto)
            v_to_check = to_check[f.name]
            assert act_val_from_proto == v_to_check
        except Exception as e:
            raise Exception("Failed to check field ('%s'): %s" % (f.name, str(e)))


def random_string():
    return ''.join([random.choice(string.ascii_letters + string.digits) for n in range(8)])


def generate_one_value(ftype):
    if ftype is FieldDescriptor.TYPE_BOOL:
        return random.randint(0, 2) > 0
    if ftype is FieldDescriptor.TYPE_STRING:
        return random_string()
    return random.randint(0, 100)


def generate_proto_label(**kwargs):
    label = label_pb2.TLabel()
    to_check = {}

    for f in label_pb2.TLabel.DESCRIPTOR.fields:
        if not f.name:
            continue
        v = kwargs.get(f.name)
        if v is None:
            if f.label == f.LABEL_REPEATED:
                v = [generate_one_value(f.type) for x in xrange(random.randint(5, 20))]
                getattr(label, f.name).extend(v)  # cannot assign to proto lists
            else:
                v = generate_one_value(f.type)
                setattr(label, f.name, v)
        else:
            setattr(label, f.name, v)
        to_check[f.name] = v
    return label, to_check
