#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/tempdir.h>
#include <util/system/fs.h>

using namespace NMarket;

void RunTest(const TString& inputXml, Market::DataCamp::ExplanationBatch& explanationBatch) {
    TTempDir tempDir;
    const TString explanationErrorsFilepath = JoinFsPaths(tempDir.Path(), "feed_errors.pbuf.sn");
    const auto actual = RunWhiteYmlFeedParserWithExplanation<TYmlFeedParser>(
        inputXml,
        [](const TQueueItem&) {
            NSc::TValue result;
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML),
        explanationErrorsFilepath
    );
    ASSERT_TRUE(NFs::Exists(explanationErrorsFilepath));
    NMarket::TSnappyProtoReader reader(explanationErrorsFilepath, NMarket::NExplanationLog::ExplanationLogMagic);
    reader.Load(explanationBatch);
}

TEST(WhiteYmlParser, Category) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1">cat1</category>
        </categories>
      </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 0);
}

TEST(WhiteYmlParser, CategoryEmptyName) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1"></category>
        </categories>
      </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 1);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "531");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::FATAL);
}

TEST(WhiteYmlParser, CategoryIdNan) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="assa">assa</category>
        </categories>
      </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 1);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "532");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::FATAL);
}

TEST(WhiteYmlParser, CategoryValidation) {
    static const TString INPUT_XML = TString(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1">category1</category>
            <category id="1234567890123456789">category2</category>
            <category id="1">category3</category>
            <category id="2">)wrap")
        + TString(1025, 'A') + TString(R"wrap(</category>
            </categories>
          </shop>
        </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 3);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "330");
    ASSERT_EQ(explanationBatch.explanation(1).code(), "331");
    ASSERT_EQ(explanationBatch.explanation(2).code(), "332");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(1).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(2).level(), Market::DataCamp::Explanation::WARNING);
}

TEST(WhiteYmlParser, CategoriesSimpleCycle) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="2" parentId="1">cat2</category>
            <category id="1" parentId="2">cat1</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 1);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "530");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::FATAL);
}

TEST(WhiteYmlParser, CategoriesCycle4) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="3" parentId="4">cat3</category>
            <category id="4" parentId="22">cat4</category>
            <category id="22" parentId="33">cat22</category>
            <category id="33" parentId="3">cat33</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 1);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "530");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::FATAL);
}

TEST(WhiteYmlParser, CategoriesCycleForest) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="8">category8</category>
            <category id="7" parentId="8">category7</category>
            <category id="5" parentId="6">category5</category>
            <category id="6" parentId="5">category6</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 1);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "530");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::FATAL);
}

TEST(WhiteYmlParser, CategoriesSimpleDuplicates) {
  static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1">category1</category>
            <category id="2">category1</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 1);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(0).details(), "{\"categoryIds\":\"1, 2\",\"code\":\"333\",\"categoryName\":\"category1\"}");
}

TEST(WhiteYmlParser, CategoriesParentsDuplicates) {
  static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1">category1</category>
            <category id="21" parentId="1">category2</category>
            <category id="22" parentId="1">category2</category>
            <category id="31" parentId="21">category3</category>
            <category id="32" parentId="22">category3</category>
            <category id="41" parentId="31">category41</category>
            <category id="42" parentId="32">category42</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 2);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(1).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(1).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(0).details(), "{\"categoryIds\":\"21, 22\",\"code\":\"333\",\"categoryName\":\"category2\"}");
    ASSERT_EQ(explanationBatch.explanation(1).details(), "{\"categoryIds\":\"31, 32\",\"code\":\"333\",\"categoryName\":\"category3\"}");
}

TEST(WhiteYmlParser, CategoriesTwoGroupsDuplicates) {
  static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1">category1</category>
            <category id="21" parentId="1">category2</category>
            <category id="22" parentId="1">category2</category>
            <category id="31" parentId="1">category3</category>
            <category id="32" parentId="1">category3</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 2);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(1).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(1).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(0).details(), "{\"categoryIds\":\"31, 32\",\"code\":\"333\",\"categoryName\":\"category3\"}");
    ASSERT_EQ(explanationBatch.explanation(1).details(), "{\"categoryIds\":\"21, 22\",\"code\":\"333\",\"categoryName\":\"category2\"}");
}

TEST(WhiteYmlParser, CategoriesDuplicatesForest) {
  static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1">category1</category>
            <category id="2" parentId="1">category2</category>
            <category id="3" parentId="1">category3</category>
            <category id="4">category1</category>
            <category id="5" parentId="4">category2</category>
            <category id="6" parentId="4">category3</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 3);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(1).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(2).code(), "333");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(1).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(2).level(), Market::DataCamp::Explanation::WARNING);
    ASSERT_EQ(explanationBatch.explanation(0).details(), "{\"categoryIds\":\"1, 4\",\"code\":\"333\",\"categoryName\":\"category1\"}");
    ASSERT_EQ(explanationBatch.explanation(1).details(), "{\"categoryIds\":\"3, 6\",\"code\":\"333\",\"categoryName\":\"category3\"}");
    ASSERT_EQ(explanationBatch.explanation(2).details(), "{\"categoryIds\":\"2, 5\",\"code\":\"333\",\"categoryName\":\"category2\"}");
}

TEST(WhiteYmlParser, CategoriesNotDuplicates) {
  static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1">category1</category>
            <category id="2" parentId="1">same_name_category</category>
            <category id="3" parentId="1">category2</category>
            <category id="4" parentId="3">same_name_category</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 0);
}

TEST(WhiteYMLParser, CategoryIdEqualsParentId) {
  static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog>
      <shop>
        <categories>
            <category id="1" parentId="1">category1</category>
            <category id="2" parentId="1">category2</category>
            <category id="3" parentId="1">category3</category>
            <category id="4" parentId="3">category4</category>
        </categories>
        </shop>
    </yml_catalog>)wrap");
    Market::DataCamp::ExplanationBatch explanationBatch;
    RunTest(INPUT_XML, explanationBatch);
    ASSERT_EQ(explanationBatch.explanation().size(), 1);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "530");
}
