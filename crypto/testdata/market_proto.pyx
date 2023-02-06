from util.generic.string cimport TString

cdef extern from "google/protobuf/descriptor.h" namespace "NProtoBuf":
    cppclass Descriptor:
        pass

cdef extern from "library/cpp/protobuf/yql/descriptor.h":
    TString GenerateProtobufTypeConfig(const Descriptor*)

cdef extern from "market/lilucrm/platform_config/src/main/proto/models/Order.pb.h" namespace "crm::platform::models":
    cppclass Order:
        @staticmethod
        Descriptor * descriptor()

def get_market_attribute():
    return GenerateProtobufTypeConfig(Order.descriptor())
