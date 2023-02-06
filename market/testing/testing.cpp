namespace {
    bool isTestingFeaturesEnabled = false;
}

namespace NMarketReport {
    namespace NGlobal {
        void SetTestingFeaturesEnabled(bool enabled) {
            isTestingFeaturesEnabled = enabled;
        }

        bool IsTestingFeaturesEnabled() {
            return isTestingFeaturesEnabled;
        }
    }
}
