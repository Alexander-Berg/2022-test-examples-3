#include <market/report/library/external_requester/external_request_history.h>
#include <market/library/ichwill/request.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

class TExternalRequestHistoryTest : public NTesting::TTest {
    void SetUp() override {
        IExternalRequestHistory::Get().InitForTesting("http://production.ichwil.yandex.ru/");
    }

    void TearDown() override {
        IExternalRequestHistory::Get().Cleanup();
    }
};

TEST_F(TExternalRequestHistoryTest, GetResponseFromCache)
{
    const auto request = NMarket::NIchWill::CreateViewedModelsRequest("userId", 100, false, "production.ichwil.yandex.ru", "/viewedModels",  {});
    IExternalRequestHistory::Get().SaveRequest(EExternalService::RECOMMENDER_VIEWED_MODELS, request.Data, "answer_data", 42);
    const auto response = IExternalRequestHistory::Get().GetResponse(EExternalService::RECOMMENDER_VIEWED_MODELS, request.Data);
    ASSERT_TRUE(response.Defined());
    ASSERT_EQ(*response, "answer_data");
}

TEST_F(TExternalRequestHistoryTest, SensitiveForServiceId)
{
    const auto request = NMarket::NIchWill::CreateViewedModelsRequest("userId", 100, false, "production.ichwil.yandex.ru", "/viewedModels",  {});
    IExternalRequestHistory::Get().SaveRequest(EExternalService::RECOMMENDER_VIEWED_MODELS, request.Data, "answer_data", 42);
    const auto response = IExternalRequestHistory::Get().GetResponse(EExternalService::RECOMMENDER_MODELS_OF_INTEREST, request.Data);
    ASSERT_FALSE(response.Defined());
}

TEST_F(TExternalRequestHistoryTest, SensitiveForParam)
{
    {
        const auto cachedRequest = NMarket::NIchWill::CreateViewedModelsRequest("userId", 100, false, "production.ichwil.yandex.ru", "/viewedModels",  {});
        IExternalRequestHistory::Get().SaveRequest(EExternalService::RECOMMENDER_VIEWED_MODELS, cachedRequest.Data, "answer_data", 42);
    }

    const auto request = NMarket::NIchWill::CreateViewedModelsRequest("userId", 500, false, "production.ichwil.yandex.ru", "/viewedModels",  {});
    const auto response = IExternalRequestHistory::Get().GetResponse(EExternalService::RECOMMENDER_VIEWED_MODELS, request.Data);
    ASSERT_FALSE(response.Defined());
}

TEST_F(TExternalRequestHistoryTest, IgnoreHostTest)
{
    {
        const auto productionRequest = NMarket::NIchWill::CreateViewedModelsRequest("userId", 100, false, "production.ichwil.yandex.ru", "/viewedModels",  {});
        IExternalRequestHistory::Get().SaveRequest(EExternalService::RECOMMENDER_VIEWED_MODELS, productionRequest.Data, "answer_data", 42);
    }

    const auto testingRequest = NMarket::NIchWill::CreateViewedModelsRequest("userId", 100, false, "testing.ichwil.yandex.ru", "/viewedModels",  {});
    const auto response = IExternalRequestHistory::Get().GetResponse(EExternalService::RECOMMENDER_VIEWED_MODELS, testingRequest.Data);
    ASSERT_TRUE(response.Defined());
    ASSERT_EQ(*response, "answer_data");
}
