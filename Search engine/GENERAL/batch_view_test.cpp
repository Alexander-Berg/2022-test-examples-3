#include "batch_view.h"
#include "ut/common.h"

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <yt/yt/core/logging/log.h>

#include <util/generic/fwd.h>
#include <util/generic/list.h>
#include <util/generic/scope.h>

namespace NPlutonium::NProcessors::NUT {

namespace {

static NYT::NLogging::TLogger Logger{"batch_view_test"};

}

TEST(BatchView, CollectoToVector) {
    TABatchView aView(0, 3);
    auto result = CollectToVector<TA>(std::move(aView));
    
    ASSERT_THAT(result, testing::ElementsAre(TA{0}, TA{1}, TA{2}));
}

TEST(BatchView, CollectoToExternalVector) {
    TABatchView aView(0, 3);
    TVector<TA> result;
    CollectToVector<TA>(std::move(aView), result);
    
    ASSERT_THAT(result, testing::ElementsAre(TA{0}, TA{1}, TA{2}));
}

TEST(BatchView, TransformUniqNotCopyable) {
    TABatchViewNoCopy aView(0, 3);
    auto transformedView = Transform<TANoCopy>(
        std::move(aView), 
        [](TTransformMapperArg<decltype(aView)> a) {
            return TA{a.X + 1};
        }
    );
    
    ASSERT_THAT(transformedView.Next().X, testing::Eq(1u));
    ASSERT_THAT(transformedView.Next().X, testing::Eq(2u));
    ASSERT_THAT(transformedView.Next().X, testing::Eq(3u));
    ASSERT_THAT(transformedView.HasNext(), testing::IsFalse());
}
TEST(BatchView, TransformAndFilterUniqNotCopyable) {
    TABatchViewNoCopy aView(0, 3);
    auto transformedView = Transform<TANoCopy>(
        std::move(aView), 
        [](TTransformMapperArg<decltype(aView)> a) {
            return TA{a.X + 1};
        },
        [](TTransformFilterArg<decltype(aView)> a) {
            return a.X > 1;
        }
    );
    
    ASSERT_THAT(transformedView.Next().X, testing::Eq(3u));
    ASSERT_THAT(transformedView.HasNext(), testing::IsFalse());
}

TEST(BatchView, TransformSharedNotCopyable) {
    TVector<TANoCopy> vec(Reserve(3));
    vec.emplace_back(0);
    vec.emplace_back(1);
    vec.emplace_back(2);
    TVectorBatchView<TANoCopy> vecView(std::move(vec));
    auto transformedView = Transform<TANoCopy>(
        vecView, 
        [](TTransformMapperArg<decltype(vecView)> a) {
            return TA{a.X + 1};
        }
    );
    ASSERT_THAT(transformedView.Next().X, testing::Eq(1u));
    ASSERT_THAT(transformedView.Next().X, testing::Eq(2u));
    ASSERT_THAT(transformedView.Next().X, testing::Eq(3u));
    ASSERT_THAT(transformedView.HasNext(), testing::IsFalse());
}

TEST(BatchView, TransformAndFilterSharedNotCopyable) {
    TVector<TANoCopy> vec(Reserve(3));
    vec.emplace_back(0);
    vec.emplace_back(1);
    vec.emplace_back(2);
    TVectorBatchView<TANoCopy> vecView(std::move(vec));
    auto transformedView = Transform<TANoCopy>(
        std::move(vecView), 
        [](TTransformMapperArg<decltype(vecView)> a) {
            return TA{a.X + 1};
        },
        [](TTransformFilterArg<decltype(vecView)> a) {
            return a.X > 1;
        }
    );

    ASSERT_THAT(transformedView.Next().X, testing::Eq(3u));
    ASSERT_THAT(transformedView.HasNext(), testing::IsFalse());
}

TEST(BatchView, ConcatUniqNotCopyableViews) {
    TList<TABatchViewNoCopy> views;
    views.emplace_back(0, 2);
    views.emplace_back(2, 4);

    auto concated = ConcatViews<TANoCopy>(std::move(views));

    ASSERT_THAT(concated.Next().X, testing::Eq(0u));
    ASSERT_THAT(concated.Next().X, testing::Eq(1u));
    ASSERT_THAT(concated.Next().X, testing::Eq(2u));
    ASSERT_THAT(concated.Next().X, testing::Eq(3u));
    ASSERT_THAT(concated.HasNext(), testing::IsFalse());
}

}