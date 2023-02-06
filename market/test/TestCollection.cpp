#include "Collection.h"
#include "CollectionFactory.h"

#include <market/library/market_servant/logger/logger.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/string/subst.h>
#include <util/stream/file.h>
#include <util/stream/str.h>
#include <util/folder/tempdir.h>
#include <util/random/random.h>

TString WriteToTmpFile(const TStringStream& stream) {
    static const TTempDir TempDir;
    const TString path = TempDir.Path() / ToString(RandomNumber<unsigned int>(1000));
    TFileOutput out(path);
    out << stream.Str();
    out.Finish();
    return path;
}

THolder<ICollection> CreateCollectionOffsetSize(const TString& datafilename, const TString& offsetsizefilename);
THolder<ICollection> CreateCollectionKeyData(
    const TString& filename,
    ECollectionContentType content_type = ECollectionContentType::CCT_NOT_DEFINED,
    bool cutOutKeys = false);
THolder<ICollection> CreateCollectionKeyData(const TStringStream& stream, bool cutOutKeys = false) {
    return CreateCollectionKeyData(WriteToTmpFile(stream), ECollectionContentType::CCT_NOT_DEFINED, cutOutKeys);
}
THolder<ICollection> CreateCollectionKeyList(const TString& filename, const TString& collection_of_values);
THolder<ICollection> CreateCollectionKeyList(const TStringStream& stream, const TString& collection_of_values) {
    return CreateCollectionKeyList(WriteToTmpFile(stream), collection_of_values);
}
THolder<ICollection> CreateCollectionKeys(const TString& filename, const TString& refname);
THolder<ICollection> CreateCollectionKeys(const TString& filename, const Collection& collection);
THolder<ICollection> CreateCollectionKeys(IInputStream& stream, const TString& refname);
THolder<ICollection> CreateCollectionKeys(IInputStream& stream, const Collection& collection);
TCollectionsVector CreateCollectionsFromDir(const TString& dirname);

THolder<ICollection> create1()
{
    return CreateCollectionKeyData(SRC_("data/data2.keydata"));
}

THolder<ICollection> create2()
{
    return CreateCollectionOffsetSize(SRC_("data/data.data"), SRC_("data/data.index"));
}

THolder<ICollection> create3(const Collection& base)
{
    return CreateCollectionKeys(SRC_("data/keys"), base);
}

THolder<ICollection> create_keydata_collection()
{
    TStringStream stream;
    stream << "1:hello\n";
    stream << "2a: \n";
    stream << "3:world\n";
    return THolder<ICollection>(CreateCollectionKeyData(stream));
}

THolder<ICollection> create_keys_collection(const Collection* base)
{
    TStringStream stream;
    stream << "1\n";
    stream << "3\n";
    return THolder<ICollection>(CreateCollectionKeys(stream, *base));
}

struct LogInitializer
{
        LogInitializer()
        {
            theLogger::Instance().SetBinLogLevel(LogLevel::ALL);
            LOG(LogLevel::INFO, "start unit tests");
        }
};

//static LogInitializer g_logInitializer;

TEST(TestKeyDataCollection, GetCard)
{
    THolder<ICollection>  collection = create_keydata_collection();

    ASSERT_TRUE(collection->getKeys().size() == 3);
    ASSERT_EQ(collection->getCard("1"), "hello");
    ASSERT_EQ(collection->getCard("2a"), " ");
    ASSERT_EQ(collection->getCard("3"), "world");
    ASSERT_EQ(collection->getCards(collection->getKeys(), false), "hello world");
};

TEST(TestKeysCollection, GetCard)
{
    THolder<ICollection> collection = create_keydata_collection();
    THolder<Collection> base_collection(new Collection(std::move(collection), "name"));
    THolder<ICollection> keys_collection = create_keys_collection(base_collection.Get());

    ASSERT_TRUE(keys_collection->getKeys().size() == 2);
    ASSERT_EQ(keys_collection->getCard("1"), "hello");
    ASSERT_EQ(keys_collection->getCard("3"), "world");
    ASSERT_EQ(keys_collection->getCards(keys_collection->getKeys(), false), "helloworld");
}

TEST(TestCollectionKeys, GetKeys)
{
    TStringStream stream;
    stream << "bla-bla\n";
    stream << "\n";
    stream << "1\n";
    stream << "2\n";
    TKey keys[] = {"1", "2"};
    THolder<ICollection> collection(CreateCollectionKeys(stream, "name"));
    ASSERT_EQ(collection->getKeys(), TKeys(keys, keys + 2));
}

TEST(TestCollectionKeyData, RandomCards)
{
    TStringStream str1;
    str1 << "1:11\n";
    str1 << "2:22\n";
    str1 << "3:33\n";
    str1 << "4:44\n";
    str1 << "5:55\n";
    Collection collection(CreateCollectionKeyData(str1), TString());

    TStringStream str2;
    str2 << "1:1\n";
    str2 << "2:1\n";
    str2 << "3:2\n";
    Collection keys(CreateCollectionKeys(str2, collection), TString());

    for (size_t i = 0; i < 10; ++i)
    {
        TString cards = keys.getRandomCards(2);
        ASSERT_FALSE(cards == "1122");
        ASSERT_FALSE(cards == "2211");
        ASSERT_TRUE(cards == "1133" || cards == "2233" || cards == "3311" || cards == "3322");
    }
}

TEST(TestCollectionKeyData, GetCard)
{
    Collection collection(create1(), "test");
    ASSERT_EQ(collection.getCard("1"), "hello");
    ASSERT_EQ(collection.getCard("20"), "world");
    ASSERT_EQ(collection.getCard("10"), ":hello world:");
    ASSERT_EQ(collection.getCard("y4-5"), "YNDX");
    ASSERT_TRUE(collection.get()->getKeys().size() != 0);
}

TEST(TestCollectionKeyData, UsersInfoCollection)
{
    TStringStream str;
    str << "10:<user id=\"10\" grades=\"1\"><social></social></user>" << Endl;
    str << "14:<user grades=\"11\"></user>" << Endl;
    str << "20:<user id=\"25\" grades=\"21\"></user>" << Endl;

    for (bool cutOffKeys: {true, false}) {
        const auto collection = CreateCollectionKeyData(str, cutOffKeys);
        ASSERT_EQ(collection->getCard("10"), "<user id=\"10\" grades=\"1\"><social></social></user>");
        ASSERT_EQ(collection->getCard("14"), "<user grades=\"11\"></user>");
        ASSERT_EQ(collection->getCard("20"), "<user id=\"25\" grades=\"21\"></user>");
    }
}

TEST(TestCollectionKeyList, GetCard)
{
    THolder<ICollection> base = CreateCollectionKeyData(SRC_("data/items.keydata"));
    THolder<ICollection> keylist = CreateCollectionKeyList(SRC_("data/key_lists.keydata"), "items");
    Collection* base_collection = new Collection(std::move(base), "items");
    Collection* keylist_collection = new Collection(std::move(keylist), "key_lists");

    TCollectionsVector collections;
    collections.push_back(base_collection);
    collections.push_back(keylist_collection);
    TCollectionSet collectionSet(collections);

    ASSERT_EQ(collectionSet.getCard("items", "3"), "<page id=\"3\"/>");
    ASSERT_EQ(collectionSet.getCard("key_lists", "nid=0"), "");
    ASSERT_TRUE(collectionSet.find("key_lists")->get()->hasKey("nid=10"));
    ASSERT_TRUE(collectionSet.find("key_lists")->get()->hasKey("nid=30"));
    ASSERT_EQ(collectionSet.getCard("key_lists","nid=30"), "<page id=\"3\"/><page id=\"2\"/><page id=\"1\"/>");

    //test splits
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=xml#split=1,3#type=collection"), "<page100/>");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=xml#split=1,2,3#type=collection"), "<page200/>");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=json#split=3,2,1#type=collection"), "{\"page\":200}");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=xml#split=3,4#type=collection"), "<page300/><page100/>");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=xml#split=1,3,4,2#type=collection"), "<page200/>");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=xml#split=1,2,3,4,5#type=collection"), "<page400/>");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=json#split=1,2,3,4,5#type=collection"), "{\"page\":200}");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=mobile#format=xml#split=6#type=collection"), "");
}

TEST(TCollectionOffsetSize, GetCard)
{
    Collection collection(create2(), "test");
    ASSERT_EQ(collection.getCard("1"), "pi");
    ASSERT_EQ(collection.getCard("2"), "1234567890");
    ASSERT_EQ(collection.getCard("3"), "hello world");
    ASSERT_TRUE(collection.get()->getKeys().size() != 0);
}

TEST(TestCollectionKeys, GetRandomCards)
{
    Collection collection_data(create2(), "data");
    THolder<ICollection> collection = create3(collection_data);
    ASSERT_TRUE(collection->getKeys().size() == 2);

    Collection keys(std::move(collection), "keys");
    TString cards = keys.getRandomCards(1);
    ASSERT_TRUE(cards == "1234567890" || cards == "pi");
    cards = keys.getRandomCards(2);
    ASSERT_TRUE(cards == "1234567890pi" || cards == "pi1234567890");
    cards = keys.getRandomCards(100);
    ASSERT_TRUE(cards == "1234567890pi" || cards == "pi1234567890");
}

TEST(TestTCollectionSet, GetCard)
{
    TCollectionsVector collections;
    collections.push_back(new Collection(create1(), "1"));
    collections.push_back(new Collection(create2(), "2"));

    TCollectionSet collectionSet(collections);
    ASSERT_EQ(collectionSet.find("1")->getCard("1"), "hello");
    ASSERT_EQ(collectionSet.find("2")->getCard("1"), "pi");
}

TEST(TestTCollectionSet, CreateBuildersFromDir)
{
    TCollectionSet collectionSet(CreateCollectionsFromDir(SRC_("data")));
    ASSERT_EQ(collectionSet.size(), 6);
    ASSERT_EQ(collectionSet.find("data")->getCard("1"), "pi");
    ASSERT_EQ(collectionSet.find("data2")->getCard("1"), "hello");
    ASSERT_EQ(collectionSet.find("keys")->getCard("1"), "pi");
};

TEST(TestCollectionKeyList, GetRearrCard)
{
    TStringStream streamBase;
    streamBase << "11:page1Default\n";

    streamBase << "21:page2Default\n";
    streamBase << "22:page2RearrA4\n";

    streamBase << "31:page3Default\n";
    streamBase << "32:page3RearrA4\n";
    streamBase << "33:page3RearrB\n";

    streamBase << "42:page4RearrA4\n";
    streamBase << "43:page4RearrB\n";

    streamBase << "51:page5MultiExp\n";


    TStringStream streamKeyList;
    // Страница без экспериментов
    streamKeyList << "device=Page1#type=collection:11\n";

    // Заведен один эксперимент. Ждем полного совпадения
    streamKeyList << "device=Page2#type=collection:21\n";
    streamKeyList << "device=Page2#rearr-factors=A=4#type=collection:22\n";

    // Есть два эксперимента. Отдаем первый попавшийся в запросе
    streamKeyList << "device=Page3#type=collection:31\n";
    streamKeyList << "device=Page3#rearr-factors=A=4#type=collection:32\n";
    streamKeyList << "device=Page3#rearr-factors=B#type=collection:33\n";

    // Без дефолтной страницы
    streamKeyList << "device=Page4#rearr-factors=A=4#type=collection:42\n";
    streamKeyList << "device=Page4#rearr-factors=B#type=collection:43\n";

    // Два эксперимента в странице
    streamKeyList << "device=Page5#rearr-factors=A=4;B#type=collection:51\n";


    streamKeyList << "device=Page1Default#format=xml#rearr-factors=A=3#type=collection:2\n";
    streamKeyList << "device=Page1Default#format=xml#type=collection:1\n";
    streamKeyList << "device=Page1Default#format=xml#type=collection:1\n";
    streamKeyList << "device=Page1Default#format=xml#type=collection:1\n";
    streamKeyList << "device=Page1Default#format=xml#type=collection:1\n";
    streamKeyList << "device=Page1Default#format=xml#type=collection:1\n";


    THolder<ICollection> base = CreateCollectionKeyData(streamBase);
    THolder<ICollection> keylist = CreateCollectionKeyList(streamKeyList, "items");
    Collection* base_collection = new Collection(std::move(base), "items");
    Collection* keylist_collection = new Collection(std::move(keylist), "key_lists");

    TCollectionsVector collections;
    collections.push_back(base_collection);
    collections.push_back(keylist_collection);
    TCollectionSet collectionSet(collections);

    // У страницы нет эксперимента
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page1#type=collection"), "page1Default");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page1#rearr-factors=A=3;B=1#type=collection"), "page1Default");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page1#rearr-factors=A=4;B=1#type=collection"), "page1Default");

    // Эксперимент A должен полностью совпадать. Эксперимент B не влияет на выбор
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page2#type=collection"), "page2Default");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page2#rearr-factors=A=3;B=1#type=collection"), "page2Default");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page2#rearr-factors=A=4;B=1#type=collection"), "page2RearrA4");

    // Порядок экспериментов в запросе не влияет на выдачу страниц.
    // Так правильно, т.к. порядок экспериментов в запросе не определен. Порядок страниц в данных более упорядочен.
    // Вообще, участие в двух экспериментах - это проблема. В темплаторе для этого завел ошибку, но здесь не вижу смысла.
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page3#type=collection"), "page3Default");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page3#rearr-factors=A=4;B#type=collection"), "page3RearrA4");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page3#rearr-factors=B;A=4#type=collection"), "page3RearrA4");

    // Если дефолтной страницы нет, то ничего не возвращается
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page4#type=collection"), "");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page4#rearr-factors=A=4;B#type=collection"), "page4RearrA4");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page4#rearr-factors=B;A=4#type=collection"), "page4RearrA4");

    // На странице было несколько экспериментов: все они ведут на одну и ту же страницу
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page5#rearr-factors=A=4#type=collection"), "page5MultiExp");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page5#rearr-factors=B#type=collection"), "page5MultiExp");
    ASSERT_EQ(collectionSet.getCard("key_lists","device=Page5#rearr-factors=A=4;B#type=collection"), "page5MultiExp");
}
