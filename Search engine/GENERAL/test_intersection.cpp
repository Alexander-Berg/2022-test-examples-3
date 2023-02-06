#include <library/cpp/testing/unittest/registar.h>
#include <search/scraper_over_yt/daemons/cache_pool_owner/lib/cache_pool_owner.h>

namespace NScraperOverYTTest {

    Y_UNIT_TEST_SUITE(IntersectionOfVectors) {
        Y_UNIT_TEST(TestIntersectionOfVectorsWithOutIntersections) {
            TVector<TString> v1 = {"hello", "darkness", "my", "old", "friend"};
            TVector<TString> v2 = {"instead", "of", "testing", "methods", "directly"};
            TVector<TString> intersections;
            NScraperOverYT::TCachePoolOwner::IntersectionOfVectors(v1, v2, intersections);
            UNIT_ASSERT_VALUES_EQUAL(intersections.size(), 0);
        }

        Y_UNIT_TEST(TestIntersectionOfVectorsWithOneIntersection) {
            TVector<TString> v1 = {"hello", "darkness", "my", "old", "friend"};
            TVector<TString> v2 = {"instead", "of", "testing", "methods", "directly", "friend"};
            TVector<TString> intersections;
            NScraperOverYT::TCachePoolOwner::IntersectionOfVectors(v1, v2, intersections);
            UNIT_ASSERT_VALUES_EQUAL(intersections.front(), "friend");
        }

        Y_UNIT_TEST(TestIntersectionOfVectorsOneIntersectionsInSecond) {
            TVector<TString> v1 = {"hello", "darkness", "my", "old", "friend"};
            TVector<TString> v2 = {"hello", "darkness", "my", "old", "friend", "instead", "of", "testing", "methods", "directly"};
            TVector<TString> intersections;
            NScraperOverYT::TCachePoolOwner::IntersectionOfVectors(v1, v2, intersections);
            int count = 0;
            for (auto& a : v1) {
                for (auto& b : intersections) {
                    if (a == b) {
                        count++;
                    }
                }
            }
            UNIT_ASSERT_VALUES_EQUAL(v1.size(), count);
        }

        Y_UNIT_TEST(TestIntersectionOfOneEmptyVector) {
            TVector<TString> v1;
            TVector<TString> v2 = {"instead", "of", "testing", "methods", "directly", "friend"};
            TVector<TString> intersections;
            NScraperOverYT::TCachePoolOwner::IntersectionOfVectors(v1, v2, intersections);
            UNIT_ASSERT_VALUES_EQUAL(intersections.size(), 0);
        }

        Y_UNIT_TEST(TestIntersectionOfBothEmptyVector) {
            TVector<TString> v1;
            TVector<TString> v2;
            TVector<TString> intersections;
            NScraperOverYT::TCachePoolOwner::IntersectionOfVectors(v1, v2, intersections);
            UNIT_ASSERT_VALUES_EQUAL(intersections.size(), 0);
        }
    }
}