#pragma once

#include <mail/notsolitesrv/src/mdbsave/types/actions.h>
#include <mail/notsolitesrv/src/mdbsave/types/attachment.h>
#include <mail/notsolitesrv/src/mdbsave/types/folders.h>
#include <mail/notsolitesrv/src/mdbsave/types/mime_part.h>
#include <mail/notsolitesrv/src/mdbsave/types/response.h>
#include <mail/notsolitesrv/src/mdbsave/types/thread_info.h>

namespace NNotSoLiteSrv::NMdbSave {

inline bool operator==(const TDuplicates& left, const TDuplicates& right) {
    return (left.Ignore == right.Ignore) && (left.Remove == right.Remove);
}

inline bool operator==(const TFolderActions& left, const TFolderActions& right) {
    return (left.StoreAsDeleted == right.StoreAsDeleted) && (left.NoSuchFolder == right.NoSuchFolder);
}

inline bool operator==(const TAttachment& left, const TAttachment& right) {
    return (left.Hid == right.Hid) && (left.Name == right.Name) && (left.Type == right.Type) &&
        (left.Size == right.Size);
}

inline bool operator==(const TPath& left, const TPath& right) {
    return (left.Path == right.Path) && (left.Delimeter == right.Delimeter);
}

inline bool operator==(const TRequestFolder& left, const TRequestFolder& right) {
    return (left.Fid == right.Fid) && (left.Path == right.Path);
}

inline bool operator==(const TRequestFolders& left, const TRequestFolders& right) {
    return (left.Destination == right.Destination) && (left.Original == right.Original);
}

inline bool operator==(const TMimePart& left, const TMimePart& right) {
    return (
        (left.Hid == right.Hid) &&
        (left.ContentType == right.ContentType) &&
        (left.ContentSubtype == right.ContentSubtype) &&
        (left.Boundary == right.Boundary) &&
        (left.Name == right.Name) &&
        (left.Charset == right.Charset) &&
        (left.Encoding == right.Encoding) &&
        (left.ContentDisposition == right.ContentDisposition) &&
        (left.FileName == right.FileName) &&
        (left.ContentId == right.ContentId) &&
        (left.Offset == right.Offset) &&
        (left.Length == right.Length));
}

inline bool operator==(const TErrorResponse& left, const TErrorResponse& right) {
    return (left.Error == right.Error) && (left.Message == right.Message);
}

inline bool operator==(const TResolvedFolder& left, const TResolvedFolder& right) {
    return left.Fid == right.Fid
        && left.Name == right.Name
        && left.Type == right.Type
        && left.TypeCode == right.TypeCode;
}

inline bool operator==(const TResponseLabel& left, const TResponseLabel& right) {
    return (left.Lid == right.Lid) && (left.Symbol == right.Symbol);
}

inline bool operator==(const TMdbSaveResponseRcpt& left, const TMdbSaveResponseRcpt& right) {
    return left.Uid == right.Uid
        && left.Status == right.Status
        && left.Description == right.Description
        && left.ImapId == right.ImapId
        && left.Tid == right.Tid
        && left.Duplicate == right.Duplicate
        && left.Folder == right.Folder
        && left.Labels == right.Labels;
}

inline bool operator==(const TMdbSaveResponseRcptNode& left, const TMdbSaveResponseRcptNode& right) {
    return (left.Id == right.Id) && (left.Rcpt == right.Rcpt);
}

inline bool operator==(const TMdbSaveResponse& left, const TMdbSaveResponse& right) {
    return left.Rcpts == right.Rcpts;
}

inline bool operator==(const TRequestLabel& left, const TRequestLabel& right) {
    return (left.Name == right.Name) && (left.Type == right.Type);
}

inline bool operator==(const TThreadHash& left, const TThreadHash& right) {
    return (left.Namespace == right.Namespace) && (left.Value == right.Value);
}

inline bool operator==(const TThreadLimits& left, const TThreadLimits& right) {
    return (left.Days == right.Days) && (left.Count == right.Count);
}

inline bool operator==(const TThreadInfo& left, const TThreadInfo& right) {
    return (
        (left.Hash == right.Hash) &&
        (left.Limits == right.Limits) &&
        (left.Rule == right.Rule) &&
        (left.ReferenceHashes == right.ReferenceHashes) &&
        (left.MessageIds == right.MessageIds) &&
        (left.InReplyToHash == right.InReplyToHash) &&
        (left.MessageIdHash == right.MessageIdHash));
}

}
