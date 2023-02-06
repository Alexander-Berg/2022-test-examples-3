#pragma once

#include <util/generic/string.h>
#include <util/stream/str.h>
#include <google/protobuf/util/json_util.h>
#include <google/protobuf/util/message_differencer.h>
#include <google/protobuf/message.h>
#include <google/protobuf/stubs/status.h>
#include <google/protobuf/io/zero_copy_stream_impl_lite.h>

namespace NRasp {
    namespace NDumper {
        TString MessageToJson(const ::google::protobuf::Message& message);

        template <class T>
        TString ToJson(const TVector<T>& items) {
            bool isFirst = true;
            TStringStream s;
            s << "[";
            for (const auto& message : items) {
                if (!isFirst) {
                    s << ", ";
                }

                s << MessageToJson(message);
                isFirst = false;
            }
            s << ']';
            return s.Str();
        }
    }
}
