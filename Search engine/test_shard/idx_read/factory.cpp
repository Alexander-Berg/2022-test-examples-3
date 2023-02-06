#include "factory.h"
#include "omni_url_iterator.h"
#include "tarc_searcher.cpp"

namespace NTestShard {

TIndexIteratorFactory::TIndexIteratorFactory(const TOptions& opts)
    : ShardDir_(opts.FirstShard().GetPath())
    , KeyInvSearcher_(ShardDir_)
    , ArcSearcher_(opts)
    , MaxAsteriskDocs_(opts.Config.GetAsterisks().GetMaxDocs())
    , MinAsteriskPrefix_(opts.Config.GetAsterisks().GetMinPrefix())
{}

THolder<IIndexIterator> TIndexIteratorFactory::CreateIterator(const TString& attribute) const {
    if (attribute == "url") {
        return MakeHolder<TUrlIterator>(ShardDir_);
    } else if (attribute == "site") {
        return MakeHolder<TSiteIterator>(ShardDir_);
    } else if (attribute == "quote") {
        return ArcSearcher_.GetQuoteIterator();
    } else if (attribute == "word") {
        return ArcSearcher_.GetWordIterator();
    } else {
        return KeyInvSearcher_.GetIterator(attribute, MaxAsteriskDocs_, MinAsteriskPrefix_);
    }
}

}
