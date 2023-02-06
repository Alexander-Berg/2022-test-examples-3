#include "test_common.h"

#include "xal.h"

#include <extsearch/geo/kernel/xml_writer/xml.h>

#include <util/string/type.h>
#include <util/string/vector.h>
#include <util/string/split.h>
#include <util/string/join.h>
#include <util/string/strip.h>

using namespace NGeosearch;

namespace {
    TString SaveToString(const NPa::Address& message) {
        static const TString tag = "Q";
        static const TStringBuf open = "<Q>";
        static const TStringBuf close = "</Q>";

        NXmlWr::TDocument d{tag};
        NAddress::PrintXAL(d.Root(), message);

        const TString dump = d.AsString();

        const size_t openPos = dump.find(open);
        const size_t closePos = dump.rfind(close);
        Y_ENSURE(openPos != TString::npos, "no " << open << " tag found");
        Y_ENSURE(closePos != TString::npos, "no " << close << " tag found");

        return TString(dump.data() + openPos + open.size(), dump.data() + closePos);
    }

} // namespace

TString Address2String(const NAddress::TAddress& address, NLocaleUtils::TPackedLocale locale,
                       const TString& houseName,
                       const TString& addressLine) {
    NPa::Address message;
    address.FillProtoMessage(&message, locale);

    message.set_formatted_address(!addressLine.empty() ? addressLine : address.GetAddressLine(locale));
    if (!houseName.empty()) {
        NAddress::AddHouseComponent(&message, houseName);
    }

    return SaveToString(message);
}

TString Node2String(const NXml::TConstNode& node) {
    TVector<TString> stroki;
    Split(node.ToString(), "\n", stroki);
    for (auto&& s : stroki) {
        s = StripInPlace(s);
    }
    return JoinSeq("", stroki);
}

TVector<TString> SplitXAL(const TString& data) {
    TVector<TString> result;

    // seek for >[[:blank:]]*<
    size_t substrBegin = 0;
    size_t pos = 0;
    while (pos < data.length()) {
        const size_t beginGap = data.find('>', pos);
        if (beginGap == TString::npos) {
            break;
        }
        const size_t endGap = data.find('<', beginGap + 1);
        if (endGap == TString::npos) {
            break;
        }
        // check if all chars in ragne [beginGap + 1, endGap - 1] are whitespaces
        const size_t gapSize = endGap - beginGap - 1;
        if (gapSize == 0 || IsSpace(data.data() + beginGap + 1, gapSize)) {
            const TString token = TString(data.data() + substrBegin, data.data() + beginGap + 1); // > is included
            result.push_back(token);
            substrBegin = endGap; // < is included
        }
        pos = endGap + 1;
    }
    result.push_back(data.substr(substrBegin));
    return result;
}

TString GetTag(const TString& tag, const TString& data) {
    const TString open = TString::Join("<", tag, ">");
    const TString close = TString::Join("</", tag, ">");

    const size_t openPos = data.find(open);
    if (openPos == TString::npos) {
        return TString();
    }
    const size_t closePos = data.find(close, openPos);
    if (closePos == TString::npos) {
        return TString();
    }
    return TString(data.data() + openPos + open.length(), data.data() + closePos);
}

Y_UNIT_TEST_SUITE(TTestCommon) {
    Y_UNIT_TEST(TestSplitXAL) {
        ASSERT_STRING_VECTORS_EQUAL(SplitXAL("<a>"), {"<a>"});
        ASSERT_STRING_VECTORS_EQUAL(SplitXAL("<a></a>"), TVector<TString>({"<a>", "</a>"}));
        ASSERT_STRING_VECTORS_EQUAL(SplitXAL("<a> </a>"), TVector<TString>({"<a>", "</a>"}));
        ASSERT_STRING_VECTORS_EQUAL(SplitXAL("<a>  </a>"), TVector<TString>({"<a>", "</a>"}));
        ASSERT_STRING_VECTORS_EQUAL(SplitXAL("<a> <b>\t</b><c>Hello</c><c> world! </c>\n</a>"),
                                    TVector<TString>({"<a>", "<b>", "</b>", "<c>Hello</c>", "<c> world! </c>", "</a>"}));
        ASSERT_STRING_VECTORS_EQUAL(SplitXAL("abacaba"), TVector<TString>({"abacaba"}));
        ASSERT_STRING_VECTORS_EQUAL(SplitXAL("aba<c>aba"), TVector<TString>({"aba<c>aba"}));
    }
}

TVector<TString> ReadChunks(IInputStream& in) {
    TVector<TString> result;
    const TString& data = in.ReadAll();
    for (const auto& t : StringSplitter(data).SplitByString("\n\n")) {
        result.push_back(TString(t.Token()));
    }
    return result;
}

TString SerializeAddress(const NPa::Address& a) {
    return a.Utf8DebugString();
}
