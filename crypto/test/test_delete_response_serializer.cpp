#include <crypta/styx/services/api/lib/logic/common/response/delete_response.pb.h>
#include <crypta/styx/services/api/lib/logic/common/response_serializers/delete_response_serializer.h>

#include <util/generic/string.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NStyx::NApi;

TEST(NDeleteResponseSerializer, Ok) {
    TDeleteResponse response;
    response.SetStatus("ok");

    EXPECT_EQ(R"({"status":"ok"})", NDeleteResponseSerializer::ToString(response));
}

TEST(NDeleteResponseSerializer, Error) {
    TDeleteResponse response;
    response.SetStatus("error");

    auto* error = response.AddErrors();
    error->SetCode("internal_code");
    error->SetMessage("something bad happened");

    const TString refResponse = R"({"status":"error","errors":[{"code":"internal_code","message":"something bad happened"}]})";
    EXPECT_EQ(refResponse, NDeleteResponseSerializer::ToString(response));
}
