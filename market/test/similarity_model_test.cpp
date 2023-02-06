#include <market/report/library/relevance/place/recom/analogs/similarity_model.h>
#include <market/library/iterator/output_stream_joiner.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <array>

using namespace testing;
using namespace  NewRel;

static const double EPSILON = 0.0001;

namespace {

struct TSimpleProp: public std::pair<uint64_t, double> {
    using TBase = std::pair<uint64_t, double>;
    using TBase::TBase;
};

uint64_t Id(const TSimpleProp& x) {
    return x.first;
}

double Value(const TSimpleProp& x) {
    return x.second;
}

EMatchType Match(TSimpleProp, TSimpleProp) {
    return EMatchType::MT_NUMERIC;
}

using TSimpleModel = TSimilarityModel<TSimpleProp, TSimpleProp>;

TSimpleModel::TSimilarityDocument GetTrivialDocument(double v) {
    return [v](uint64_t i) -> TSimpleProp { return TSimpleProp(i, v); };
}

TSimpleModel::TSimilarityDocument GetVectorDocument(TVector<double> v) {
    return [v](uint64_t i) -> TSimpleProp { return TSimpleProp(i, v.at(i)); };
}

} // anonymous namespace

TEST(SimilarityModel, Trivial) {
    TSimpleModel m;
    double d = 0.0;
    ASSERT_NO_THROW(d = m.CalculateDistance(GetTrivialDocument(0.0)));
    ASSERT_EQ(0.0, d);
}

TEST(SimilarityModel, Simple) {
    TSimpleModel::TPropInfo p(0.0, 1.0, 1.0, TSimpleProp(1, 1.0));
    TSimpleModel m({p});
    EXPECT_NEAR(0.0, m.CalculateDistance(GetTrivialDocument(1.0)), EPSILON);
}

using TTestDocumentData = TVector<double>;
using TTestArrangedList = TVector<TTestDocumentData>;

void RunModelTest(const TSimpleModel& m, const TTestArrangedList& test) {
    double prevDist = -1.0;
    for (const auto& data : test) {
        const double curDist = m.CalculateDistance(GetVectorDocument(data));
        // assert ordering
        ASSERT_TRUE(curDist >= prevDist);
        prevDist = curDist;
    }
}

TEST(SimilarityModel, NumericSimple) {
    // define test data
    TVector<TSimpleModel::TPropInfo> props;
    props.push_back(TSimpleModel::TPropInfo(0.0, 1.0, 1.0, TSimpleProp(0, 0.0)));
    props.push_back(TSimpleModel::TPropInfo(0.0, 1.0, 1.0, TSimpleProp(1, 1.0)));
    const TSimpleModel m(props);
    const TTestArrangedList testData = {
        {1.0, 1.0},
        {1.0, 0.5},
        {1.0, 0.0},
    };
    RunModelTest(m, testData);
}

TEST(SimilarityModel, Importance) {
    // define test data
    TVector<TSimpleModel::TPropInfo> props;
    props.push_back(TSimpleModel::TPropInfo(0.0, 10.0, 0.1, TSimpleProp(0, 0.0)));
    props.push_back(TSimpleModel::TPropInfo(0.0, 10.0, 0.9, TSimpleProp(1, 0.0)));
    const TSimpleModel m(props);

    const TTestArrangedList testData = {
        {10.0, 2.0},
        {4.0, 4.0},
        {2.0, 10.0},
    };
    RunModelTest(m, testData);
}
