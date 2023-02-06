#include <extsearch/images/robot/library/yt_test/client.h>

#include <library/cpp/testing/unittest/registar.h>

void PrintLimit(const NYT::TReadLimit& limit) {
    Cerr << "        key       = " << " bla bla bla some keys i dunno" << Endl;
    Cerr << "        row index = " << limit.RowIndex_ << Endl;
    Cerr << "        offset    = " << limit.Offset_ << Endl;
}

void PrintRanges(const TVector<NYT::TReadRange>& ranges) {
    Cerr << "{" << Endl;;
    const int size = ranges.size();
    for (int counter = 0; counter < size; ++counter) {
        Cerr << "    " << counter << ") lower limit" << Endl;
        PrintLimit(ranges[counter].LowerLimit_);

        Cerr << "    " << counter << ") upper limit" << Endl;
        PrintLimit(ranges[counter].UpperLimit_);

        Cerr << "    " << counter << ") exact" << Endl;
        PrintLimit(ranges[counter].Exact_);
    }
    Cerr << "}" << Endl;;
}

void CreateTestFun(NYT::IClientBasePtr client, const TString& wd = "") {
    client->Create(wd + "images_test", NYT::ENodeType::NT_MAP);

    // Can not create the already existing "images_test" node
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "images_test", NYT::ENodeType::NT_MAP), yexception);

    // Should be ok with ignore existing
    client->Create(wd + "images_test", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().IgnoreExisting(true));

    // Can not create the already existing "images_test" node with another node type (even with ignore exising)
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "images_test", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().IgnoreExisting(true)), yexception);

    // Should be ok with force option
    client->Create(wd + "images_test", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Force(true));

    // Can not create sub_table since images is a table, not a map_node
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "images_test/sub_table", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Force(true)), yexception);

    // Can not create "state_table" due to missing "video_test" and "index" map node
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "video_test/index/state_table", NYT::ENodeType::NT_TABLE), yexception);

    // Should be ok with recursive option
    client->Create(wd + "video_test/index/state_table", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true));

    // Create method is not supported for atthributes for any condition
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "images_test/@some_attr", NYT::ENodeType::NT_MAP), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "images_test/@some_attr", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true)), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "images_test/@some_attr", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().IgnoreExisting(true)), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Create(wd + "images_test/@some_attr", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true)), yexception);

    // But you can specify attributes through NYT::TCreateOptions
    client->Create(wd + "ads_test/index/state_table", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true).Attributes(NYT::TNode()("attr1", "value1")("attr2", "value2")));
    UNIT_ASSERT(client->Exists(wd + "ads_test/index/state_table"));
    UNIT_ASSERT(client->Exists(wd + "ads_test/index/state_table/@attr1"));
    UNIT_ASSERT(client->Exists(wd + "ads_test/index/state_table/@attr2"));

    // Now try with multilevel attributes
    client->Create(wd + "ads_test/index/state_table2", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true).Attributes(NYT::TNode()("attr1", NYT::TNode()("attr2", "value2"))));
    UNIT_ASSERT(client->Exists(wd + "ads_test/index/state_table2"));
    UNIT_ASSERT(client->Exists(wd + "ads_test/index/state_table2/@attr1"));
    UNIT_ASSERT(client->Exists(wd + "ads_test/index/state_table2/@attr1/attr2"));
}

void RemoveTestFun(NYT::IClientBasePtr client, const TString& wd = "") {
    // Can not remove non existing "images_test" node
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "images_test"), yexception);

    // Should be ok with "force" option
    client->Remove(wd + "images_test", NYT::TRemoveOptions().Force(true));

    client->Create(wd + "images_test/index", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true));
    client->Create(wd + "images_test/index/table1", NYT::ENodeType::NT_TABLE);
    client->Create(wd + "images_test/index/table2", NYT::ENodeType::NT_TABLE);

    // Should be ok, simply removing a single table
    client->Remove(wd + "images_test/index/table1");

    // Can not delete a map node without "recursive" option
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "images_test/index"), yexception);

    // Still fail, "force" option works only on missing directories
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "images_test/index", NYT::TRemoveOptions().Force(true)), yexception);

    // Should be ok now
    client->Remove(wd + "images_test/index", NYT::TRemoveOptions().Recursive(true));

    // Now tests for attributes
    // can't remove any attribute for missing cypress node
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "video_test/@attr1"), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "video_test/@attr2"), yexception);

    client->Create(wd + "video_test", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true));

    // can't remove missing attribute, even if the cypress node exists
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "video_test/@attr1"), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "video_test/@attr2"), yexception);

    client->Create(wd + "video_test", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true).Attributes(NYT::TNode()("attr1", "value1")));

    // now we can remove existing attribute with existing cypress node
    UNIT_ASSERT(client->Exists(wd + "video_test/@attr1"));
    client->Remove(wd + "video_test/@attr1");
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1"));

    // still fails on missing "attr2" attribute
    UNIT_CHECK_GENERATED_EXCEPTION(client->Remove(wd + "video_test/@attr2"), yexception);

    // the same restrictions and rules shall prevail for multilevel attributes
    client->Create(wd + "video_test", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true).Attributes(NYT::TNode()("attr1", NYT::TNode()("sub_attr1", "value1")("sub_attr2", "value2"))));
    UNIT_ASSERT(client->Exists(wd + "video_test/@attr1"));
    UNIT_ASSERT(client->Exists(wd + "video_test/@attr1/sub_attr1"));
    UNIT_ASSERT(client->Exists(wd + "video_test/@attr1/sub_attr2"));
    client->Remove(wd + "video_test/@attr1/sub_attr1");
    UNIT_ASSERT(client->Exists(wd + "video_test/@attr1"));
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1/sub_attr1"));
    UNIT_ASSERT(client->Exists(wd + "video_test/@attr1/sub_attr2"));

    // can directly delete complex attribute
    client->Remove(wd + "video_test/@attr1");
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1"));
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1/sub_attr1"));
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1/sub_attr2"));

    client->Create(wd + "video_test", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true).Attributes(NYT::TNode()("attr1", NYT::TNode()("sub_attr1", "value1")("sub_attr2", "value2"))));

    // should be also ok with "recursive" options
    client->Remove(wd + "video_test/@attr1", NYT::TRemoveOptions().Recursive(true));
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1"));
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1/sub_attr1"));
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1/sub_attr2"));
}

void CopyTestFun(NYT::IClientBasePtr client, const TString& wd = "") {
    // Can not copy due to "images_test" map node do not exists
    UNIT_CHECK_GENERATED_EXCEPTION(client->Copy(wd + "images_test", wd + "video_test"), yexception);

    client->Create(wd + "images_test/index/data_table", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true));

    // Should be ok now
    client->Copy(wd + "images_test", wd + "video_test");
    UNIT_ASSERT(client->Exists(wd + "video_test/index"));
    UNIT_ASSERT(client->Exists(wd + "video_test/index/data_table"));

    // Can not copy, "video_test" map node already exists
    UNIT_CHECK_GENERATED_EXCEPTION(client->Copy(wd + "images_test", wd + "video_test"), yexception);

    // Should be ok with "force" option
    client->Copy(wd + "images_test", wd + "video_test", NYT::TCopyOptions().Force(true));

    // Can not copy, "tmp" map node does not exist
    UNIT_CHECK_GENERATED_EXCEPTION(client->Copy(wd + "images_test", wd + "tmp/video_test"), yexception);

    // Should be ok with "recursive" option
    client->Copy(wd + "images_test", wd + "tmp/video_test", NYT::TCopyOptions().Recursive(true));

    // Copy function does not work with apparently specified attributes
    client->Create(wd + "ads_test", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Attributes(NYT::TNode()("attr1", "value1")));
    UNIT_CHECK_GENERATED_EXCEPTION(client->Copy(wd + "ads_test/@attr1", wd + "ads_test/@attr2"), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Copy(wd + "ads_test/@attr1", wd + "ads_test/sub_dir"), yexception);
    client->Create(wd + "ads_test/sub_dir", NYT::ENodeType::NT_TABLE);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Copy(wd + "ads_test/sub_dir", wd + "ads_test/@attr2"), yexception);

    // But we attributes do copy together with its cypress node father
    client->Create(wd + "web_test/sub_dir1", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true).Attributes(NYT::TNode()("attr1", "value1")("attr2", "value2")));
    UNIT_ASSERT(client->Exists(wd + "web_test/sub_dir1"));
    UNIT_ASSERT(client->Exists(wd + "web_test/sub_dir1/@attr1"));
    UNIT_ASSERT(client->Exists(wd + "web_test/sub_dir1/@attr2"));
    UNIT_ASSERT(!client->Exists(wd + "web_test/sub_dir2"));
    client->Copy(wd + "web_test/sub_dir1", wd + "web_test/sub_dir2");
    UNIT_ASSERT(client->Exists(wd + "web_test/sub_dir2"));
    UNIT_ASSERT(client->Exists(wd + "web_test/sub_dir2/@attr1"));
    UNIT_ASSERT(client->Exists(wd + "web_test/sub_dir2/@attr2"));
}

void MoveTestFun(NYT::IClientBasePtr client, const TString& wd = "") {
    // Can not move due to "images_test" map node do not exists
    UNIT_CHECK_GENERATED_EXCEPTION(client->Move(wd + "images_test", wd + "video_test"), yexception);

    client->Create(wd + "images_test/index/data_table", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true));

    // Should be ok now
    client->Move(wd + "images_test", wd + "video_test");
    UNIT_ASSERT(client->Exists(wd + "video_test/index"));
    UNIT_ASSERT(client->Exists(wd + "video_test/index/data_table"));
    UNIT_ASSERT(!client->Exists(wd + "images_test"));
    UNIT_ASSERT(!client->Exists(wd + "images_test/data_table"));

    // Can not mopy, "video_test" map node already exists
    client->Create(wd + "images_test/index/data_table", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true));
    UNIT_CHECK_GENERATED_EXCEPTION(client->Move(wd + "images_test", wd + "video_test"), yexception);

    // Should be ok with "force" option
    client->Move(wd + "images_test", wd + "video_test", NYT::TMoveOptions().Force(true));
    UNIT_ASSERT(!client->Exists(wd + "images_test"));
    UNIT_ASSERT(!client->Exists(wd + "images_test/data_table"));

    // Can not move, "tmp" map node does not exist
    UNIT_CHECK_GENERATED_EXCEPTION(client->Move(wd + "video_test", wd + "tmp/video_test"), yexception);

    // Should be ok with "recursive" option
    client->Move(wd + "video_test", wd + "tmp/video_test", NYT::TMoveOptions().Recursive(true));

    // Move function does not work with apparently specified attributes
    client->Create(wd + "ads_test", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Attributes(NYT::TNode()("attr1", "value1")));
    UNIT_CHECK_GENERATED_EXCEPTION(client->Move(wd + "ads_test/@attr1", wd + "ads_test/@attr2"), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Move(wd + "ads_test/@attr1", wd + "ads_test/sub_dir"), yexception);
    client->Create(wd + "ads_test/sub_dir", NYT::ENodeType::NT_TABLE);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Move(wd + "ads_test/sub_dir", wd + "ads_test/@attr2"), yexception);

    // But attributes move together with cypress node
    client->Move(wd + "ads_test", wd + "web_test");
    UNIT_ASSERT(!client->Exists(wd + "ads_test"));
    UNIT_ASSERT(!client->Exists(wd + "ads_test/sub_dir"));
    UNIT_ASSERT(!client->Exists(wd + "ads_test/@attr1"));
    UNIT_ASSERT(client->Exists(wd + "web_test"));
    UNIT_ASSERT(client->Exists(wd + "web_test/sub_dir"));
    UNIT_ASSERT(client->Exists(wd + "web_test/@attr1"));
}

void GetTestFun(NYT::IClientBasePtr client, const TString& wd = "") {
    client->Create(wd + "images_test/index/data_table_1", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true));
    client->Create(wd + "images_test/index/data_table_2", NYT::ENodeType::NT_TABLE, NYT::TCreateOptions().Recursive(true));

    // Get on a table should return an empty entity (TNull)
    UNIT_ASSERT(client->Get(wd + "images_test/index/data_table_1").IsNull());
    UNIT_ASSERT(client->Get(wd + "images_test/index/data_table_2").IsNull());

    // Get on a map node of a cypress should return a map that shows filesystem-like structure
    NYT::TNode bigNode = client->Get(wd + "images_test");
    UNIT_ASSERT(bigNode.IsMap());
    UNIT_ASSERT(bigNode.AsMap().size() == 1);
    UNIT_ASSERT(bigNode.HasKey("index"));
    UNIT_ASSERT(bigNode["index"].IsMap());
    UNIT_ASSERT(bigNode["index"].AsMap().size() == 2);
    UNIT_ASSERT(bigNode["index"].HasKey("data_table_1"));
    UNIT_ASSERT(bigNode["index"].HasKey("data_table_2"));
    UNIT_ASSERT(bigNode["index"]["data_table_1"].IsNull());
    UNIT_ASSERT(bigNode["index"]["data_table_2"].IsNull());

    // Get on nonexisting node should fail
    UNIT_CHECK_GENERATED_EXCEPTION(client->Get(wd + "images_test/index/some_weirdo_table"), yexception);

    // Now get on attributes
    client->Create(wd + "video_test", NYT::ENodeType::NT_MAP,
                    NYT::TCreateOptions().Force(true).Attributes(NYT::TNode()("attr2", "value2")("attr1", NYT::TNode()("sub_attr1", "value1")("sub_attr2", 42))));
    NYT::TNode attrNode = client->Get(wd + "video_test/@");
    UNIT_ASSERT(attrNode.IsMap());
    UNIT_ASSERT(attrNode.HasKey("attr1"));
    UNIT_ASSERT(attrNode.HasKey("attr2"));
    UNIT_ASSERT(attrNode["attr2"].AsString() == "value2");
    UNIT_ASSERT(attrNode["attr1"].IsMap());
    UNIT_ASSERT(attrNode["attr1"].AsMap().size() == 2);
    UNIT_ASSERT(attrNode["attr1"].HasKey("sub_attr1"));
    UNIT_ASSERT(attrNode["attr1"].HasKey("sub_attr2"));
    UNIT_ASSERT(attrNode["attr1"]["sub_attr1"].AsString() == "value1");
    UNIT_ASSERT(attrNode["attr1"]["sub_attr2"].AsInt64() == 42);

    // Direct access
    UNIT_ASSERT(client->Get(wd + "video_test/@attr1/sub_attr2").AsInt64() == 42);

    // Get on nonexisting attribute should fail
    UNIT_CHECK_GENERATED_EXCEPTION(client->Get(wd + "video_test/@attr1/sub_attr_42"), yexception);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Get(wd + "video_test/@attr4242"), yexception);
}

void SetTestFun(NYT::IClientBasePtr client, const TString& wd = "") {
    client->Create(wd + "video_test", NYT::ENodeType::NT_MAP,
                    NYT::TCreateOptions().Force(true).Attributes(NYT::TNode()("attr2", "value2")("attr1", NYT::TNode()("sub_attr1", "value1")("sub_attr2", 42))));
    UNIT_ASSERT(client->Get(wd + "video_test/@attr1/sub_attr2").AsInt64() == 42);
    UNIT_CHECK_GENERATED_EXCEPTION(client->Set(wd + "video_test/@attr1/sub_attr2", NYT::TNode("new_value")), yexception);
    client->Set(wd + "video_test/@attr1", NYT::TNode()("sub_attr3", 4567));
    UNIT_ASSERT(!client->Exists(wd + "video_test/@attr1/sub_attr2"));
    UNIT_ASSERT(client->Exists(wd + "video_test/@attr1/sub_attr3"));
    UNIT_ASSERT(client->Get(wd + "video_test/@attr1/sub_attr3").AsInt64() == 4567);
}

void CanonizeYPathTestFun(NYT::IClientBasePtr client, const TString& ytPrefix) {
    // Just simple path with no extra data
    {
        NYT::TRichYPath pathNoRanges = client->CanonizeYPath("images_test/index");

        UNIT_ASSERT_VALUES_EQUAL(ytPrefix + "images_test/index", pathNoRanges.Path_);
        UNIT_ASSERT(pathNoRanges.Ranges_.empty());
    }

    // Will generate malicious (double slash "//" in the middle) path, path should be ok in canonization
    {
        NYT::TRichYPath pathMalicious = client->CanonizeYPath("/images_test/index");

        UNIT_ASSERT_VALUES_EQUAL(ytPrefix + "/images_test/index", pathMalicious.Path_);
        UNIT_ASSERT(pathMalicious.Ranges_.empty());
    }

    // Now no yt prefix should be applied since path for canonization already starts with double slash "//"
    {
        NYT::TRichYPath pathNoYtPrefix = client->CanonizeYPath("//images_test/index");

        UNIT_ASSERT_VALUES_EQUAL("//images_test/index", pathNoYtPrefix.Path_);
        UNIT_ASSERT(pathNoYtPrefix.Ranges_.empty());
    }

    // Now test some ranges
    {
        NYT::TRichYPath pathRanges = client->CanonizeYPath("images_test/index/test_table[#217:#355]");

        UNIT_ASSERT_VALUES_EQUAL(ytPrefix + "images_test/index/test_table", pathRanges.Path_);

        UNIT_ASSERT_VALUES_EQUAL(pathRanges.Ranges_.size(), 1);

        UNIT_ASSERT_VALUES_EQUAL(pathRanges.Ranges_[0].LowerLimit_.RowIndex_.GetRef(), 217);
        UNIT_ASSERT(pathRanges.Ranges_[0].LowerLimit_.Offset_.Empty());
        UNIT_ASSERT(pathRanges.Ranges_[0].LowerLimit_.Key_.Empty());

        UNIT_ASSERT_VALUES_EQUAL(pathRanges.Ranges_[0].UpperLimit_.RowIndex_.GetRef(), 355);
        UNIT_ASSERT(pathRanges.Ranges_[0].UpperLimit_.Offset_.Empty());
        UNIT_ASSERT(pathRanges.Ranges_[0].UpperLimit_.Key_.Empty());
    }
}


Y_UNIT_TEST_SUITE(YtTests_CypressTests) {
    Y_UNIT_TEST(CreateTest) {
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            CreateTestFun(testClient);
        }
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            CreateTestFun(testClient, "//home_dir/");
        }

        if (false) // you can remove "if" to ensure everything works on real YT client
        {
            NYT::JoblessInitialize();
            auto realClient = NYT::CreateClient("hahn");
            realClient->Create("images/dev/mseifullin/test_dir", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true));
            CreateTestFun(realClient, "images/dev/mseifullin/test_dir/");
            realClient->Remove("images/dev/mseifullin/test_dir", NYT::TRemoveOptions().Recursive(true));
        }

    }

    Y_UNIT_TEST(RemoveTest) {
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            RemoveTestFun(testClient);
        }
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            RemoveTestFun(testClient, "//home_dir/");
        }

         if (false) // you can remove "if" to ensure everything works on real YT client
        {
            NYT::JoblessInitialize();
            auto realClient = NYT::CreateClient("hahn");
            realClient->Create("images/dev/mseifullin/test_dir", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true));
            RemoveTestFun(realClient, "images/dev/mseifullin/test_dir/");
            realClient->Remove("images/dev/mseifullin/test_dir", NYT::TRemoveOptions().Recursive(true));
        }
    }

    Y_UNIT_TEST(CopyTest) {
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            CopyTestFun(testClient);
        }
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            CopyTestFun(testClient, "//home_dir/");
        }

        if (false) // you can remove "if" to ensure everything works on real YT client
        {
            NYT::JoblessInitialize();
            auto realClient = NYT::CreateClient("hahn");
            realClient->Create("images/dev/mseifullin/test_dir", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true));
            CopyTestFun(realClient, "images/dev/mseifullin/test_dir/");
            realClient->Remove("images/dev/mseifullin/test_dir", NYT::TRemoveOptions().Recursive(true));
        }
    }

    Y_UNIT_TEST(MoveTest) {
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            MoveTestFun(testClient);
        }
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            MoveTestFun(testClient, "//home_dir/");
        }

        if (false) // you can remove "if" to ensure everything works on real YT client
        {
            NYT::JoblessInitialize();
            auto realClient = NYT::CreateClient("hahn");
            realClient->Create("images/dev/mseifullin/test_dir", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true));
            MoveTestFun(realClient, "images/dev/mseifullin/test_dir/");
            realClient->Remove("images/dev/mseifullin/test_dir", NYT::TRemoveOptions().Recursive(true));
        }
    }

    Y_UNIT_TEST(GetTest) {
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            GetTestFun(testClient);
        }
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            GetTestFun(testClient, "//home_dir/");
        }
        if (false) // you can remove "if" to ensure everything works on real YT client
        {
            NYT::JoblessInitialize();
            auto realClient = NYT::CreateClient("hahn");
            realClient->Create("images/dev/mseifullin/test_dir", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true).Attributes(NYT::TNode()("attr1", "value1")));
            GetTestFun(realClient, "images/dev/mseifullin/test_dir/");
            realClient->Remove("images/dev/mseifullin/test_dir", NYT::TRemoveOptions().Recursive(true));
        }
    }

    Y_UNIT_TEST(SetTest) {
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            SetTestFun(testClient);
        }
        {
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", "//home_dir/"));
            SetTestFun(testClient, "//home_dir/");
        }

        if (false) // you can remove "if" to ensure everything works on real YT client
        {
            NYT::JoblessInitialize();
            auto realClient = NYT::CreateClient("hahn");
            realClient->Create("images/dev/mseifullin/test_dir", NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Force(true).Attributes(NYT::TNode()("attr1", "value1")));
            SetTestFun(realClient, "images/dev/mseifullin/test_dir/");
            realClient->Remove("images/dev/mseifullin/test_dir", NYT::TRemoveOptions().Recursive(true));
        }
    }

    Y_UNIT_TEST(CanonizeYPathTest) {
        {
            const TString ytPrefix = "//home/some_prefix/";
            NYT::IClientBasePtr testClient(new TClientBaseMock(".", ytPrefix));
            CanonizeYPathTestFun(testClient, ytPrefix);
        }

        if (false) // you can remove "if" to ensure everything works on real YT client
        {
            NYT::JoblessInitialize();
            auto realClient = NYT::CreateClient("arnold");
            CanonizeYPathTestFun(realClient, "//home/");
        }
    }
}
