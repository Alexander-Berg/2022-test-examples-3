#include <market/report/src/wizards/util/model_data.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

TEST(NMarketReport, GetModelName) {
    // Все кейсы из реального индекса на февраль 2020 года
    const TVector<std::tuple<TString, TString, TString, TString>> testCases({
        {"Радар-детектор Mongoose HD-200", "Радар-детектор HD-200,", "Mongoose HD-200", "HD-200"}, // Обычный случай
        {"Matopat марля перевязочная нестерильная 17-нит. Matocomp", "Марля перевязочная нестерильная 17-нит. Matocomp", "Matopat марля перевязочная нестерильная 17-нит. Matocomp", "Марля перевязочная нестерильная 17-нит. Matocomp"}, // Вендор в начале строки, вместо середины, и различие в регистрах русских букв
        {"12 Parfumeurs  Francais Le Fantome ", " Le  Fantome", "12 Parfumeurs Francais Le Fantome", "Le Fantome"}, // Лишние пробелы и несовпадение начал тайтлов
        {"Корм для собак Canagan (12 кг) For large breed dogs GF Free-Run Chicken{", "Корм для собак (12 кг) For large breed dogs GF Free-Run Chicken", "Canagan (12 кг) For large breed dogs GF Free-Run Chicken", "(12 кг) For large breed dogs GF Free-Run Chicken"}, // Лишние символы в конце строки
        {" Наушники Creative Aurvana Live!", "Наушники  Aurvana Live! ", "Creative Aurvana Live!", "Aurvana Live!"}, // Нелишние символы в конце строки и лишние пробелы
        {"Линзы Acuvue Oasys (12 линз)", "Линзы Oasys (12 линз)", "Acuvue Oasys (12 линз)", "Oasys (12 линз)"}, // Нелишние символы в конце строки
        {"Apple iPhone 8 64GB", "", "Apple iPhone 8 64GB", "Apple iPhone 8 64GB"}, // отсутствует titleNoVendor
        {"Apple iPhone 8 64GB", "Apple iPhone 8 64GB", "Apple iPhone 8 64GB", "Apple iPhone 8 64GB"} // дублируются тайтлы
    });
    for (const auto& [title, titleNoVendor, titleNoCategory, titleNoVendorNoCategory] : testCases) {
        EXPECT_EQ(titleNoCategory, GetModelName(title, titleNoVendor, /*noCategory*/ true, /*noVendor*/ false));
        EXPECT_EQ(titleNoVendorNoCategory, GetModelName(title, titleNoVendor, /*noCategory*/ true, /*noVendor*/ true));
    }
}
