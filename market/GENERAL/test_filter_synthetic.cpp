#include <market/idx/datacamp/lib/processors/filter_synthetic/filter_synthetic.h>
#include <library/cpp/testing/gtest/gtest.h>

TEST(FilterSynthetic, Empty) {
    auto message = NDataCamp::FilterSyntheticOffers({});
    EXPECT_TRUE(message.united_offers().empty());
}

TEST(FilterSyntetic, SyntheticBasic) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_basic()->mutable_meta()->set_synthetic(true);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_TRUE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, NaturalBasic) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_basic()->mutable_meta()->set_synthetic(false);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_FALSE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, SyntheticService) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_service()->insert({1, {}});
    offer.mutable_service()->at(1).mutable_meta()->set_synthetic(true);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_TRUE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, NaturalService) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_service()->insert({1, {}});
    offer.mutable_service()->at(1).mutable_meta()->set_synthetic(false);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_FALSE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, SyntheticActual) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_actual()->insert({1, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->insert({2, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->at(2).mutable_meta()->set_synthetic(true);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_TRUE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, NaturalActual) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_actual()->insert({1, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->insert({2, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->at(2).mutable_meta()->set_synthetic(false);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_FALSE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, FilterFullSynthetic) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_basic()->mutable_meta()->set_synthetic(true);
    offer.mutable_service()->insert({1, {}});
    offer.mutable_service()->at(1).mutable_meta()->set_synthetic(true);
    offer.mutable_actual()->insert({1, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->insert({2, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->at(2).mutable_meta()->set_synthetic(true);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_TRUE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, NaturalBasicFilterSynthetic) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_basic()->mutable_meta()->set_synthetic(false);
    offer.mutable_service()->insert({1, {}});
    offer.mutable_service()->at(1).mutable_meta()->set_synthetic(true);
    offer.mutable_actual()->insert({1, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->insert({2, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->at(2).mutable_meta()->set_synthetic(true);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_TRUE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, NaturalServiceFilterSynthetic) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_basic()->mutable_meta()->set_synthetic(true);
    offer.mutable_service()->insert({1, {}});
    offer.mutable_service()->at(1).mutable_meta()->set_synthetic(false);
    offer.mutable_service()->insert({10, {}});
    offer.mutable_service()->at(10).mutable_meta()->set_synthetic(true);
    offer.mutable_actual()->insert({1, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->insert({2, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->at(2).mutable_meta()->set_synthetic(true);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_TRUE(filteredMessage.united_offers().empty());
}

TEST(FilterSyntetic, NaturalActualFilterSynthetic) {
    Market::DataCamp::UnitedOffer offer;
    offer.mutable_basic()->mutable_meta()->set_synthetic(true);
    offer.mutable_service()->insert({1, {}});
    offer.mutable_service()->at(1).mutable_meta()->set_synthetic(true);
    offer.mutable_actual()->insert({1, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->insert({2, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->at(2).mutable_meta()->set_synthetic(false);
    offer.mutable_actual()->at(1).mutable_warehouse()->insert({3, {}});
    offer.mutable_actual()->at(1).mutable_warehouse()->at(3).mutable_meta()->set_synthetic(true);
    Market::DataCamp::API::DatacampMessage message;
    *message.add_united_offers()->add_offer() = offer;
    Market::DataCamp::API::DatacampMessage filteredMessage = NDataCamp::FilterSyntheticOffers(std::move(message));
    EXPECT_TRUE(filteredMessage.united_offers().empty());
}
