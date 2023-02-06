#include <yplatform/zerocopy/iterator.h>

#include <gtest.h>

#include <list>

namespace {

using namespace testing;
using namespace yplatform::zerocopy;

struct IteratorTest : public Test
{
    typedef detail::fragment Fragment;
    typedef detail::basic_raii_fragment<std::allocator<Fragment::byte_t>> FragmentInstance;
    typedef std::shared_ptr<Fragment> FragmentPtr;
    typedef std::list<FragmentPtr> FragmentList;
    typedef iterator<Fragment::byte_t, Fragment, FragmentList> Iterator;

    FragmentList fragments;

    enum
    {
        fragmentSize = 16,
        fragmentsCount = 3,
    };

    FragmentPtr createFragment() const
    {
        return FragmentPtr(new FragmentInstance(fragmentSize));
    }

    void appendFragment()
    {
        fragments.push_back(createFragment());
    }

    IteratorTest()
    {
        for (int i = 0; i != fragmentsCount; ++i)
        {
            appendFragment();
        }
    }

    FragmentList::iterator at(std::size_t fragmentNo)
    {
        FragmentList::iterator i(fragments.begin());
        while (fragmentNo--)
        {
            ++i;
        }
        return i;
    }

    Fragment::iterator begin(std::size_t fragmentNo)
    {
        return (*at(fragmentNo))->begin();
    }

    Fragment::iterator end(std::size_t fragmentNo)
    {
        return (*at(fragmentNo))->end();
    }
};

TEST_F(IteratorTest, constructor_withFirstFragmentBeginPosition_setsPositionToFirstFragmentBegin)
{
    Iterator i(fragments, begin(0), fragments.begin());
    EXPECT_EQ(begin(0), &(*i));
}

TEST_F(IteratorTest, constructor_withEmptyFragment_constructsIterator)
{
    FragmentList f;
    EXPECT_NO_THROW(Iterator(f, 0, FragmentList::iterator{}));
}

TEST_F(IteratorTest, equal_twoIteratorsWithEmptyFragment)
{
    FragmentList f;
    EXPECT_EQ(Iterator(f, 0, FragmentList::iterator{}), Iterator(f, 0, FragmentList::iterator{}));
}

TEST_F(IteratorTest, constructor_withLastFragmentEndPosition_setsPositionToLastFragmentEnd)
{
    Iterator i(fragments, end(fragmentsCount - 1), at(fragmentsCount - 1));
    EXPECT_EQ(end(fragmentsCount - 1), &(*i));
}

TEST_F(IteratorTest, increment_withinFragment_setsPositionToIncrementedFragmentPosition)
{
    Fragment::iterator fragmentIter = begin(0);
    Iterator i(fragments, fragmentIter, at(0));
    EXPECT_EQ(++fragmentIter, &(*++i));
}

TEST_F(IteratorTest, increment_onFragmentBoundary_setsPositionToNextFragmentBegin)
{
    Iterator i(fragments, end(0) - 1, at(0));
    EXPECT_EQ(begin(1), &(*++i));
}

TEST_F(IteratorTest, decrement_withinFragment_setsPositionToDecrementedOneOfFragment)
{
    Fragment::iterator fragmentIter = begin(0) + 1;
    Iterator i(fragments, fragmentIter, at(0));
    EXPECT_EQ(--fragmentIter, &(*--i));
}

TEST_F(IteratorTest, decrement_withEndOfFragment_setsPositionToLastOfFragment)
{
    Fragment::iterator fragmentIter = end(0);
    Iterator i(fragments, fragmentIter, at(0));
    EXPECT_EQ(--fragmentIter, &(*--i));
}

TEST_F(IteratorTest, decrement_onFragmentBoundary_setsPositionToPreviousFragmentLast)
{
    Iterator i(fragments, begin(1), at(1));
    EXPECT_EQ(end(0) - 1, &(*--i));
}

TEST_F(IteratorTest, advance_withinFragmentSize_setsPositionToAdvancedFragmentPosition)
{
    Fragment::iterator fragmentIter = begin(0);
    Iterator i(fragments, fragmentIter, at(0));
    const Iterator::difference_type adv = fragmentSize / 2;
    EXPECT_EQ(fragmentIter + adv, &(*(i + adv)));
}

TEST_F(IteratorTest, advance_moreThanFragmentSize_setsPositionToNextFragment)
{
    const Iterator::difference_type adv = fragmentSize + 2;
    Iterator i(fragments, begin(0), at(0));
    EXPECT_EQ(begin(1) + 2, &(*(i + adv)));
}

TEST_F(IteratorTest, advance_moreThanAllFragmentSize_setsPositionToLastFragmentEnd)
{
    const Iterator::difference_type adv = fragmentSize * fragmentsCount + 2;
    Iterator i(fragments, begin(0), at(0));
    EXPECT_EQ(end(fragmentsCount - 1), &(*(i + adv)));
}

TEST_F(IteratorTest, advance_negativeWithinFragmentSize_setsPositionToAdvancedFragmentPosition)
{
    Fragment::iterator fragmentIter = end(0) - 1;
    Iterator i(fragments, fragmentIter, at(0));
    const Iterator::difference_type adv = -fragmentSize / 2;
    EXPECT_EQ(fragmentIter + adv, &(*(i + adv)));
}

TEST_F(IteratorTest, advance_negativeOneFromFragmentEnd_setsPositionFragmentLast)
{
    Fragment::iterator fragmentIter = end(0);
    Iterator i(fragments, fragmentIter, at(0));
    EXPECT_EQ(fragmentIter - 1, &(*(i - 1)));
}

TEST_F(IteratorTest, advance_negativeMoreThanFragmentSize_setsPositionToPreviousFragment)
{
    const Iterator::difference_type adv = -(fragmentSize + 2);
    Iterator i(fragments, begin(1) + 2, at(1));
    EXPECT_EQ(begin(0), &(*(i + adv)));
}

TEST_F(IteratorTest, equal_withSameIterators_returnsTrue)
{
    EXPECT_TRUE(Iterator(fragments, begin(1), at(1)) == Iterator(fragments, begin(1), at(1)));
}

TEST_F(IteratorTest, equal_withDifferentIterators_returnsFalse)
{
    EXPECT_FALSE(Iterator(fragments, begin(0), at(0)) == Iterator(fragments, begin(1), at(1)));
}

TEST_F(IteratorTest, distance_withSameIterators_returnsZero)
{
    Iterator first(fragments, begin(1), at(1));
    Iterator last(fragments, begin(1), at(1));
    EXPECT_EQ(last - first, 0);
}

TEST_F(IteratorTest, distance_withIteratorsDistance_returnsDistance)
{
    Iterator first(fragments, begin(0), at(0));
    Iterator last(fragments, begin(1), at(1));
    EXPECT_EQ(last - first, fragmentSize);
}

TEST_F(IteratorTest, reverseDistance_withIteratorsDistance_returnsNegativeDistance)
{
    Iterator first(fragments, begin(0), at(0));
    Iterator last(fragments, begin(1), at(1));
    EXPECT_EQ(first - last, -fragmentSize);
}

TEST_F(IteratorTest, distance_withFirstLastIteratorsDistance_returnsDistance)
{
    Iterator first(fragments, begin(0), at(0));
    Iterator last(fragments, end(fragmentsCount - 1), at(fragmentsCount - 1));
    EXPECT_EQ(last - first, fragmentsCount * fragmentSize);
}

TEST_F(IteratorTest, reverseDistance_withFirstLastIteratorsDistance_returnsNegativeDistance)
{
    Iterator first(fragments, begin(0), at(0));
    Iterator last(fragments, end(fragmentsCount - 1), at(fragmentsCount - 1));
    EXPECT_EQ(first - last, -fragmentsCount * fragmentSize);
}

TEST_F(
    IteratorTest,
    dereference_withLastFragmentEndPositionAfterAppendAnotherFragment_returnsFirstValueOfTheFragment)
{
    Iterator i(fragments, end(1), at(1));
    appendFragment();
    EXPECT_EQ(begin(2), &(*i));
}

TEST_F(IteratorTest, iteratorEqualityForSpecificAllocationCase)
{
    FragmentList list;

    const FragmentPtr f1(new Fragment(reinterpret_cast<Fragment::byte_t*>(3), 1));
    const FragmentPtr f2(new Fragment(reinterpret_cast<Fragment::byte_t*>(2), 1));
    const FragmentPtr f3(new Fragment(reinterpret_cast<Fragment::byte_t*>(1), 1));

    list.push_back(f1);
    list.push_back(f2);

    const Iterator f2_end(list, f2->end(), --list.end());

    list.push_back(f3);

    const Iterator f3_begin(list, f3->begin(), --list.end());

    EXPECT_TRUE(f2_end == f3_begin);
}

} // namespace
