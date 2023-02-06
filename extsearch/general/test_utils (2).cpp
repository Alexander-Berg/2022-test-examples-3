#include "test_utils.h"

void ExtractBody(TString& s) {
    const auto pos = s.find("\r\n\r\n");
    if (pos != TString::npos) {
        s.erase(0, pos + 4);
    }
}

TSearchContextHelper::TSearchContextHelper() {
    ON_CALL(Context.MockReqEnv, GetArchiveAccessor(_, _, _))
        .WillByDefault(Return(&DocInfo));
}

TString TSearchContextHelper::GetResponse() const {
    auto collectedData = GetCollectedData();
    ExtractBody(collectedData);
    return collectedData;
}

TString TSearchContextHelper::GetCollectedData() const {
    return Decorator.GetCollectedData();
}
