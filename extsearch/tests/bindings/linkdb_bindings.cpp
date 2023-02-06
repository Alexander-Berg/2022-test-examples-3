#include "linkdb_bindings.h"

#include "contrib/python/py3c/py3c.h"

#include <extsearch/images/robot/library/identifier/imageurl.h>
#include <extsearch/images/robot/mrdb/linkdb/library/validate.h>
#include <extsearch/images/robot/rtrobot/dblayer/db_formats.pb.h>
#include <extsearch/images/robot/rt/linkdb/stateful/lib/state.pb.h>
#include <extsearch/images/robot/rt/linkdb/stateful/lib/state.h>
#include <quality/user_sessions/rt/lib/compress/compress.h>

#include <library/cpp/framing/unpacker.h>
#include <library/cpp/framing/packer.h>
#include <library/cpp/pybind/cast.h>
#include <library/cpp/pybind/ptr.h>
#include <library/cpp/pybind/v2.h>

#include <robot/rthub/yql/protos/queries.pb.h>

#include <util/generic/yexception.h>
#include <util/stream/str.h>
#include <util/stream/zlib.h>
#include <util/system/env.h>

namespace {
    class TLinkDBBindings {
    public:
        TString ZLibCompress(TStringBuf str) {
            TString result;
            TStringOutput output(result);
            TZLibCompress compress(&output, ZLib::ZLib);
            compress.Write(str);
            compress.Flush();
            return result;
        }

        TVector<TString> UnpackPacker(TStringBuf record) {
            TVector<TString> result;
            NFraming::TUnpacker unpacker(NFraming::EFormat::Auto, record);
            TStringBuf skip;
            NImages::NRtRobot::TSingleLinkPB inputMessage;
            while (unpacker.NextFrame(inputMessage, skip)) {
                result.emplace_back();
                Y_PROTOBUF_SUPPRESS_NODISCARD inputMessage.SerializeToString(&result.back());
            }
            return result;
        }

        TString CreateAndPackSingleLink(
            const TVector<TString>& pageAttrs,
            const TVector<TString>& imageLinks) {
            Y_ENSURE(pageAttrs.size() == imageLinks.size(), "Needs same size vectors");

            TStringStream stream;
            NFraming::TPacker packer(stream);

            for (size_t i = 0; i < pageAttrs.size(); ++i) {
                NImages::NLinkDB::TPageAttrsPB pageAttr;
                const bool canParsePageAttrs = pageAttr.ParseFromString(pageAttrs[i]);
                Y_ENSURE(canParsePageAttrs, "Can not parse TPageAttrsPB");

                NImages::NLinkDB::TImageLinkPB imageLinkPB;
                const bool canParseImageLink = imageLinkPB.ParseFromString(imageLinks[i]);
                Y_ENSURE(canParseImageLink, "Can not parse TImageLinkPB");
                NImages::NRtRobot::TSingleLinkPB inputMessage;
                inputMessage.MutablePageAttrs()->CopyFrom(pageAttr);
                inputMessage.MutableImageLink()->CopyFrom(imageLinkPB);
                std::cout << "URL LINKDB " << inputMessage.GetImageLink().GetUrl() << '\n';
                packer.Add(inputMessage, false);
            }

            TString result = stream.Str();
            return result;
        }

        ui64 GetImageUrlId(const TString& url) {
            const auto imageUrlId = NImages::NIndex::TSimpleUrlId::FromUrl(url);
            return imageUrlId.AsNumber();
        }

        TString GetUrlFromSingleLink(const TString& message) {
            NImages::NRtRobot::TSingleLinkPB singleLink;
            const bool canParse = singleLink.ParseFromString(message);
            Y_ENSURE(canParse, "Can not parse TSingleLinkPB");
            return singleLink.GetImageLink().GetUrl();
        }

        float GetRankFromSingleLink(const TString& message) {
            NImages::NRtRobot::TSingleLinkPB singleLink;
            const bool canParse = singleLink.ParseFromString(message);
            Y_ENSURE(canParse, "Can not parse TSingleLinkPB");
            return singleLink.GetPageAttrs().GetWebAttrs().GetMetaRank();
        }

        bool IsValidRthub(const TString& message) {
            NKwYT::TImagePageItem item;
            const bool canParse = item.ParseFromString(message);
            Y_ENSURE(canParse, "Can not parse TSingleLinkPB");
            return NImages::NLinkDB::IsValid(item);
        }

        TVector<TString> GetSingleLinksFromCompressedTLinks(
            const TString& base,
            const TString& patch,
            const TString& codec) {
            NImages::NRT::TLinkDBState::TMessage linkRT;
            linkRT.SetCompressedLinks(base);
            linkRT.SetCompressedLinksPatch(patch);
            linkRT.SetCodec(codec);
            NImages::NRT::TLinkDBState item(linkRT, 1u);

            TVector<TString> result;
            for (const auto& singleLink : item.GetMessage().GetLinks().GetLink()) {
                result.emplace_back();
                Y_PROTOBUF_SUPPRESS_NODISCARD singleLink.SerializeToString(&result.back());
            }
            return result;
        }
    };

    void ExportLinkDBBindingsImpl() {
        ::NPyBind::TPyClass<TLinkDBBindings, NPyBind::TPyClassConfigTraits<true>>("TLinkDBBindings")
            .Def("zLib_compress", &TLinkDBBindings::ZLibCompress)
            .Def("unpack_packer", &TLinkDBBindings::UnpackPacker)
            .Def("get_image_urlid", &TLinkDBBindings::GetImageUrlId)
            .Def("get_image_url", &TLinkDBBindings::GetUrlFromSingleLink)
            .Def("get_image_rank", &TLinkDBBindings::GetRankFromSingleLink)
            .Def("is_valid_rthub", &TLinkDBBindings::IsValidRthub)
            .Def("create_and_pack_singlelink", &TLinkDBBindings::CreateAndPackSingleLink)
            .Def("get_singlelinks_from_linkrt", &TLinkDBBindings::GetSingleLinksFromCompressedTLinks)
            .Complete();
    }
}

void NImages::NPyLib::ExportLinkDBBindings() {
    ExportLinkDBBindingsImpl();
}
