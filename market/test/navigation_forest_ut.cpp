#include "global_mock.h"
#include <market/report/library/category/navigation_forest.h>
#include <market/report/library/global/category/tree.h>
#include <market/library/trees/category_tree.h>
#include <market/library/algorithm/Algorithm.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <set>
#include <library/cpp/testing/unittest/gtest.h>

using TMboCategory = Market::Mbo::Parameters::Category;
using EOutputType = Market::Mbo::Parameters::OutputType;

namespace {
    using namespace NMarketReport;

    const char* in = R"delimiter(
    <navigation>
        <navigation-tree id="1">
            <node id="11" hid="1" is_primary="1">
                <node id="22" hid="2" is_primary="1"/>
            </node>
        </navigation-tree>
        <navigation-tree id="3">
            <node id="999">
                <node id="11" is_primary="0">
                    <node id="99" hid="0" is_primary="1">
                        <node id="22" hid="2" is_primary="1"/>
                        <node id="88" hid="0" is_primary="0">
                            <node id="44" hid="4" is_primary="1"/>
                        </node>
                   </node>
                </node>
                <node id="33" is_primary="0">
                    <node id="55" hid="5" is_primary="1"/>
                    <node id="66" link_id="66666" hid="0"/>
                </node>
            </node>
        </navigation-tree>
        <navigation-tree id="57964">
            <node id="999">
                <node id="11" hid="1" is_primary="0"/>
                <node id="22" hid="1" is_primary="1"/>
                <node id="33" hid="0" is_primary="1"/>
                <node id="44" hid="1" is_primary="0"/>
                <node id="55" hid="777" is_primary="1"/>
            </node>
        </navigation-tree>
        <links>
            <link id="66666">
                <url>
                    <target>list</target>
                    <params>
                        <param>
                            <name>hid</name>
                            <value>1111</value>
                        </param>
                        <param>
                            <name>how</name>
                            <value>aprice</value>
                        </param>
                    </params>
                </url>
            </link>
        </links>
    </navigation>
    )delimiter";
}

struct NavigationForestTest: public ::testing::Test {
    NavigationForestTest() {
        THolder<NMarket::NMbo::TWriter> writer(new NMarket::NMbo::TWriter("tovar-tree.pb", "MBOC"));

        {
            TMboCategory category;
            category.set_hid(90401);
            category.set_tovar_id(1);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);
            category.set_no_search(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(2);
            category.set_tovar_id(2);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(1);
            category.set_tovar_id(3);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(0);
            category.set_tovar_id(3);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(4);
            category.set_tovar_id(3);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(5);
            category.set_tovar_id(3);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(777);
            category.set_tovar_id(3);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        {
            TMboCategory category;
            category.set_hid(1111);
            category.set_tovar_id(3);
            category.set_parent_hid(90401);
            category.set_output_type(EOutputType::SIMPLE);
            category.set_published(true);

            auto uniqueName = category.add_unique_name();
            uniqueName->set_name("");
            uniqueName->set_lang_id(225);

            auto name = category.add_name();
            name->set_name("");
            name->set_lang_id(225);

            writer->Write(category);
        }
        writer.Destroy();

        NavigationForest = LoadNavigationForestFromString(in);
        NGlobal::LoadCategoryTree("tovar-tree.pb");
    }

    THolder<Market::INavigationForest> NavigationForest;
};

TEST_F(NavigationForestTest, CollectsHidsFromNode) {
    TNavigationConverter NavConverter(NavigationForest->GetTree(3), NGlobal::CategoryTree());
    const auto result = NavConverter.ExpandToHids(999);
    EXPECT_TRUE(contains(result, 5));
    EXPECT_TRUE(contains(result, 2));
    EXPECT_TRUE(contains(result, 4));
}

TEST_F(NavigationForestTest, SearchesByHid) {
    TNavigationConverter NavConverter(NavigationForest->GetTree(3), NGlobal::CategoryTree());
    EXPECT_EQ(NavConverter.HidToNid(2), 22);

    const auto& treeId = FindNavTree(*NavigationForest);
    TNavigationConverter NavConverterUnknownTree(NavigationForest->GetTree(treeId), NGlobal::CategoryTree());
    EXPECT_EQ(NavConverterUnknownTree.HidToNid(777), 55);
}
