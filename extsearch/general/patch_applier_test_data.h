#pragma once

#include <extsearch/geo/fast_export/applier/iface/provider.h>
#include <extsearch/geo/fast_export/applier/patch_applier.h>
#include <extsearch/geo/fast_export/protos/patch/patch.pb.h>

#include <extsearch/geo/kernel/pbreport/intermediate/metadatacollection.h>

namespace NTestData {

    class TTestDataProvider: public NFastExport::IPatchApplierDataProvider {
    public:
        void EraseDocAttribute(const TString& key) override {
            Attributes_.erase(key);
        }

        void AddDocAttribute(const TString& key, const TString& value) override {
            if (!Attributes_.contains(key)) {
                Attributes_.insert({key, {}});
            }
            Attributes_.at(key).push_back(value);
        }

        TString GetDocAttribute(const TString& key) const override {
            return Attributes_.at(key)[0];
        }

        const TVector<TString>& GetDocAttributes(const TString& key) const {
            return Attributes_.at(key);
        }

        bool HasDocAttribute(const TString& key) override {
            return Attributes_.contains(key);
        }

        void SetTitle(const TString& name) override {
            Title_ = name;
        }

        TString GetTitle() const override {
            return Title_;
        }

        void SetUrl(const TString& url) override {
            Url_ = url;
        }

        TString GetUrl() const override {
            return Url_;
        }

        NFastExport::TMetadataCollection& MetadataCollection() override {
            return Metadata_;
        }

        void CommitChangesInMetadata() override {
        }

    private:
        THashMap<TString, TVector<TString>> Attributes_;
        NFastExport::TMetadataCollection Metadata_;
        TString Title_;
        TString Url_;
    };

    template <class TMessage>
    TVector<TMessage> LoadTestProtos(const TString& filename);

    TVector<TTestDataProvider> CreateDocs();
    TTestDataProvider CreateDocWithEntrances(bool entranceMetadata, bool routePointMetadata);
    TTestDataProvider CreateDocWithVisualHints();
    NFastExport::TSnippetPatch GenerateSnippetPatch(int patchNumber);
    NFastExport::TDeleteUrlsPatch GenerateDeleteUrlPatch();
    THashMap<TString, TString> GetAttributions();
    NFastExport::TSnippetPatch ConstructClosedForVisitorsPatch();
    NFastExport::TSnippetPatch ConstructClosedPatch(bool permanently);
    void AddDescriptionToPatch(NFastExport::TSnippetPatch& patch, const TString& description);
    void AddTycoonExtraDataToPatch(NFastExport::TSnippetPatch& patch, const NSpravTDS::Company& data);
    void AddTycoonExtraDataToAttributes(TTestDataProvider& dataProvider, const NSpravTDS::Company& data);
    NSpravTDS::Company ParseTycoonExtraData(TTestDataProvider& dataProvider);
} // namespace NTestData
