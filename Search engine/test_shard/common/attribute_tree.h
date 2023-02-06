#pragma once

#include "attribute_tree.fwd.h"
#include "inverse_index.h"

#include <search/tools/test_shard/proto/config.pb.h>

#include <util/generic/ptr.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/generic/xrange.h>
#include <util/string/builder.h>
#include <util/string/cast.h>

namespace NTestShard {
namespace NPrivate {

template<typename T>
bool EqualPtrs(const THolder<T>& lhs, const THolder<T>& rhs) {
    if (lhs && rhs) {
        return *lhs == *rhs;
    } else {
        return (!lhs && !rhs);
    }
}

} // NPrivate

template<class String>
struct TNode {
    using TKey = String;

    EOperation Type = EOperation::Leaf;
    TKey Key;
    THolder<TNode> Child;
    THolder<TNode> RightBrother;

    TNode() = default;

    TNode(EOperation type, const TKey& key)
        : Type(type)
        , Key(key)
    {}

    TNode(const NProto::TNode& node)
        : Type(node.GetOp())
        , Key(node.GetAttribute())
    {
        if (Type != EOperation::Leaf) {
            Y_ENSURE(node.NodeSize() > 0, "The node marked as non-terminal, but it has no children");
            Child = MakeHolder<TNode>(node.GetNode(0));
            TNode* NextChild = Child.Get();
            for (size_t i = 1; i < node.NodeSize(); ++i) {
                NextChild->AddBrother(MakeHolder<TNode>(node.GetNode(i)));
                NextChild = NextChild->RightBrother.Get();
            }
        }
    }

    TNode(const TNode& other) {
        operator=(other);
    }

    TNode& operator=(const TNode& other) {
        Type = other.Type;
        Key = other.Key;
        if (other.Child) {
            Child = MakeHolder<TNode>(*other.Child);
        }
        if (other.RightBrother) {
            RightBrother = MakeHolder<TNode>(*other.RightBrother);
        }
        return *this;
    }

    TNode(TNode&& other) = default;

    TNode& operator=(TNode&& other) = default;

    bool operator==(const TNode& other) const {
        return
            Type == other.Type
            && Key == other.Key
            && NPrivate::EqualPtrs(Child, other.Child)
            && NPrivate::EqualPtrs(RightBrother, other.RightBrother);
    }

    void AddChild(THolder<TNode>&& child) {
        Key = TKey{};
        if (!Child) {
            Child = std::move(child);
        } else {
            Child->AddBrother(std::move(child));
        }
    }

    void AddBrother(THolder<TNode>&& brother) {
        brother->RightBrother = std::move(RightBrother);
        RightBrother = std::move(brother);
    }

    template<typename F1, typename F2, typename F3>
    void Traverse(F1&& f1, F2&& f2, F3&& f3, const TNode* parent = nullptr) const {
        f1(*this);
        if (Child) {
            Child->Traverse(f1, f2, f3, this);
        }
        f3(*this);
        if (RightBrother) {
            if (parent) {
                f2(*parent);
            }
            RightBrother->Traverse(f1, f2, f3, parent);
        }
    }

    TInverseIndex BuildInvIndex(const TAttributeCache& cache) const {
        if (Type == EOperation::Leaf) {
            return cache.Get(Key);
        } else {
            Y_ENSURE(Child);
            TInverseIndex result = Child->BuildInvIndex(cache);
            TNode* nextChild = Child.Get();
            while (nextChild = nextChild->RightBrother.Get()) {
                result.ApplyOperation(Type, nextChild->BuildInvIndex(cache));
            }
            return result;
        }
    }

    NProto::TNode ToNode() const {
        NProto::TNode result;
        result.SetOp(Type);
        result.SetAttribute(ToString(Key));
        if (Child) {
            TNode* ptr = Child.Get();
            while (ptr) {
                *result.AddNode() = ptr->ToNode();
                ptr = ptr->RightBrother.Get();
            }
        }
        return result;
    }
};

template<class Node>
class TAttributeTree {
public:
    using TKey = typename Node::TKey;

    TAttributeTree() = default;

    TAttributeTree(const TKey& key)
        : Root_(EOperation::Leaf, key)
    {}

    TAttributeTree(const NProto::TNode& root)
        : Root_(root)
    {}

    bool operator==(const TAttributeTree& other) const {
        return Root_ == other.Root_;
    }

    template<typename F>
    inline void TraverseLeaves(F&& callback) const {
        auto id = [](const Node&){};
        auto wrapper = [&callback](const Node& node) {
            if (node.Type == EOperation::Leaf) {
                callback(node.Key);
            }
        };
        TraverseInternal(wrapper, id, id);
    }

    TInverseIndex BuildInvIndex(const TAttributeCache& cache) const {
        return Root_.BuildInvIndex(cache);
    }

    void Merge(EOperation op, TAttributeTree&& other) {
        Node oldRoot = std::move(Root_);
        Root_ = Node(op, TKey(""));
        Root_.AddChild(MakeHolder<Node>(std::move(oldRoot)));
        Root_.AddChild(MakeHolder<Node>(std::move(other.Root_)));
    }

    void Merge(EOperation op, const TAttributeTree& other) {
        Node oldRoot = std::move(Root_);
        Root_ = Node(op, TKey(""));
        Root_.AddChild(MakeHolder<Node>(std::move(oldRoot)));
        Root_.AddChild(MakeHolder<Node>(other.Root_));
    }

    TString Serialize() const {
        TStringBuilder result;
        auto open = [&result](const Node& node) {
            if (node.Type == EOperation::Leaf) {
               result << node.Key;
            } else {
                result << '(';
            }
        };
        auto iter = [&result](const Node& node) {
            if (node.Type == EOperation::Or) {
                result << " |";
            }
            result << ' ';
        };
        auto close = [&result](const Node& node) {
            if (node.Type != EOperation::Leaf) {
                result << ')';
            }
        };
        TraverseInternal(open, iter, close);
        return result;
    }

    NProto::TNode ToNode() const {
        return Root_.ToNode();
    }

private:
    template<typename F1, typename F2, typename F3>
    void TraverseInternal(F1&& f1, F2&& f2, F3&& f3) const {
        Root_.template Traverse<F1, F2, F3>(std::forward<F1>(f1), std::forward<F2>(f2), std::forward<F3>(f3));
    }

private:
    Node Root_;
};

}
