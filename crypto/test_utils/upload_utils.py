from copy import deepcopy
import json
from operator import itemgetter

from crypta.cm.services.common.test_utils import (
    fields,
    id_utils,
)


def serialize_upload_body(ext_id, matched_ids, track_back_reference=None):
    ids = [id_utils.id_to_dict(matched_id.Id, matched_id.Attributes) for matched_id in matched_ids]

    body = {
        fields.EXT_ID: {
            fields.TYPE: ext_id.Type,
            fields.VALUE: ext_id.Value
        },
        fields.IDS: ids
    }

    if track_back_reference is not None:
        body[fields.TRACK_BACK_REFERENCE] = bool(track_back_reference)

    return json.dumps(body)


def get_sorted_ids(ids):
    return sorted(ids, key=itemgetter(fields.TYPE, fields.VALUE))


def get_reference_forward_refs(matched_ids):
    return get_sorted_ids([id_utils.id_to_dict(matched_id.Id, matched_id.Attributes, cas=0) for matched_id in matched_ids])


def get_reference_back_refs(ext_ids, matched_id):
    return get_sorted_ids([id_utils.id_to_dict(ext_id, matched_id.Attributes, cas=0) for ext_id in ext_ids])


def strip_match_ts_and_sort(ids):
    result = deepcopy(ids)
    for matched_id in result:
        if fields.MATCH_TS in matched_id:
            del matched_id[fields.MATCH_TS]
    return get_sorted_ids(result)
