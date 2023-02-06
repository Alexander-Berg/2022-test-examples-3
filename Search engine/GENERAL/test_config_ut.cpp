#include <search/web/rearrange/prs_log/logger/proto/config.pb.h>

#include <kernel/web_factors_info/factors_gen.h>
#include <kernel/web_meta_factors_info/factors_gen.h>
#include <library/cpp/protobuf/util/pb_io.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/registar.h>

namespace {
    bool SliceFactorNamesCorrect(const TLoggedSlice& sliceInfo, const NFactorSlices::EFactorSlice sliceName) {
        if (sliceInfo.FactorNamesSize()) {
            TFactorStorage storage;
            const TFactorDomain& domain = storage.GetDomain();

            for (size_t i = 0; i < sliceInfo.FactorNamesSize(); ++i) {
                const TString factorName = sliceInfo.GetFactorNames(i);

                NFactorSlices::TFactorIndex factorIdx = -1;
                if (!domain.TryGetRelativeIndexByName(sliceName, factorName, factorIdx)) {
                    Cerr << "Wrong factor name: " << factorName << ". The corresponding index is " << factorIdx << " which cannot be logged\n";
                    return false;
                }
            }
        }
        return true;
    }
}

Y_UNIT_TEST_SUITE(TestConfigSuite) {
    const TVector<TString> configs = {"/web_config"};

    Y_UNIT_TEST(LogProbabilitiesTest) {
        for (const TString& configName : configs) {
            TLoggerConfig loggerConfig;
            TStringStream content = NResource::Find(configName);
            ParseFromTextFormat(content, loggerConfig);

            float sum = 0.f;
            for (size_t i = 0; i < loggerConfig.LogConfigsSize(); ++i) {
                const float prob = loggerConfig.GetLogConfigs(i).GetLogProbability();
                UNIT_ASSERT(prob >= 0.f);
                sum += prob;
            }
            UNIT_ASSERT(sum <= 1.f);
        }
    }

    Y_UNIT_TEST(FactorNamesTest) {
        for (const TString& configName : configs) {
            TLoggerConfig loggerConfig;
            TStringStream content = NResource::Find(configName);
            ParseFromTextFormat(content, loggerConfig);

            for (size_t i = 0; i < loggerConfig.LogConfigsSize(); ++i) {
                const TLogConfig& config = loggerConfig.GetLogConfigs(i);

                UNIT_ASSERT(SliceFactorNamesCorrect(config.GetWebFactors(), NFactorSlices::EFactorSlice::WEB_PRODUCTION));
                UNIT_ASSERT(SliceFactorNamesCorrect(config.GetWebMetaFactors(), NFactorSlices::EFactorSlice::WEB_META));
                UNIT_ASSERT(SliceFactorNamesCorrect(config.GetRapidClicksFactors(), NFactorSlices::EFactorSlice::RAPID_CLICKS));
            }
        }
    }
};
