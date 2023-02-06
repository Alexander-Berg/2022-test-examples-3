#include <market/report/src/redirects/RedirectParams.h>
#include <library/cpp/testing/unittest/gtest.h>

TEST(TMarketRedirectUrl, Brand) {
    {
        NMarketReport::TMarketRedirectUrl url("/brands/8340189?hid=7812586&suggest=1");
        EXPECT_EQ("brands", url.GetId());
        EXPECT_EQ(true, url.IsVisual());
    }

    {
        NMarketReport::TMarketRedirectUrl url("/brands.xml?brand=153061&suggest=1");
        EXPECT_EQ("brands", url.GetId());
        EXPECT_EQ(false, url.IsVisual());
    }

    {
        NMarketReport::TMarketRedirectUrl url("/brands/12715700");
        EXPECT_EQ("brands", url.GetId());
        EXPECT_EQ("12715700", url.GetParameters().Get("brand-id"));
    }

    {
        NMarketReport::TMarketRedirectUrl url("/brands--chanel/12715700");
        EXPECT_EQ("brands", url.GetId());
        EXPECT_EQ("chanel", url.GetParameters().Get("slug"));
        EXPECT_EQ("12715700", url.GetParameters().Get("brand-id"));
    }
}

TEST(TMarketRedirectUrl, Model) {
    {
        NMarketReport::TMarketRedirectUrl url("/model.xml?modelid=10495456&hid=91491&suggest=1");
        EXPECT_EQ("model", url.GetId());
        EXPECT_EQ(false, url.IsVisual());
    }

    {
        NMarketReport::TMarketRedirectUrl url("/product/10495456");
        EXPECT_EQ("model", url.GetId());
        EXPECT_EQ("10495456", url.GetParameters().Get("modelid"));
        EXPECT_FALSE(url.GetParameters().Has("slug"));
    }

    {
        NMarketReport::TMarketRedirectUrl url("/product--model-slug/10495456");
        EXPECT_EQ("model", url.GetId());
        EXPECT_EQ("10495456", url.GetParameters().Get("modelid"));
        EXPECT_EQ("model-slug", url.GetParameters().Get("slug"));
    }

    {
        NMarketReport::TMarketRedirectUrl url("/product/10495456", /* isBlue */ true);
        EXPECT_EQ("model", url.GetId());
        EXPECT_EQ("10495456", url.GetParameters().Get("modelid"));
        EXPECT_FALSE(url.GetParameters().Has("slug"));
    }

    {
        NMarketReport::TMarketRedirectUrl url("/product/model-slug/10495456", /* isBlue */ true);
        EXPECT_EQ("model", url.GetId());
        EXPECT_EQ("10495456", url.GetParameters().Get("modelid"));
        EXPECT_EQ("model-slug", url.GetParameters().Get("slug"));
    }
}

TEST(TMarketRedirectUrl, Catalog) {
    {
        NMarketReport::TMarketRedirectUrl url("/catalog.xml?hid=6427100&suggest=1");
        EXPECT_EQ("catalog", url.GetId());
        EXPECT_EQ(false, url.IsVisual());
    }

    {
        NMarketReport::TMarketRedirectUrl url("/catalog/6427100");
        EXPECT_EQ("catalog", url.GetId());
        EXPECT_EQ("6427100", url.GetParameters().Get("nid"));
        EXPECT_FALSE(url.GetParameters().Has("slug"));
    }

    {
        NMarketReport::TMarketRedirectUrl url("/catalog--catalog-slug/6427100");
        EXPECT_EQ("catalog", url.GetId());
        EXPECT_EQ("6427100", url.GetParameters().Get("nid"));
        EXPECT_EQ("catalog-slug", url.GetParameters().Get("slug"));
    }

    {
        NMarketReport::TMarketRedirectUrl url("/catalog/6427100", /* isBlue */ true);
        EXPECT_EQ("catalog", url.GetId());
        EXPECT_EQ("6427100", url.GetParameters().Get("nid"));
        EXPECT_FALSE(url.GetParameters().Has("slug"));
    }

    {
        NMarketReport::TMarketRedirectUrl url("/catalog/catalog-slug/6427100", /* isBlue */ true);
        EXPECT_EQ("catalog", url.GetId());
        EXPECT_EQ("6427100", url.GetParameters().Get("nid"));
        EXPECT_EQ("catalog-slug", url.GetParameters().Get("slug"));
    }
}

TEST(TMarketRedirectUrl, Guru) {
    NMarketReport::TMarketRedirectUrl url("/guru.xml?hid=6427100&suggest=1");
    EXPECT_EQ("guru", url.GetId());
    EXPECT_EQ(false, url.IsVisual());
}

TEST(TMarketRedirectUrl, Search) {
    {
        NMarketReport::TMarketRedirectUrl url("/search?hid=7812177&suggest=1");
        EXPECT_EQ("search", url.GetId());
        EXPECT_EQ(true, url.IsVisual());
    }
    {
        NMarketReport::TMarketRedirectUrl url("/search.xml?text=люстра&suggest=2");
        EXPECT_EQ("search", url.GetId());
        EXPECT_EQ(false, url.IsVisual());
    }
}

TEST(TMarketRedirectUrl, ShopOpinions) {
    NMarketReport::TMarketRedirectUrl url("/shop-opinions.xml?shop_id=310298&suggest=1");
    EXPECT_EQ("shop-opinions", url.GetId());
    EXPECT_EQ(false, url.IsVisual());
}

TEST(TMarketRedirectUrl, Collections) {
    NMarketReport::TMarketRedirectUrl url("/collections/ny-presents-vam");
    EXPECT_EQ("collections", url.GetId());
    EXPECT_EQ("ny-presents-vam", url.GetParameters().Get("collection-id"));
}

TEST(TMarketRedirectUrl, Articles) {
    NMarketReport::TMarketRedirectUrl url("/articles/kak-vybrat-mobilnyj-telefon");
    EXPECT_EQ("articles", url.GetId());
    EXPECT_EQ("kak-vybrat-mobilnyj-telefon", url.GetParameters().Get("article-id"));
}

TEST(TMarketRedirectUrl, Franchise) {
    NMarketReport::TMarketRedirectUrl url("/franchise/14713996");
    EXPECT_EQ("franchise", url.GetId());
    EXPECT_EQ("14713996", url.GetParameters().Get("franchise-id"));
}

TEST(TMarketRedirectUrl, Gifts) {
    NMarketReport::TMarketRedirectUrl url("/gifts/teen-boy");
    EXPECT_EQ("gifts", url.GetId());
    EXPECT_EQ("teen-boy", url.GetParameters().Get("gift-id"));
}
