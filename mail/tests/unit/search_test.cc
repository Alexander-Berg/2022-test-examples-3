#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/spaniel/service/include/search.h>


using namespace ::testing;

namespace spaniel::tests {

const SearchResult receivedDate1{.uid=Uid(10), .id=Id(10), .received_date=1 };
const SearchResult receivedDate2{.uid=Uid(10), .id=Id(10), .received_date=2 };
const SearchResult receivedDate3{.uid=Uid(10), .id=Id(10), .received_date=3 };

struct RequestResultsMock {
    MOCK_METHOD(void, request, (const MailSearchCommonParams&, SearchResults&), (const));
};

MATCHER_P2(WithLengthAndDate, date, length, "length or date are not equal to expected") {
    *result_listener << "actual: " << arg.length;
    return arg.length == length && arg.dateTo == date && arg.dateFrom == date;
}

MATCHER(EmptyArray, "array is not empty") {
    *result_listener << "actual: " << arg.size();
    return arg.empty();
}

TEST(ComparatorTest, shouldCompareByReceivedDate) {
    EXPECT_TRUE(detail::comparator(
        SearchResult {.received_date=2},
        SearchResult {.received_date=1}
    ));
}

TEST(ComparatorTest, shouldCompareByIdInCaseOfEqualsReceivedDate) {
    EXPECT_TRUE(detail::comparator(
        SearchResult {.id=Id(2), .received_date=1},
        SearchResult {.id=Id(1), .received_date=1}
    ));
}

TEST(ComparatorTest, shouldCompareByUidInCaseOfEqualsIdAndReceivedDate) {
    EXPECT_TRUE(detail::comparator(
        SearchResult {.uid=Uid(2), .id=Id(1), .received_date=1 },
        SearchResult {.uid=Uid(1), .id=Id(1), .received_date=1 }
    ));
}

TEST(ComparatorTest, shouldReturnFalseInCaseOfEqualsResults) {
    EXPECT_FALSE(detail::comparator(
        SearchResult {.uid=Uid(1), .id=Id(1), .received_date=1},
        SearchResult {.uid=Uid(1), .id=Id(1), .received_date=1}
    ));
}

struct ExtractNextFromSequenceTest: public Test {
    const SearchResult receivedDate1{.received_date=1};
    const SearchResult receivedDate2{.received_date=2};
    SearchResults sequence;

    void SetUp() override {
        sequence.clear();
        sequence.push_back(receivedDate1);
    }

    void withTail(unsigned size) {
        std::fill_n(std::back_inserter(sequence), size, receivedDate1);
    }
};

TEST_F(ExtractNextFromSequenceTest, shouldReturnDateInCaseOfTailWithUnknownLengthAndSameReceivedDates) {
    withTail(detail::ADDITIONAL_SEARCH_RESULTS);

    EXPECT_EQ(detail::extractNextFromSequence(sequence, 1), 1);
}

TEST_F(ExtractNextFromSequenceTest, shouldReturnNullInCaseOfShortTail) {
    withTail(detail::ADDITIONAL_SEARCH_RESULTS - 1);

    EXPECT_EQ(detail::extractNextFromSequence(sequence, 1), std::nullopt);
}

TEST_F(ExtractNextFromSequenceTest, shouldReturnNullInCaseOfDifferentReceivedDates) {
    withTail(detail::ADDITIONAL_SEARCH_RESULTS - 1);
    sequence.push_back(receivedDate2);

    EXPECT_EQ(detail::extractNextFromSequence(sequence, 1), std::nullopt);
}

TEST_F(ExtractNextFromSequenceTest, shouldReturnNullInCaseOfEmptyTail) {
    EXPECT_EQ(detail::extractNextFromSequence(sequence, 1), std::nullopt);
}

TEST(RequestFullPageOfResultsTest, shouldRequestUntilPageSizeIsGreaterThanResultsSize) {
    const auto mock = std::make_shared<StrictMock<RequestResultsMock>>();

    const InSequence s;

    const std::time_t date = 1;

    EXPECT_CALL(*mock, request(
        WithLengthAndDate(date, detail::PAGE_LENGTH_FOR_LOADING_RESULTS),
        EmptyArray()
    )).WillOnce(Invoke([] (const auto& p, auto& r) { r.resize(p.length); }));

    EXPECT_CALL(*mock, request(
        WithLengthAndDate(date, 2 * detail::PAGE_LENGTH_FOR_LOADING_RESULTS),
        EmptyArray()
    )).WillOnce(Invoke([] (const auto& p, auto& r) { r.resize(p.length); }));

    EXPECT_CALL(*mock, request(
        WithLengthAndDate(date, 3 * detail::PAGE_LENGTH_FOR_LOADING_RESULTS),
        EmptyArray()
    )).WillOnce(Invoke([] (const auto& p, auto& r) { r.resize(p.length - 1); }));

    detail::requestFullPageOfResults(date, [&] (const auto& p, auto& r) { mock->request(p, r); });
}

struct SearchTest: public Test {
    std::shared_ptr<RequestResultsMock> mock;
    detail::RequestResults req;
    MailSearchCommonParams params;
    SearchResults unordered;
    SearchResults ordered;

    void SetUp() override {
        mock = std::make_shared<StrictMock<RequestResultsMock>>();
        req = [this] (const auto& p, auto& r) { this->mock->request(p, r); };
        params = { .dateFrom=0, .dateTo=3, .length = 3 };

        unordered = {receivedDate1, receivedDate3, receivedDate2};
        ordered   = {receivedDate3, receivedDate2, receivedDate1};
    }
};

TEST_F(SearchTest, shouldSortResults) {
    EXPECT_CALL(*mock, request(_, _))
        .WillOnce(Invoke([&] (const auto&, auto& r) { r = unordered; }));

    EXPECT_EQ(detail::search(params, req), ordered);
}

TEST_F(SearchTest, shouldAppendAndSortAllFromPageOfRequestedResults) {
    const SearchResult receivedDate1WithId1 = {.uid=Uid(10), .id=Id(1), .received_date=1};
    const SearchResult receivedDate1WithId2 = {.uid=Uid(10), .id=Id(2), .received_date=1};

    const SearchResults additional = {receivedDate1WithId2};
    const SearchResults tail(detail::ADDITIONAL_SEARCH_RESULTS, receivedDate1WithId1);

    std::copy(tail.begin(), tail.end(), std::back_inserter(unordered));

    SearchResults expected = ordered;
    std::copy(additional.begin(), additional.end(), std::back_inserter(expected));
    std::copy(tail.begin(), tail.end(), std::back_inserter(expected));

    EXPECT_CALL(*mock, request(_, _))
        .WillOnce(Invoke([&] (const auto&, auto& r) { r = unordered; }))
        .WillOnce(Invoke([&] (const auto&, auto& r) { r = additional; }));

    EXPECT_EQ(detail::search(params, req), expected);
}

}
