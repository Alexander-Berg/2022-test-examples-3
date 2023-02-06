#pragma once

#include <gmock/gmock.h>
#include <internal/storage/service.h>

namespace mail_getter {
namespace storage {

class ServiceMock: public Service {
public:
    MOCK_METHOD(void, asyncGetBlob, (const Stid&, OnGetBlob), (const, override));
    MOCK_METHOD(void, asyncPutBlob, (const std::string&, const std::string&, std::chrono::seconds, OnPutBlob), (const, override));
    MOCK_METHOD(void, asyncGetXml, (const Stid&, OnGetXml), (const, override));
    MOCK_METHOD(void, asyncGetByRange, (const Stid&, const Range&, OnGetBlob), (const, override));
};

using ServiceMockPtr = std::shared_ptr<ServiceMock>;

} // namespace storage
} // namespace mail_getter
