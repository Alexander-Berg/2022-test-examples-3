#include <contrib/libs/flatbuffers/include/flatbuffers/flatbuffers.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/media_adv/incut_search/access/resources/reserve_prices/reserve_prices.h>
#include <market/media_adv/incut_search/access/resources/reserve_prices/reserve_prices.fbs.h>
#include <filesystem>
#include <fstream>

namespace fs = std::filesystem;
using namespace NMarket::NMediaAdv::NAccess::NResource;

class TReservePricesFBTest: public ::testing::Test {
public:
    void SetUp() final {
        /*
        flatbuffers::FlatBufferBuilder builder;

        std::vector<flatbuffers::Offset<NReservePrices::ReservePrices>> rpVector;
        for(int i=1; i<5; i++){
            auto rpValue = NReservePrices::CreateReservePrices(builder, i, i*10);
            rpVector.push_back(rpValue);
        }
        auto resPrices = NReservePrices::CreateReservePricesMap(builder, builder.CreateVector(rpVector));
        builder.Finish(resPrices);

        fs::path examplePath(FileExample.c_str());
        std::ofstream ofs(FileExample);
        const char* ptr = reinterpret_cast<char*>(builder.GetBufferPointer());
        ofs.write(ptr, builder.GetSize());
        ofs.close();

        fs::path examplePathEmpty(FileExampleEmpty.c_str());
        std::ofstream ofsEmpty(FileExampleEmpty);
        ofsEmpty << "some data";
        ofsEmpty.close();
        */
    }

    void TearDown() final {
        fs::remove(FileExample.c_str());
        fs::remove(FileExampleEmpty.c_str());
    }

protected:
    TString FileExample = "file_example.fb";
    TString FileExampleEmpty = "file_example_empty.fb";
};

TEST_F(TReservePricesFBTest, Load) {
    /*
    NMarket::NMediaAdv::NAccess::NResource::TReservePricesFB reservePrices;
    fs::path path(FileExample.c_str());
    fs::path pathEmpty(FileExampleEmpty.c_str());
    ASSERT_TRUE(reservePrices.Load(path.string()));
    ASSERT_FALSE(reservePrices.Load(pathEmpty.string()));    //empty file
    */
    //without Access mocking is not working
    /*using TRp = NMarket::NMediaAdv::NAccess::NResource::TReservePricesFB::TRp;
    EXPECT_EQ(TRp(10), reservePrices.GetReservePrice(1));
    EXPECT_EQ(TRp(20), reservePrices.GetReservePrice(2));
    EXPECT_EQ(TRp(30), reservePrices.GetReservePrice(3));
    EXPECT_EQ(TRp(0), reservePrices.GetReservePrice(10));
    */
}
