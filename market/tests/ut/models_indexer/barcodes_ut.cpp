#include <market/idx/models/lib/models-indexer/barcodes.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/tempdir.h>
#include <util/stream/file.h>


TEST(BARCODE, SIMPLE) {
    TTempDir tmp("tmp");
    const TString barcodesFile = tmp() + "/barcodes.txt";

    {
        TUnbufferedFileOutput output(barcodesFile);
        output << "4242002358932\t985872\n"
                  "4242002365879\t638862\n"
                  "4242002367378\t786014\n";
    }


    NMarket::NMbo::TBarcodeStorage barcodeStorage(barcodesFile);

    ASSERT_EQ(*barcodeStorage.Find(985872)->begin(), "4242002358932");
    ASSERT_EQ(*barcodeStorage.Find(638862)->begin(), "4242002365879");
    ASSERT_EQ(*barcodeStorage.Find(786014)->begin(), "4242002367378");
}


TEST(BARCODE, GROUPED) {
    TTempDir tmp("tmp");

    const TString barcodesFile = tmp() + "/barcodes.txt";

    {
        TUnbufferedFileOutput output(barcodesFile);
        output << "4718050600687\t6152888\t6152940\n"
                  "2900000043343\t6152888\t6152940\n"
                  "001000005252\t6152888\t6152940\n"
                  "0001000005252\t6152888\t6152940\n"
                  "2029894000000\t6152888\t6152940\n"
                  "4110483349148\t6152888\t6152940\n"
                  "4242002367378\t786014\n";
    }


    NMarket::NMbo::TBarcodeStorage barcodeStorage(barcodesFile);
    const NMarket::NMbo::TBarcodes* barcodes = barcodeStorage.Find(6152940);

    TSet<TStringBuf> barcodeSet(barcodes->begin(), barcodes->end());
    ASSERT_TRUE(barcodeSet.contains("4718050600687"));
    ASSERT_TRUE(barcodeSet.contains("2900000043343"));
    ASSERT_TRUE(barcodeSet.contains("001000005252"));
    ASSERT_TRUE(barcodeSet.contains("0001000005252"));
    ASSERT_TRUE(barcodeSet.contains("2029894000000"));
    ASSERT_TRUE(barcodeSet.contains("4110483349148"));
    ASSERT_FALSE(barcodeSet.contains("4242002367378"));

    ASSERT_EQ(*barcodeStorage.Find(786014)->begin(), "4242002367378");
}
