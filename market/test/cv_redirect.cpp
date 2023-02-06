#include <market/report/src/cv_card_selection.h>
#include <market/report/library/relevance/cards.h>
#include "global_mock.h"
#include <library/cpp/testing/unittest/gtest.h>

/// @note test of requirements https://wiki.yandex-team.ru/Market/Development/redir
namespace {
    using namespace testing;
    using namespace NMarketReport;

    class TGlobalForRedirects: public TGlobalMockBase {
    public:
        virtual const UIntSet& getRedirectStopVendors(const TString&) const {
            static UIntSet empty;
            return empty;
        }

        virtual const UIntSet& getRedirectStopCategories(const TString&) const {
            static UIntSet empty;
            return empty;
        }
    };

    class TCardCollectionProviderMock: public ICardCollectionProvider {
    public:
        virtual const TVisualCategoryVendorCard::TListType& GetVisualCategoryVendorCards() const {
            Y_FAIL("unexpected call");
        }

        virtual const TVisualCategoryCard::TListType& GetVisualCategoryCards() const {
            Y_FAIL("unexpected call");
        }

        virtual const TVisualVendorCard::TListType& GetVisualVendorCards() const {
            static TVisualVendorCard::TListType empty;
            return empty;
        }

        virtual const TVendorCard::TListType& GetNonVisualVendorCards() const {
            return Vendors;
        }

        virtual const TCategoryVendorCard::TListType& GetNonVisualCategoryVendorCards() const {
            return CategoryVendors;
        }

        virtual const TCategoryCard::TListType& GetNonVisualCategoryCards() const {
            return Categories;
        }

        virtual const TVirtualNavigationCard::TListType& GetVirtualNavigationCards() const {
            static TVirtualNavigationCard::TListType empty;
            return empty;
        }

    public:
        TCardCollectionProviderMock& AddCategory(TTovarCategoryId id, const TString& name, std::size_t models = 0) {
            TCategoryCard category;
            category.Id = id;
            category.Name = name;
            category.ModelCount = models;
            Categories.push_back(category);
            return *this;
        }

        TCardCollectionProviderMock& AddCategoryVendor(TCategoryVendorId id, const TString& name) {
            TCategoryVendorCard categoryVendor;
            categoryVendor.Id = id;
            categoryVendor.Name = name;
            CategoryVendors.push_back(categoryVendor);
            return *this;
        }

        TCardCollectionProviderMock& AddVendor(TVendorId id, const TString& name) {
            TVendorCard vendor;
            vendor.Id = id;
            vendor.Name = name;
            Vendors.push_back(vendor);
            return *this;
        }

    private:
        TCategoryCard::TListType Categories;
        TCategoryVendorCard::TListType CategoryVendors;
        TVendorCard::TListType Vendors;
    };

    class CategoryVendorCardConsumerStub: public ICategoryVendorCardConsumer {
    public:
        virtual void Consume(const TVendorCard& vendor) {
            Vendors.insert(vendor.Id);
        }

        virtual void Consume(const TCategoryCard& category) {
            Categories.insert(category.Id);
        }

        virtual void Consume(const TCategoryVendorCard& categoryVendor) {
            CategoryVendors.insert(categoryVendor.Id);
        }

        virtual void Consume(const TVirtualNavigationCard&) {
        }

    public:
        bool HasCategory(TTovarCategoryId id) const {
            return Categories.count(id);
        }

        bool HasCategoryVendor(TCategoryVendorId id) const {
            return CategoryVendors.count(id);
        }

        bool HasVendor(TVendorId id) const {
            return Vendors.count(id);
        }

    private:
        std::set<TVendorId> Vendors;
        std::set<TTovarCategoryId> Categories;
        std::set<TCategoryVendorId> CategoryVendors;
    };

    TCardSelectionContext UserRequest(const TString& request) {
        TCardSelectionContext ctx;
        ctx.Strict = true;
        ctx.CanonicalRequest = request;
        ctx.OriginalRequest = request;
        return ctx;
    }
}

struct Cvredirect: public testing::Test {
    TGlobalForRedirects Global;
    DummyCategoryVendorCardSelectionEvents Events;
    CategoryVendorCardConsumerStub Selected;
};

using Id = std::size_t;
using Models = std::size_t;

TEST_F(Cvredirect, SelectsCategoryExactlyMatchingRequest) {
    TCardCollectionProviderMock found;
    found
        .AddCategory(Id(1), "телевизоры")
        .AddCategory(Id(2), "холодильники")
        .AddCategory(Id(3), "телефоны");

    EXPECT_TRUE(SelectRelevantCard(found, UserRequest("холодильники"), Events, Selected));
    EXPECT_TRUE(Selected.HasCategory(Id(2)));
}

TEST_F(Cvredirect, SelectsAloneCategory) {
    TCardCollectionProviderMock found;
    found.AddCategory(Id(1), "телевизоры");

    EXPECT_TRUE(SelectRelevantCard(found, UserRequest("запрос про телевизоры"), Events, Selected));
    EXPECT_TRUE(Selected.HasCategory(Id(1)));
}

TEST_F(Cvredirect, SelectsCategoryWithModelMajority) {
    TCardCollectionProviderMock found;
    found
        .AddCategory(Id(1), "телевизоры", Models(11))
        .AddCategory(Id(2), "тетрисы", Models(2))
        .AddCategory(Id(3), "стиральные машины", Models(1));

    EXPECT_TRUE(SelectRelevantCard(found, UserRequest("запрос про телевизоры, тетрисы и стиральные машины"), Events, Selected));
    EXPECT_TRUE(Selected.HasCategory(Id(1)));
}

TEST_F(Cvredirect, DoesNotSelectTooManyCategories) {
    TCardCollectionProviderMock found;
    found
        .AddCategory(Id(1), "телевизоры", Models(10))
        .AddCategory(Id(2), "тетрисы", Models(2))
        .AddCategory(Id(3), "стиральные машины", Models(1))
        .AddCategory(Id(4), "розовые ушки", Models(1));

    EXPECT_FALSE(SelectRelevantCard(found, UserRequest("запрос про телевизоры, тетрисы, стиральные машины и розовые ушки"), Events, Selected));
}

TEST_F(Cvredirect, SelectsCategoryVendor) {
    TCardCollectionProviderMock found;
    found.AddCategoryVendor(Id(1), "телевизоры самсунг");

    EXPECT_TRUE(SelectRelevantCard(found, UserRequest("телевизоры самсунг"), Events, Selected));
    EXPECT_TRUE(Selected.HasCategoryVendor(Id(1)));
}

TEST_F(Cvredirect, SelectsExactlyMatchedCategoryInsteadOfCategoryVendor) {
    TCardCollectionProviderMock found;
    found
        .AddCategoryVendor(Id(1), "телевизоры самсунг")
        .AddCategory(Id(2), "телевизоры");

    EXPECT_TRUE(SelectRelevantCard(found, UserRequest("телевизоры"), Events, Selected));
    EXPECT_TRUE(Selected.HasCategory(Id(2)));
    EXPECT_FALSE(Selected.HasCategoryVendor(Id(1)));
}

TEST_F(Cvredirect, SelectsExactlyMatchedCategoryInsteadOfVendor) {
    TCardCollectionProviderMock found;
    found
        .AddVendor(Id(1), "факс")
        .AddCategory(Id(2), "факс");

    EXPECT_TRUE(SelectRelevantCard(found, UserRequest("факс"), Events, Selected));
    EXPECT_TRUE(Selected.HasCategory(Id(2)));
    EXPECT_FALSE(Selected.HasVendor(Id(1)));
}

/// @todo write test for vendor redirection after resolving issue https://st.yandex-team.ru/MARKETOUT-4541
