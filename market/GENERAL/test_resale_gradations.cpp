#include <market/library/resale_gradations/resale_gradations.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>

void SaveToFile(const TString& filename, const TString& content) {
    TFileOutput out(filename);
    out << content;
    out.Finish();
}

bool operator==(const NResaleGradations::TGradationsNames& lhs, const NResaleGradations::TGradationsNames& rhs) {
    return lhs.Perfect_ == rhs.Perfect_ && lhs.Excellent_ == rhs.Excellent_ && lhs.Good_ == rhs.Good_;
}

Market::TCategoryId NormalParentCategory(Market::TCategoryId hid) {
    /* дерево
        100
      /     \
    50      150
      \
      60
    */
    if (hid == 50 || hid == 150) {
        return 100;
    }
    if (hid == 60) {
        return 50;
    }
    return 0;
}

Market::TCategoryId InvalidParentCategory(Market::TCategoryId) {
    // проверка подстраховки от зацикливания
    return 12345;
}


Y_UNIT_TEST_SUITE(TestResaleGradations) {
    Y_UNIT_TEST(TestWrongPath) {
        NResaleGradations::TStorage storageWithValidation("/a/b/c", /* validate */ true);
        UNIT_ASSERT_EQUAL(storageWithValidation.CategoriesCount(), 0);
        UNIT_ASSERT_EQUAL(storageWithValidation.GetErrors()[0], "Can't open file /a/b/c");

        NResaleGradations::TStorage storageWithoutValidation("/a/b/c");
        UNIT_ASSERT_EQUAL(storageWithoutValidation.CategoriesCount(), 0);
        UNIT_ASSERT(storageWithoutValidation.GetErrors().empty());
    }

    Y_UNIT_TEST(TestWrongRootFormat) {
        TTempDir tmp;

        const TString pathInvaludJson = tmp.Path() / "invalid.json";
        SaveToFile(pathInvaludJson, "{]");
        NResaleGradations::TStorage storage1(pathInvaludJson, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage1.CategoriesCount(), 0);
        UNIT_ASSERT_EQUAL(storage1.GetErrors()[0], "Failing to parse " + pathInvaludJson);

        const TString pathNotMap = tmp.Path() / "not_map.json";
        SaveToFile(pathNotMap, "[]");
        NResaleGradations::TStorage storage2(pathNotMap, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage2.CategoriesCount(), 0);
        UNIT_ASSERT_EQUAL(storage2.GetErrors()[0], "File " + pathNotMap + " must contain a map at the top level");

        const TString pathNoGradations = tmp.Path() / "no_gradation.json";
        SaveToFile(pathNoGradations, "{\"reasons\": {}}");
        NResaleGradations::TStorage storage3(pathNoGradations, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage3.CategoriesCount(), 0);
        UNIT_ASSERT_EQUAL(storage3.GetErrors()[0], "File " + pathNoGradations + " must contain a 'gradations' array");

        const TString pathBadGradations = tmp.Path() / "bad_gradation.json";
        SaveToFile(pathBadGradations, "{\"gradations\": {}, \"reasons\": {}}");
        NResaleGradations::TStorage storage4(pathBadGradations, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage4.CategoriesCount(), 0);
        UNIT_ASSERT_EQUAL(storage4.GetErrors()[0], "File " + pathBadGradations + " must contain a 'gradations' array");

        const TString pathNoReasons = tmp.Path() / "no_reasons.json";
        SaveToFile(pathNoReasons, "{\"gradations\": []}");
        NResaleGradations::TStorage storage5(pathNoReasons, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage5.CategoriesCount(), 0);
        UNIT_ASSERT_EQUAL(storage5.GetErrors()[0], "File " + pathNoReasons + " must contain a 'reasons' map");

        const TString pathBadReasons = tmp.Path() / "bad_reasons.json";
        SaveToFile(pathBadReasons, "{\"gradations\": [], \"reasons\": []}");
        NResaleGradations::TStorage storage6(pathBadReasons, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage6.CategoriesCount(), 0);
        UNIT_ASSERT_EQUAL(storage6.GetErrors()[0], "File " + pathBadReasons + " must contain a 'reasons' map");
    }

    Y_UNIT_TEST(TestBadParams) {
        TTempDir tmp;
        const TString path = tmp.Path() / "bad_params.json";
        SaveToFile(path,
"{"
"  \"gradations\": ["
"    {\"perfect_text\": \"a\", \"excellent_text\": \"b\", \"good_text\": \"c\"},\n" // нет category_id
"    {\"category_id\": \"123\", \"perfect_text\": \"a\", \"excellent_text\": \"b\", \"good_text\": \"c\"},\n" // category_id строка, а не число
"    {\"category_id\": 123, \"excellent_text\": \"b\", \"good_text\": \"c\"},\n" // нет perfect_text
"    {\"category_id\": 123, \"perfect_text\": {}, \"excellent_text\": \"b\", \"good_text\": \"c\"},\n" // perfect_text не строка
"    {\"category_id\": 123, \"perfect_text\": \"a\", \"good_text\": \"c\"},\n" // нет excellent_text
"    {\"category_id\": 123, \"perfect_text\": \"a\", \"excellent_text\": 1, \"good_text\": \"c\"},\n" // excellent_text не строка
"    {\"category_id\": 123, \"perfect_text\": \"a\", \"excellent_text\": \"b\"},\n" // нет good_text
"    {\"category_id\": 123, \"perfect_text\": \"a\", \"excellent_text\": \"b\", \"good_text\": []},\n" // good_text не строка
"    {\"category_id\": 456, \"perfect_text\": \"d\", \"excellent_text\": \"e\", \"good_text\": \"f\"}\n" // единственная нормальная строка
"  ],"
"  \"reasons\": {"
"    \"abc\": \"used\"," // ключ не число
"    \"2\": [\"showcase_sample\"]," // значение не строка
"    \"3\": \"reduction\"" // нормальная строка
"  }"
"}"
    );
        NResaleGradations::TStorage storage(path, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage.CategoriesCount(), 1); // в файле есть нормальная запись
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(456), NResaleGradations::TGradationsNames({.Perfect_ = "d", .Excellent_ = "e", .Good_ = "f"}));
        UNIT_ASSERT_EQUAL(*storage.GetReasonText(3), "reduction"); // и нормальное описание проблемы
        const auto& errors = storage.GetErrors();
        UNIT_ASSERT_EQUAL(errors.size(), 10);
        UNIT_ASSERT_EQUAL(errors[0], "index 1: bad param category_id");
        UNIT_ASSERT_EQUAL(errors[1], "index 2: bad param category_id");
        UNIT_ASSERT_EQUAL(errors[2], "index 3: bad param perfect_text");
        UNIT_ASSERT_EQUAL(errors[3], "index 4: bad param perfect_text");
        UNIT_ASSERT_EQUAL(errors[4], "index 5: bad param excellent_text");
        UNIT_ASSERT_EQUAL(errors[5], "index 6: bad param excellent_text");
        UNIT_ASSERT_EQUAL(errors[6], "index 7: bad param good_text");
        UNIT_ASSERT_EQUAL(errors[7], "index 8: bad param good_text");

        const TString reasonError1 = "reason type must be a number: 'abc'";
        const TString reasonError2 = "reason is not string, key: 2";
        UNIT_ASSERT(errors[8] == reasonError1 && errors[9] == reasonError2 ||
                    errors[8] == reasonError2 && errors[9] == reasonError1);
    }

    Y_UNIT_TEST(TestBase) {
        TTempDir tmp;
        const TString path = tmp.Path() / "normal_file.json";
        SaveToFile(path,
"{"
"  \"gradations\": ["
"    {\"category_id\": 123, \"perfect_text\": \"a\", \"excellent_text\": \"b\", \"good_text\": \"c\"},\n"
"    {\"category_id\": 456, \"perfect_text\": \"d\", \"excellent_text\": \"e\", \"good_text\": \"f\"}\n"
"  ],"
"  \"reasons\": {"
"    \"1\": \"used\","
"    \"2\": \"showcase_sample\","
"    \"3\": \"reduction\","
"    \"10000\": \"default_reason\""
"  }"
"}"
    );
        NResaleGradations::TStorage storage(path, /* validate */ true);
        UNIT_ASSERT_EQUAL(storage.CategoriesCount(), 2);
        UNIT_ASSERT(storage.GetErrors().empty());
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(123), NResaleGradations::TGradationsNames({.Perfect_ = "a", .Excellent_ = "b", .Good_ = "c"}));
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(456), NResaleGradations::TGradationsNames({.Perfect_ = "d", .Excellent_ = "e", .Good_ = "f"}));
        UNIT_ASSERT_EQUAL(storage.GetNamesByCategory(789), nullptr);

        UNIT_ASSERT_EQUAL(*storage.GetReasonText(1), "used");
        UNIT_ASSERT_EQUAL(*storage.GetReasonText(2), "showcase_sample");
        UNIT_ASSERT_EQUAL(*storage.GetReasonText(3), "reduction");
        UNIT_ASSERT_EQUAL(*storage.GetReasonText(4), "default_reason");
    }

    Y_UNIT_TEST(TestAbsenceDefaultReason) {
        TTempDir tmp;
        const TString path = tmp.Path() / "file_without_default_reason.json";
        SaveToFile(path,
"{"
"  \"gradations\": [],"
"  \"reasons\": {"
"    \"1\": \"used\""
"  }"
"}"
    );
        NResaleGradations::TStorage storage(path);
        UNIT_ASSERT_EQUAL(*storage.GetReasonText(1), "used");
        UNIT_ASSERT_EQUAL(storage.GetReasonText(2), nullptr);
    }

    Y_UNIT_TEST(TestRealFile) {
        const auto path = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data/resale_gradations.json");
        NResaleGradations::TStorage storage(path, /* validate */ true);
        UNIT_ASSERT(storage.GetErrors().empty());
    }

    Y_UNIT_TEST(TestParentCategory) {
        TTempDir tmp;
        const TString path = tmp.Path() / "file_with_not_leaf_categories.json";
        SaveToFile(path,
"{"
"  \"gradations\": ["
"    {\"category_id\": 100, \"perfect_text\": \"a\", \"excellent_text\": \"b\", \"good_text\": \"c\"},\n"
"    {\"category_id\": 50, \"perfect_text\": \"d\", \"excellent_text\": \"e\", \"good_text\": \"f\"}\n"
"  ],"
"  \"reasons\": {"
"  }"
"}"
        );
        NResaleGradations::TStorage storage(path);
        const NResaleGradations::TGradationsNames names1({.Perfect_ = "a", .Excellent_ = "b", .Good_ = "c"});
        const NResaleGradations::TGradationsNames names2({.Perfect_ = "d", .Excellent_ = "e", .Good_ = "f"});

        // для категории 100 задано правило
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(100, NormalParentCategory), names1);
        // категория 150 потомок 100, для нее должны находиться названия для 100
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(150, NormalParentCategory), names1);
        // для категории 50 задано правило
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(50, NormalParentCategory), names2);
        // категория 60 тоже потомок 100, но еще ближе к ней категория 50, поэтому выбираются её названия
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(60, NormalParentCategory), names2);
        UNIT_ASSERT_EQUAL(storage.GetNamesByCategory(100500, NormalParentCategory), nullptr);

        // проверка, что с плохой родительской функцией ничего не находится, но и не зависает
        // для 100 задано правило, найдется и при плохой функции
        UNIT_ASSERT_EQUAL(*storage.GetNamesByCategory(100, NormalParentCategory), names1);
        UNIT_ASSERT_EQUAL(storage.GetNamesByCategory(150, InvalidParentCategory), nullptr);
    }
}
