#include <extsearch/images/robot/library/yt_test/client.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(YtTests_SmokeTests) {

    Y_UNIT_TEST(ReadWriteTest){
        NYT::IClientBasePtr client(new TClientBaseMock(".", "//home/", true));
        client->Create("images", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().IgnoreExisting(true));
        {
            auto writer = client->CreateTableWriter<NYT::TNode>("images/some_table");
            writer->AddRow(NYT::TNode()("line1", "value1"));
            writer->AddRow(NYT::TNode()("line2", "value2"));
            writer->AddRow(NYT::TNode()("line3", 42));
        }
        client->Copy("images", "images2");
        client->Remove("images", NYT::TRemoveOptions().Recursive(true));
        {
            auto reader = client->CreateTableReader<NYT::TNode>("images2/some_table");
            const auto& row1 = reader->GetRow();
            UNIT_ASSERT_VALUES_EQUAL(row1["line1"].AsString(), "value1");
            reader->Next();
            const auto& row2 = reader->GetRow();
            UNIT_ASSERT_VALUES_EQUAL(row2["line2"].AsString(), "value2");
            reader->Next();
            const auto& row3 = reader->GetRow();
            UNIT_ASSERT_VALUES_EQUAL(row3["line3"].AsInt64(), 42);
            reader->Next();
            UNIT_ASSERT(!reader->IsValid());
        }
    }

}

