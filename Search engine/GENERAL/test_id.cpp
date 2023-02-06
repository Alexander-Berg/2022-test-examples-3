#include "test_id.h"

#include <util/generic/strbuf.h>

#include <array>

namespace {
    constexpr std::array<TStringBuf, 12> USEFULL_FIELDS = {{
        TStringBuf("query"),
        TStringBuf("fullRequest"),
        TStringBuf("numDocs"),
        TStringBuf("timestamp"),
        TStringBuf("requestId"),
        TStringBuf("experiments"),
        TStringBuf("user_uid"),
        TStringBuf("referer"),
        TStringBuf("searchProps"),
        TStringBuf("pageNo"),
        TStringBuf("uiLanguage"),
    }};
}

namespace NTestId {

    void TMapper::Do(NYT::TTableReader<NYT::TNode>* reader, NYT::TTableWriter<NYT::TNode>* writer) {
        for (; reader->IsValid(); reader->Next()) {
            const auto& row = reader->GetRow();
            if (AnyOf(row["experiments"].AsList(), [&](const auto& it) { return IsIn(TestIds_, it["testId"].AsString()); })) {
                NYT::TNode out;
                for (TStringBuf usefull : USEFULL_FIELDS) {
                    if (row.HasKey(usefull)) {
                        out[usefull] = row[usefull];
                    }
                }
                if (!row.HasKey("user_uid")) {
                    out["user_uid"] = row["userId"]["yandexUid"];
                }
                writer->AddRow(out);
            }
        }
    }

} // NTestId

REGISTER_MAPPER(NTestId::TMapper);
