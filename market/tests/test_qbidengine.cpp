#include <market/qpipe/qbid/qbidengine/qbid_engine.cpp>
#include <market/qpipe/qbid/qbidengine/legacy.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <google/protobuf/descriptor.h>


#include <time.h>


#include "util.h"
#include <util/stream/file.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <utility>


using TMboCategory = Market::Mbo::Parameters::Category;
using EOutputType = Market::Mbo::Parameters::OutputType;

NQBid::TShopIndex CreateShopIndex(NQBid::TShopId shop_id)
{
    NQBid::TShopIndex index;
    index.ShopId = shop_id;
    index.BidPtr = nullptr;

    return index;
}
NQBid::TTitleIndex CreateTitleIndex(NQBid::TShopId shop_id,
                                    NQBid::TTitlePtr title_ptr,
                                    NQBid::TBidPtr bid_ptr)
{
    NQBid::TTitleIndex index;
    index.ShopId = shop_id;
    index.TitlePtr = title_ptr;
    index.BidPtr = bid_ptr;

    return index;
}

NQBid::TModelIdIndex CreateModelIdIndex(NQBid::TModelId model_id,
                                        NQBid::TVendorDsId vendor_ds_id,
                                        NQBid::TBidPtr bid_ptr = nullptr)
{
    NQBid::TModelIdIndex index;
    index.ModelId = model_id;
    index.VendorDsId = vendor_ds_id;
    index.BidPtr = bid_ptr;

    return index;
}

TEST(NQBid, TSearchChecker_fillErrors)
{
    using namespace NQBid;

    /* initialization of bid */
    size_t size = sizeof(TMeta)+sizeof(TBid::TBidValueStatus);
    char* test_buffer = new char[size];
    *(TMeta*)test_buffer = (0x1 << BF_YBID);
    TBid::TBidValueStatus* bid_ptr = (TBid::TBidValueStatus*)((TMeta*)test_buffer + 1);
    bid_ptr->BidValue = 10;
    bid_ptr->BidStatus = TBid::NotFound;


    TString first_title ("Машинка");
    TString second_title("Машинка Красивая");
    TString third_title ("Машинка  Красивая");

    char* title_index = new char[sizeof(size_t) + sizeof(TTitleIndex)*3];
    *(size_t*)title_index = 2*sizeof(TTitleIndex);
    TTitleIndex* title_ptr = (TTitleIndex*)((size_t*) title_index + 1);
    *title_ptr++ = CreateTitleIndex(128, (char*)first_title.c_str(), test_buffer);
    *title_ptr++ = CreateTitleIndex(128, (char*)second_title.c_str(), test_buffer);

    TSearchChecker<TTitleSearch, TTitleIndex>::FillDuplicateErrors(TTitleSearch(title_index));
    ASSERT_EQ(TBid::NotFound, bid_ptr->BidStatus);

    *(size_t*)title_index = 3*sizeof(TTitleIndex);
    *title_ptr++ = CreateTitleIndex(128, (char*)third_title.c_str(), test_buffer);

    TTitleSearch title_search(title_index);

    TSearchChecker<TTitleSearch, TTitleIndex>::FillDuplicateErrors(title_search);
    ASSERT_EQ(TBid::NotAllowed, bid_ptr->BidStatus);

    /* search title simple test */
    TResult<TTitleIndex> result = title_search.Search(128, (char*)third_title.c_str());
    ASSERT_TRUE(0 == second_title.compare(result.Begin->TitlePtr));


    delete [] title_index;
    delete [] test_buffer;
}

NQBid::TBid::TBidValueStatus* CreateModelSearchBid(void* startAddress, NQBid::TBidValue value, NQBid::TBidStatus status)
{
    using namespace NQBid;
    *(TMeta*)startAddress = (0x1 << BF_MODELSEARCH) | (BT_MODEL_ID << 16);
    TBid::TBidValueStatus* bid_ptr = (TBid::TBidValueStatus*)((TMeta*)startAddress + 1);
    bid_ptr->BidValue = value;
    bid_ptr->BidStatus = status;
    // возвратим указатель на саму ставку
    return (TBid::TBidValueStatus*)((TMeta*)startAddress + 1);
}

void CheckModelSearchBid(void* startAddress, NQBid::TBidValue value, NQBid::TBidStatus status)
{
    using namespace NQBid;
    ASSERT_EQ(*(TMeta*)startAddress, (0x1 << BF_MODELSEARCH) | (BT_MODEL_ID << 16));
    TBid::TBidValueStatus* bid_ptr = (TBid::TBidValueStatus*)((TMeta*)startAddress + 1);
    ASSERT_EQ(bid_ptr->BidValue, value);
    ASSERT_EQ(bid_ptr->BidStatus, status);
}

void CheckModelIdIndex(NQBid::TModelIdIndex *index, uint32_t model_id, uint64_t vendor_ds_id, void* bid_ptr)
{
    ASSERT_EQ(index->ModelId, model_id);
    ASSERT_EQ(index->VendorDsId, vendor_ds_id);
    ASSERT_EQ(index->BidPtr, bid_ptr);
}

void FillModelSearchBidToRecord(MBI::Bid& bid_record, const TString& model_id, uint32_t bid_value, uint64_t vendor_ds_id)
{
    bid_record.set_domain_type(MBI::Bid::MODEL_ID);
    bid_record.set_partner_id(vendor_ds_id);
    bid_record.clear_domain_ids();
    bid_record.add_domain_ids(model_id);
    bid_record.mutable_value_for_model_search()->set_value(bid_value);
}

TEST(NQBid, TFiller_Model)
{
    using namespace NQBid;

    int number_of_test_bids = 3;
    size_t size4Index = sizeof(size_t) + sizeof(TModelIdIndex) * number_of_test_bids;
    size_t size4Bids = (sizeof(TMeta) + sizeof(TBid::TBidValueStatus)) * number_of_test_bids;
    char* index_buffer = new char[size4Index];
    char* bids_buffer = new char[size4Bids];
    char *current_bid_ptr = bids_buffer;

    size4Index -= sizeof(size_t);  //в filler'e храним размер без учета самого размера
    TModelIdFiller filler(index_buffer, size4Index);
    MBI::Bid bid_record;

    // ставки в порядке убывания по  modelId пишутся в bids_buffer и в индекс
    for (int i = number_of_test_bids - 1; i >= 0 ; i--) {
        FillModelSearchBidToRecord(bid_record, ::ToString(i * 10), i * 10, i * 10);
        TBid bid(current_bid_ptr);
        bid.Save(BT_MODEL_ID, bid_record);
        filler.Save(bid_record, current_bid_ptr);
        current_bid_ptr += bid.GetSize();
    }

    // ставки в индексе сортируются по возрастанию modelId
    filler.Sort(true);

    // Проверим память со ставками и индексом
    char* current_index_ptr = index_buffer;
    ASSERT_EQ(*(size_t*)current_index_ptr, size4Index);
    current_index_ptr += sizeof(size_t);
    current_bid_ptr = bids_buffer;

    for (int i = number_of_test_bids - 1; i >= 0; i--) {
        // ставки в памяти лежат как лежали - в порядке убывания modelId
        CheckModelSearchBid(current_bid_ptr, i * 10, TBid::NotFound);
        current_bid_ptr += TBid::GetSize(bid_record);
    }

    for (int i = 0; i < number_of_test_bids; i++) {
        // индекс отсортирован по model_id, указатели на ставки в порядке,
        // обратном первоначальному заполнению bids_buffer
        int bids_reverse_position = number_of_test_bids - 1 - i;
        CheckModelIdIndex((TModelIdIndex*)current_index_ptr, i * 10, i * 10,
            bids_buffer + bids_reverse_position * TBid::GetSize(bid_record));
        current_index_ptr += sizeof(TModelIdIndex);
    }

    delete[] index_buffer;
    delete[] bids_buffer;
}

TEST(NQBid, TSearchChecker_Model)
{
    using namespace NQBid;

    size_t size = sizeof(TMeta) + sizeof(TBid::TBidValueStatus);
    char* test_buffer1 = new char[size];
    TBid::TBidValueStatus* bid_ptr1 = CreateModelSearchBid(test_buffer1, 10, TBid::NotFound);
    char* test_buffer2 = new char[size];
    TBid::TBidValueStatus* bid_ptr2 = CreateModelSearchBid(test_buffer2, 20, TBid::Applied);
    char* test_buffer3 = new char[size];
    TBid::TBidValueStatus* bid_ptr3 = CreateModelSearchBid(test_buffer3, 30, TBid::AppliedCorrected);

    char* modelid_index = new char[sizeof(size_t) + sizeof(TModelIdIndex) * 3];
    *(size_t*)modelid_index = 3 * sizeof(TModelIdIndex);

    TModelIdIndex* modelid_index_ptr = (TModelIdIndex*)((size_t*)modelid_index + 1);

    // modelId одинаковые => элементы эквивалентны несмотря на разные vendor_ds_id
    *modelid_index_ptr++ = CreateModelIdIndex(128, 200, test_buffer1);
    TModelIdIndex* modelid_index_ptr_first = modelid_index_ptr;
    *modelid_index_ptr++ = CreateModelIdIndex(128, 300, test_buffer2);
    *modelid_index_ptr = CreateModelIdIndex(128, 400, test_buffer3);

    TModelIdSearch modelid_search(modelid_index);

    // Сами ставки
    ASSERT_EQ(TBid::NotFound, bid_ptr1->BidStatus);
    ASSERT_EQ(TBid::Applied, bid_ptr2->BidStatus);
    ASSERT_EQ(TBid::AppliedCorrected, bid_ptr3->BidStatus);
    // Как они лежат в индексе (посмотрим несколько)
    ASSERT_EQ((0x1 << BF_MODELSEARCH) | (BT_MODEL_ID << 16), *(TMeta*)(modelid_index_ptr_first->BidPtr));
    ASSERT_EQ((0x1 << BF_MODELSEARCH) | (BT_MODEL_ID << 16), *(TMeta*)(modelid_index_ptr->BidPtr));
    // Находим их!
    TResult<TModelIdIndex> result = modelid_search.Search(128);
    ASSERT_EQ(result.Count(), 3);
    ASSERT_EQ(result.GetBidPtr(), test_buffer1);    // ставка присоединена к индексу
    ASSERT_EQ(result.GetBidData().ModelSearchBid(), 10);
    ASSERT_EQ(GetVendorDsId(result), 200);     //находим первый из vendor_ds_id (до устранения дубликатов)

    // Предполагается, что индекс уже отсортирован!
    TSearchChecker<TModelIdSearch, TModelIdIndex>::FillDuplicateErrors(modelid_search);
    // Будем убирать эквивалентные элементы (дубликаты), причем все:
    // 1. поставим ставкам-дубликатам статус NotAllowed (для отчета MBI)
    ASSERT_EQ(TBid::NotAllowed, bid_ptr1->BidStatus);
    ASSERT_EQ(TBid::NotAllowed, bid_ptr2->BidStatus);
    ASSERT_EQ(TBid::NotAllowed, bid_ptr3->BidStatus);
    // 2. отсоединим ставки-дубликаты от индекса, в индексе они станут BT_DUMMY
    ASSERT_EQ((BT_DUMMY << 16), *(TMeta*)(modelid_index_ptr_first->BidPtr));
    ASSERT_EQ((BT_DUMMY << 16), *(TMeta*)(modelid_index_ptr->BidPtr));
    // Находим model_id в индексе, но ставки отсоединены от него
    result = modelid_search.Search(128);
    ASSERT_FALSE(result.GetBidPtr() == test_buffer1 ||
        result.GetBidPtr() == test_buffer2 ||
        result.GetBidPtr() == test_buffer3);
    ASSERT_EQ(*(TMeta*)result.GetBidPtr(), (BT_DUMMY << 16));
    ASSERT_EQ(*(TMeta*)result.GetBidPtr(), (BT_DUMMY << 16));
    ASSERT_EQ(result.GetBidData().ModelSearchBid(), 0);
    ASSERT_EQ(GetVendorDsId(result), 200); // vendor_ds_id прежнему находим, но без ставки он не интересен

    delete[] modelid_index;
    delete[] test_buffer1;
    delete[] test_buffer2;
    delete[] test_buffer3;
}

TEST(NQBid, TestDeprecatedFields)
{
    MBI::Bid bidNew, bidDeprecated;

    bidNew.set_target(MBI::Bid::OFFER);
    bidNew.add_domain_ids("3");
    bidNew.add_domain_ids("offer_id");

    ASSERT_EQ(NQBid::NLegacy::GetFeedId(bidNew), 3);
    ASSERT_EQ(NQBid::NLegacy::GetOfferId(bidNew), "offer_id");

    bidDeprecated.set_target(MBI::Bid::OFFER);
    bidDeprecated.set_feed_id(3);
    bidDeprecated.set_domain_id("offer_id");

    ASSERT_EQ(NQBid::NLegacy::GetFeedId(bidDeprecated), 3);
    ASSERT_EQ(NQBid::NLegacy::GetOfferId(bidDeprecated), "offer_id");
}

TEST(NQBid, TFiller_isSorted)
{
    using namespace NQBid;
    std::vector<TShopIndex> v;

    v.push_back(CreateShopIndex(81));
    v.push_back(CreateShopIndex(91));
    v.push_back(CreateShopIndex(101));
    v.push_back(CreateShopIndex(101));
    v.push_back(CreateShopIndex(121));
    ASSERT_TRUE(IsSorted(v.begin(), v.end()));

    v.push_back(CreateShopIndex(100));
    ASSERT_FALSE(IsSorted(v.begin(), v.end()));
}

TEST(NQBid, TFiller_isSorted_Model)
{
    using namespace NQBid;
    std::vector<TModelIdIndex> v;

    v.push_back(CreateModelIdIndex(81, 1));
    v.push_back(CreateModelIdIndex(91, 1));
    v.push_back(CreateModelIdIndex(101, 1));
    v.push_back(CreateModelIdIndex(101, 1));
    v.push_back(CreateModelIdIndex(121, 1));
    ASSERT_TRUE(IsSorted(v.begin(), v.end()));

    v.push_back(CreateModelIdIndex(100, 1));
    ASSERT_FALSE(IsSorted(v.begin(), v.end()));
}

TEST(NQBid, TBid)
{
    using namespace NQBid;

    size_t size = sizeof(TMeta)+sizeof(TBid::TBidValueStatus)*3;
    char* test_buffer = new char[size];

    std::memset(test_buffer, 0xA, size);

    *(TMeta*)test_buffer = (0x1 << BF_YBID) | (0x1 << BF_CBID) | (0x1 << BF_FEE);

    TBid bid(test_buffer);
    bid.setNotAllowed();

    TBid::TData data = bid.get();
    ASSERT_EQ(TBid::NotAllowed, data.Bids[BF_YBID].BidStatus);
    ASSERT_EQ(TBid::NotAllowed, data.Bids[BF_CBID].BidStatus);
    ASSERT_EQ(TBid::NotAllowed, data.Bids[BF_FEE].BidStatus);

    ASSERT_EQ(0x0A0A, data.Bids[BF_YBID].BidValue);
    ASSERT_EQ(0x0A0A, data.Bids[BF_CBID].BidValue);
    ASSERT_EQ(0x0A0A, data.Bids[BF_FEE].BidValue);


    data.Bids[BF_YBID].BidStatus = TBid::Applied;
    data.Bids[BF_YBID].BidValue = 65535;
    data.Bids[BF_CBID].BidStatus = TBid::AppliedCorrected;
    data.Bids[BF_CBID].BidValue = 65535;
    data.Bids[BF_FEE].BidStatus = TBid::NotFound;
    data.Bids[BF_FEE].BidValue = 65535;

    bid.set(data); /* value не должно сохраняться */

    TBid::TBidValueStatus* biddata_ptr = (TBid::TBidValueStatus*)((TMeta*)test_buffer + 1);
    TBid::TBidValueStatus* ptr = biddata_ptr;

    ASSERT_EQ(0x0A0A, ptr->BidValue);
    ASSERT_EQ(TBid::Applied, ptr->BidStatus);
    ptr++;

    ASSERT_EQ(0x0A0A, ptr->BidValue);
    ASSERT_EQ(TBid::AppliedCorrected, ptr->BidStatus);
    ptr++;

    ASSERT_EQ(0x0A0A, ptr->BidValue);
    ASSERT_EQ(TBid::NotFound, ptr->BidStatus);
    ASSERT_EQ(size, bid.GetSize());

    /* сменим расположение в памяти fee ставки */
    *(TMeta*)test_buffer = (0x1 << BF_FEE);
    data = bid.get();

    ASSERT_EQ(0, data.Bids[BF_YBID].BidValue);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_YBID].BidStatus);

    ASSERT_EQ(0, data.Bids[BF_CBID].BidValue);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_CBID].BidStatus);

    ASSERT_EQ(TBid::Applied, data.Bids[BF_FEE].BidStatus);
    ASSERT_EQ(0x0A0A, data.Bids[BF_FEE].BidValue);

    delete [] test_buffer;
}

TEST(NQBid, TBid_Model)
{
    using namespace NQBid;

    size_t size = sizeof(TMeta) + sizeof(TBid::TBidValueStatus) * 3;
    char* test_buffer = new char[size];

    std::memset(test_buffer, 0xA, size);

    *(TMeta*)test_buffer = (0x1 << BF_MODELSEARCH);

    TBid bid(test_buffer);
    bid.setNotAllowed();

    TBid::TData data = bid.get();

    ASSERT_EQ(TBid::NotAllowed, data.Bids[BF_MODELSEARCH].BidStatus);
    ASSERT_EQ(0x0A0A, data.Bids[BF_MODELSEARCH].BidValue);

    // Нет ставок - по нулям
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_YBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_CBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_FEE].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_MBID].BidStatus);
    ASSERT_EQ(0, data.Bids[BF_YBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_CBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_FEE].BidValue);
    ASSERT_EQ(0, data.Bids[BF_MBID].BidValue);

    data.Bids[BF_MODELSEARCH].BidStatus = TBid::Applied;
    data.Bids[BF_MODELSEARCH].BidValue = 65535;
    data.Bids[BF_CBID].BidStatus = TBid::AppliedCorrected;
    data.Bids[BF_CBID].BidValue = 65535;

    bid.set(data); /* value не должно сохраняться */

    TBid::TBidValueStatus* biddata_ptr = (TBid::TBidValueStatus*)((TMeta*)test_buffer + 1);
    TBid::TBidValueStatus* ptr = biddata_ptr;

    ASSERT_EQ(0x0A0A, ptr->BidValue);
    ASSERT_EQ(TBid::Applied, ptr->BidStatus);
    ptr++;

    // память, не испольуемая ставкой не аффектится
    ASSERT_EQ(0x0A0A, ptr->BidValue);
    ASSERT_EQ(0x0A0A, ptr->BidStatus);
    ptr++;
    ASSERT_EQ(0x0A0A, ptr->BidValue);
    ASSERT_EQ(0x0A0A, ptr->BidStatus);

    //учитывается только размер указанных ставок
    ASSERT_EQ(sizeof(TMeta) + sizeof(TBid::TBidValueStatus) * 1, bid.GetSize());

    /* внезапно добавляется ставка */
    *(TMeta*)test_buffer |= (0x1 << BF_FEE);
    data = bid.get();

    // это та, что была BF_MODELSEARCH
    ASSERT_EQ(TBid::Applied, data.Bids[BF_FEE].BidStatus);
    ASSERT_EQ(0x0A0A, data.Bids[BF_FEE].BidValue);
    // BF_MODELSEARCH уехала на след. 4 байта памяти
    ASSERT_EQ(0x0A0A, data.Bids[BF_MODELSEARCH].BidStatus);
    ASSERT_EQ(0x0A0A, data.Bids[BF_MODELSEARCH].BidValue);

    delete[] test_buffer;
}

/*
 * Заполнить все bid_record.value_for_***().value() поля bid_record'a значениями bids_value
 */
void FillBidsToRecord(MBI::Bid& bid_record, const uint32_t bids_value)
{
    const google::protobuf::Descriptor* descriptor = bid_record.GetDescriptor();
    const google::protobuf::Reflection* reflection = bid_record.GetReflection();
    const unsigned fields_count = descriptor->field_count();

    for (unsigned idx = 0; idx < fields_count; idx++)
    {
        const google::protobuf::FieldDescriptor *field_descriptor = descriptor->field(idx);

        if ((field_descriptor->type() == google::protobuf::FieldDescriptor::TYPE_MESSAGE) && (!field_descriptor->is_repeated())) {
            google::protobuf::Message* details_message = reflection->MutableMessage(&bid_record, field_descriptor);
            const google::protobuf::Descriptor* details_descriptor = details_message->GetDescriptor();
            const google::protobuf::Reflection* details_reflection = details_message->GetReflection();

            if (details_descriptor->name() == TString("Value"))
            {
                const TString value("value");
                for (int didx=0; didx < details_descriptor->field_count(); didx++)
                {
                    const google::protobuf::FieldDescriptor* details_field_descriptor = details_descriptor->field(didx);
                    if (0 == details_field_descriptor->name().compare(0, value.length(), value.data()))
                        details_reflection->SetInt32(details_message, details_field_descriptor, bids_value);
                }
            }
        }
    }
}


TEST(NQBid, TBidSizeCalculation)
{
    const uint32_t bids_value(10000);

    MBI::Bid bid_record;
    FillBidsToRecord(bid_record, bids_value);
    bid_record.set_domain_type(MBI::Bid::OFFER_TITLE);

    size_t memory_size = NQBid::TBid::GetSize(bid_record); // our memory consumption 100% prediction
    char* buffer = new char[memory_size];

    NQBid::TBid bid(buffer);
    bid.Save(NQBid::BT_OFFER_TITLE, bid_record);

    /* sizes of prediction method and TBid method must be equal OR SEGSERV WILL FIND US*/
    ASSERT_EQ(bid.GetSize(), memory_size);

    NQBid::TBid::TData data = bid.get();
    for (unsigned idx = 0; idx < NQBid::BF_COUNT; idx++)
    {
        ASSERT_EQ(bids_value, data.Bids[idx].BidValue);
        ASSERT_EQ(NQBid::TBid::NotFound, data.Bids[idx].BidStatus);
    }

    NQBid::TMeta must_bids_flags = (0x1 << NQBid::BF_COUNT) - 1;
    const NQBid::TMeta real_bids_flags = (*(NQBid::TMeta*)buffer) & must_bids_flags;
    ASSERT_EQ(real_bids_flags, must_bids_flags);

    delete [] buffer;
    /* написать тест на равенство GetSize(MbiBidRecord) == GetSize(TBid) */
}

TEST(NQBid, TBid_Title)
{
    using namespace NQBid;

    size_t size = sizeof(TMeta) +
                  sizeof(TFeedId) + sizeof(TOfferDigitId) +
                  sizeof(TBid::TBidValueStatus);
    char* test_buffer = new char[size];

    *(TMeta*)test_buffer = (0x1 << BF_FEE) | (BT_OFFER_TITLE << 16);
    TBid bid(test_buffer);
    bid.SetSwitchedFeedId(1001);
    bid.SetSwitchedOfferId(2001, true);

    bid.setApplied();

    ASSERT_EQ(1001, *(TFeedId*)((TMeta*)test_buffer + 1));
    ASSERT_EQ(2001, *(TOfferDigitId*)((char*)test_buffer + sizeof(TMeta)+sizeof(TFeedId)));
    ASSERT_TRUE(0x80000000 & (*(TMeta*)test_buffer));

    ASSERT_EQ(TBid::Applied, bid.get().Bids[BF_FEE].BidStatus);

    /* допустим, прошло второе срабатывание по title */
    bid.SetSwitchedFeedId(1080);
    bid.SetSwitchedOfferId(108010801080, false);

    ASSERT_EQ(1080, *(TFeedId*)((TMeta*)test_buffer + 1));
    ASSERT_EQ(108010801080,
              *(TOfferDigitId*)((char*)test_buffer + sizeof(TMeta)+sizeof(TFeedId)));
    ASSERT_FALSE(0x80000000 & (*(TMeta*)test_buffer));

    delete [] test_buffer;
}

TEST(NQBid, TCheck_IsDigit)
{
    using namespace NQBid;

    ASSERT_TRUE(TCheck::IsDigit<ui8>("255"));
    ASSERT_TRUE(TCheck::IsDigit<ui8>("1"));

    ASSERT_FALSE(TCheck::IsDigit<ui8>("0"));
    ASSERT_FALSE(TCheck::IsDigit<ui8>("00"));
    ASSERT_FALSE(TCheck::IsDigit<ui8>("0000123"));
    ASSERT_FALSE(TCheck::IsDigit<ui8>(""));
    ASSERT_FALSE(TCheck::IsDigit<ui8>("  "));
    ASSERT_FALSE(TCheck::IsDigit<ui8>("  123"));
    ASSERT_FALSE(TCheck::IsDigit<ui8>("-25"));
    ASSERT_FALSE(TCheck::IsDigit<ui8>("256"));
    ASSERT_FALSE(TCheck::IsDigit<ui8>("2244"));
}


TEST(NQBid, TBid_Dummy)
{
    using namespace NQBid;
    TBid dummy(TBid::GetDummyBidPtr());

    TBid::TData data = dummy.get();
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_YBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_CBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_FEE].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_MBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_MODELSEARCH].BidStatus);

    ASSERT_EQ(0, data.Bids[BF_YBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_CBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_FEE].BidValue);
    ASSERT_EQ(0, data.Bids[BF_MBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_MODELSEARCH].BidValue);

    dummy.setApplied();
    data = dummy.get();

    ASSERT_EQ(TBid::NotFound, data.Bids[BF_YBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_CBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_FEE].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_MBID].BidStatus);
    ASSERT_EQ(TBid::NotFound, data.Bids[BF_MODELSEARCH].BidStatus);

    data.Bids[BF_YBID].BidValue = 0xD9;
    data.Bids[BF_CBID].BidValue = 0xD9;
    data.Bids[BF_FEE].BidValue = 0xD9;
    data.Bids[BF_MBID].BidValue = 0xD9;
    data.Bids[BF_MODELSEARCH].BidValue = 0xD9;

    dummy.set(data);
    data = dummy.get();

    ASSERT_EQ(0, data.Bids[BF_YBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_CBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_FEE].BidValue);
    ASSERT_EQ(0, data.Bids[BF_MBID].BidValue);
    ASSERT_EQ(0, data.Bids[BF_MODELSEARCH].BidValue);
}


using namespace NQBid;

class TQBidEngineTestBase : public ::testing::Test
{
protected:
    using TBidValueAndPresence = std::pair<TBidValue, bool>;

    unsigned TotalBids;

    unsigned OfferIdBidsCount;
    unsigned OfferDiditIdBidsCount;
    unsigned OfferTitleBidsCount;
    unsigned OfferCategoryBidsCount;
    unsigned OfferShopBidsCount;
    unsigned ModelIdBidsCount;
    unsigned VendorIdBidsCount;
    unsigned VendorCategoryIdBidsCount;

    size_t OfferTitleCStringsSize;
    size_t OfferIdCStringSize;

    size_t BidsMemorySize;


public:
    static TString TMP_DIR(){ return "tmp/qbid_temp"; }
    static TString CATEGORIES_FILE() { return TMP_DIR() + "/tovar-tree.pb"; }
    static TBid::TData& D() { static TBid::TData dummy; return dummy; }
    static TBidsTimestamps& BT() { static TBidsTimestamps dummy; return dummy; }

private:
    void DumpBidValues(
            MBI::Bid* bid,
            TBidValueAndPresence search_bid_value,
            TBidValueAndPresence card_bid_value,
            TBidValueAndPresence fee_bid_value,
            TBidValueAndPresence model_search_value,
            TBidValueAndPresence flag_dont_pull_up_bids)
    {
        if (search_bid_value.second)
            bid->mutable_value_for_search()->set_value(search_bid_value.first);
        if (card_bid_value.second)
            bid->mutable_value_for_card()->set_value(card_bid_value.first);
        if (fee_bid_value.second)
            bid->mutable_value_for_marketplace()->set_value(fee_bid_value.first);
        if (model_search_value.second)
            bid->mutable_value_for_model_search()->set_value(model_search_value.first);
        if (flag_dont_pull_up_bids.second)
            bid->mutable_flag_dont_pull_up_bids()->set_value(flag_dont_pull_up_bids.first);
    }

    inline void DumpBidOnOfferValues(MBI::Bid* bid,
            TBidValue search_bid_value,
            TBidValue card_bid_value,
            TBidValue fee_bid_value,
            TBidValue flag_dont_pull_up_bids)
    {
        DumpBidValues(
                bid,
                std::make_pair(search_bid_value, search_bid_value != 0),
                std::make_pair(card_bid_value, card_bid_value != 0),
                std::make_pair(fee_bid_value, fee_bid_value != 0),
                std::make_pair(0, false),
                std::make_pair(flag_dont_pull_up_bids, flag_dont_pull_up_bids != 0)
        );
    }

protected:
    void DumpShop(MBI::Bid* bid,
            TShopId   shop_id,
                TBidValue search_bid_value,
                TBidValue card_bid_value,
                TBidValue fee_bid_value)
    {
        bid->set_partner_id(shop_id);
        bid->set_domain_type(MBI::Bid::SHOP_ID);
        const TString str_shop_id = ::ToString(shop_id);
        // пока ставки на магазин только офферные, и мы читаем shop_id из partner_id
        DumpBidOnOfferValues(bid, search_bid_value, card_bid_value, fee_bid_value, 0);

        OfferShopBidsCount++;
        BidsMemorySize += TBid::GetSize(*bid);
        TotalBids++;
    }

    void DumpCategory(MBI::Bid* bid,
            TShopId     shop_id,
            TCategoryId category_id,
            TBidValue   search_bid_value,
            TBidValue   card_bid_value,
            TBidValue   fee_bid_value,
            bool deprecated_mode = false)

    {
        bid->set_partner_id(shop_id);
        bid->set_domain_type(MBI::Bid::CATEGORY_ID);
        const TString str_category_id = ::ToString(category_id);
        if (deprecated_mode) {
            bid->set_domain_id(str_category_id.c_str());    //deprecated. see MARKETINDEXER-6229
        } else {
            bid->add_domain_ids(str_category_id.c_str());
        }

        DumpBidOnOfferValues(bid, search_bid_value, card_bid_value, fee_bid_value, 0);

        OfferCategoryBidsCount++;
        BidsMemorySize += TBid::GetSize(*bid);
        TotalBids++;
    }

    void DumpTitle(MBI::Bid* bid,
            TShopId        shop_id,
            const TString& title,
            TBidValue      search_bid_value,
            TBidValue      card_bid_value,
            TBidValue      fee_bid_value,
            bool deprecated_mode = false)
    {
        bid->set_partner_id(shop_id);
        bid->set_domain_type(MBI::Bid::OFFER_TITLE);
        if (deprecated_mode) {
            bid->set_domain_id(title.c_str());    //deprecated. see MARKETINDEXER-6229
        } else {
            bid->add_domain_ids(title.c_str());
        }

        DumpBidOnOfferValues(bid, search_bid_value, card_bid_value, fee_bid_value, 0);

        OfferTitleBidsCount++;
        OfferTitleCStringsSize += (title.size() + 1);
        BidsMemorySize += TBid::GetSize(*bid);
        TotalBids++;
    }

    void DumpOfferId(MBI::Bid* bid,
            TShopId        shop_id,
            TFeedId        feed_id,
            const TString& offer_id,
            TBidValue      search_bid_value,
            TBidValue      card_bid_value,
            TBidValue      fee_bid_value,
            TBidValue      flag_dont_pull_up_bids,
            bool deprecated_mode = false)

    {
        bid->set_partner_id(shop_id);
        bid->set_domain_type(MBI::Bid::FEED_OFFER_ID);
        if (deprecated_mode) {
            bid->set_domain_id(offer_id.c_str());    //deprecated. see MARKETINDEXER-6229
            bid->set_feed_id(feed_id);
        } else {
            bid->add_domain_ids(::ToString(feed_id));
            bid->add_domain_ids(offer_id.c_str());
        }

        DumpBidOnOfferValues(bid, search_bid_value, card_bid_value, fee_bid_value, flag_dont_pull_up_bids);

        if (TCheck::IsDigit<TOfferDigitId>(offer_id))
            OfferDiditIdBidsCount++;
        else
        {
            OfferIdBidsCount++;
            OfferIdCStringSize += (offer_id.size() + 1);
        }

        BidsMemorySize += TBid::GetSize(*bid);
        TotalBids++;
    }

    void DumpModelId(MBI::Bid* bid,
            TVendorDsId vendor_ds_id,
            TModelId    model_id,
            TBidValue   model_search_bid_value)
    {
        bid->set_partner_id(vendor_ds_id);
        bid->set_domain_type(MBI::Bid::MODEL_ID);
        bid->add_domain_ids(::ToString(model_id));

        DumpBidValues(
                bid,
                std::make_pair(0, false),
                std::make_pair(0, false),
                std::make_pair(0, false),
                std::make_pair(model_search_bid_value, true),
                std::make_pair(0, false)
        );

        ModelIdBidsCount++;
        BidsMemorySize += TBid::GetSize(*bid);
        TotalBids++;
    }

    void DumpVendorId(MBI::Bid* bid,
        TVendorDsId vendor_ds_id,
        TVendorId   vendor_id,
        TBidValue   model_search_bid_value)
    {
        bid->set_partner_id(vendor_ds_id);
        bid->set_domain_type(MBI::Bid::VENDOR_ID);
        bid->add_domain_ids(::ToString(vendor_id));

        DumpBidValues(
                bid,
                std::make_pair(0, false),
                std::make_pair(0, false),
                std::make_pair(0, false),
                std::make_pair(model_search_bid_value, true),
                std::make_pair(0, false)
        );

        VendorIdBidsCount++;
        BidsMemorySize += TBid::GetSize(*bid);
        TotalBids++;
    }

    void DumpVendorCategoryId(MBI::Bid* bid,
        TVendorDsId vendor_ds_id,
        TVendorId   vendor_id,
        TCategoryId category_id,
        TBidValue   model_search_bid_value)
    {
        bid->set_partner_id(vendor_ds_id);
        bid->set_domain_type(MBI::Bid::VENDOR_CATEGORY_ID);
        bid->add_domain_ids(::ToString(vendor_id));
        bid->add_domain_ids(::ToString(category_id));

        DumpBidValues(
                bid,
                std::make_pair(0, false),
                std::make_pair(0, false),
                std::make_pair(0, false),
                std::make_pair(model_search_bid_value, true),
                std::make_pair(0, false)
        );

        VendorCategoryIdBidsCount++;
        BidsMemorySize += TBid::GetSize(*bid);
        TotalBids++;
    }

    void Dump(IOutputStream& os, const char* key, const size_t value) const
    {
        os << key << "\t" << value << "\n";
    }

    void DumpMeta(const char* filepath)
    {
        TUnbufferedFileOutput meta(filepath);
        Dump(meta, "TOTAL", TotalBids);

        Dump(meta, "ID", OfferIdBidsCount);
        Dump(meta, "DIGITID", OfferDiditIdBidsCount);
        Dump(meta, "TITLE", OfferTitleBidsCount);
        Dump(meta, "CATEGORY", OfferCategoryBidsCount);
        Dump(meta, "SHOP", OfferShopBidsCount);
        Dump(meta, "MODELID", ModelIdBidsCount);
        Dump(meta, "VENDORID", VendorIdBidsCount);
        Dump(meta, "VENDOR_CATEGORYID", VendorCategoryIdBidsCount);

        Dump(meta, "ID_CSTRING", OfferIdCStringSize);
        Dump(meta, "TITLE_CSTRINGS", OfferTitleCStringsSize);
        Dump(meta, "BIDS_MEMORY", BidsMemorySize);

        Dump(meta, "EXS_TIME", time(nullptr));
    }

    void CreateTestCategoriesXML()
    {
        NMarket::NMbo::TWriter writer(CATEGORIES_FILE(), "MBOC");
        {
            TMboCategory category;
            category.set_hid(90401);
            category.set_tovar_id(0);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);
            category.set_no_search(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Все товары");
            name->set_lang_id(225);

            writer.Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(90402);
            category.set_tovar_id(1);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("Товары для авто- и мототехники");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Авто, мото");
            name->set_lang_id(225);
            writer.Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(90477);
            category.set_tovar_id(645);
            category.set_parent_hid(90402);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("Автохимия");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Автохимия");
            name->set_lang_id(225);

            writer.Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(90829);
            category.set_tovar_id(9);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("Книги");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Книги");
            name->set_lang_id(225);

            writer.Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(90886);
            category.set_tovar_id(172);
            category.set_parent_hid(90829);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("Книги по бизнесу и экономике");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Книги по бизнесу и экономике");
            name->set_lang_id(225);

            writer.Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(90830);
            category.set_tovar_id(168);
            category.set_parent_hid(90829);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("Журналы и газеты");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Журналы и газеты");
            name->set_lang_id(225);

            writer.Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(90839);
            category.set_tovar_id(286);
            category.set_parent_hid(90830);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("Журналы и газеты о связи и телефонии");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("Журналы и газеты о связи и телефонии");
            name->set_lang_id(225);

            writer.Write(category);
        }
    }
public:
    TQBidEngineTestBase()
        : TotalBids(0)

        , OfferIdBidsCount(0)
        , OfferDiditIdBidsCount(0)
        , OfferTitleBidsCount(0)
        , OfferCategoryBidsCount(0)
        , OfferShopBidsCount(0)
        , ModelIdBidsCount(0)
        , VendorIdBidsCount(0)
        , VendorCategoryIdBidsCount(0)

        , OfferTitleCStringsSize(0)
        , OfferIdCStringSize(0)

        , BidsMemorySize(0)
    {}

    virtual ~TQBidEngineTestBase() = default;

    void DoSetUp()
    {
        create_test_environment(TMP_DIR());

        MBI::Parcel parcel;
        /* dump test data */
        {
            NMarket::TSnappyProtoWriter outputStream(TMP_DIR()+"/qbid_test.pbuf.sn",
                                                     "BIDS");
            this->InitTestData(parcel);
            outputStream.Write(parcel);
        }

        /* dump test metadata */
        DumpMeta((TMP_DIR()+"/qbid_test.meta").c_str());
        /* load category tree */
        CreateTestCategoriesXML();
        InitCategoryTree(CATEGORIES_FILE());
        InitEngine();
    }

    virtual void SetUp()
    {
        DoSetUp();
    }

private:

    /* TEST DATA INITIALIZATION HERE */
    virtual void InitTestData(MBI::Parcel& parcel) = 0;
    virtual void InitEngine() = 0;
};

class TQBidEngineTest : public TQBidEngineTestBase
{
public:
    TQBidEngineTest() = default;
    ~TQBidEngineTest() = default;
private:
    /* TEST DATA INITIALIZATION HERE */
    void InitTestData(MBI::Parcel& parcel) override
    {
        /* shop_id, bid, cbid, fee */
        DumpShop(parcel.add_bids(), 1, 15, 0, 11);
        DumpShop(parcel.add_bids(), 3, 30, 35, 0);
        DumpShop(parcel.add_bids(), 4, 15, 0, 0);
        DumpShop(parcel.add_bids(), 6, 30, 35, 0);
        DumpShop(parcel.add_bids(), 7, 21, 0, 0);
        DumpShop(parcel.add_bids(), 8, 20, 20, 0);
        DumpShop(parcel.add_bids(), 9, 0, 4, 0);
        DumpShop(parcel.add_bids(), 10, 0, 0, 0);

        /* shop_id, category_id, bid, cbid, fee */
        DumpCategory(parcel.add_bids(), 1, 9, 0, 9, 0); //книги
        DumpCategory(parcel.add_bids(), 1, 168, 0, 12, 0); //журналы и газеты
        DumpCategory(parcel.add_bids(), 4, 9, 0, 9, 0); //книги
        DumpCategory(parcel.add_bids(), 4, 168, 0, 12, 0); //журналы и газеты

        /* shop_id, title, bid, cbid, fee */
        DumpTitle(parcel.add_bids(), 3, "OfferTitle", 22, 0, 0);
        DumpTitle(parcel.add_bids(), 6, "OfferTitle", 22, 0, 25);
        DumpTitle(parcel.add_bids(), 7, "OfferTitle", 22, 0, 33);
        DumpTitle(parcel.add_bids(), 7, "OfferTitle1", 0, 0, 34);
        DumpTitle(parcel.add_bids(), 9, "OfferTitle", 20, 0, 0);
        DumpTitle(parcel.add_bids(), 10, "OfferTitle", 0, 0, 0);

        /* shop_id, feed_id, offer_id, bid, cbid, fee */
        DumpOfferId(parcel.add_bids(), 7, 700, "1", 23, 0, 0, 0);
        DumpOfferId(parcel.add_bids(), 7, 701, "1", 24, 25, 28, 0);
        DumpOfferId(parcel.add_bids(), 8, 702, "1", 0, 8, 200, 0); // cbid = 8 less 10 (minimal)
        DumpOfferId(parcel.add_bids(), 8, 715, "1", 24, 25, 28, 1); // flag_dont_pull_up_bids=1

        DumpOfferId(parcel.add_bids(), 10, 800, "1", 8, 0, 0, 0);
        DumpOfferId(parcel.add_bids(), 10, 800, "1", 0, 8, 0, 0);
        DumpOfferId(parcel.add_bids(), 10, 800, "1", 0, 0, 8, 0);

        DumpOfferId(parcel.add_bids(), 10, 801, "1", 8, 0, 8, 0);
        DumpOfferId(parcel.add_bids(), 10, 801, "1", 8, 0, 0, 0);
        DumpOfferId(parcel.add_bids(), 10, 801, "1", 0, 8, 8, 0);

        // немного ставок, где вместо новых полей еще используются deprecated-поля MbiBids.proto
        /* shop_id, category_id, bid, cbid, fee */
        DumpCategory(parcel.add_bids(), 10, 168, 0, 16, 0, true); //журналы и газеты
        /* shop_id, title, bid, cbid, fee */
        DumpTitle(parcel.add_bids(), 10, "OfferTitleDeprecated", 14, 0, 0, true);
        /* shop_id, feed_id, offer_id, bid, cbid, fee */
        DumpOfferId(parcel.add_bids(), 10, 703, "2", 26, 27, 29, 0, true);

    }

    void InitEngine() override
    {
        NQBid::Init(
            TMP_DIR() + "/qbid_test.meta",
            TMP_DIR() + "/qbid_test.pbuf.sn",
            /* smallBids = */ nullptr  // TODO(a-square): also test small and matched bids
        );
    }
};

static Market::TMinBids ZeroMinBids() {
    Market::TMinBids minBids;

    minBids.YBid = 0;
    minBids.MBid = 0;
    minBids.CBid = 0;
    minBids.Fee = 0;

    return minBids;
}

static Market::TMinBids CreateMinBids(NQBid::TBidValue bid, NQBid::TFee fee) {
    Market::TMinBids minBids;

    minBids.YBid = bid;
    minBids.MBid = bid;
    minBids.CBid = bid;
    minBids.Fee = fee;

    return minBids;
}

TEST_F(TQBidEngineTest, Legacy_testShopWithoutRules1)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);

    NQBid::Apply(indexer_answer, D(), D(), 2, 90401, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(10, indexer_answer.YBid());   //default
    ASSERT_EQ(10, indexer_answer.CBid());  //default
    ASSERT_EQ(10, indexer_answer.Fee());   //default
}

TEST_F(TQBidEngineTest, Legacy_testShopWithoutRules2)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);

    NQBid::Apply(indexer_answer, D(), D(), 5, 90401, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(10, indexer_answer.YBid());   //default
    ASSERT_EQ(10, indexer_answer.CBid());  //default
    ASSERT_EQ(10, indexer_answer.Fee());   //default
}

TEST_F(TQBidEngineTest, Legacy_testShopRules1)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);

    NQBid::Apply(indexer_answer, D(), D(), 1, 90401, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());   //from shop
    ASSERT_EQ(15, indexer_answer.CBid());  //from shop
    ASSERT_EQ(11, indexer_answer.Fee());   //from shop
}

TEST_F(TQBidEngineTest, Legacy_testShopRules2)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);

    NQBid::Apply(indexer_answer, D(), D(), 4, 90401, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ(15, indexer_answer.CBid()); //from shop
    ASSERT_EQ(10, indexer_answer.Fee());  //default
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule1)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    // 90477 - бытовая химия
    NQBid::Apply(indexer_answer, D(), D(), 1, 90477, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ(15, indexer_answer.CBid()); //from shop
    ASSERT_EQ(11, indexer_answer.Fee());  //from shop
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule2)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90829 - книжки
    NQBid::Apply(indexer_answer, D(), D(), 1, 90829, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ( 9, indexer_answer.CBid()); //from category
    ASSERT_EQ(11, indexer_answer.Fee());  //from shop
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule3)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 1, 90886, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ( 9, indexer_answer.CBid()); //from category
    ASSERT_EQ(11, indexer_answer.Fee());  //from shop
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule4)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90830 - журналы и газеты
    NQBid::Apply(indexer_answer, D(), D(), 1, 90830, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ(12, indexer_answer.CBid()); //from category
    ASSERT_EQ(11, indexer_answer.Fee());  //from shop
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule5)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90839 - журналы и газеты о связи и телефонии
    NQBid::Apply(indexer_answer, D(), D(), 1, 90839, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ(12, indexer_answer.CBid()); //from category
    ASSERT_EQ(11, indexer_answer.Fee());  //from shop
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule6)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90477 - автохимия
    NQBid::Apply(indexer_answer, D(), D(), 4, 90477, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ(15, indexer_answer.CBid()); //from shop
    ASSERT_EQ(10, indexer_answer.Fee());  //default
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule7)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90829 - книжки
    NQBid::Apply(indexer_answer, D(), D(), 4, 90829, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ( 9, indexer_answer.CBid()); //from category
    ASSERT_EQ(10, indexer_answer.Fee());  //default
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule8)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 4, 90886, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ( 9, indexer_answer.CBid()); //from category
    ASSERT_EQ(10, indexer_answer.Fee());  //default
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule9)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90830 - журналы и газеты
    NQBid::Apply(indexer_answer, D(), D(), 4, 90830, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ(12, indexer_answer.CBid()); //from category
    ASSERT_EQ(10, indexer_answer.Fee());  //default
}

TEST_F(TQBidEngineTest, Legacy_testCategoryRule10)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90839 - журналы и газеты о связи и телефонии
    NQBid::Apply(indexer_answer, D(), D(), 4, 90839, "OfferTitle", 10, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(15, indexer_answer.YBid());  //from shop
    ASSERT_EQ(12, indexer_answer.CBid()); //from category
    ASSERT_EQ(10, indexer_answer.Fee());  //default
}

TEST_F(TQBidEngineTest, Legacy_testTitleRule1)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 3, 90886, "OfferTitle", 310, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(22, indexer_answer.YBid());
    ASSERT_EQ(22, indexer_answer.CBid());
    ASSERT_EQ(10, indexer_answer.Fee());
}

TEST_F(TQBidEngineTest, Legacy_testTitleRule2)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 6, 90886, "OfferTitle", 610, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(22, indexer_answer.YBid());
    ASSERT_EQ(22, indexer_answer.CBid());
    ASSERT_EQ(25, indexer_answer.Fee());
}

TEST_F(TQBidEngineTest, Legacy_testOfferIdRule1)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 7, 90886, "OfferTitle", 700, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(23, indexer_answer.YBid());
    ASSERT_EQ(23, indexer_answer.CBid());
    ASSERT_EQ(33, indexer_answer.Fee());
}

TEST_F(TQBidEngineTest, Legacy_testOfferIdFlagDontPullUpBids1)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 7, 90886, "OfferTitle", 715, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(24, indexer_answer.YBid());
    ASSERT_EQ(25, indexer_answer.CBid());
    ASSERT_EQ(28, indexer_answer.Fee());
    ASSERT_EQ(1, indexer_answer.FlagDontPullUpBids());
}

TEST_F(TQBidEngineTest, Legacy_testOfferIdRule2)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(10);
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 7, 90886, "OfferTitle", 701, "1", ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(24, indexer_answer.YBid());
    ASSERT_EQ(25, indexer_answer.CBid());
    ASSERT_EQ(28, indexer_answer.Fee());
}

TEST_F(TQBidEngineTest, testIncorrectCBid)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0); //<-- zero(0)
    //90886 - книжки по бизнесу
    NQBid::Apply(indexer_answer, D(), D(), 8, 90886, "SomeUnknownTitle", 702, "1", CreateMinBids(11, 11), nullptr, BT());
    ASSERT_EQ(20, indexer_answer.YBid());
    ASSERT_EQ(11, indexer_answer.CBid());
    ASSERT_EQ(200, indexer_answer.Fee());
}

TEST_F(TQBidEngineTest, testWikiExample)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);
    NQBid::Apply(indexer_answer, D(), D(), 9, 90401, "OfferTitle", 0, "1", CreateMinBids(10, 100), nullptr, BT());

    ASSERT_EQ(20, indexer_answer.YBid());
    ASSERT_EQ(20, indexer_answer.MBid());
    ASSERT_EQ(20, indexer_answer.CBid());
    ASSERT_EQ(100,indexer_answer.Fee());
}

TEST_F(TQBidEngineTest, OfferIdMBidMustBeEmpty)
{
    TBid::TData offer_id_naked;

    //90886 - книжки по бизнесу
    NQBid::Apply(D(), D(), offer_id_naked, 7, 90886, "OfferTitle", 701, "1",  ZeroMinBids(), nullptr, BT());
    ASSERT_EQ(24, offer_id_naked.YBid());
    ASSERT_EQ( 0, offer_id_naked.MBid());
    ASSERT_EQ(25, offer_id_naked.CBid());
    ASSERT_EQ(28, offer_id_naked.Fee());
}

TEST_F(TQBidEngineTest, testFeedBids)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);
    indexer_answer.SetYBid(50);

    NQBid::Apply(indexer_answer, D(), D(), 666 /* nonexistent shop id */, 90401, "OfferTitle", 0, "1", CreateMinBids(10, 100), nullptr, BT());

    ASSERT_EQ(50, indexer_answer.YBid());
    ASSERT_EQ(50, indexer_answer.MBid());
    ASSERT_EQ(50, indexer_answer.CBid());
    ASSERT_EQ(100,indexer_answer.Fee());
}

TEST_F(TQBidEngineTest, testEmptyBids)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);

    NQBid::Apply(indexer_answer, D(), D(), 666 /* nonexistent shop id */, 90401, "OfferTitle", 0, "1", CreateMinBids(10, 30), nullptr, BT());

    ASSERT_EQ(10, indexer_answer.YBid());
    ASSERT_EQ(10, indexer_answer.MBid());
    ASSERT_EQ(10, indexer_answer.CBid());
    ASSERT_EQ(30,indexer_answer.Fee());
}


TEST_F(TQBidEngineTest, testMergeBids1)
{
    //проверяем мерж ставок с одинаковым offerid и feedid
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);

    NQBid::Apply(indexer_answer, D(), D(), 99, 90401, "OfferTitle", 800, "1", ZeroMinBids(), nullptr, BT());

    ASSERT_EQ(8, indexer_answer.YBid());   //FEED_OFFER_ID ставка
    ASSERT_EQ(8, indexer_answer.CBid());   //FEED_OFFER_ID ставка
    ASSERT_EQ(8, indexer_answer.Fee());    //FEED_OFFER_ID ставка
}

TEST_F(TQBidEngineTest, testMergeBids2)
{
    //проверяем что пересекающиеся ставки будут отфильтовываться
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);

    NQBid::Apply(indexer_answer, D(), D(), 99, 90401, "OfferTitle", 801, "1", ZeroMinBids(), nullptr, BT());

    ASSERT_EQ(0, indexer_answer.YBid());   //FEED_OFFER_ID ставка
    ASSERT_EQ(0, indexer_answer.CBid());   //FEED_OFFER_ID ставка
    ASSERT_EQ(0, indexer_answer.Fee());    //FEED_OFFER_ID ставка
}


TEST_F(TQBidEngineTest, testDeprecatedBids1)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);

    NQBid::Apply(indexer_answer, D(), D(), 10, 90401, "OfferTitleDeprecated", 703, "2", ZeroMinBids(), nullptr, BT());

    ASSERT_EQ(26, indexer_answer.YBid());   //FEED_OFFER_ID ставка
    ASSERT_EQ(27, indexer_answer.CBid());   //FEED_OFFER_ID ставка
    ASSERT_EQ(29, indexer_answer.Fee());    //FEED_OFFER_ID ставка
}

TEST_F(TQBidEngineTest, testDeprecatedBids2)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);

    NQBid::Apply(indexer_answer, D(), D(), 10, 90830, "OfferTitleDeprecated", 703, "3", ZeroMinBids(), nullptr, BT());

    ASSERT_EQ(14, indexer_answer.YBid());   //OFFER_TITLE ставка
    ASSERT_EQ(14, indexer_answer.CBid());   //OFFER_TITLE ставка
    ASSERT_EQ(0, indexer_answer.Fee());     //default
}

TEST_F(TQBidEngineTest, testDeprecatedBids3)
{
    TBid::TData indexer_answer;
    indexer_answer.InitAll(0);

    NQBid::Apply(indexer_answer, D(), D(), 10, 90830, "SomeUnknownTitle", 703, "3", ZeroMinBids(), nullptr, BT());

    ASSERT_EQ(0, indexer_answer.YBid());   //default
    ASSERT_EQ(16, indexer_answer.CBid());  //CATEGORY_ID ставка
    ASSERT_EQ(0, indexer_answer.Fee());    //default
}

class TQBidOnModelEngineTest : public TQBidEngineTestBase
{
public:
    TQBidOnModelEngineTest() = default;
    ~TQBidOnModelEngineTest() = default;
private:
    /* TEST DATA INITIALIZATION HERE */
    void InitTestData(MBI::Parcel& parcel) override
    {
        /* vendor_ds_id, model_id, model_search_bid*/
        DumpModelId(parcel.add_bids(), 5, 50, 500);
        // model_id дублируются
        DumpModelId(parcel.add_bids(), 3, 30, 300);
        DumpModelId(parcel.add_bids(), 4, 30, 400);

        DumpModelId(parcel.add_bids(), 1, 10, 100);
        // нулевая ставка
        DumpModelId(parcel.add_bids(), 2, 20, 0);


        /*vendor_ds_id, vendor_id, model_search_bid*/
        DumpVendorId(parcel.add_bids(), 1, 10, 101);
        DumpVendorId(parcel.add_bids(), 6, 50, 201);

        DumpVendorId(parcel.add_bids(), 2, 30, 301);
        DumpVendorId(parcel.add_bids(), 2, 30, 302);    //дубликат!


        /*vendor_ds_id, vendor_id, category_id, model_search_bid*/
        DumpVendorCategoryId(parcel.add_bids(), 1, 10, 90401, 102);

        DumpVendorCategoryId(parcel.add_bids(), 7, 40, 90402, 401);
        DumpVendorCategoryId(parcel.add_bids(), 7, 40, 90829, 402);

        DumpVendorCategoryId(parcel.add_bids(), 7, 50, 90886, 501);
        DumpVendorCategoryId(parcel.add_bids(), 7, 50, 90886, 502);    //дубликат!


        // Для тестирования DepthCategoryApply
        /*vendor_ds_id, vendor_id, category_id, model_search_bid*/
        DumpVendorCategoryId(parcel.add_bids(), 2, 11, 90401, 102);
        DumpVendorCategoryId(parcel.add_bids(), 2, 11, 90477, 103);

        DumpVendorCategoryId(parcel.add_bids(), 2, 11, 90830, 104);
        DumpVendorCategoryId(parcel.add_bids(), 2, 11, 90839, 105);

        // эти две ставки уйдут как дубликаты, т.е. при подъеме по дереву
        // категорий в поисках ставок не остановимся в узле hyper_categ_id = 90829
        DumpVendorCategoryId(parcel.add_bids(), 2, 11, 90829, 106);
        DumpVendorCategoryId(parcel.add_bids(), 2, 11, 90829, 107);
        DumpVendorCategoryId(parcel.add_bids(), 3, 11, 90829, 108);
    }

    void InitEngine() override
    {
        NModelBid::Init((TMP_DIR() + "/qbid_test.meta").c_str(),
            (TMP_DIR() + "/qbid_test.pbuf.sn").c_str());
    }
};

TEST_F(TQBidOnModelEngineTest, Model_SimpleTest)
{
    using namespace NMarket;

    //1. проверим расчет необходимой памяти
    TInit init;
    init(
            TMP_DIR() + "/qbid_test.meta",
            TMP_DIR() + "/qbid_test.pbuf.sn",
            /* smallBids */ nullptr  // NOTE(a-square): model bids don't use small or prematched bids
    );

    ASSERT_EQ(init.ShopSize, sizeof(size_t));   //размер индекса - size_t (8 байт), ставок - нет
    ASSERT_EQ(init.CategorySize, sizeof(size_t));
    ASSERT_EQ(init.TitleSize, sizeof(size_t));
    ASSERT_EQ(init.OfferIdSize, sizeof(size_t));
    ASSERT_EQ(init.OfferDigitIdSize, sizeof(size_t));
    ASSERT_EQ(init.ModelIdSize, sizeof(size_t) + sizeof(TModelIdIndex) * ModelIdBidsCount);
    ASSERT_EQ(init.VendorIdSize, sizeof(size_t) + sizeof(TVendorIdIndex) * VendorIdBidsCount);
    ASSERT_EQ(init.VendorCategoryIdSize, sizeof(size_t) + sizeof(TVendorCategoryIdIndex) * VendorCategoryIdBidsCount);
    ASSERT_EQ(init.BidsSize, 8 * (ModelIdBidsCount + VendorIdBidsCount + VendorCategoryIdBidsCount));   //4б метадата + 2б ставка +  2б статус
    ASSERT_EQ(init.TitleCStringSize, 0);    //нет ставок по OFFER_TITLE
    ASSERT_EQ(init.OfferIdCStringSize, 0);  //нет ставок по OFFER_ID-строке
    ASSERT_EQ(init.TotalSize(), init.ShopSize + init.CategorySize + init.TitleSize + init.OfferIdSize + init.OfferDigitIdSize
        + init.ModelIdSize + init.VendorIdSize + init.VendorCategoryIdSize + init.BidsSize + init.TitleCStringSize + init.OfferIdCStringSize);

    //2. проверим применение ставок
    TVendorPropsRecord result;

    //2.1 ставки по MODEL_ID
    // находим
    result = NModelBid::Apply(10, 0, 0);
    ASSERT_TRUE(result == TVendorPropsRecord(100, true, 1));
    // находим нулевую ставку
    result = NModelBid::Apply(20, 0, 0);
    ASSERT_TRUE(result == TVendorPropsRecord(0, true, 2));

    // не находим
    result = NModelBid::Apply(777, 0, 0);
    ASSERT_TRUE(result == TVendorPropsRecord());
    // не находим, т.к. дублирующиеся ставки на один model_id были удалены из индекса!
    result = NModelBid::Apply(30, 0, 0);
    ASSERT_TRUE(result == TVendorPropsRecord());

    // 2.2 ставки по VENDOR_CATEGORY_ID
    // ставки по model_id = 777 нет, но есть ставка по  vendor_id + category_id
    result = NModelBid::Apply(777, 10, 90401);
    ASSERT_TRUE(result == TVendorPropsRecord(102, true, 1));

    // находим две ставки с одинаковыми vendor_id, но разными category_id
    // ставки по MODEL_ID не подходят, т.к. там дубликаты
    result = NModelBid::Apply(30, 40, 90402);
    ASSERT_TRUE(result == TVendorPropsRecord(401, true, 7));
    result = NModelBid::Apply(30, 40, 90829);
    ASSERT_TRUE(result == TVendorPropsRecord(402, true, 7));

    // не находим дубликаты. найденная ставка - по VENDOR_ID
    result = NModelBid::Apply(777, 50, 90886);
    ASSERT_TRUE(result == TVendorPropsRecord(201, true, 6));

    // хоть у нас есть подходящая ставка VENDOR_CATEGORY_ID, первой найдем ставку по MODEL_ID
    result = NModelBid::Apply(10, 40, 90401);
    ASSERT_TRUE(result == TVendorPropsRecord(100, true, 1));

    // 2.3 Ставки по VENDOR_ID
    // находим
    result = NModelBid::Apply(0, 50, 0);
    ASSERT_TRUE(result == TVendorPropsRecord(201, true, 6));

    //не находим, т.к. дубликат
    result = NModelBid::Apply(0, 30, 0);
    ASSERT_TRUE(result == TVendorPropsRecord());

    // раньше находим VENDOR_CATEGORY-ставку
    result = NModelBid::Apply(0, 10, 90401);
    ASSERT_TRUE(result == TVendorPropsRecord(102, true, 1));
    // раньше находим MODEL_ID ставку
    result = NModelBid::Apply(10, 10, 0);
    ASSERT_TRUE(result == TVendorPropsRecord(100, true, 1));


    // 2.4 Проверяем обработку дерева категорий для VENDOR_CATEGORY ставок
    // находим ставку на vendor_id = 11, hyper_categ_id = 90401
    result = NModelBid::Apply(0, 11, 90401);
    ASSERT_TRUE(result == TVendorPropsRecord(102, true, 2));
    // ставки на vendor_id = 11, hyper_categ_id = 90402 не существует, поднимаемся по дереву
    // и используем ставку на vendor_id = 11, hyper_categ_id = 90401
    result = NModelBid::Apply(0, 11, 90402);
    ASSERT_TRUE(result == TVendorPropsRecord(102, true, 2));
    //  находим ставку на vendor_id = 11, hyper_categ_id = 90477
    result = NModelBid::Apply(0, 11, 90477);
    ASSERT_TRUE(result == TVendorPropsRecord(103, true, 2));
    // находим ставку vendor_id = 11, hyper_categ_id = 90839
    result = NModelBid::Apply(0, 11, 90839);
    ASSERT_TRUE(result == TVendorPropsRecord(105, true, 2));
    // vendor_id = 11, hyper_categ_id = 90886 - поднимаемся до hyper_categ_id = 90401
    // узел hyper_categ_id = 90829 пропускаем, т.к. там дубликаты ставок
    result = NModelBid::Apply(0, 11, 90886);
    ASSERT_TRUE(result == TVendorPropsRecord(102, true, 2));
    // неверная hyper_categ_id (нет в дереве категорий) -
    // ставка по VENDOR_CATEGORY_ID не применяется, пытаемся применить ставку по VENDOR_ID..
    result = NModelBid::Apply(0, 11, 0);
    ASSERT_TRUE(result == TVendorPropsRecord());    //..не нашли
    result = NModelBid::Apply(0, 50, 0);
    ASSERT_TRUE(result == TVendorPropsRecord(201, true, 6));    //..нашли
    // если нет ставки по VENDOR_CATEGORY_ID, даже учитывая дерево категорий -
    // переключаемся на поиск ставок по VENDOR_ID..
    result = NModelBid::Apply(0, 50, 90839);
    ASSERT_TRUE(result == TVendorPropsRecord(201, true, 6));    //..нашли
    result = NModelBid::Apply(0, 777, 90839);
    ASSERT_TRUE(result == TVendorPropsRecord());    //..не нашли


    // 3. Проверим проставление статусов и генерацию выхлопа со статусами
    NModelBid::SaveAnswer(TMP_DIR() + "/qbid_test_result.pbuf.sn");
    NMarket::TSnappyProtoReader reader(TMP_DIR() + "/qbid_test_result.pbuf.sn", "BIDS");
    MBI::Parcel parcel;
    reader.Load(parcel);


    ASSERT_EQ(parcel.bids_size(), TotalBids);
    // Порядок - как дампили в InitTestData (т.е. порядок - как во входном файле, что используется при мерже)
    // Найти в нашем выхлопе можно только измененные поля ставки, а именно - publication_status
    // Потом этот выхлоп мержится с исходным входным файликом и получается результат применения ставок

    // 3.1 MODEL_ID
    // эту ставку не применяли
    ASSERT_EQ(parcel.mutable_bids(0)->value_for_model_search().publication_status(), MBI::Bid::NOT_FOUND);
    // тут две ставки дублировались, они не применены
    ASSERT_EQ(parcel.mutable_bids(1)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);
    ASSERT_EQ(parcel.mutable_bids(2)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);
    // эти две ставки применились
    ASSERT_EQ(parcel.mutable_bids(3)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    ASSERT_EQ(parcel.mutable_bids(4)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);

    // 3.2 VENDOR_ID
    //эту ставку опередили ставки по MODEL_ID и VENDOR_CATEGORY_ID
    ASSERT_EQ(parcel.mutable_bids(5)->value_for_model_search().publication_status(), MBI::Bid::NOT_FOUND);
    ASSERT_EQ(parcel.mutable_bids(6)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    //дубликаты
    ASSERT_EQ(parcel.mutable_bids(7)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);
    ASSERT_EQ(parcel.mutable_bids(8)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);

    // 3.3 VENDOR_CATEGORY_ID
    ASSERT_EQ(parcel.mutable_bids(9)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    ASSERT_EQ(parcel.mutable_bids(10)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    ASSERT_EQ(parcel.mutable_bids(11)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    //дубликаты
    ASSERT_EQ(parcel.mutable_bids(12)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);
    ASSERT_EQ(parcel.mutable_bids(13)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);

    // 3.4 VENDOR_CATEGORY_ID (тестируем дерево категорий)
    ASSERT_EQ(parcel.mutable_bids(14)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    ASSERT_EQ(parcel.mutable_bids(15)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    // это ставка на vendor_id=11, hyper_categ_id=90830, напрямую именно ее не искали,
    // искали дочернюю категорию, там была ставка и она сыгрыла, поэтому у этой NOT_FOUND
    ASSERT_EQ(parcel.mutable_bids(16)->value_for_model_search().publication_status(), MBI::Bid::NOT_FOUND);
    ASSERT_EQ(parcel.mutable_bids(17)->value_for_model_search().publication_status(), MBI::Bid::APPLIED);
    //дубликаты
    ASSERT_EQ(parcel.mutable_bids(18)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);
    ASSERT_EQ(parcel.mutable_bids(19)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);
    ASSERT_EQ(parcel.mutable_bids(20)->value_for_model_search().publication_status(), MBI::Bid::NOT_ALLOWED);
}
