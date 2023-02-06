#pragma once

#include <search/tools/test_shard/proto/config.pb.h>

#include <util/generic/string.h>

namespace NTestShard {

template<typename TKey>
struct TNode;

template<typename Node>
class TAttributeTree;

using TAttrRefTree = TAttributeTree<TNode<TStringBuf>>;
using TAttrSchemeTree = TAttributeTree<TNode<TString>>;

using EOperation = NProto::ENodeType;

using TDocId = ui32;
using TDocHash = ui64;

}
