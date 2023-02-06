#pragma once

#include <market/idx/feeds/qparser/inc/processor.h>

#include <library/cpp/testing/unittest/gtest.h>

namespace NMarket {

template<typename TProcessor>
class TestProcessor : public ::testing::Test {
public:
    virtual ~TestProcessor() = default;

protected:
    void SetUp() override {
        Processor_ = MakeHolder<TProcessor>();
    }

    IWriter::TMsgPtr Process(const TFeedInfo& feedInfo, const TFeedShopInfo& feedShopInfo, const IFeedParser::TMsgPtr raw) {
        Processor_->Process(feedInfo, feedShopInfo, raw);
        return raw;
    }

    IWriter::TMsgPtr Process(IFeedParser::TMsgPtr raw) {
        return Process(TFeedInfo{}, TFeedShopInfo{}, raw);
    }

    THolder<TProcessor> Processor_;
};

}  // namespace NMarket
