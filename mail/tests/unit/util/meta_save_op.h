#pragma once

#include <mail/notsolitesrv/src/meta_save_op/types/response.h>

namespace NNotSoLiteSrv::NMetaSaveOp {

inline bool operator==(const TAutoReply& left, const TAutoReply& right) {
    return (left.address == right.address) && (left.body == right.body);
}

inline bool operator==(const TForward& left, const TForward& right) {
    return left.address == right.address;
}

inline bool operator==(const TNotify& left, const TNotify& right) {
    return left.address == right.address;
}

inline bool operator==(const TResolvedFolder& left, const TResolvedFolder& right) {
    return (left.fid == right.fid) && (left.name == right.name) && (left.type == right.type) &&
        (left.type_code == right.type_code);
}

inline bool operator==(const TResolvedLabel& left, const TResolvedLabel& right) {
    return (left.lid == right.lid) && (left.symbol == right.symbol);
}

inline bool operator==(const TMdbCommitResponse& left, const TMdbCommitResponse& right) {
    return
        (left.uid == right.uid) &&
        (left.status == right.status) &&
        (left.description == right.description) &&
        (left.mid == right.mid) &&
        (left.imap_id == right.imap_id) &&
        (left.duplicate == right.duplicate) &&
        (left.tid == right.tid) &&
        (left.folder == right.folder) &&
        (left.labels == right.labels);
}

}
