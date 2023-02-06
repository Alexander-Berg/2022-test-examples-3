#include <crypta/lib/native/ip_filter/ip_filter.h>

#include <library/cpp/testing/unittest/registar.h>


Y_UNIT_TEST_SUITE(TIpFilter) {
    using namespace NCrypta;
    using TData = TVector<TString>;

    Y_UNIT_TEST(Empty) {
        const TVector<TString> empty{};
        TIpFilter filter(empty);

        UNIT_ASSERT(!filter.IsPassing("0.0.0.0"));
        UNIT_ASSERT(!filter.IsPassing("::"));
    }

    Y_UNIT_TEST(TIpFilter) {
        const TVector<TString> ipRanges = {
            "5.3.0.0 - 5.3.255.255",
            "5.16.0.0 - 5.19.255.255",
            "1.2.3.0 - 1.2.3.0",
        };
        const TVector<TString> cidrRanges = {
            "5.3.0.0/16",
            "5.16.0.0/14",
            "1.2.3.0/32",
        };
        const TVector<TString> mixedRanges = {
            "5.3.0.0 - 5.3.255.255",
            "5.16.0.0/14",
            "1.2.3.0 - 1.2.3.0",
        };

        for (const auto& ranges : {ipRanges, cidrRanges, mixedRanges}) {
            TIpFilter filter(ranges);

            UNIT_ASSERT(!filter.IsPassing("5.2.255.255"));
            UNIT_ASSERT(filter.IsPassing("5.3.0.0"));
            UNIT_ASSERT(filter.IsPassing("5.3.255.255"));
            UNIT_ASSERT(!filter.IsPassing("5.4.0.0"));

            UNIT_ASSERT(!filter.IsPassing("5.15.255.255"));
            UNIT_ASSERT(filter.IsPassing("5.16.0.0"));
            UNIT_ASSERT(filter.IsPassing("5.19.255.255"));
            UNIT_ASSERT(!filter.IsPassing("5.20.0.0"));

            UNIT_ASSERT(filter.IsPassing("1.2.3.0"));
            UNIT_ASSERT(!filter.IsPassing("1.2.3.1"));
            UNIT_ASSERT(!filter.IsPassing("1.2.2.255"));
        }
    }

    Y_UNIT_TEST(v6Filter) {
        const TVector<TString> ip6Ranges = {
            "5:3::0 - 5:3::ffff:ffff",
            "5.16.0.0 - 5.19.255.255",
            "1:2:3:4:5:6:7:8 - 1:2:3:4:5:6:7:8",
        };
        TIpFilter filter(ip6Ranges);

        UNIT_ASSERT(!filter.IsPassing("wrong"));

        UNIT_ASSERT(!filter.IsPassing("5:2:ffff:ffff:ffff:ffff:ffff:ffff"));
        UNIT_ASSERT(filter.IsPassing("5:3::"));
        UNIT_ASSERT(filter.IsPassing("5:3::ffff:ffff"));
        UNIT_ASSERT(!filter.IsPassing("5:4::"));

        UNIT_ASSERT(!filter.IsPassing("5.15.255.255"));
        UNIT_ASSERT(filter.IsPassing("5.16.0.0"));
        UNIT_ASSERT(filter.IsPassing("5.19.255.255"));
        UNIT_ASSERT(filter.IsPassing("::ffff:5.19.255.255"));
        UNIT_ASSERT(!filter.IsPassing("5.20.0.0"));

        UNIT_ASSERT(filter.IsPassing("1:2:3:4:5:6:7:8"));
        UNIT_ASSERT(!filter.IsPassing("1:2:3:4:5:6:7:9"));
        UNIT_ASSERT(!filter.IsPassing("1:2:3:4:5:6:7:7"));
    }

    Y_UNIT_TEST(Normalization) {
        const TVector<TString> ipRanges = {
            "::ffff:1.1.1.0 - ::ffff:1.1.1.255",
            "2.2.2.0 - 2.2.2.255",
        };
        TIpFilter filter(ipRanges);

        UNIT_ASSERT(filter.IsPassing("1.1.1.1"));
        UNIT_ASSERT(filter.IsPassing("::ffff:101:101"));
        UNIT_ASSERT(filter.IsPassing("::ffff:2.2.2.1"));
        UNIT_ASSERT(filter.IsPassing("::ffff:202:201"));
    }

    Y_UNIT_TEST(InvalidRange) {
        const TVector<TString> badRange = {
            "1.2.3.1 - 1.2.3.0",
        };
        const TVector<TString> mixedRange = {
            "1.2.3.1 - ffff::",
        };

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(badRange), TInvalidIpRangeException, "Invalid IP address range: from 1.2.3.1 to 1.2.3.0");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(mixedRange), TInvalidIpRangeException, "start address type is IPv4 end type is IPv6");
    }

    Y_UNIT_TEST(InvalidData) {
        const TVector<TString> badBegin = { "bad begin - 1.2.3.0", };
        const TVector<TString> badEnd = { "12::12 - bad end", };
        const TVector<TString> badBoth = { "bad both - bad both", };
        const TVector<TString> crasherBegin = { "::ffff:0.0.0.0 - 10.10.10.10", };
        const TVector<TString> crasherEnd = { "10.10.10.10 - ::ffff:0.0.0.0", };

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(badBegin), yexception, "Malformed ip range description: 'bad begin - 1.2.3.0'");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(badEnd), yexception, "bad end");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(badBoth), yexception, "bad both");
        UNIT_ASSERT(TIpFilter(crasherBegin).IsPassing("0.0.0.0"));
        UNIT_ASSERT(TIpFilter(crasherBegin).IsPassing("10.10.10.10"));
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(crasherEnd), TInvalidIpRangeException, "Invalid IP address range: from 10.10.10.10 to 0.0.0.0");
    }

    Y_UNIT_TEST(Crasher) {
        {
            TIpFilter filter(TData{{"5.3.0.0 - 5.3.255.255",}});
            UNIT_ASSERT(!filter.IsPassing("::ffff:0.0.0.0"));
        }

        {
            TIpFilter filter(TData{{"0.0.0.0 - 255.255.255.255",}});
            UNIT_ASSERT(filter.IsPassing("::ffff:0.0.0.0"));
        }
    }

    Y_UNIT_TEST(CornerCases) {
        const TIpFilter emptyFilter(TData{});
        UNIT_ASSERT(!emptyFilter.IsPassing("0.0.0.0"));

        const TIpFilter singleAddress(TData{"195.208.175.1 - 195.208.175.1"});
        UNIT_ASSERT(singleAddress.IsPassing("195.208.175.1"));
        UNIT_ASSERT(!singleAddress.IsPassing("195.208.175.0"));
        UNIT_ASSERT(!singleAddress.IsPassing("195.208.175.2"));

        const TIpFilter singleSubnet(TData{"4.2.2.0 - 4.2.2.255"});
        UNIT_ASSERT(!singleSubnet.IsPassing("4.2.1.255"));
        UNIT_ASSERT(singleSubnet.IsPassing("4.2.2.0"));
        UNIT_ASSERT(singleSubnet.IsPassing("4.2.2.255"));
        UNIT_ASSERT(!singleSubnet.IsPassing("4.2.3.0"));

        const TIpFilter doubleSubnet(TData{"8.8.8.0 - 8.8.8.255", "8.8.9.0 - 8.8.10.0"});
        UNIT_ASSERT(!doubleSubnet.IsPassing("8.8.7.255"));
        UNIT_ASSERT(doubleSubnet.IsPassing("8.8.8.0"));
        UNIT_ASSERT(doubleSubnet.IsPassing("8.8.8.255"));
        UNIT_ASSERT(doubleSubnet.IsPassing("8.8.9.0"));
        UNIT_ASSERT(doubleSubnet.IsPassing("8.8.10.0"));
        UNIT_ASSERT(!doubleSubnet.IsPassing("8.8.10.1"));

        const TIpFilter prickedOut(TData{"8.8.8.0 - 8.8.8.255", "8.8.9.1 - 8.8.10.0"});
        UNIT_ASSERT(!prickedOut.IsPassing("8.8.7.255"));
        UNIT_ASSERT(prickedOut.IsPassing("8.8.8.0"));
        UNIT_ASSERT(prickedOut.IsPassing("8.8.8.255"));
        UNIT_ASSERT(!prickedOut.IsPassing("8.8.9.0"));
        UNIT_ASSERT(prickedOut.IsPassing("8.8.9.1"));
        UNIT_ASSERT(prickedOut.IsPassing("8.8.10.0"));
        UNIT_ASSERT(!prickedOut.IsPassing("8.8.10.1"));
    }

    Y_UNIT_TEST(IntersectedRanges) {
        using TData = TVector<TString>;
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.2.0.0 - 1.10.0.0", "1.3.0.0 - 1.4.0.0"}), TIntersectedRangesException, "Range intersection between 1.2.0.0-1.10.0.0 and 1.3.0.0-1.4.0.0");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.3.0.0 - 1.4.0.0", "1.2.0.0 - 1.10.0.0"}), TIntersectedRangesException, "1.3.0.0-1.4.0.0");

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.2.3.4 - 5.6.7.8", "1.2.3.4 - 1.2.3.4"}), TIntersectedRangesException, "1.2.3.4-5.6.7.8");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.2.3.4 - 1.2.3.4", "1.2.3.4 - 5.6.7.8"}), TIntersectedRangesException, "1.2.3.4-1.2.3.4");

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.2.3.4 - 5.6.7.8", "2.3.4.5 - 6.7.8.9"}), TIntersectedRangesException, "1.2.3.4-5.6.7.8");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"2.3.4.5 - 6.7.8.9", "1.2.3.4 - 5.6.7.8"}), TIntersectedRangesException, "2.3.4.5-6.7.8.9");

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"2.3.4.5 - 6.7.8.9", "6.7.8.9 - 6.7.8.9"}), TIntersectedRangesException, "2.3.4.5-6.7.8.9");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"6.7.8.9 - 6.7.8.9", "2.3.4.5 - 6.7.8.9"}), TIntersectedRangesException, "6.7.8.9-6.7.8.9");

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"2.3.4.5 - 6.7.8.9", "6.7.8.9 - 6.7.8.10"}), TIntersectedRangesException, "2.3.4.5-6.7.8.9");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"6.7.8.9 - 6.7.8.10", "2.3.4.5 - 6.7.8.9"}), TIntersectedRangesException, "6.7.8.9-6.7.8.10");

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.128.0.0/9", "1.131.0.0/16"}), TIntersectedRangesException, "1.128.0.0-1.255.255.255");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.131.0.0/16", "1.128.0.0/9"}), TIntersectedRangesException, "1.131.0.0-1.131.255.255");

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.3.4.0/24", "1.3.4.255/32"}), TIntersectedRangesException, "1.3.4.0-1.3.4.255");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.3.4.255/32", "1.3.4.0/24"}), TIntersectedRangesException, "1.3.4.255-1.3.4.255");

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.2.0.0/16", "1.2.0.0/17"}), TIntersectedRangesException, "1.2.0.0-1.2.127.255 ");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"1.2.0.0/17", "1.2.0.0/16"}), TIntersectedRangesException, "1.2.0.0-1.2.255.255");
    }

    Y_UNIT_TEST(FullRange) {
        const TIpFilter fullv4(TData{"0.0.0.0 - 255.255.255.255"});

        UNIT_ASSERT(fullv4.IsPassing("0.0.0.0"));
        UNIT_ASSERT(fullv4.IsPassing("255.255.255.255"));
        UNIT_ASSERT(fullv4.IsPassing("8.8.8.8"));

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"0.0.0.0/0", "1.2.3.4 - 255.255.255.255"}), TIntersectedRangesException, "1.2.3.4-255.255.255.255");
        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"0.0.0.0/0", "1.2.3.4 - 4.5.255.255"}), TIntersectedRangesException, "0.0.0.0-255.255.255.255");

        const TIpFilter fullv6(TData{"::/0"});
        UNIT_ASSERT(fullv6.IsPassing("::"));
        UNIT_ASSERT(fullv6.IsPassing("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
        UNIT_ASSERT(fullv6.IsPassing("2a02:6b8:c1b:220b:0:1411:955b:1"));

        UNIT_ASSERT_EXCEPTION_CONTAINS(TIpFilter(TData{"::/0", "2a02:6b8:c1b:220b:0:1411:955b:1/128"}), TIntersectedRangesException, "Range intersection between ::-ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff and 2a02:6b8:c1b:220b:0:1411:955b:1-2a02:6b8:c1b:220b:0:1411:955b:1");
    }
}
