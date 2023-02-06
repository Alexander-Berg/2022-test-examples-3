#pragma once

#include <algorithm>
#include <util/generic/string.h>
#include <market/report/library/request_results/request_results.h>

class FakeRequestResults: public IRequestResults {
public:
    struct Doc {
        TString attr;
        int group_index;
        int document_index;
        TString property;
        TString value;
    };

    virtual int getGroupCount(const char* attr) const {
        int max_group_index = -1;
        for (size_t i = 0; i < docs_.size(); ++i) {
            const Doc& doc = docs_[i];
            if (doc.attr == attr)
                max_group_index = std::max(max_group_index, doc.group_index);
        }
        return max_group_index + 1;
    }

    virtual int getGroupSize(const GroupRef& group) const {
        int max_doc_index = -1;
        for (size_t i = 0; i < docs_.size(); ++i) {
            const Doc& doc = docs_[i];
            if (doc.attr == group.attr && doc.group_index == group.index)
                max_doc_index = std::max(max_doc_index, doc.document_index);
        }
        return max_doc_index + 1;
    }

    virtual int getDocumentPropertyCount(const DocumentRef& doc_ref, const char* property) const {
        int count = 0;
        for (size_t i = 0; i < docs_.size(); ++i) {
            const Doc& doc = docs_[i];
            if (doc.attr == doc_ref.attr && doc.group_index == doc_ref.group_index &&
                doc.document_index == doc_ref.document_index && doc.property == property) {
                ++count;
            }
        }
        return count;
    }

    virtual TString getDocumentProperty(const DocumentRef& doc_ref, const char* property) const {
        for (size_t i = 0; i < docs_.size(); ++i) {
            const Doc& doc = docs_[i];
            if (doc.attr == doc_ref.attr && doc.group_index == doc_ref.group_index &&
                doc.document_index == doc_ref.document_index && doc.property == property) {
                return doc.value.c_str();
            }
        }
        return "-1";
    }

    void add(const TString& attr, int group, int document,
             const TString& property, const TString& value) {
        docs_.push_back(Doc());
        Doc& d = docs_.back();
        d.attr = attr;
        d.group_index = group;
        d.document_index = document;
        d.property = property;
        d.value = value;
    }

private:
    std::vector<Doc> docs_;
};
