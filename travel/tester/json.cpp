#include "json.h"

namespace NRasp {
    namespace NDumper {
        TString MessageToJson(const ::google::protobuf::Message& message) {
            TString jsonString;
            NProtoBuf::util::JsonOptions options;
            options.always_print_primitive_fields = true;
            options.preserve_proto_field_names = true;
            NProtoBuf::util::MessageToJsonString(message, &jsonString, options);
            return jsonString;
        }
    }
}
