#include <crypta/styx/services/api/lib/logic/common/response/status_response.pb.h>
#include <crypta/styx/services/api/lib/logic/common/response_serializers/status_response_serializer.h>

#include <util/generic/string.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NStyx::NApi;

TEST(NStatusResponseSerializer, Ok) {
    TStatusResponse response;
    response.SetStatus("ok");

    auto* firstItem = response.AddData();
    firstItem->SetId("10000000002000000000");
    firstItem->SetSlug("bookmarks");
    firstItem->SetState("ready_to_delete");

    auto* secondItem = response.AddData();
    secondItem->SetId("10000000002000000001");
    secondItem->SetSlug("searches");
    secondItem->SetState("delete_in_progress");

    auto* thirdItem = response.AddData();
    thirdItem->SetId("10000000002000000002");
    thirdItem->SetSlug("places");
    thirdItem->SetState("empty");
    thirdItem->SetUpdateDate("2020-12-11T08:11:47.631Z");

    const TString refResponse =
            R"({"status":"ok","data":[)"
                R"({"id":"10000000002000000000","slug":"bookmarks","state":"ready_to_delete","update_date":""},)"
                R"({"id":"10000000002000000001","slug":"searches","state":"delete_in_progress","update_date":""},)"
                R"({"id":"10000000002000000002","slug":"places","state":"empty","update_date":"2020-12-11T08:11:47.631Z"})"
            "]}";

    EXPECT_EQ(refResponse, NStatusResponseSerializer::ToString(response));
}

TEST(NStatusResponseSerializer, Error) {
    TStatusResponse response;
    response.SetStatus("error");

    auto* error = response.AddErrors();
    error->SetCode("internal_code");
    error->SetMessage("something bad happened");

    const TString refResponse = R"({"status":"error","errors":[{"code":"internal_code","message":"something bad happened"}]})";
    EXPECT_EQ(refResponse, NStatusResponseSerializer::ToString(response));
}
