#include "fake_linguistic_provider.h"

namespace NRasp {
    namespace NDumper {
        NGeobase::TLinguistics TFakeLinguisticProvider::GetLinguistics(i32 id, const TString& lang) const {
            Key key(id, lang);
            auto it = ByKey_.find(key);
            if (it == ByKey_.end()) {
                return {};
            }
            return it->second;
        }

        void TFakeLinguisticProvider::AddLinguistics(const NGeobase::TLinguistics& linguistic, const Key& key) {
            ByKey_[key] = linguistic;
        }
    }
}
