#include "shard_info.h"

#include <util/folder/path.h>
#include <util/stream/file.h>
#include <util/string/builder.h>

TShardInfo::TShardInfo(const TString& path)
    : ShardDir_(path)
{
    ParseStampTag();
    ParseDocCount();
}

EShardTier TShardInfo::GetTier() const {
    return FromString(SafeGetValue("SearchZone"));
}

const TString& TShardInfo::GetName() const {
    return SafeGetValue("ShardName");
}

const TString& TShardInfo::GetPath() const {
    return ShardDir_;
}

size_t TShardInfo::GetDocs() const {
    return DocCount_;
}

void TShardInfo::ParseStampTag() {
    TFileInput stamp(JoinFsPaths(ShardDir_, "stamp.TAG"));
    TString line;
    while (stamp.ReadLine(line)) {
        ParseLine(line);
    }
}

void TShardInfo::ParseDocCount() {
    TFileInput docCount(JoinFsPaths(ShardDir_, "jupiter-doccount.txt"));
    docCount >> DocCount_;
}

void TShardInfo::ParseLine(TStringBuf line) {
    TStringBuf key;
    TStringBuf value;
    line.Split('=', key, value);
    KeyValue_.emplace(key, value);
}

const TString& TShardInfo::SafeGetValue(TStringBuf key) const {
    Y_ENSURE(KeyValue_.contains(key), TStringBuilder() << "There is no line with key " << key << " in stamp.TAG");
    return KeyValue_.at(key);
}
