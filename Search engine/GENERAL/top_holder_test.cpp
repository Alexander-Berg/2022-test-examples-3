#include <search/plutonium/workers/panther/l2/lib/top_holder.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/iterator/zip.h>

#include <util/random/shuffle.h>


namespace NPlutonium::NWorkers::NPanther {
struct TSimpleObject {
    ui16 Key = 0;
    ui16 SubKey1 = 0;
    ui64 Value = 0;
    ui32 Weight;
    bool operator==(const TSimpleObject& other) const {
        return std::tie(Key, SubKey1, Value, Weight) ==
            std::tie(other.Key, other.SubKey1, other.Value, other.Weight);
    }
};
class TTestTopHolder : public IKVTopHolder<ui16, TSimpleObject, ui32, TGreater<ui32>> {
public:
    TTestTopHolder(ui64 maxSize)
    : IKVTopHolder(maxSize) {}
};
class TTestConstrainedTopHolder : public IKVTopHolder<ui16, TSimpleObject, ui32, TGreater<ui32>> {
public:
    static ui16 ExtractSubKey(const TSimpleObject& value) {
        return value.SubKey1;
    }
    TTestConstrainedTopHolder(ui64 maxSize, ui64 subKeyMax)
    : IKVTopHolder(maxSize, {
        TConstraint(subKeyMax, ExtractSubKey)
    }) {}
};

TEST(TKVTopHolderSuite, UnconstrainedOneKey) {
    TTestTopHolder holder(1);
    TSimpleObject obj1{
        .Key = 0,
        .SubKey1 = 0,
        .Value = 42,
        .Weight = 1,
    };
    EXPECT_TRUE(holder.Add(obj1.Key, obj1, obj1.Weight));
    EXPECT_EQ(holder.size(), (size_t)1);
    EXPECT_TRUE(holder.Add(obj1.Key, obj1, obj1.Weight));
    EXPECT_EQ(holder.size(), (size_t)1);
    auto it = holder.begin();
    EXPECT_EQ(it->first, obj1.Weight);
    EXPECT_EQ(it->second.Key, obj1.Key);
    EXPECT_EQ(it->second.Value, obj1);
}

TEST(TKVTopHolderSuite, UnconstrainedRemove) {
    TTestTopHolder holder(1);
    TSimpleObject obj1{
        .Key = 0,
        .SubKey1 = 0,
        .Value = 42,
        .Weight = 1,
    };
    EXPECT_TRUE(holder.Add(obj1.Key, obj1, obj1.Weight));
    EXPECT_EQ(holder.size(), (size_t)1);
    EXPECT_FALSE(holder.Remove(1));
    EXPECT_EQ(holder.size(), (size_t)1);
    EXPECT_TRUE(holder.Remove(obj1.Key));
    EXPECT_EQ(holder.size(), (size_t)0);
    EXPECT_FALSE(holder.Remove(obj1.Key));
}

TEST(TKVTopHolderSuite, UnconstrainedUniqueKeys) {
    TVector<TSimpleObject> objects;
    for (size_t i = 0; i < 10; i++) {
        objects.push_back(TSimpleObject{
            .Key = (ui16)i,
            .SubKey1 = 0,
            .Value = i*2,
            .Weight = (ui32)i,
        });
    }
    Shuffle(objects.begin(), objects.end());
    TTestTopHolder holder(5);
    for (const auto& obj : objects) {
        holder.Add(obj.Key, obj, obj.Weight);
    }
    EXPECT_EQ(holder.size(), (size_t)5);
    Sort(objects, [](const TSimpleObject& lhs, const TSimpleObject& rhs){
        return lhs.Weight > rhs.Weight;
    });
    for (const auto [from_holder, from_array] : Zip(holder, objects)) {
        EXPECT_EQ(from_holder.first, from_array.Weight);
        EXPECT_EQ(from_holder.second.Key, from_array.Key);
        EXPECT_EQ(from_holder.second.Value, from_array);
    }
}

TEST(TKVTopHolderSuite, UnconstrainedDuplicateKeysSameWeight) {
    TVector<TSimpleObject> objects;
    for (size_t i = 0; i < 10; i++) {
        objects.push_back(TSimpleObject{
            .Key = (ui16)i,
            .SubKey1 = 0,
            .Value = i*2,
            .Weight = (ui32)i,
        });
    }
    Shuffle(objects.begin(), objects.end());
    TTestTopHolder holder(5);
    for (const auto& obj : objects) {
        TSimpleObject tmpObj = obj;
        tmpObj.Value +=1;
        // object is expected to be rewritten the second time, or must fall out both times
        EXPECT_EQ(holder.Add(tmpObj.Key, tmpObj, tmpObj.Weight),
                holder.Add(obj.Key, obj, obj.Weight));
    }
    EXPECT_EQ(holder.size(), (size_t)5);
    Sort(objects, [](const TSimpleObject& lhs, const TSimpleObject& rhs){
        return lhs.Weight > rhs.Weight;
    });
    for (const auto [from_holder, from_array] : Zip(holder, objects)) {
        EXPECT_EQ(from_holder.first, from_array.Weight);
        EXPECT_EQ(from_holder.second.Key, from_array.Key);
        EXPECT_EQ(from_holder.second.Value, from_array);
    }
}

TEST(TKVTopHolderSuite, ConstrainedOneKey) {
    TTestConstrainedTopHolder holder(1, 1);
    TSimpleObject obj1{
        .Key = 0,
        .SubKey1 = 0,
        .Value = 42,
        .Weight = 1,
    };
    EXPECT_TRUE(holder.Add(obj1.Key, obj1, obj1.Weight));
    EXPECT_EQ(holder.size(), (size_t)1);
    EXPECT_TRUE(holder.Add(obj1.Key, obj1, obj1.Weight));
    EXPECT_EQ(holder.size(), (size_t)1);
    auto it = holder.begin();
    EXPECT_EQ(it->first, obj1.Weight);
    EXPECT_EQ(it->second.Key, obj1.Key);
    EXPECT_EQ(it->second.Value, obj1);
}

TEST(TKVTopHolderSuite, ConstrainedUniqueKeysUniqueSubKeys) {
    TVector<TSimpleObject> objects;
    for (size_t i = 0; i < 10; i++) {
        objects.push_back(TSimpleObject{
            .Key = (ui16)i,
            .SubKey1 = (ui8)i,
            .Value = i*2,
            .Weight = (ui32)i,
        });
    }
    Shuffle(objects.begin(), objects.end());
    TTestConstrainedTopHolder holder(5, 2);
    for (const auto& obj : objects) {
        holder.Add(obj.Key, obj, obj.Weight);
    }
    EXPECT_EQ(holder.size(), (size_t)5);
    Sort(objects, [](const TSimpleObject& lhs, const TSimpleObject& rhs){
        return lhs.Weight > rhs.Weight;
    });
    for (const auto [from_holder, from_array] : Zip(holder, objects)) {
        EXPECT_EQ(from_holder.first, from_array.Weight);
        EXPECT_EQ(from_holder.second.Key, from_array.Key);
        EXPECT_EQ(from_holder.second.Value, from_array);
    }
}

TEST(TKVTopHolderSuite, ConstrainedUniqueKeysOneSubKey) {
    TVector<TSimpleObject> objects;
    for (size_t i = 0; i < 10; i++) {
        objects.push_back(TSimpleObject{
            .Key = (ui16)i,
            .SubKey1 = (ui8)0,
            .Value = i*2,
            .Weight = (ui32)i,
        });
    }
    Shuffle(objects.begin(), objects.end());
    TTestConstrainedTopHolder holder(5, 2);
    for (const auto& obj : objects) {
        holder.Add(obj.Key, obj, obj.Weight);
    }
    EXPECT_EQ(holder.size(), (size_t)2);
    Sort(objects, [](const TSimpleObject& lhs, const TSimpleObject& rhs){
        return lhs.Weight > rhs.Weight;
    });
    for (const auto [from_holder, from_array] : Zip(holder, objects)) {
        EXPECT_EQ(from_holder.first, from_array.Weight);
        EXPECT_EQ(from_holder.second.Key, from_array.Key);
        EXPECT_EQ(from_holder.second.Value, from_array);
    }
}

TEST(TKVTopHolderSuite, ConstrainedUniqueKeysTwoSubKeys) {
    TVector<TSimpleObject> objects;
    for (size_t i = 0; i < 10; i++) {
        objects.push_back(TSimpleObject{
            .Key = (ui16)i,
            .SubKey1 = (ui8)(i%2),
            .Value = i*2,
            .Weight = (ui32)i,
        });
    }
    Shuffle(objects.begin(), objects.end());
    TTestConstrainedTopHolder holder(5, 2);
    for (const auto& obj : objects) {
        holder.Add(obj.Key, obj, obj.Weight);
    }
    EXPECT_EQ(holder.size(), (size_t)4);
    Sort(objects, [](const TSimpleObject& lhs, const TSimpleObject& rhs){
        return lhs.Weight > rhs.Weight;
    });
    for (const auto [from_holder, from_array] : Zip(holder, objects)) {
        EXPECT_EQ(from_holder.first, from_array.Weight);
        EXPECT_EQ(from_holder.second.Key, from_array.Key);
        EXPECT_EQ(from_holder.second.Value, from_array);
    }
}
TEST(TKVTopHolderSuite, UnconstrainedSameKeyAddResults) {
    TSimpleObject o{
        .Key = 0,
        .SubKey1 = 0,
        .Value = 0,
        .Weight = 50,
    };
    TTestTopHolder holder(10);
    for (size_t i = 0; i < 10; i++) {
        o.Value = i;
        o.Weight -=i;
        // same key must always replace previous value, no matter the weight
        EXPECT_TRUE(holder.Add(o.Key, o, o.Weight));
    }
    EXPECT_EQ(holder.size(), (size_t)1);
    EXPECT_EQ(holder.begin()->first, o.Weight);
    EXPECT_EQ(holder.begin()->second.Key, o.Key);
    EXPECT_EQ(holder.begin()->second.Value, o);
}

TEST(TKVTopHolderSuite, UnconstrainedUniqueKeysAddResults) {
    // holder is empty - must be true
    // expected top contents {o1}
    TSimpleObject o1{
        .Key = 0,
        .SubKey1 = 0,
        .Value = 0,
        .Weight = 9,
    };
    TTestTopHolder holder(2);
    EXPECT_TRUE(holder.Add(o1.Key, o1, o1.Weight));

    // holder is not full - must be true
    // expected top contents {o2, o1}
    TSimpleObject o2{
        .Key = 1,
        .SubKey1 = 0,
        .Value = 1,
        .Weight = 10,
    };
    EXPECT_TRUE(holder.Add(o2.Key, o2, o2.Weight));

    // holder is full and weight is lower - must be false
    // expected top contents {o2, o1}
    TSimpleObject o3{
        .Key = 2,
        .SubKey1 = 0,
        .Value = 2,
        .Weight = 8,
    };
    EXPECT_FALSE(holder.Add(o3.Key, o3, o3.Weight));

    // holder is full and weight is higher - must be true
    // expected top contents {o4, o2} (o1 falls out)
    TSimpleObject o4{
        .Key = 3,
        .SubKey1 = 0,
        .Value = 3,
        .Weight = 11,
    };
    EXPECT_TRUE(holder.Add(o4.Key, o4, o4.Weight));
    EXPECT_EQ(holder.size(), (size_t)2);
    auto it = holder.begin();
    EXPECT_EQ(it->first, o4.Weight);
    EXPECT_EQ(it->second.Key, o4.Key);
    EXPECT_EQ(it->second.Value, o4);
    ++it;
    EXPECT_EQ(it->first, o2.Weight);
    EXPECT_EQ(it->second.Key, o2.Key);
    EXPECT_EQ(it->second.Value, o2);

}

TEST(TKVTopHolderSuite, ConstrainedSameSubKeyAddResults) {
    // holder is empty - must be true
    // expected top contents {o1}
    TSimpleObject o1{
        .Key = 0,
        .SubKey1 = 0,
        .Value = 0,
        .Weight = 9,
    };
    TTestConstrainedTopHolder holder(5, 2);
    EXPECT_TRUE(holder.Add(o1.Key, o1, o1.Weight));

    // holder is not full - must be true
    // expected top contents {o2, o1}
    TSimpleObject o2{
        .Key = 1,
        .SubKey1 = 0,
        .Value = 1,
        .Weight = 10,
    };
    EXPECT_TRUE(holder.Add(o2.Key, o2, o2.Weight));

    // holder is full and weight is lower - must be false
    // expected top contents {o2, o1}
    TSimpleObject o3{
        .Key = 2,
        .SubKey1 = 0,
        .Value = 2,
        .Weight = 8,
    };
    EXPECT_FALSE(holder.Add(o3.Key, o3, o3.Weight));

    // holder is full and weight is higher - must be true
    // expected top contents {o4, o2} (o1 falls out)
    TSimpleObject o4{
        .Key = 3,
        .SubKey1 = 0,
        .Value = 3,
        .Weight = 11,
    };
    EXPECT_TRUE(holder.Add(o4.Key, o4, o4.Weight));
    EXPECT_EQ(holder.size(), (size_t)2);
    auto it = holder.begin();
    EXPECT_EQ(it->first, o4.Weight);
    EXPECT_EQ(it->second.Key, o4.Key);
    EXPECT_EQ(it->second.Value, o4);
    ++it;
    EXPECT_EQ(it->first, o2.Weight);
    EXPECT_EQ(it->second.Key, o2.Key);
    EXPECT_EQ(it->second.Value, o2);

}
}
