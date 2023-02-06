#include <library/cpp/testing/unittest/gtest.h>
#include <market/library/offers_common/navigation_tree.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/library/trees/navigation_forest.h>

#include <market/proto/content/mbo/MboParameters.pb.h>

using TMboCategory = Market::Mbo::Parameters::Category;
using EOutputType = Market::Mbo::Parameters::OutputType;

namespace {
    const char* NAVIGATION_FOREST = R"(
    <navigation>
        <navigation-tree id="123">
            <node id="90" hid="90">
                <node id="901" hid="901" name="Для детей">
                    <node id="90101" hid="90101" is_primary="1" name="Памперсы"/>
                    <node id="90102" hid="90102" is_primary="1" name="Шампуни"/>
                </node>
                <node id="902" hid="902" name="Хозтовары">
                    <node id="9020002" hid="90102" name="Шампуни для детей"/>
                </node>
                <node id="800" name="Гигиена">
                    <node id="80001" hid="90101" name="Детские памперсы"/>
                </node>
            </node>
        </navigation-tree>
        <navigation-tree id="456">
            <node id="9" hid="90" name="Молодым родителям">
                <node id="1" hid="90101" name="Памперсы"/>
            </node>
        </navigation-tree>
    </navigation>
    )";

    TSet<ui64> Set(const TVector<ui64>& items) {
        return {items.begin(), items.end()};
    }
}


TEST(NID_LITERALS, GENERATOR) {
    // prepare tovar-tree.pb
    THolder<NMarket::NMbo::TWriter> writer(new NMarket::NMbo::TWriter("tovar-tree.pb", "MBOC"));

    {
        TMboCategory category;
        category.set_hid(90);
        category.set_tovar_id(1);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name("Все товары");
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name("Все товары");
        name->set_lang_id(225);

        writer->Write(category);
    }

    {
        TMboCategory category;
        category.set_hid(901);
        category.set_tovar_id(11);
        category.set_parent_hid(90);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name("Детские товары");
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name("Детские товары");
        name->set_lang_id(225);

        writer->Write(category);
    }

    {
        TMboCategory category;
        category.set_hid(902);
        category.set_tovar_id(12);
        category.set_parent_hid(90);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name("Хозтовары");
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name("Хозтовары");
        name->set_lang_id(225);

        writer->Write(category);
    }

    {
        TMboCategory category;
        category.set_hid(90101);
        category.set_tovar_id(111);
        category.set_parent_hid(901);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name("Детские памперсы");
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name("Памперсы");
        name->set_lang_id(225);

        writer->Write(category);
    }

    {
        TMboCategory category;
        category.set_hid(90102);
        category.set_tovar_id(112);
        category.set_parent_hid(901);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name("Детские шампуни и пенки для купания");
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name("Детские шампуни и пенки для купания");
        name->set_lang_id(225);

        writer->Write(category);
    }

    {
        TMboCategory category;
        category.set_hid(90201);
        category.set_tovar_id(121);
        category.set_parent_hid(902);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name("Шампуни и кондиционеры");
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name("Шампуни и кондиционеры");
        name->set_lang_id(225);

        writer->Write(category);
    }
    writer.Destroy();

    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    const auto forest = Market::LoadNavigationForestFromString(NAVIGATION_FOREST);

    auto nidLiterals = Market::TNidLiteralsGenerator(*categories);
    const auto& tree = forest->GetTree(123);
    TStringStream warnings;
    nidLiterals.Add(tree, warnings);
    ASSERT_TRUE(warnings.empty());

    {
        // по hid=90101 выдает ниды 90101, 901, 90 являющиеся предками непосредственно привязанного primary-nid 90101
        // отдает литерал для нида 80001 и его виртуального родителя 800
        auto literals = nidLiterals.GetLiterals(90101);
        ASSERT_EQ(Set(literals), TSet<ui64>({90101, 901, 90, 800, 80001}));
    }

    {
        // hid=902 не является родительской категорией hid=90102
        // но навигационный узел nid=902 является предком nid=9020002 к которому привязана категория 90102
        auto literals = nidLiterals.GetLiterals(90102);
        ASSERT_EQ(Set(literals), TSet<ui64>({9020002, 902, 90102, 901, 90}));
    }

    {
        // hid=90201 не привязана ни к какому навигационному узлу
        // литералы для нее формируются родительской категорией hid=902
        auto literals = nidLiterals.GetLiterals(90201);
        ASSERT_EQ(Set(literals), TSet<ui64>({902, 90}));
    }

    // дополнительное навигационное дерево дает навигационные узлы 1 и 9
    warnings.Clear();
    nidLiterals.Add(forest->GetTree(456), warnings);
    ASSERT_TRUE(warnings.empty());
    {
        auto literals = nidLiterals.GetLiterals(90101);
        ASSERT_EQ(Set(literals), TSet<ui64>({90101, 901, 90, 800, 80001, 1, 9}));
    }
}

TEST(NID_LITERALS, LINKER) {
    // Проверяем, что литералы будут проставлены в соответствии с линками между вершинами.
    // Если вершина А имеет линк на вершину Б
    // То Вершина Б и все её дочерние вершины получают в свой набор литералов все литералы привязанные к A
    // На вершину A и её дочерние вершины не должно оказываться никакого влияния.

    // prepare tovar-tree.pb
    THolder <NMarket::NMbo::TWriter> writer(new NMarket::NMbo::TWriter("tovar-tree.pb", "MBOC"));

    auto buildCategory = [&writer](size_t hid, size_t tovarId, TString nameVal, int parent_hid = 0) {
        TMboCategory category;
        category.set_hid(hid);
        category.set_tovar_id(tovarId);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        if (parent_hid) {
            category.set_parent_hid(parent_hid);
        }

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name(nameVal);
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name(nameVal);
        name->set_lang_id(225);

        writer->Write(category);
    };

    buildCategory(1, 1, "Все товары");
    buildCategory(9102, 2, "102", 1);
    buildCategory(910201, 3, "3", 1);
    buildCategory(91020101, 4, "4", 1);
    buildCategory(9101, 5, "5", 1);
    buildCategory(910101, 6, "6", 1);
    buildCategory(9103, 7, "7", 1);
    buildCategory(910301, 8, "8", 1);
    buildCategory(91030101, 9, "9", 1);
    buildCategory(91030102, 10, "10", 1);

    writer.Destroy();

    const char* NavForest = R"(
    <navigation>
        <navigation-tree id="123" code="blue">
            <node id="1" hid="1">
                <node id="101" hid="0" name="name101" link_id="3">
                    <node id="10101" hid="910101" is_primary="1" name="name10101"/>
                    <node id="10102" hid="0" is_primary="1" link_id="1" name="name10102"/>
                </node>
                <node id="102" hid="9102" name="name102">
                    <node id="10201" hid="910201" name="name10201">
                        <node id="1020101" hid="91020101" name="name1020101"/>
                    </node>
                </node>
                <node id="103" hid="9103" name="name103">
                    <node id="10301" hid="0" is_primary="1" link_id="2" name="name10301">
                        <node id="1030101" hid="91030101" is_primary="1" name="name1030101"/>
                        <node id="1030102" hid="91030102" is_primary="1" name="name1030102"/>
                    </node>
                </node>
            </node>
        </navigation-tree>
        <links>
            <link id="1">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>10201</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="2">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>101</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="2">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>101</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="3">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>1030101</value>
                        </param>
                    </params>
                </url>
            </link>
        </links>
    </navigation>
    )";
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    const auto forest = Market::LoadNavigationForestFromString(NavForest);

    auto nidLiterals = Market::TNidLiteralsGenerator(*categories);

    TStringStream warnings;
    nidLiterals.Add(forest->GetTree(123), warnings);
    ASSERT_TRUE(warnings.empty());

    {
        // hid 910101 Проверяем, что линк и его предки попадут в поисковые литералы для данной вершины
        auto literals = nidLiterals.GetLiterals(910101);
        ASSERT_EQ(Set(literals), TSet<ui64>({10101, 101, 1, 10301, 103}));
    }

    {
        // hid 91030101 проверяем не сломаемся ли мы в том случае, если в дереве есть цикл
        auto literals = nidLiterals.GetLiterals(91030101);
        ASSERT_EQ(Set(literals), TSet<ui64>({1030101, 1, 10301, 103, 101}));
    }

    {
        //  hid 91020101 Проверяем, что если в предках линка окажется другая слинкованная вершина, то она тоже разрешится
        auto literals = nidLiterals.GetLiterals(91020101);
        ASSERT_EQ(Set(literals), TSet<ui64>({1020101, 10201, 102, 1, 10102, 101, 10301, 103}));
    }

    {
        //  hid 91030102 Проверяем, что дочкам линка не достанутся поисковые литералы предков слинкованной вершины
        auto literals = nidLiterals.GetLiterals(91030102);
        ASSERT_EQ(Set(literals), TSet<ui64>({1030102, 10301, 103, 1}));
    }

    {
        //  hid 91030102 Проверяем, что для слинкованой вершины тоже всё сработает
        auto literals = nidLiterals.GetLiterals(910201);
        ASSERT_EQ(Set(literals), TSet<ui64>({10102, 101, 1, 102, 10201, 10301, 103}));
    }
}

TEST(NID_LITERALS, LINKER_CONDITIONS) {
    // линковка поисковых литералов должна работать только
    // Для синего дерева
    // Для линков, цель которых "catalog"

    // prepare tovar-tree.pb
    THolder <NMarket::NMbo::TWriter> writer(new NMarket::NMbo::TWriter("tovar-tree.pb", "MBOC"));

    auto buildCategory = [&writer](size_t hid, size_t tovarId, TString nameVal, int parent_hid = 0) {
        TMboCategory category;
        category.set_hid(hid);
        category.set_tovar_id(tovarId);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        if (parent_hid) {
            category.set_parent_hid(parent_hid);
        }

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name(nameVal);
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name(nameVal);
        name->set_lang_id(225);

        writer->Write(category);
    };

    buildCategory(1, 1, "Все товары");
    buildCategory(9311, 2, "2", 1);
    buildCategory(93111, 3, "3", 1);
    buildCategory(93112, 4, "4", 1);
    buildCategory(93113, 5, "5", 1);
    buildCategory(9321, 6, "6", 1);
    buildCategory(93211, 7, "7", 1);
    buildCategory(93212, 8, "8", 1);
    buildCategory(93213, 9, "9", 1);

    writer.Destroy();

    const char* NavForest = R"(
    <navigation>
        <navigation-tree id="31" code="blue">
            <node id="311" hid="311">
                <node id="3111" hid="93111" is_primary="1" name="name"/>
                <node id="3112" hid="0" is_primary="1" link_id="1311" name="name2"/>
                <node id="3113" hid="0" is_primary="1" link_id="1312" name="name22"/>
            </node>
        </navigation-tree>
        <navigation-tree id="32">
            <node id="321" hid="321">
                <node id="3211" hid="93211" is_primary="1" name="name3"/>
                <node id="3212" hid="0" is_primary="1" link_id="1321" name="name4"/>
                <node id="3213" hid="0" is_primary="1" link_id="1322" name="name44"/>
            </node>
        </navigation-tree>
        <links>
            <link id="1311">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>3111</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="1312">
                <url>
                    <target>search</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>3111</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="1321">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>3211</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="1322">
                <url>
                    <target>search</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>3211</value>
                        </param>
                    </params>
                </url>
            </link>
        </links>
    </navigation>
    )";
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    const auto forest = Market::LoadNavigationForestFromString(NavForest);

    auto nidLiterals = Market::TNidLiteralsGenerator(*categories);
    TStringStream warnings;
    nidLiterals.Add(forest->GetTree(31), warnings);
    nidLiterals.Add(forest->GetTree(32), warnings);
    ASSERT_TRUE(warnings.empty());


    {
        // hid 3111 в синем дереве должны обрабатываться только линки у которых target - catalog остальное - отбрасывается
        auto literals = nidLiterals.GetLiterals(93111);
        ASSERT_EQ(Set(literals), TSet<ui64>({3111, 311, 3112}));
    }

    {
        // hid 3211 в несинем дереве линковка отключена
        auto literals = nidLiterals.GetLiterals(93211);
        ASSERT_EQ(Set(literals), TSet<ui64>({3211, 321}));
    }
}

TEST(NID_LITERALS, LINKER_ERRORS) {
    // Код линковки без категорий вызывается для валидации
    // проверяем что валидация отбрасывает некорректные ссылки
    // продолжая при этом собирать ошибки дальше
    // валидация не должна сломать деревья не синего кода

    // prepare tovar-tree.pb
    THolder <NMarket::NMbo::TWriter> writer(new NMarket::NMbo::TWriter("tovar-tree.pb", "MBOC"));

    auto buildCategory = [&writer](size_t hid, size_t tovarId, TString nameVal, int parent_hid = 0) {
        TMboCategory category;
        category.set_hid(hid);
        category.set_tovar_id(tovarId);
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        if (parent_hid) {
            category.set_parent_hid(parent_hid);
        }

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name(nameVal);
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name(nameVal);
        name->set_lang_id(225);

        writer->Write(category);
    };
    buildCategory(1, 1, "Все товары");
    buildCategory(11, 2, "11", 1);
    buildCategory(21, 3, "21", 1);
    buildCategory(22, 4, "22", 1);
    buildCategory(23, 5, "23", 1);
    buildCategory(24, 6, "24", 1);

    writer.Destroy();


    // Дерево 1. две ошибки - отсутствие nid в одной из ссылок и нечисловое значение nid в сообщении об
    // ошибке должны быть упомяунты обе проблемы
    // Дерево 2. ошибка с отсутствием nid
    // Дерево 3. ошибка с нечисловым значением
    // Дерево 4. не синее дерево. валидация игнорирует ссылки.
    const char* NavForest = R"(
    <navigation>
        <navigation-tree id="1" code="blue">
            <node id="11" hid="11">
                <node id="111" hid="0" is_primary="1" link_id="1" name="name2"/>
                <node id="112" hid="0" is_primary="1" link_id="2" name="name22"/>
            </node>
        </navigation-tree>
        <navigation-tree id="2" code="blue">
            <node id="21" hid="21">
                <node id="211" hid="0" is_primary="1" link_id="1" name="name2"/>
            </node>
        </navigation-tree>
        <navigation-tree id="3" code="blue">
            <node id="31" hid="31">
                <node id="311" hid="0" is_primary="1" link_id="2" name="name22"/>
            </node>
        </navigation-tree>
        <navigation-tree id="4">
            <node id="41" hid="41">
                <node id="411" hid="0" is_primary="1" link_id="1" name="name2"/>
                <node id="412" hid="0" is_primary="1" link_id="2" name="name22"/>
            </node>
        </navigation-tree>
        <navigation-tree id="5" code="blue">
            <node id="51" hid="51">
                <node id="511" hid="511">
                    <node id="5111" hid="0" is_primary="1" link_id="512" name="cclink 5111"/>
                </node>
                <node id="512" hid="512">
                    <node id="5121" hid="0" is_primary="1" link_id="511" name="cclink 5121"/>
                </node>
            </node>
        </navigation-tree>
        <links>
            <link id="1">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>not_nid</name>
                          <value>3111</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="2">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>not_int_value</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="512">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>512</value>
                        </param>
                    </params>
                </url>
            </link>
            <link id="511">
                <url>
                    <target>catalog</target>
                    <params>
                        <param>
                          <name>nid</name>
                          <value>511</value>
                        </param>
                    </params>
                </url>
            </link>
        </links>
    </navigation>
    )";
    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    const auto forest = Market::LoadNavigationForestFromString(NavForest);

    auto nidLiterals = Market::TNidLiteralsGenerator(*categories);

    auto checkTree = [&] (size_t treeId) -> std::tuple<bool, TString, TString> {
        bool success = true;
        TString msg;
        TStringStream warnings;
        try {
            nidLiterals.Add(forest->GetTree(treeId), warnings);
        } catch (const yexception& e) {
            msg = e.what();
            success = false;
        } catch (...) {
            success = false;
        }
        return std::make_tuple(success, msg, warnings.ReadAll());
    };

    auto getSuffix = [](TString message, TString expect) {
        if (message.size() <= expect.size()) {
            return message;
        }
        return TString(message.begin() + message.size() - expect.size(), message.end());
    };

    {
        auto [success, msg, warnings] = checkTree(1);
        ASSERT_TRUE(success);
        TString expectedSuffix = "No nid in link on processing node \"111\"\nBad nid : \"not_int_value\" in link on processing node \"112\"\n";
        ASSERT_EQ(getSuffix(warnings, expectedSuffix), expectedSuffix);
    }

    {
        auto [success, msg, warnings] = checkTree(2);
        ASSERT_TRUE(success);
        TString expectedSuffix = "No nid in link on processing node \"211\"\n";
        ASSERT_EQ(getSuffix(warnings, expectedSuffix), expectedSuffix);
    }

    {
        auto [success, msg, warnings] = checkTree(3);
        ASSERT_TRUE(success);
        TString expectedSuffix = "Bad nid : \"not_int_value\" in link on processing node \"311\"\n";
        ASSERT_EQ(getSuffix(warnings, expectedSuffix), expectedSuffix);
    }

    {
        auto [success, msg, warnings] = checkTree(4);
        ASSERT_TRUE(success);
        ASSERT_EQ(msg, "");
        ASSERT_EQ(warnings, "");
    }

    {
        auto [success, msg, warnings] = checkTree(5);
        ASSERT_TRUE(success);
        ASSERT_EQ(msg, "");
        ASSERT_EQ(warnings, "it seem's to be a link cycle with links on nodes: \"511\" \"512\" \n");
    }

}


TEST(NID_LITERALS, MODELLIST_FILTERING) {

    const char* NavForest = R"(
    <navigation>
        <navigation-tree id="123">
            <node id="100" hid="500">
                <node id="101" hid="501" name="Ведьмаки и Чародейки">
                    <node id="1101" hid="0" is_primary="1" name="Ведьмаки - список моделей" model_list_id="100500"/>
                    <node id="1102" hid="502" is_primary="1" name="Чародейки"/>
                </node>
                <node id="102" hid="0" name="Нильфгаард - список моделей" model_list_id="100501">
                    <node id="1103" hid="502" name="Чародейки Нильфгаарда"/>
                </node>
                <node id="103" name="Гвинтокарты">
                    <node id="1104" hid="0" name="Карты ведьмаков - список моделей" model_list_id="100502"/>
                </node>
                <node id="104" hid="0" name="Виртуальный Велен">
                    <node id="1105" hid="0" name="Список моделей 100503" model_list_id="100503"/>
                </node>
            </node>
        </navigation-tree>
        <model-lists>
            <model-list id="100500">
                <model id="3220"/>
                <model id="3221"/>
            </model-list>
            <model-list id="100501">
                <model id="3223"/>
            </model-list>
            <model-list id="100502">
                <model id="3220"/>
                <model id="3224"/>
            </model-list>
            <model-list id="100503">
                <model id="3223"/>
            </model-list>
        </model-lists>
    </navigation>
    )";

    // prepare tovar-tree.pb
    THolder<NMarket::NMbo::TWriter> writer(new NMarket::NMbo::TWriter("tovar-tree.pb", "MBOC"));

    const auto writeCategory = [&writer] (int hid, int tovar_id, int parent_hid, const TString& nodeName) {
        TMboCategory category;
        category.set_hid(hid);
        category.set_tovar_id(tovar_id);
        if (parent_hid > 0) {
            category.set_parent_hid(parent_hid);
        }
        category.set_output_type(EOutputType::SIMPLE);
        category.set_published(true);

        auto uniqueName = category.add_unique_name();
        uniqueName->set_name(nodeName);
        uniqueName->set_lang_id(225);

        auto name = category.add_name();
        name->set_name(nodeName);
        name->set_lang_id(225);

        writer->Write(category);
    };
    
    writeCategory(500, 1, 0, "Все товары");
    writeCategory(501, 11, 500, "Ведьмаки и Чародейки");
    writeCategory(502, 12, 500, "Нильфгаард");

    writer.Destroy();

    const auto categories = Market::CreateCategoryTreeFromProtoFile("tovar-tree.pb");
    const auto forest = Market::LoadNavigationForestFromString(NavForest);

    auto nidLiterals = Market::TNidLiteralsGenerator(*categories);
    const auto& tree = forest->GetTree(123);
    TStringStream warnings;
    nidLiterals.Add(tree, warnings);
    ASSERT_TRUE(warnings.empty());

    {
        // GetLiteralsFromModelLists принимает на вход модель и мску
        // принцип для мску точно такой же
        //
        // С model_id == 3220, ниды из списков моделей должны быть == {1101, 101, 100, 1104, 103}
        // Тк родительские ниды так же наследуются
        auto literals = nidLiterals.GetLiteralsFromModelLists(3220, 0);
        ASSERT_EQ(Set(literals), TSet<ui64>({1101, 101, 100, 1104, 103}));

        // С model_id == 3221, ниды из списков моделей должны быть == {1101, 101, 100}
        literals = nidLiterals.GetLiteralsFromModelLists(0, 3221);
        ASSERT_EQ(Set(literals), TSet<ui64>({1101, 101, 100}));

        // С model_id == 3224, ниды из списков моделей должны быть == {1104, 103, 100}
        literals = nidLiterals.GetLiteralsFromModelLists(3224, 0);
        ASSERT_EQ(Set(literals), TSet<ui64>({1104, 103, 100}));

        // С model_id == 3223, ниды из списков моделей должны быть == {102, 100, 1105, 104}
        literals = nidLiterals.GetLiteralsFromModelLists(0, 3223);
        ASSERT_EQ(Set(literals), TSet<ui64>({102, 100, 1105, 104}));

        // Модели model_id == 322 нет ни в одном списке
        literals = nidLiterals.GetLiteralsFromModelLists(322, 0);
        ASSERT_TRUE(literals.empty());

        // Также списки моделей должны игнорироваться для дочерних категорий
        // Например для хида 502 не будет литерала 102
        literals = nidLiterals.GetLiterals(502);
        ASSERT_EQ(Set(literals), TSet<ui64>({100, 101, 1102, 1103}));
    }
}
