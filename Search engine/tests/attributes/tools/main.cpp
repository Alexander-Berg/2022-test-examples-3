#include <library/cpp/getopt/last_getopt.h>

#include <kernel/attributes/filter/attr_restrictions_filter.h>
#include <kernel/attributes/restrictions/attr_restrictions_decomposer.h>
#include <kernel/qtree/richrequest/richnode.h>
#include <kernel/qtree/richrequest/nodeiterator.h>
#include <kernel/qtree/request/reqattrlist.h>
#include <kernel/doom/offroad_attributes_wad/offroad_attributes_wad_io.h>
#include <kernel/doom/wad/mega_wad_writer.h>
#include <kernel/doom/wad/wad.h>

#include <library/cpp/offroad/flat/flat_searcher.h>

#include <util/generic/algorithm.h>
#include <util/generic/hash.h>
#include <util/generic/ptr.h>
#include <util/generic/string.h>
#include <util/generic/yexception.h>

#include <utility>

using TKeySearcher = NDoom::TOffroadAttributesKeyIo::TSearcher;
using TKeyIterator = TKeySearcher::TIterator;
using TKeyData = TKeySearcher::TKeyData;
using TKeyRef = TKeySearcher::TKeyRef;
using THitSearcher = NDoom::TOffroadAttributesHitIo::TSearcher;
using THitIterator = THitSearcher::TIterator;
using THit = THitSearcher::THit;

static const THashSet<TString> SegmentTreeAttributes = { "#date" };
size_t numDocs = 0;

bool IsSegmentAttr(const NAttributes::TSingleAttrRestriction& restriction) {
    for (const TString& attr : SegmentTreeAttributes) {
        if (restriction.GetLeft().StartsWith(attr)) {
            return true;
        }
    }
    return false;
}

TString ExtractSegmentAttribute(const NAttributes::TSingleAttrRestriction& restriction) {
    for (const TString& attr : SegmentTreeAttributes) {
        if (restriction.GetLeft().StartsWith(attr)) {
            return attr;
        }
    }
    return "";
}


void AddDocs(const TKeyData& data, THashSet<ui32>* filter, THitSearcher* hitSearcher) {
    THitIterator hitIter;
    hitSearcher->Seek(data.Start(), data.End(), &hitIter);
    THit hit;
    while (hitIter.ReadHit(&hit)) {
        filter->insert(hit.DocId());
    }
}

THashSet<ui32> FillAccepted(const NAttributes::TAttrRestrictions<>& restrictions, const NAttributes::TSingleAttrRestriction& restriction, THitSearcher* hitSearcher, TKeySearcher* keySearcher) {
    THashSet<ui32> filter;
    switch (restriction.GetTreeOper()) {
        case NAttributes::TSingleAttrRestriction::Leaf: {
            TKeyRef ref;
            TKeyData data;
            TKeyIterator keyIter;
            Cout << restriction.GetLeft() << " " <<  restriction.GetRight() << " " << static_cast<int>(restriction.GetCmpOper()) << Endl;
            if (IsSegmentAttr(restriction)) {
                TString attr = ExtractSegmentAttribute(restriction);
                Y_ENSURE(keySearcher->LowerBound(attr, &ref, &data, &keyIter));
                while (keyIter.ReadKey(&ref, &data) && ref.StartsWith(attr)) {
                    THitIterator hitIter;
                    if (ref <= restriction.GetRight() && ref >= restriction.GetLeft()) {
                        if (restriction.GetCmpOper() == NAttributes::TSingleAttrRestriction::cmpLT) {
                            if (ref < restriction.GetRight()) {
                                AddDocs(data, &filter, hitSearcher);
                            }
                            continue;
                        }
                        if (restriction.GetCmpOper() == NAttributes::TSingleAttrRestriction::cmpGT) {
                            if (ref > restriction.GetLeft()) {
                                AddDocs(data, &filter, hitSearcher);
                            }
                            continue;
                        }
                        AddDocs(data, &filter, hitSearcher);
                    }
                }
            } else {
                Y_ENSURE(keySearcher->LowerBound(restriction.GetLeft(), &ref, &data, &keyIter));
                Cout << ref << " " << restriction.GetLeft() << Endl;
                if (ref == restriction.GetLeft()) {
                    AddDocs(data, &filter, hitSearcher);
                }
            }
            return filter;
        }
        case NAttributes::TSingleAttrRestriction::And: {
            for (size_t docId = 0; docId < numDocs; ++docId) {
                filter.insert(docId);
            }
            for (ui32 child : restriction.GetChildren()) {
                THashSet<ui32> current = FillAccepted(restrictions, restrictions[child], hitSearcher, keySearcher);
                EraseNodesIf(filter, [&current](ui32 docId) { return !current.contains(docId); } );
            }
            return filter;
        }
        case NAttributes::TSingleAttrRestriction::Or: {
            for (ui32 child : restriction.GetChildren()) {
                THashSet<ui32> current = FillAccepted(restrictions, restrictions[child], hitSearcher, keySearcher);
                filter.insert(current.begin(), current.end());
            }
            return filter;
        }
        case NAttributes::TSingleAttrRestriction::AndNot: {
            for (ui32 child : restriction.GetChildren()) {
                THashSet<ui32> current = FillAccepted(restrictions, restrictions[child], hitSearcher, keySearcher);
                for (size_t docId = 0; docId < numDocs; ++docId) {
                    if (!current.contains(docId)) {
                        filter.insert(docId);
                    }
                }
            }
            return filter;
        }
        default: {
            break;
        }
    }
    Y_ENSURE(false);
    return {};
}

int main(int argc, const char* argv[]) {
    NLastGetopt::TOpts opts;

    opts.SetTitle("attributes_segment_tree -- requires key,inv,flat,doc and qtrees file with segment attributes and validates the opened iterators, saves the attrs to stdout");

    TString keyFile;
    opts
        .AddLongOption("key", "Key")
        .Optional()
        .RequiredArgument("Key")
        .StoreResult(&keyFile)
        .DefaultValue("none");

    TString invFile;
    opts
        .AddLongOption("inv", "Inv")
        .Optional()
        .RequiredArgument("Inv")
        .StoreResult(&invFile)
        .DefaultValue("none");

    TString flatFile;
    opts
        .AddLongOption("flat", "Flat")
        .Optional()
        .RequiredArgument("Flat")
        .StoreResult(&flatFile)
        .DefaultValue("none");

    TString docFile;
    opts
        .AddLongOption("doc", "Doc")
        .Optional()
        .RequiredArgument("Doc")
        .StoreResult(&docFile)
        .DefaultValue("none");

    TString qtreesFile;
    opts
        .AddLongOption("qtrees", "Qtrees")
        .Optional()
        .RequiredArgument("Qtrees")
        .StoreResult(&qtreesFile)
        .DefaultValue("none");

    opts.AddHelpOption('h');
    NLastGetopt::TOptsParseResult(&opts, argc, argv);

    THolder<NDoom::IWad> keyWad = NDoom::IWad::Open(keyFile);
    THolder<NDoom::IWad> invWad = NDoom::IWad::Open(invFile);
    TKeySearcher keySearcher(keyWad.Get());
    THitSearcher hitSearcher(invWad.Get());
    NDoom::TAttributesSegTreeOffsetsSearcher flatSearcher;
    TBlob flatBlob = TBlob::FromFile(flatFile);
    flatSearcher.Reset(flatBlob);

    THashMap<TString, std::pair<size_t, size_t>> ranges = { { "#date", { 0, 0 } } };
    for (auto& attr : ranges) {
        size_t start = 0;
        size_t end = 0;
        TKeyRef currentKeyBuf;
        TKeyData currentRange;
        TKeyIterator keyIterator;
        Y_ENSURE(keySearcher.LowerBound(attr.first, &currentKeyBuf, &currentRange, &keyIterator, &start));
        Y_ENSURE(keySearcher.LowerBound(attr.first + "\xff", &currentKeyBuf, &currentRange, &keyIterator, &end));
        attr.second = { start, end };
    }

    TFileInput docCount(docFile);
    docCount >> numDocs;

    // correctness
    THitIterator iter;
    for (size_t i = 0; i < flatSearcher.Size(); ++i) {
        NDoom::TAttributesHitRange first = flatSearcher.ReadData(i);
        hitSearcher.Seek(first.Start(), first.End(), &iter);
        THit hit;
        while (iter.ReadHit(&hit)) {
            Y_ENSURE(hit < THit(numDocs));
        }
    }

    // indexes are ok
    TKeyRef ref;
    TKeyData data;
    size_t firstIndex = 0;
    size_t cur = 0;
    size_t lastIndex = 0;
    TKeyIterator keyIter;
    for (const TString& segmentTreeAttribute : SegmentTreeAttributes) {
        Y_ENSURE(keySearcher.LowerBound(segmentTreeAttribute, &ref, &data, &keyIter, &firstIndex));
        cur = firstIndex;
        while (keyIter.ReadKey(&ref, &data) && ref.StartsWith(segmentTreeAttribute)) {
            TKeyRef lowerBoundRef;
            TKeyIterator localKeyIter;
            size_t localIndex = 0;
            Y_ENSURE(keySearcher.LowerBound(ref, &lowerBoundRef, &data, &localKeyIter, &localIndex));
            Y_ENSURE(localIndex == cur++);
            Y_ENSURE(lowerBoundRef == ref);
        }
        Y_ENSURE(keySearcher.LowerBound(segmentTreeAttribute + "\xff", &ref, &data, &keyIter, &lastIndex));
        Y_ENSURE(cur == lastIndex);
        Y_ENSURE(lastIndex - firstIndex < 10000); // 30 years
    }

    TFileInput qtrees(qtreesFile);
    TString qtree;
    const EQtreeDeserializeMode dFlag = QTREE_DEFAULT_DESERIALIZE;
    while (qtrees.ReadLine(qtree)) {
        TBinaryRichTree bin = DecodeRichTreeBase64(qtree);
        TRichTreeConstPtr tree = DeserializeRichTree(bin, dFlag);
        NAttributes::TAttrRestrictions<> restrictions = NAttributes::DecomposeAttrRestrictions(tree, false);
        if (restrictions.Empty()) {
            continue;
        }
        THashSet<ui32> acceptedDocs = FillAccepted(restrictions, restrictions[0], &hitSearcher, &keySearcher);
        NAttributes::TAttrRestrictionsFilter filter(&hitSearcher, &keySearcher, &flatSearcher, &ranges, ui32(1) << 19, &restrictions);
        Cout << acceptedDocs.size() << Endl;
        for (size_t docId = 0; docId < numDocs; ++docId) {
            Y_ENSURE(filter.IsAccepted(docId) == acceptedDocs.contains(docId));
        }
    }
    return 0;
}
