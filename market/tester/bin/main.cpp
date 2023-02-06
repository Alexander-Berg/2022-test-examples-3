#include <market/library/libpromo/matcher/tester/src/params.h>
#include <market/library/libpromo/matcher/tester/src/promo_matcher_tester.h>

#include <library/cpp/getopt/small/last_getopt.h>
#include <library/cpp/logger/global/global.h>

#include <util/folder/path.h>

/*
Добавить CXXFLAGS(-DTRACE_MATCHER) в ya.make для трассировки
Пример вызова для тестинга
$ ./promo_matcher_tester --table-promo '//home/market/testing/indexer/stratocaster/promos/collected_promo_details/20211216_1622' \
--tovar-tree '/var/lib/yandex/indexer/market/last_complete/last_complete/input/mbo/tovar-tree.pb.gz' \
--shops-dat '/var/lib/yandex/indexer/market/last_complete/last_complete/report-data/shops.dat' \
--table-genlog '//home/market/testing/indexer/stratocaster/mi3/main/last_complete/genlog/0011' \
--yt-server-name 'arnold.yt.yandex.net' --yt-token-path '/etc/datasources/yt-market-indexer' \
--waremd5 '<offer_ware_md5>'
*/

int main(int argc, char** argv) {
    InitGlobalLog2Console();

    INFO_LOG << "PromoMatcherTester" << Endl;

    auto params = TParams();

    auto opts = NLastGetopt::TOpts::Default();
    opts.AddLongOption("yt-server-name").StoreResult(&params.YtServerName).DefaultValue("hahn.yt.yandex.net").Help("Yt Server name");
    opts.AddLongOption("yt-token-path").StoreResult(&params.YtTokenPath).Help("Yt token path");
    opts.AddLongOption("table-promo").StoreResult(&params.TablePromo).Required().DefaultValue("//home/market/production/indexer/stratocaster/promos/collected_promo_details").Help("Path to table with promos");
    opts.AddLongOption("table-genlog").StoreResult(&params.TableGenlog).Optional().Help("Path to table with genlogs, eg. //home/market/production/indexer/gibson/mi3/main/last_complete/genlog/0000");

    opts.AddLongOption("tovar-tree").StoreResult(&params.CategoryTreeFile).Required().Help("tovar-tree.pb.gz");
    opts.AddLongOption("shops-dat").StoreResult(&params.ShopsDatFile).Required().Help("shops.dat");

    /// TODO сделать поиск атрибутов оффера по ware_md5
    opts.AddLongOption("waremd5").AppendTo(&params.WareMd5s).Optional().Help("List of offer's waremd5s");

    opts.AddLongOption("promo_key").StoreResult(&params.PromoKey).Optional().Help("promo_key to filter out others promos");

    opts.AddLongOption("hid").StoreResult(&params.Hid).Optional().Help("offer HID (category)");
    opts.AddLongOption("msku").StoreResult(&params.Msku).Optional().Help("offer MSKU");
    opts.AddLongOption("shop-id").StoreResult(&params.ShopId).Optional().Help("offer shop_id");
    opts.AddLongOption("vendor-id").StoreResult(&params.VendorId).Optional().Help("offer vendor_id");
    opts.AddLongOption("feed-id").StoreResult(&params.FeedId).Optional().Help("offer feed_id");
    opts.AddLongOption("warehouse-id").StoreResult(&params.WarehouseId).Optional().Help("offer warehouse_id");

    NLastGetopt::TOptsParseResult res(&opts, argc, argv);

    INFO_LOG << "Yt server:                   " << params.YtServerName << Endl;
    INFO_LOG << "Yt token path:               " << params.YtTokenPath << Endl;

    try {
        TPromoMatcherTester(params).Run();
        return 0;
    } catch (const std::exception& e) {
        FATAL_LOG << "died with exception " << e.what() << Endl;
    } catch (...) {
        FATAL_LOG << "died with unknown reason" << Endl;
    }

    return 1;
}
