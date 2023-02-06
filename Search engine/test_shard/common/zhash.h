#pragma once

#include <kernel/urlid/doc_handle.h>

using TDocHash = ui64;

inline TString GetDocZHash(TDocHash hash) {
    TDocHandle handle(hash);
    handle.SetUnique(true);
    return handle.ToString(TDocHandle::PrintHashOnly);
}
