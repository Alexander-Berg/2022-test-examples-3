#pragma once

#include <travel/rasp/rasp_data/dumper/lib/linguistic_provider/linguistic_provider.h>

#include <library/cpp/geobase/lookup.hpp>

#include <util/generic/map.h>

namespace NRasp {
    namespace NDumper {
        class TFakeLinguisticProvider: public ILinguisticProvider {
            using Key = std::pair<i32, TString>;

        public:
            NGeobase::TLinguistics GetLinguistics(i32 id, const TString& lang) const;
            void AddLinguistics(const NGeobase::TLinguistics& linguistic, const Key& key);

        private:
            TMap<Key, NGeobase::TLinguistics> ByKey_;
        };
    }
}
