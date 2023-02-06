#include "omni_url_iterator.h"

#include <robot/jupiter/library/utils/url_tools.h>
#include <util/folder/path.h>
#include <util/string/builder.h>
#include <library/cpp/string_utils/url/url.h>

namespace NTestShard {

TOmniUrlIterator::TOmniUrlIterator(const TString& shardDir)
    : Wad_(NDoom::IWad::Open(TFsPath(shardDir) / "index.l2.wad"))
    , Reader_(nullptr, nullptr, Wad_.Get(), TString(), true)
    , Accessor_(TOmniAccessorFactory::NewAccessor<TIo>(&Reader_))
    , IndexManager_("", &Reader_)
    , DocId_(0)
{
}

bool TOmniUrlIterator::Read(THit* hit) {
    if (DocId_ >= IndexManager_.Size()) {
        return false;
    }
    hit->Doc = DocId_;
    hit->Url = Find(DocId_);
    ++DocId_;
    return true;
}

TStringBuf TOmniUrlIterator::Find(TDoc doc) {
    return IndexManager_.Get(doc, Accessor_.Get());
}

TUrlIterator::TUrlIterator(const TString& shardDir)
    : TOmniUrlIterator(shardDir)
{}

bool TUrlIterator::Read(TAttributeWithDocs* attr) {
    THit hit;
    bool result = TOmniUrlIterator::Read(&hit);
    if (result) {
        attr->Clear();
        attr->AddDoc(hit.Doc);
        attr->SetAttribute("url:\"" + TString{hit.Url} + "\"");
    }
    return result;
}

TSiteIterator::TSiteIterator(const TString& shardDir)
    : TOmniUrlIterator(shardDir)
{}

bool TSiteIterator::Read(TAttributeWithDocs* attr) {
    THit hit;
    bool result = TOmniUrlIterator::Read(&hit);
    if (result) {
        TStringBuf host = GetOnlyHost(hit.Url);
        attr->Clear();
        attr->AddDoc(hit.Doc);
        attr->SetAttribute("site:\"" + TString{host} + "\"");
    }
    return result;
}

}
