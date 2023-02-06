#include <market/library/installment_options/installment_options.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>
#include <util/generic/vector.h>

using TCategories = TVector<uint64_t>;
using TVendors = TVector<uint64_t>;
using TInstallmentTimes = TVector<uint32_t>;

struct TTestOptions {
    uint64_t ShopId = 0;
    TCategories Categories;
    TVendors Vendors;
    TInstallmentTimes InstallmentTimes;
    TString StartDate;
    TString EndDate;
    TMaybe<bool> BnplAvailable;
    TVector<uint64_t> Skus;
    bool EmptyOfferId = false;
};

TString SaveToProto(const TString& pbufPath, const TVector<TTestOptions> options, const TString& version = "") {
    MarketIndexer::MbiInstallmentOptions::InstallmentOptionsStorage optionsStorage;
    if (version) {
        optionsStorage.set_version(version);
    }
    for (size_t i = 0; i < options.size();) {
        const auto shopId = options[i].ShopId;
        if (!shopId) {
            ++i;
            continue;
        }
        auto* shopOptions = optionsStorage.mutable_shops_options()->Add();
        shopOptions->set_shop_id(shopId);
        size_t groupIndex = 1;
        while (i < options.size() && options[i].ShopId == shopId) {
            auto* optionsGroup = shopOptions->mutable_options()->Add();
            TString groupName = "group " + ToString(groupIndex++) + " for shop " + ToString(shopId);
            optionsGroup->set_group_name(groupName);
            for (const auto& category: options[i].Categories) {
                optionsGroup->mutable_categories()->Add(category);
            }
            for (const auto& vendor: options[i].Vendors) {
                optionsGroup->mutable_vendors()->Add(vendor);
            }
            for (const auto& installmentTime: options[i].InstallmentTimes) {
                optionsGroup->mutable_installment_time_in_days()->Add(installmentTime);
            }
            if (options[i].StartDate) {
                optionsGroup->set_start_date(options[i].StartDate);
            }
            if (options[i].EndDate) {
                optionsGroup->set_end_date(options[i].EndDate);
            }
            if (options[i].BnplAvailable.Defined()) {
                optionsGroup->set_bnpl_available(*options[i].BnplAvailable);
            }
            for (const auto& sku: options[i].Skus) {
                auto* skuNote = optionsGroup->add_sku_list();
                skuNote->set_market_sku(sku);
                if (options[i].EmptyOfferId) {
                    continue;
                }
                skuNote->set_shop_sku("offer 1 for sku " + ToString(sku));
                if (sku % 2 == 0) {
                    skuNote = optionsGroup->add_sku_list();
                    skuNote->set_market_sku(sku);
                    skuNote->set_shop_sku("offer 2 for sku " + ToString(sku));
                }
            }
            ++i;
        }
    }

    TFileOutput out(pbufPath);
    optionsStorage.SerializeToArcadiaStream(&out);
    return pbufPath;
}

Y_UNIT_TEST_SUITE(TInstallmentOptionsTests) {
    Y_UNIT_TEST(TestBaseReading) {
        TVector<TTestOptions> options = {
            /* shop_id -> params */
            {.ShopId = 100500, .Categories = {100, 200, 300}, .InstallmentTimes = {31, 365}, .BnplAvailable = true},
            {.ShopId = 100500, .Vendors = {1000, 2000}, .InstallmentTimes = {365}, .StartDate = "2020-12-05", .EndDate = "2030-12-05"},
            {.Vendors = {3000}, .InstallmentTimes = {365}}, // для проверки, что запись без shop_id проигнорится
            {.ShopId = 100501, .Categories = {100}, .Vendors = {1000}, .InstallmentTimes = {365}},
            {.ShopId = 100502, .InstallmentTimes = {100}, .Skus = {1001, 1002}},
        };
        TTempDir tmp;
        const TString pbufPath = tmp.Path() / "installment_options1.pb";
        SaveToProto(pbufPath, options, "20211124_0119");

        const auto& storage = TInstallmentOptionsStorage(pbufPath);
        UNIT_ASSERT_EQUAL(storage.ShopsCount(), 3);
        UNIT_ASSERT_EQUAL(storage.GetFileVersion(), "20211124_0119");

        const auto& options100500 = *storage.GetAllOptions(100500);
        UNIT_ASSERT_EQUAL(options100500.size(), 2);
        const auto& group0 = options100500[0];
        const auto& group1 = options100500[1];
        UNIT_ASSERT_EQUAL(group0.GroupName, "group 1 for shop 100500");
        UNIT_ASSERT_EQUAL(group0.Categories.size(), 3);
        UNIT_ASSERT_EQUAL(group0.Vendors.size(), 0);
        UNIT_ASSERT_EQUAL(group0.Skus.size(), 0);
        UNIT_ASSERT_EQUAL(group0.InstallmentTimeInDays.size(), 2);
        // В первой группе срок действий опций не задан
        UNIT_ASSERT(!group0.StartDate.Defined());
        UNIT_ASSERT(!group0.EndDate.Defined());
        UNIT_ASSERT(group0.BnplAvailable);

        UNIT_ASSERT_EQUAL(group1.GroupName, "group 2 for shop 100500");
        UNIT_ASSERT_EQUAL(group1.Categories.size(), 0);
        UNIT_ASSERT_EQUAL(group1.Vendors.size(), 2);
        UNIT_ASSERT_EQUAL(group1.InstallmentTimeInDays.size(), 1);
        UNIT_ASSERT_EQUAL(group1.StartDate->ToString(), "2020-12-05T00:00:00.000000Z");
        UNIT_ASSERT_EQUAL(group1.EndDate->ToString(), "2030-12-05T00:00:00.000000Z");
        UNIT_ASSERT(!group1.BnplAvailable);

        const auto& options100501 = *storage.GetAllOptions(100501);
        UNIT_ASSERT_EQUAL(options100501.size(), 1);
        const auto& group2 = options100501[0];
        UNIT_ASSERT_EQUAL(group2.GroupName, "group 1 for shop 100501");
        UNIT_ASSERT_EQUAL(*group2.Categories.begin(), 100);
        UNIT_ASSERT_EQUAL(*group2.Vendors.begin(), 1000);
        UNIT_ASSERT_EQUAL(group2.InstallmentTimeInDays[0], 365);

        const auto& options100503 = *storage.GetAllOptions(100502);
        UNIT_ASSERT_EQUAL(options100503.size(), 1);
        const auto& group3 = options100503[0];
        UNIT_ASSERT_EQUAL(group3.GroupName, "group 1 for shop 100502");
        UNIT_ASSERT_EQUAL(group3.Categories.size(), 0);
        UNIT_ASSERT_EQUAL(group3.Vendors.size(), 0);
        UNIT_ASSERT_EQUAL(group3.Skus.size(), 2);
        UNIT_ASSERT(group3.Skus.FindPtr(1001)->contains("offer 1 for sku 1001"));
        UNIT_ASSERT(group3.Skus.FindPtr(1002)->contains("offer 1 for sku 1002"));
        UNIT_ASSERT(group3.Skus.FindPtr(1002)->contains("offer 2 for sku 1002"));
        UNIT_ASSERT_EQUAL(group3.InstallmentTimeInDays[0], 100);
    }

    Y_UNIT_TEST(TestInvalidDate) {
        TVector<TTestOptions> options = {
            {.ShopId = 100502, .StartDate = "AAAA"},
            {.ShopId = 100503, .EndDate = "BBBBB"},
            {.ShopId = 100504, .StartDate = "12345-10-11"},
            {.ShopId = 100505, .StartDate = "2020-20-11"},
            {.ShopId = 100506, .StartDate = "2020.10.11"},
            {.ShopId = 100507, .StartDate = "2020-10-11", .EndDate = "2030-10-11"},
        };
        TTempDir tmp;
        const TString pbufPath = tmp.Path() / "installment_options2.pb";
        SaveToProto(pbufPath, options);

        const auto& storage = TInstallmentOptionsStorage(pbufPath);
        UNIT_ASSERT_EQUAL(storage.ShopsCount(), 1);
        UNIT_ASSERT(storage.GetAllOptions(100507));
    }

    Y_UNIT_TEST(TestSuitableOptions) {
        TVector<TTestOptions> options = {
            /* shop_id -> params */
            {.ShopId = 100508, .Categories = {100, 200, 300}},
            {.ShopId = 100508, .Vendors = {1000, 2000}},
            // специальная рассрочка для айфонов
            {.ShopId = 100508, .Categories = {100}, .Vendors = {3000}},
            {.ShopId = 100509},
            // рассрочка для отдельных скю
            {.ShopId = 100510, .Skus = {1100, 1200}},
            // такой записи не должно быть: в реальных или категории с вендорами, или скю
            // но все-таки покроем тестами текущую логику
            {.ShopId = 100510, .Categories = {200}, .Skus = {2100}},
            {.ShopId = 100511, .Skus = {0}}, // с нулевой скю должно работать
            {.ShopId = 100511, .Skus = {1000}, .EmptyOfferId = true}, // а с пустым offerId нет
        };
        TTempDir tmp;
        const TString pbufPath = tmp.Path() / "installment_options3.pb";
        SaveToProto(pbufPath, options);

        const auto& storage = TInstallmentOptionsStorage(pbufPath);
        UNIT_ASSERT_EQUAL(storage.GetFileVersion(), "");

        // Магазин без опций
        auto result = storage.GetSuitableOptions(100500, 100, 1000);
        UNIT_ASSERT_EQUAL(result.size(), 0);

        // Есть категорийная опция, нет вендорной
        result = storage.GetSuitableOptions(100508, 300, 5000);
        UNIT_ASSERT_EQUAL(result.size(), 1);
        UNIT_ASSERT_EQUAL(result[0]->GroupName, "group 1 for shop 100508");

        // Есть вендорная опция, нет категорийной
        result = storage.GetSuitableOptions(100508, 500, 1000);
        UNIT_ASSERT_EQUAL(result.size(), 1);
        UNIT_ASSERT_EQUAL(result[0]->GroupName, "group 2 for shop 100508");

        // Есть и категорийная опция, и вендорная
        result = storage.GetSuitableOptions(100508, 100, 1000);
        UNIT_ASSERT_EQUAL(result.size(), 2);
        UNIT_ASSERT_EQUAL(result[0]->GroupName, "group 1 for shop 100508");
        UNIT_ASSERT_EQUAL(result[1]->GroupName, "group 2 for shop 100508");

        // Есть общекатегорийная опция, и специальная опция для айфонов
        result = storage.GetSuitableOptions(100508, 100, 3000);
        UNIT_ASSERT_EQUAL(result.size(), 2);
        UNIT_ASSERT_EQUAL(result[0]->GroupName, "group 1 for shop 100508");
        UNIT_ASSERT_EQUAL(result[1]->GroupName, "group 3 for shop 100508");

        // У магазина 100509 опция для всех товаров
        result = storage.GetSuitableOptions(100509, 123, 321);
        UNIT_ASSERT_EQUAL(result.size(), 1);
        UNIT_ASSERT_EQUAL(result[0]->GroupName, "group 1 for shop 100509");

        // В магазине 100510 проверяем опции для скю
        result = storage.GetSuitableOptions(100510, 100, 321, 1100, "offer 1 for sku 1100");
        UNIT_ASSERT_EQUAL(result.size(), 1);
        UNIT_ASSERT_EQUAL(result[0]->GroupName,  "group 1 for shop 100510"); // опция для скю 1100
        // неправильный айдишник оффера
        result = storage.GetSuitableOptions(100510, 100, 321, 1100, "offer");
        UNIT_ASSERT_EQUAL(result.size(), 0);

        const auto resultOffer1 = storage.GetSuitableOptions(100510, 200, 321, 1200, "offer 1 for sku 1200");
        const auto resultOffer2 = storage.GetSuitableOptions(100510, 200, 321, 1200, "offer 2 for sku 1200");
        UNIT_ASSERT_EQUAL(resultOffer1.size(), 1);
        UNIT_ASSERT_EQUAL(resultOffer2.size(), 1);
        UNIT_ASSERT_EQUAL(resultOffer1[0], resultOffer2[0]);
        UNIT_ASSERT_EQUAL(resultOffer1[0]->GroupName,  "group 1 for shop 100510"); // опция для скю 1200
        // подходит еще под "плохую" опцию по категории 200
        // но если в опции есть скю, то категории и вендора игнорятся

        result = storage.GetSuitableOptions(100510, 300, 321, 2100, "offer 1 for sku 2100");
        // не совпадает хид в запросе и опции, но из-за игнора категории опция находится
        UNIT_ASSERT_EQUAL(result.size(), 1);
        UNIT_ASSERT_EQUAL(result[0]->GroupName,  "group 2 for shop 100510");

        // В магазине 100511 проверяем опции с нулевым скю и с пустым оффером
        result = storage.GetSuitableOptions(100511, 123, 321, 0, "offer 1 for sku 0");
        UNIT_ASSERT_EQUAL(result.size(), 1);
        result = storage.GetSuitableOptions(100511, 123, 321, 1000, "");
        UNIT_ASSERT_EQUAL(result.size(), 0);
    }

    Y_UNIT_TEST(TestCategoriesAncestors) {
        MarketIndexer::MbiInstallmentOptions::InstallmentOptionsStorage optionsStorage;
        auto* shopOptions = optionsStorage.mutable_shops_options()->Add();
        shopOptions->set_shop_id(100);
        auto* optionsAppliances = shopOptions->mutable_options()->Add();
        optionsAppliances->set_group_name("all appliances departament");
        optionsAppliances->mutable_categories()->Add(1000);

        auto optionsKitchen = shopOptions->mutable_options()->Add();
        optionsKitchen->set_group_name("large appliances for kitchen");
        optionsKitchen->mutable_categories()->Add(1100);

        //1110 - категория "холодильники". Она листовая и своих собственных опций не имеет
        auto* categoriesAncestors = optionsStorage.mutable_categories_ancestors();
        (*categoriesAncestors)[1110] = 1100; // холодильники -> бытовая техника для кухни
        (*categoriesAncestors)[1100] = 1000; // для кухни -> бытовая техника

        TTempDir tmp;
        const TString pbufPath = tmp.Path() / "installment_options4.pb";
        TFileOutput out(pbufPath);
        optionsStorage.SerializeToArcadiaStream(&out);
        out.Finish();

        const auto& storage = TInstallmentOptionsStorage(pbufPath);
        // Для холодильников получаем опции и из "техники для кухни" и из всей "бытовой техники"
        const auto result = storage.GetSuitableOptions(100, 1110, 500);
        UNIT_ASSERT_EQUAL(result.size(), 2);
        UNIT_ASSERT_EQUAL(result[0]->GroupName, "all appliances departament");
        UNIT_ASSERT_EQUAL(result[1]->GroupName, "large appliances for kitchen");

        UNIT_ASSERT_EQUAL(storage.ParentCategory(1110), 1100);
        UNIT_ASSERT_EQUAL(storage.ParentCategory(1100), 1000);
        UNIT_ASSERT_EQUAL(storage.ParentCategory(1000), 0);
        UNIT_ASSERT_EQUAL(storage.ParentCategory(100500), 0);
    }
}
