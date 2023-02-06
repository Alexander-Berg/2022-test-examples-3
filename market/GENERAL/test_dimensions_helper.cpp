#include <market/idx/delivery/lib/dimensions_helper/dimensions_helper.h>

#include <util/generic/fwd.h>
#include <util/folder/path.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

int weightFromMDM = 6000000;
int lengthFromMDM = 70000;
int widthFromMDM = 80000;
int heightFromMDM = 90000;

int weightFromFeed = 1000;
int lengthFromFeed = 20000;
int widthFromFeed = 30000;
int heightFromFeed = 40000;

int weightFromUC = 10;
int lengthFromUC = 20;
int widthFromUC = 30;
int heightFromUC = 40;

int weightFromDC = 100;
int lengthFromDC = 200;
int widthFromDC = 300;
int heightFromDC = 400;

NDimensionsHelper::TDimensionsSourceCounter dimensionsSourceCounter;

NDimensionsHelper::TAverageOfferDimensionsAndWeight CreateFullFromDC() {
    NDimensionsHelper::TAverageOfferDimensionsAndWeight fullAverageOfferDimensionsAndWeight;
    fullAverageOfferDimensionsAndWeight.Weight = weightFromDC;
    fullAverageOfferDimensionsAndWeight.Length = lengthFromDC;
    fullAverageOfferDimensionsAndWeight.Width = widthFromDC;
    fullAverageOfferDimensionsAndWeight.Height = heightFromDC;
    return fullAverageOfferDimensionsAndWeight;
}

TDatacampOffer FillMasterDataDimension(TDatacampOffer& datacampOffer) {
    datacampOffer.mutable_content()->mutable_master_data()->mutable_weight_gross()->set_value_mg(weightFromMDM);
    datacampOffer.mutable_content()->mutable_master_data()->mutable_dimensions()->set_length_mkm(lengthFromMDM);
    datacampOffer.mutable_content()->mutable_master_data()->mutable_dimensions()->set_width_mkm(widthFromMDM);
    datacampOffer.mutable_content()->mutable_master_data()->mutable_dimensions()->set_height_mkm(heightFromMDM);
    return datacampOffer;
}

TDatacampOffer FillFromUC(TDatacampOffer& datacampOffer) {
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_weight(weightFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_length(lengthFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_width(widthFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_height(heightFromUC);
    return datacampOffer;
}

NCategoriesDimensions::TCategoriesDimensions GetDefaultCategoriesDimensions() {
    TString categoriesDimensionsFilePath = JoinFsPaths(ArcadiaSourceRoot(), "market/idx/delivery/lib/dimensions_helper/ut/data/categories_dimensions.csv");
    NCategoriesDimensions::TCategoriesDimensions categoriesDimensions(categoriesDimensionsFilePath);
    return categoriesDimensions;
}

NCategoriesDimensions::TCategoriesDimensions GetBadCategoriesDimensions() {
    TString categoriesDimensionsFilePath = JoinFsPaths(ArcadiaSourceRoot(), "market/idx/delivery/lib/dimensions_helper/ut/data/bad_categories_dimensions.csv");
    NCategoriesDimensions::TCategoriesDimensions categoriesDimensions(categoriesDimensionsFilePath);
    return categoriesDimensions;
}

// Проверяем корректность заполнения веса для запроса в зависимости от полей в оффере:
// новое поле (value_mg) vs старое поле (grams)
// (см на делитель в ассерте)
TEST(SetWeightAndDimensionsByPriority, WeightFromOldOrNewField) {
    auto categoriesDimensions = GetDefaultCategoriesDimensions();
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight;

    // кейс_1: заполнено и старое, и новое - отправим из нового
    delivery_calc::mbi::Offer offer_1;
    TDatacampOffer datacampOffer_1;
    datacampOffer_1.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_grams(weightFromFeed);
    datacampOffer_1.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_value_mg(weightFromFeed);

    SetWeightByPriority(offer_1, datacampOffer_1, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_EQ(weightFromFeed/1000000., offer_1.weight());

    // кейс_2: заполнено только старое - используем данные из него
    delivery_calc::mbi::Offer offer_2;
    TDatacampOffer datacampOffer_2;
    datacampOffer_2.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_grams(weightFromFeed);

    SetWeightByPriority(offer_2, datacampOffer_2, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_EQ(weightFromFeed/1000., offer_2.weight());
}

// Есть вес из фида, UC и DC, будет использован из фида
// Нет всех габаритов из фида, есть полный комплект из UC - будут взят из UC
TEST(SetWeightAndDimensionsByPriority, ComboWeightFromFeedDimensionsFromUC) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight;
    auto categoriesDimensions = GetDefaultCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_grams(weightFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_length_mkm(lengthFromFeed);

    FillFromUC(datacampOffer);

    averageOfferDimensionsAndWeight.Weight = weightFromDC;

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_EQ(weightFromFeed/1000, offer.weight());
    ASSERT_EQ(lengthFromUC, offer.length());
    ASSERT_EQ(widthFromUC, offer.width());
    ASSERT_EQ(heightFromUC, offer.height());
}

// Нет веса из фида, есть из UC и DC, будет использован из UC
// Есть полный набор габаритов из фида и из UC, от DC - будет использован из фида
TEST(SetWeightAndDimensionsByPriority, ComboWeightFromUCDimensionsFromFeed) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    auto categoriesDimensions = GetDefaultCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_length_mkm(lengthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_width_mkm(widthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_height_mkm(heightFromFeed);

    FillFromUC(datacampOffer);
    auto averageOfferDimensionsAndWeight = CreateFullFromDC();

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_EQ(weightFromUC, offer.weight());
    ASSERT_EQ(lengthFromFeed/10000, offer.length());
    ASSERT_EQ(widthFromFeed/10000, offer.width());
    ASSERT_EQ(heightFromFeed/10000, offer.height());
}

// Вес от UC и вес от DC - будет использован от UC
// Габариты только от DC - будут взяты они.
TEST(SetWeightAndDimensionsByPriority, ComboWeightFromUCDimensionsFromDC) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    auto categoriesDimensions = GetDefaultCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_weight(weightFromUC);
    auto averageOfferDimensionsAndWeight = CreateFullFromDC();

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_EQ(weightFromUC, offer.weight());
    ASSERT_EQ(lengthFromDC, offer.length());
    ASSERT_EQ(widthFromDC, offer.width());
    ASSERT_EQ(heightFromDC, offer.height());
}

// Вес от UC и вес от DC - будет использован от UC
// Габариты от UC и от DC, но не полные комплекты - никакие не будут взяты.
TEST(SetWeightAndDimensionsByPriority, WeightFromUCEmptyDimensions) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight;
    auto categoriesDimensions = GetDefaultCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_weight(weightFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_width(widthFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_height(heightFromUC);

    averageOfferDimensionsAndWeight.Weight = weightFromDC;
    averageOfferDimensionsAndWeight.Length = lengthFromDC;
    averageOfferDimensionsAndWeight.Width = widthFromDC;

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_EQ(weightFromUC, offer.weight());
    ASSERT_TRUE(!offer.has_length());
    ASSERT_TRUE(!offer.has_width());
    ASSERT_TRUE(!offer.has_height());
}

TEST(SetWeightAndDimensionsByPriority, AllFromCategoriesDimensionsFile) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight = NDimensionsHelper::TAverageOfferDimensionsAndWeight();
    auto categoriesDimensions = GetDefaultCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(90407);

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_EQ(0.123, offer.weight());
    ASSERT_EQ(111, offer.length());
    ASSERT_EQ(222, offer.width());
    ASSERT_EQ(333, offer.height());
}

TEST(SetWeightAndDimensionsByPriority, CategoryNotInCategoriesDimensionsFile) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight = NDimensionsHelper::TAverageOfferDimensionsAndWeight();
    auto categoriesDimensions = GetDefaultCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(12345);

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_TRUE(!offer.has_weight());
    ASSERT_TRUE(!offer.has_length());
    ASSERT_TRUE(!offer.has_width());
    ASSERT_TRUE(!offer.has_height());
}

TEST(SetWeightAndDimensionsByPriority, BadCategoriesDimensionsFile) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight = NDimensionsHelper::TAverageOfferDimensionsAndWeight();
    auto categoriesDimensions = GetBadCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(90407);

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_TRUE(!offer.has_weight());
    ASSERT_TRUE(!offer.has_length());
    ASSERT_TRUE(!offer.has_width());
    ASSERT_TRUE(!offer.has_height());
}

TEST(SetWeightAndDimensionsByPriority, BadPathToCategoriesDimensionsFile) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight = NDimensionsHelper::TAverageOfferDimensionsAndWeight();
    NCategoriesDimensions::TCategoriesDimensions categoriesDimensions("bad/path");

    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->set_category_id(90407);

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);
    ASSERT_TRUE(!offer.has_weight());
    ASSERT_TRUE(!offer.has_length());
    ASSERT_TRUE(!offer.has_width());
    ASSERT_TRUE(!offer.has_height());
}

// Когда заполнены полностью данные из фида и UC - UseDimensionsFromUC вернет true
TEST(UseDimensionsFromUC, BothFull) {
    TDatacampOffer datacampOffer;

    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_grams(weightFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_length_mkm(lengthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_width_mkm(widthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_height_mkm(heightFromFeed);

    FillFromUC(datacampOffer);

    bool result = NDimensionsHelper::UseDimensionsFromUC(datacampOffer);
    ASSERT_EQ(true, result);
}

// Если UC не вернул один габарит, а в фиде этот габарит заполнен - UseDimensionsFromUC вернет false
TEST(UseDimensionsFromUC, UCnotFullFeedFull) {
    TDatacampOffer datacampOffer;

    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_grams(weightFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_length_mkm(lengthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_width_mkm(widthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_height_mkm(heightFromFeed);

    // нет length
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_weight(weightFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_width(widthFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_height(heightFromUC);

    bool result = NDimensionsHelper::UseDimensionsFromUC(datacampOffer);
    ASSERT_EQ(false, result);
}

// Если UC не вернул один габарит, и в фиде этого габарита не было UseDimensionsFromUC вернет true
TEST(UseDimensionsFromUC, UCnotFullFeedNotFull) {
    TDatacampOffer datacampOffer;

    // нет length
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_grams(weightFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_width_mkm(widthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_height_mkm(heightFromFeed);

    // нет length
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_weight(weightFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_width(widthFromUC);
    datacampOffer.mutable_content()->mutable_market()->mutable_enriched_offer()->mutable_dimensions()->set_height(heightFromUC);

    bool result = NDimensionsHelper::UseDimensionsFromUC(datacampOffer);
    ASSERT_EQ(true, result);
}

// Брать вес и габариты из мастер-данных, если они есть (приоритет 1)
TEST(SetWeightAndDimensionsByPriority, PreferDimensionsFromMasterData) {
    delivery_calc::mbi::Offer offer;
    TDatacampOffer datacampOffer;
    NDimensionsHelper::TAverageOfferDimensionsAndWeight averageOfferDimensionsAndWeight;
    auto categoriesDimensions = GetDefaultCategoriesDimensions();

    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_weight()->set_grams(weightFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_length_mkm(lengthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_width_mkm(widthFromFeed);
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions()->set_height_mkm(heightFromFeed);

    FillFromUC(datacampOffer);
    FillMasterDataDimension(datacampOffer);

    SetWeightAndDimensionsByPriority(offer, datacampOffer, averageOfferDimensionsAndWeight, categoriesDimensions, dimensionsSourceCounter);

    ASSERT_EQ(weightFromMDM/1000000., offer.weight());
    ASSERT_EQ(lengthFromMDM/10000., offer.length());
    ASSERT_EQ(widthFromMDM/10000., offer.width());
    ASSERT_EQ(heightFromMDM/10000., offer.height());
}
