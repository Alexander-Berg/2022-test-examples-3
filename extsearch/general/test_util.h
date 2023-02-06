#pragma once

#include <google/protobuf/text_format.h>

namespace NVideoTestUtil {

    template<class TFactory>
    auto FromProtoConfig(const TString& configString) {
        typename TFactory::TProtoConfig config;
        Y_ENSURE(google::protobuf::TextFormat::ParseFromString(configString, &config));
        return TFactory::Instance()(config);
    }

} //namespace NVideoTestUtil
