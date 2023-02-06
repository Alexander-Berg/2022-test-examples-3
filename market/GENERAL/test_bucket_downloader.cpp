#include <market/idx/datacamp/miner/processors/delivery_calc_enricher/bucket_downloader.h>

#include <library/cpp/testing/unittest/gtest.h>


TEST (TestBucketsCacheValue, SerializeDeserialize)
{
    using namespace NMiner;

    {
        TBucketsCacheValue value{123, Nothing()};
        TString serializedValue = SerializeBucketsCacheValue(value);
        TBucketsCacheValue deserializedValue = DeserializeBucketsCacheValue(serializedValue);

        ASSERT_EQ(value, deserializedValue);
    }
    {
        TBucketsCacheValue value{1234, 1};
        TString serializedValue = SerializeBucketsCacheValue(value);
        TBucketsCacheValue deserializedValue = DeserializeBucketsCacheValue(serializedValue);

        ASSERT_EQ(value, deserializedValue);
    }
    {
        TBucketsCacheValue value{1235, 0};
        TString serializedValue = SerializeBucketsCacheValue(value);
        TBucketsCacheValue deserializedValue = DeserializeBucketsCacheValue(serializedValue);

        ASSERT_EQ(value, deserializedValue);
    }
}
