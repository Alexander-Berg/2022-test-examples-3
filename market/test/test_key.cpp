#include <market/library/cms_collections/Key.h>
#include <market/library/cms_collections/CmsKeyList.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/string_utils/scan/scan.h>
#include <util/generic/strbuf.h>
#include <util/string/cast.h>


using NCmsCollections::DecodeKey;
using NCmsCollections::EOnKeyCallbackResult;
using NCmsCollections::TSplitsSet;
using NCmsCollections::ProcessKeys;
using NCmsCollections::ICmsParams;

class TDefaultParser : public ICmsParams {
public:
    TDefaultParser(const TStringBuf& request) {
        auto adder = [this](const TStringBuf& key, const TStringBuf& val) {
            params.emplace(ToString(key), ToString(val));
        };

        ScanKeyValue<true, '&', '='> (request, adder);
    }

    void AllValues(const TOnKeyValueCallback& callback) const override {
        for(const auto& it : params)
            callback(it.first, it.second);
    }

    void ValuesByKey(const TString& key, const TOnValueCallback& callback) const override {
        auto itPair = params.equal_range(key);
        for(; itPair.first != itPair.second; ++itPair.first)
            callback(itPair.first->second);
    }

private:
    TMultiMap<TString, TString> params;
};


TEST(TestKey, TestDecoSupleSplitParam)
{
    auto assertEqList = [](const TSplitsSet& value, const TSplitsSet& sample) {
        ASSERT_EQ(value.size(), sample.size());
        ASSERT_EQ(value, sample);
    };

    auto outKey = DecodeKey("foo=bar");
    ASSERT_EQ(outKey.BaseKey, "foo=bar");
    ASSERT_TRUE(outKey.Splits.empty());

    outKey = DecodeKey("foo=100#bar=500");
    ASSERT_EQ(outKey.BaseKey, "foo=100#bar=500");
    ASSERT_TRUE(outKey.Splits.empty());

    outKey = DecodeKey("foo=100#bar=500#split=1");
    ASSERT_EQ(outKey.BaseKey, "foo=100#bar=500");
    assertEqList(outKey.Splits, {1});

    outKey = DecodeKey("foo=bar#split=1,2,3");
    ASSERT_EQ(outKey.BaseKey, "foo=bar");
    assertEqList(outKey.Splits, {1, 2 ,3});

    outKey = DecodeKey("foo=300#split=1,5#bar=800");
    ASSERT_EQ(outKey.BaseKey, "foo=300#bar=800");
    assertEqList(outKey.Splits, {1, 5});

    outKey = DecodeKey("foo=300#split=1#bar=800");
    ASSERT_EQ(outKey.BaseKey, "foo=300#bar=800");
    assertEqList(outKey.Splits, {1});

    outKey = DecodeKey("split=1,5#bar=800");
    ASSERT_EQ(outKey.BaseKey, "bar=800");
    assertEqList(outKey.Splits, {1, 5});

    outKey = DecodeKey("split=1,3,5");
    ASSERT_EQ(outKey.BaseKey, "");
    assertEqList(outKey.Splits, {1, 3, 5});

    outKey = DecodeKey("device=phone&semantic_id=obzor-invertornoj-split-sistemy&type=article");
    ASSERT_EQ(outKey.BaseKey, "device=phone&semantic_id=obzor-invertornoj-split-sistemy&type=article");
    ASSERT_EQ(outKey.Splits.size(), 0);

    outKey = DecodeKey("semantic_id=obzor-invertornoj-split-sistemy#split=1,2,3");
    ASSERT_EQ(outKey.BaseKey, "semantic_id=obzor-invertornoj-split-sistemy");
    assertEqList(outKey.Splits, {1, 2, 3});

    outKey = DecodeKey("split=5#semantic_id=obzor-invertornoj-split-sistemy");
    ASSERT_EQ(outKey.BaseKey, "semantic_id=obzor-invertornoj-split-sistemy");
    assertEqList(outKey.Splits, {5});

};


TEST(TestKey, TestDecoSupleRearrParam)
{
    using TExperiments = THashSet<NCmsCollections::TRearrExperiment>;

    auto outKey = DecodeKey("foo=bar");
    ASSERT_EQ(outKey.BaseKey, "foo=bar");
    ASSERT_TRUE(outKey.Experiments.empty());

    outKey = DecodeKey("foo=100#bar=500");
    ASSERT_EQ(outKey.BaseKey, "foo=100#bar=500");
    ASSERT_TRUE(outKey.Experiments.empty());

    outKey = DecodeKey("foo=100#bar=500#rearr-factors=A=1");
    ASSERT_EQ(outKey.BaseKey, "foo=100#bar=500");
    TExperiments sample3 = {"A=1"};
    ASSERT_EQ(outKey.Experiments, sample3);

    outKey = DecodeKey("foo=bar#rearr-factors=A=1;B=2;C");
    ASSERT_EQ(outKey.BaseKey, "foo=bar");
    TExperiments sample4 = {"A=1", "B=2", "C"};
    ASSERT_EQ(outKey.Experiments, sample4);

    outKey = DecodeKey("foo=300#rearr-factors=A=1;C#bar=800");
    ASSERT_EQ(outKey.BaseKey, "foo=300#bar=800");
    TExperiments sample5 = {"A=1", "C"};
    ASSERT_EQ(outKey.Experiments, sample5);

    outKey = DecodeKey("foo=300#rearr-factors=D#bar=800");
    ASSERT_EQ(outKey.BaseKey, "foo=300#bar=800");
    TExperiments sample6 = {"D"};
    ASSERT_EQ(outKey.Experiments, sample6);

    outKey = DecodeKey("rearr-factors=D;A=3#bar=800");
    ASSERT_EQ(outKey.BaseKey, "bar=800");
    TExperiments sample7 = {"A=3", "D"};
    ASSERT_EQ(outKey.Experiments, sample7);

    outKey = DecodeKey("rearr-factors=A=4;B;C");
    ASSERT_EQ(outKey.BaseKey, "");
    TExperiments sample8 = {"A=4", "B", "C"};
    ASSERT_EQ(outKey.Experiments, sample8);

    outKey = DecodeKey("device=phone&semantic_id=obzor-invertornoj-rearr-factors-sistemy&type=article");
    ASSERT_EQ(outKey.BaseKey, "device=phone&semantic_id=obzor-invertornoj-rearr-factors-sistemy&type=article");
    ASSERT_EQ(outKey.Experiments.size(), 0);

    outKey = DecodeKey("semantic_id=obzor-invertornoj-rearr-factors-sistemy#rearr-factors=A=1;B=2;C");
    ASSERT_EQ(outKey.BaseKey, "semantic_id=obzor-invertornoj-rearr-factors-sistemy");
    TExperiments sample10 = {"A=1", "B=2", "C"};
    ASSERT_EQ(outKey.Experiments, sample10);

    outKey = DecodeKey("rearr-factors=B=2#semantic_id=obzor-invertornoj-rearr-factors-sistemy");
    ASSERT_EQ(outKey.BaseKey, "semantic_id=obzor-invertornoj-rearr-factors-sistemy");
    TExperiments sample11 = {"B=2"};
    ASSERT_EQ(outKey.Experiments, sample11);

    outKey = DecodeKey("foo=100#bar=500#split=3#rearr-factors=A=1");
    ASSERT_EQ(outKey.BaseKey, "foo=100#bar=500");
    ASSERT_EQ(outKey.Splits.size(), 1);
    TExperiments sample12 = {"A=1"};
    ASSERT_EQ(outKey.Experiments, sample12);
};

struct TSampleKey {
    TSampleKey(TString value, bool result = false)
        : Value(std::move(value))
        , Result(result ? EOnKeyCallbackResult::Stop : EOnKeyCallbackResult::Continue)
    { }
public:
    TString Value;
    EOnKeyCallbackResult Result;
};

class TProcessKeysValidator {
public:

    TProcessKeysValidator& operator << (TSampleKey key) {
        Keys.emplace_back(std::move(key));
        return *this;
    }

    TProcessKeysValidator& operator << (TString key) {
        Keys.emplace_back(TSampleKey(key));
        return *this;
    }

    EOnKeyCallbackResult CheckKey(const TString& key) {
        ASSERT_LT(CurrentIndex, Keys.size());

        ASSERT_EQ(key, Keys[CurrentIndex].Value);

        return Keys[CurrentIndex++].Result;
    }

    void CheckFinished() {
        ASSERT_EQ(CurrentIndex, Keys.size());
    }

private:
    TVector<TSampleKey> Keys;
    mutable size_t CurrentIndex = 0;
};

static void CheckProcessKey(const TString& request, THashSet<TString> serviceKeys, TProcessKeysValidator validator) {
    const TDefaultParser params(request);
    ProcessKeys(params, serviceKeys, [&validator](const TString& key) -> EOnKeyCallbackResult {
        return validator.CheckKey(key);
    });
    validator.CheckFinished();
}

TEST(TestKey, TestProcessKeysBase) {
    CheckProcessKey("a=1&c=2&b=3", {},
                    TProcessKeysValidator()
                        << TSampleKey{"a=1#b=3#c=2", true}
                );

    // Всего один ключ. Не важно применился он или нет
    CheckProcessKey("a=1&c=2&b=3", {},
                    TProcessKeysValidator()
                        << TSampleKey{"a=1#b=3#c=2", false}
                );

    // Порядок параметров в запросе не влияет на ключ. Ключи отсортированы в алфавитном порядке
    CheckProcessKey("b=3&a=1&c=2", {},
                    TProcessKeysValidator()
                        << TSampleKey{"a=1#b=3#c=2", false}
                );

    // Значения тоже не влияют на порядок
    CheckProcessKey("b=1&a=2&c=3", {},
                    TProcessKeysValidator()
                        << TSampleKey{"a=2#b=1#c=3", false}
                );
}

TEST(TestKey, TestProcessKeysSkipService) {
    TString req = "a=1&c=2&b=3";

    CheckProcessKey(req, {"b"},
                    TProcessKeysValidator()
                        << "a=1#c=2"
                );

    CheckProcessKey(req, {"a", "c"},
                    TProcessKeysValidator()
                        << "b=3"
                );
}

TEST(TestKey, TestProcessKeysSkip) {
    // Проверка параметра skip
    // Сперва обрабатывает полный список ключей
    // Потом последовательно отбрасывает по одному ключу
    CheckProcessKey("a=1&c=2&b=3&skip=b", {"skip"},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=1#c=2"
                );

    CheckProcessKey("a=1&c=2&b=3&skip=a,c", {"skip"},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "b=3#c=2"
                        << "b=3"
                );

    // Важен порядок ключей в skip
    CheckProcessKey("a=1&c=2&b=3&skip=c,a", {"skip"},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=1#b=3"
                        << "b=3"
                );
}

TEST(TestKey, TestProcessKeysOneOf) {
    // Проверка параметра one_of
    // За основу берутся все ключи, не входящие в one_of
    // К ним добавляются группы ключей из one_of. Группы разделены запятой
    CheckProcessKey("a=1&c=2&b=3&one_of=b,c", {"one_of"},
                    TProcessKeysValidator()
                        << "a=1#b=3"
                        << "a=1#c=2"
                );

    // Ключи в группе могут быть объеденены символом "*"
    CheckProcessKey("a=1&c=2&b=3&one_of=b*c,b,c", {"one_of"},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=1#b=3"
                        << "a=1#c=2"
                );

    // Важен порядок групп
    CheckProcessKey("a=1&c=2&b=3&one_of=b,b*c,c", {"one_of"},
                    TProcessKeysValidator()
                        << "a=1#b=3"
                        << "a=1#b=3#c=2"
                        << "a=1#c=2"
                );

    //  Но не важен порядок ключей в группе
    CheckProcessKey("a=1&c=2&b=3&one_of=b,c*b,c", {"one_of"},
                    TProcessKeysValidator()
                        << "a=1#b=3"
                        << "a=1#b=3#c=2"
                        << "a=1#c=2"
                );

    // Может быть пустая группа, отделенная запятой
    CheckProcessKey("a=1&c=2&b=3&one_of=b,c,", {"one_of"},
                    TProcessKeysValidator()
                        << "a=1#b=3"
                        << "a=1#c=2"
                        << "a=1"
                );

    // Пустая группа может быть в самом начале
    CheckProcessKey("a=1&c=2&b=3&one_of=,b,c", {"one_of"},
                    TProcessKeysValidator()
                        << "a=1"
                        << "a=1#b=3"
                        << "a=1#c=2"
                );
}

TEST(TestKey, TestProcessKeysBulk) {
    // Проверка запросов со множественными значениями
    CheckProcessKey("a=1,2&c=2&b=3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=2#b=3#c=2"
                );

    CheckProcessKey("a=1&c=2,4&b=3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=1#b=3#c=4"
                );

    CheckProcessKey("a=1,5&c=2,4&b=3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=1#b=3#c=4"
                        << "a=5#b=3#c=2"
                        << "a=5#b=3#c=4"
                );
}

TEST(TestKey, TestProcessKeysBulkStop) {

    // Требование останова не срабатывает для множественных запросов.
    // Будут опрошены все ключи
    CheckProcessKey("a=1,5&c=2,4&b=3", {},
                    TProcessKeysValidator()
                        << TSampleKey{"a=1#b=3#c=2", true}
                        << "a=1#b=3#c=4"
                        << "a=5#b=3#c=2"
                        << "a=5#b=3#c=4"
                );

    // Но если появляется параметр skip, будут опрошены только множественные запросы текущего набора ключей
    CheckProcessKey("a=1,5&c=2,4&b=3&skip=b,c", {"skip"},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=1#b=3#c=4"
                        << "a=5#b=3#c=2"
                        << "a=5#b=3#c=4"
                        << "a=1#c=2"
                        << "a=1#c=4"
                        << "a=5#c=2"
                        << "a=5#c=4"
                        << "a=1"
                        << "a=5"
                );


    CheckProcessKey("a=1,5&c=2,4&b=3&skip=b,c", {"skip"},
                    TProcessKeysValidator()
                        // Если в блоке есть хотя бы один ключ, требующий остановку
                        << TSampleKey{"a=1#b=3#c=2", true}
                        // Остальные ключи этого блока будут опрошены
                        << "a=1#b=3#c=4"
                        << "a=5#b=3#c=2"
                        << "a=5#b=3#c=4"
                        // Следующий блок не будет опрошен
                );

    CheckProcessKey("a=1,5&c=2,4&b=3&skip=b,c", {"skip"},
                    TProcessKeysValidator()
                        << TSampleKey{"a=1#b=3#c=2", false}
                        << TSampleKey{"a=1#b=3#c=4", true}      // Не важно в каком ключе будет останов, весь блок будет опрошен
                        << "a=5#b=3#c=2"
                        << "a=5#b=3#c=4"
                        // Следующий блок не будет опрошен
                );

    CheckProcessKey("a=1,5&c=2,4&b=3&skip=b,c", {"skip"},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2"
                        << "a=1#b=3#c=4"
                        << "a=5#b=3#c=2"
                        << "a=5#b=3#c=4"
                        // Опрашиваем второй блок
                        << "a=1#c=2"
                        << "a=1#c=4"
                        << TSampleKey{"a=5#c=2", true}
                        << "a=5#c=4"
                        // Третий блок опрошен не будет
                );

}

TEST(TestKey, TestProcessKeysBulkSplit) {
    // Параметр split считается одним значением
    CheckProcessKey("a=1&c=2&b=3&split=1,2,3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2#split=1,2,3"
                );

    CheckProcessKey("a=1&split=1,2,3&c=2&b=3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2#split=1,2,3"
                );

    CheckProcessKey("split=1,2,3&a=1&c=2&b=3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2#split=1,2,3"
                );

    // Другие параметры при этом обладают множественными значениями
    CheckProcessKey("split=1,2,3&a=1,2&c=2&b=3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2#split=1,2,3"
                        << "a=2#b=3#c=2#split=1,2,3"
                );
}

TEST(TestKey, TestProcessKeysRearrFactors) {
    // проверяем, что реарр-факторы не разделяются по запятой
    CheckProcessKey("a=1&c=2&b=3&rearr-factors=1,2,3", {},
                    TProcessKeysValidator()
                        << "a=1#b=3#c=2#rearr-factors=1,2,3"
                );
}


TEST(TestKey, TestProcessKeysSkipOneOfBulk) {
    // Тестирование полной комбинации:
    // 1. Выбор одного из параметров
    // 2. Множественные значения параметров
    // 3. Пропуск параметров

    CheckProcessKey("a=1,2&c=2&b=3&one_of=a,&skip=b", {"one_of", "skip"},
                    TProcessKeysValidator()
                        // Первая группа one_of: a
                        << TSampleKey{"a=1#b=3#c=2", true}      // Первый ключ подошел
                        << TSampleKey{"a=2#b=3#c=2", false}     // Но будет опрошен еще один ключ из множества.
                        // Обработка ключей прекращается
                );


    CheckProcessKey("a=1,2&c=2&b=3&one_of=a,&skip=b", {"one_of", "skip"},
                    TProcessKeysValidator()
                        // Первая группа one_of: a
                        << TSampleKey{"a=1#b=3#c=2", false}
                        << TSampleKey{"a=2#b=3#c=2", false}
                        // Была взята следующая группа из one_of: пустой параметр (после запятой)
                        << TSampleKey{"b=3#c=2", true}
                        // Обработка ключей прекращается
                );

    CheckProcessKey("a=1,2&c=2&b=3&one_of=a,&skip=b", {"one_of", "skip"},
                    TProcessKeysValidator()
                        // Первая группа one_of: a
                        << TSampleKey{"a=1#b=3#c=2", false}
                        << TSampleKey{"a=2#b=3#c=2", false}
                        // Была взята следующая группа из one_of: пустой параметр (после запятой)
                        << TSampleKey{"b=3#c=2", false}
                        // Применяется параметр skip (b)
                        // Снова первая группа one_of: a
                        << TSampleKey{"a=1#c=2", false}
                        << TSampleKey{"a=2#c=2", false}
                        // Была взята следующая группа из one_of: пустой параметр (после запятой)
                        << TSampleKey{"c=2", false}
                        // Больше ключей нет
                );
}


TEST(TestKey, TestProcessKeyUseFirstBatchValueFor) {
    // Проверка параметра use_first_batch_value_for
    //значения одного параметра будут перебираться в указанном порядке
    CheckProcessKey("a=1&c=2&b=3,4,5&use_first_batch_value_for=b", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << "a=1#b=3#c=2"
                    << "a=1#b=4#c=2"
                    << "a=1#b=5#c=2"
               );

    // значения будут перебираться в порядке, указанном в параметре
    CheckProcessKey("a=1&c=2,4&b=7,5&use_first_batch_value_for=c,b", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << "a=1#b=7#c=2"
                    << "a=1#b=5#c=2"
                    << "a=1#b=7#c=4"
                    << "a=1#b=5#c=4"
               );

    // фэйковый аргумент игнорируется
    CheckProcessKey("a=1&c=2,4&b=7,5&use_first_batch_value_for=c,b,fake", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << "a=1#b=7#c=2"
                    << "a=1#b=5#c=2"
                    << "a=1#b=7#c=4"
                    << "a=1#b=5#c=4"
               );

    // дублирующийся аргумент игнорируется
    CheckProcessKey("a=1&c=2,4&b=7,5&use_first_batch_value_for=c,b,c", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << "a=1#b=7#c=2"
                    << "a=1#b=5#c=2"
                    << "a=1#b=7#c=4"
                    << "a=1#b=5#c=4"
    );

    // пустой аргумент игнорируется
    CheckProcessKey("a=1&c=2,4&b=7,5&use_first_batch_value_for=c,b", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << "a=1#b=7#c=2"
                    << "a=1#b=5#c=2"
                    << "a=1#b=7#c=4"
                    << "a=1#b=5#c=4"
    );

    // не сломали множественные запросы
    CheckProcessKey("a=1,5&c=2,4&use_first_batch_value_for=a", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << TSampleKey{"a=1#c=2", true}
                    << TSampleKey{"a=1#c=4", false}
    );

    // обрабатываем пустой флаг
    CheckProcessKey("a=1,5&c=2,4&use_first_batch_value_for=", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << TSampleKey{"a=1#c=2", true}
                    << TSampleKey{"a=1#c=4", false}
                    << TSampleKey{"a=5#c=2", false}
                    << TSampleKey{"a=5#c=4", false}
    );

    // значения будут перебираться в порядке, указанном в параметре
    CheckProcessKey("a=1&c=2,4&b=7,5&use_first_batch_value_for=c,b", {"use_first_batch_value_for"},
                    TProcessKeysValidator()
                    << TSampleKey{"a=1#b=7#c=2", true}
    );
}

TEST(TestKey, TestKeyList) {
    using TAnswer = NCmsCollections::TCmsKeyList::TListType;

    // Проверяем простой поиск по ключу
    NCmsCollections::TCmsKeyList listOfPages;
    listOfPages.addKey("A=1", "1");
    listOfPages.addKey("A=1#B=2", "1,2");
    // Страницы экспериментов
    listOfPages.addKey("A=1#rearr-factors=t", "1,3");           // Простой тест
    listOfPages.addKey("A=1#rearr-factors=t=1", "1,4");         // Тест со значением
    listOfPages.addKey("A=1#rearr-factors=t=1,2", "1,5");       // Тест со сложным значением
    listOfPages.addKey("A=1#rearr-factors=t=7;g", "1,6");       // Два теста
    // Тест без дефолтной страницы
    listOfPages.addKey("A=2#rearr-factors=t=7", "2,1");
    // Обратные эксперименты
    listOfPages.addKey("A=3", "3");
    listOfPages.addKey("A=3#rearr-factors=!t=3", "3,1");        // Один обратный эксп
    listOfPages.addKey("A=4", "4");
    listOfPages.addKey("A=4#rearr-factors=!t=3;!t=4", "4,1");   // Два обратных экспа
    listOfPages.addKey("A=5", "5");
    listOfPages.addKey("A=5#rearr-factors=!t=3;t=5", "5,1");    // Один прямой, один обратный

    auto check = [&listOfPages](const TString& key, const TAnswer& sample) {
        auto* answer = listOfPages.getValuesList(key);
        ASSERT_EQ(*answer, sample);
    };

    // Простой ключ, простой ответ
    check("A=1", {1});

    // Этого ключа нет в списке
    ASSERT_EQ(listOfPages.getValuesList("A=0"), nullptr);

    // Страница с двумя страницами в ответе
    check("A=1#B=2", {1, 2});

    // Эксперименты
    // Такого экспа нет в списке, значит берется дефолтная страница
    check("A=1#rearr-factors=e", {1});

    // Простой эксп без значения
    check("A=1#rearr-factors=t", {1, 3});

    // Эксп со значением
    check("A=1#rearr-factors=t=1", {1, 4});

    // Такого значения экспа нет. Берется дефолтная страница
    check("A=1#rearr-factors=t=2", {1});

    // Эксп со сложным значением
    check("A=1#rearr-factors=t=1,2", {1, 5});

    // Два теста в странице. Получаем по любому из значений
    check("A=1#rearr-factors=t=7", {1, 6});
    check("A=1#rearr-factors=g", {1, 6});

    // Тест без дефолтной страницы
    check("A=2#rearr-factors=t=7", {2, 1});
    ASSERT_EQ(listOfPages.getValuesList("A=2"), nullptr);
    ASSERT_EQ(listOfPages.getValuesList("A=2#rearr-factors=t=6"), nullptr);

    // Обратные эксперименты. Если пользователь попал в эксп, то ему не показывается спец страница. Берется дефолтная
    check("A=3#rearr-factors=t=3", {3});
    check("A=3#rearr-factors=t=2", {3, 1});
    check("A=3", {3, 1});

    // Обратные эксперименты. Два обратных на странице.
    // Если пользователь учатвует хоть в одном экспе, то спец страница ему не показывается
    check("A=4#rearr-factors=t=3", {4});
    check("A=4#rearr-factors=t=4", {4});
    check("A=4#rearr-factors=t=2", {4, 1});
    check("A=4", {4, 1});

    // Обратный эксп и прямой. Экспериментальная покажется только если пользователь участвует в прямой (t=5), но не участвует в обратном (t=3)
    check("A=5#rearr-factors=t=3", {5});
    check("A=5#rearr-factors=t=4", {5});
    check("A=5#rearr-factors=t=5", {5, 1});
    check("A=5#rearr-factors=t=5;t=3", {5});
    check("A=5#rearr-factors=t=3;t=5", {5});    // Порядок экспериментов не влияет
    check("A=5", {5});
}

TEST(TestKey, TestKeyListExpError) {
    // Проблема с выбором экспа
    using TAnswer = NCmsCollections::TCmsKeyList::TListType;

    const TString ERROR_TEXT = "Experiments detect error for user. Multiple result";

    NCmsCollections::TCmsKeyList listOfPages;

    TString logMessage;
    auto logger = [&logMessage](const TString& message) {
        logMessage = message;
    };

    auto check = [&listOfPages, &logger, &logMessage](const TString& key, const TAnswer& sample) {
        logMessage = "";
        auto* answer = listOfPages.getValuesList(key, logger);
        ASSERT_EQ(*answer, sample);
    };

    // Плохие настройки. Оба документа подходят всегда
    listOfPages.addKey("A=6#rearr-factors=t=1", "6,1");
    listOfPages.addKey("A=6#rearr-factors=!t=2", "6");

    // Ошибка будет, если пользователь участвует в обоих экспах
    listOfPages.addKey("A=7", "7");
    listOfPages.addKey("A=7#rearr-factors=t=1", "7,1");
    listOfPages.addKey("A=7#rearr-factors=e=1", "7,2");

    // Тут проблем нет
    check("A=6", {6});
    ASSERT_EQ(logMessage, "");

    // Проблема. Оба экспа подподают под условия экспа
    listOfPages.getValuesList("A=6#rearr-factors=t=1", logger);
    ASSERT_EQ(logMessage, ERROR_TEXT);

    // Проблема с двумя разными экспами.
    // По отдельности экспы работают нормально
    check("A=7#rearr-factors=t=1", {7, 1});
    ASSERT_EQ(logMessage, "");

    check("A=7#rearr-factors=e=1", {7, 2});
    ASSERT_EQ(logMessage, "");

    // Вместе у одного пользователя экспы вызывают ошибку
    listOfPages.getValuesList("A=6#rearr-factors=t=1;e=1", logger);
    ASSERT_EQ(logMessage, ERROR_TEXT);
}
