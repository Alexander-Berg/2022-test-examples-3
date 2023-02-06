#include <market/idx/offers/lib/iworkers/OfferCtx.h>
#include <market/idx/offers/lib/iworkers/OfferDocumentBuilder.h>

#include <market/idx/offers/processors/model_id_processor/model_id_processor.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>


TEST(TModelIdProcessorTests, ValidateByPermittedList)
{
    TCompactModelValidities map;
    map.insert({ 100, { 100, 0, true, false, false } });
    map.insert({ 110, { 110, 0, true, false, false } });
    map.insert({ 120, { 120, 0, false, false, false } });
    map.Finalize();

    TGlRecord glRecord1;
    glRecord1.add_pictures();
    glRecord1.set_cluster_id(110);   // id from set

    TModelIdProcessor validator(map);
    validator.ProcessOffer(&glRecord1);

    ASSERT_EQ(110, glRecord1.cluster_id());

    TGlRecord glRecord2;
    glRecord2.set_cluster_id(2000);  // id is not from set
    validator.ProcessOffer(&glRecord2);

    ASSERT_EQ(0, glRecord2.cluster_id());

    TGlRecord glRecord3;
    glRecord3.set_model_id(100);    // id from set
    validator.ProcessOffer(&glRecord3);

    ASSERT_EQ(100, glRecord3.model_id());

    glRecord3.set_model_id(3000);  // id is not from set
    validator.ProcessOffer(&glRecord3);

    ASSERT_EQ(0, glRecord3.model_id());

    TGlRecord glRecord4;
    glRecord4.set_model_id(120);  // id banned on the white market
    validator.ProcessOffer(&glRecord4);

    ASSERT_EQ(0, glRecord4.model_id());
}

TEST(TModelIdProcessorTests, ValidateClusterAndZeroModel)
{
    TCompactModelValidities map;
    map.insert({ 100, { 100, 0, true, false, false } });
    map.insert({ 110, { 110, 0, true, false, false } });
    map.Finalize();

    TModelIdProcessor validator(map);

    TGlRecord glRecord;
    glRecord.add_pictures();
    glRecord.set_cluster_id(1716559764);
    glRecord.set_model_id(0);

    validator.ProcessOffer(&glRecord);
    ASSERT_EQ(0, glRecord.cluster_id());
}

TEST(TModelIdProcessorTests, ValidateClusterWithoutPicture)
{
    TCompactModelValidities map;
    map.insert({ 1716559764, { 1716559764, 0, true, false, false } });
    map.Finalize();

    TModelIdProcessor validator(map);

    TGlRecord glRecord;
    glRecord.set_cluster_id(1716559764);
    glRecord.set_model_id(0);

    validator.ProcessOffer(&glRecord);
    ASSERT_EQ(0, glRecord.cluster_id());
}

TEST(TModelIdProcessorTests, ValidateZeroMatchedId)
{
    TCompactModelValidities map;
    map.insert({ 100, { 100, 0, true, false, false } });
    map.insert({ 110, { 110, 0, true, false, false } });
    map.Finalize();

    TModelIdProcessor validator(map);

    // drops group_id for a model not in the set
    {
        TGlRecord glRecord;
        glRecord.set_model_id(0);

        validator.ProcessOffer(&glRecord);
        ASSERT_EQ(0, glRecord.model_id());
    }
}

TEST(TModelIdProcessorTests, ChangeCategory)
{
    const int CAT_NOT_EXISTS = 0;
    const int CAT_MOBILE = 91491;
    const int CAT_TABLET = 6427100;

    TCompactModelValidities map;
    map.insert({ 2, { 2, CAT_NOT_EXISTS, true, false, false } });
    map.insert({ 3, { 3, CAT_TABLET, true, false, false } });
    map.Finalize();
    TModelIdProcessor validator(map);

    {
        // Индексатор не знает про модель.
        // Ничего не делаем, оставляем исходную категорию от UC.
        TGlRecord glRecord;
        glRecord.set_model_id(1);
        glRecord.set_category_id(CAT_MOBILE);
        validator.ProcessOffer(&glRecord);
        ASSERT_EQ(CAT_MOBILE, glRecord.category_id());
    }
    {
        // Индексатор знает про модель, но категория 0 (например потому что категория еще не прокинулась в индексатор).
        // Ничего не делаем, оставляем исходную категорию от UC.
        TGlRecord glRecord;
        glRecord.set_model_id(2);
        glRecord.set_category_id(CAT_MOBILE);
        validator.ProcessOffer(&glRecord);
        ASSERT_EQ(CAT_MOBILE, glRecord.category_id());
    }
    {
        // Используем категорию от модели.
        TGlRecord glRecord;
        glRecord.set_model_id(3);
        glRecord.set_category_id(CAT_MOBILE);
        validator.ProcessOffer(&glRecord);
        ASSERT_EQ(CAT_TABLET, glRecord.category_id());
    }
}

TEST(TModelIdProcessorTests, OfferFlagsFromModelValidity)
{
    using namespace NMarket::NDocumentFlags;

    TCompactModelValidities map;
    //model_id, hid, published_on_white, published_on_blue, is_dummy
    map.insert({ 1, { 1, 91491, true, true, false } });
    map.insert({ 2, { 2, 91491, false, true, false } });
    map.insert({ 3, { 3, 91491, true, false, true } });
    map.Finalize();
    TModelIdProcessor validator(map);
    OfferDocumentBuilder offersBuilder({}, nullptr);

    {
        TGlRecord glRecord;
        glRecord.set_model_id(1);
        validator.ProcessOffer(&glRecord);
        offersBuilder.GenerateOfferFlags(&glRecord);
        ASSERT_TRUE(glRecord.flags() & MODEL_COLOR_WHITE);
        ASSERT_TRUE(glRecord.flags() & MODEL_COLOR_BLUE);
        ASSERT_FALSE(glRecord.flags() & HAS_GURU_DUMMY_MODEL);
    }
    {
        TGlRecord glRecord;
        glRecord.set_model_id(2);
        glRecord.set_is_blue_offer(true); // без этого обнулится model_id
        validator.ProcessOffer(&glRecord);
        offersBuilder.GenerateOfferFlags(&glRecord);
        ASSERT_FALSE(glRecord.flags() & MODEL_COLOR_WHITE);
        ASSERT_TRUE(glRecord.flags() & MODEL_COLOR_BLUE);
        ASSERT_FALSE(glRecord.flags() & HAS_GURU_DUMMY_MODEL);
    }
    {
        TGlRecord glRecord;
        glRecord.set_model_id(3);
        validator.ProcessOffer(&glRecord);
        offersBuilder.GenerateOfferFlags(&glRecord);
        ASSERT_TRUE(glRecord.flags() & MODEL_COLOR_WHITE);
        ASSERT_FALSE(glRecord.flags() & MODEL_COLOR_BLUE);
        ASSERT_TRUE(glRecord.flags() & HAS_GURU_DUMMY_MODEL);
    }
}
