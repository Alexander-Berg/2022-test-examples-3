package cache

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/commerce/libs/goblogs"
	"a.yandex-team.ru/library/go/httputil/headers"
)

func handlerWith(value interface{}) http.HandlerFunc {
	return func(res http.ResponseWriter, req *http.Request) {
		res.Header().Set(headers.ContentTypeKey, string(headers.TypeApplicationJSON))

		b, _ := json.Marshal(value)
		_, _ = fmt.Fprint(res, string(b))
	}
}

func TestWarmUp_ShouldFillCache(t *testing.T) {
	blog := &goblogs.Blog{
		Slug: "adv-news",

		Language:              "ru-RU",
		LanguagesForTranslate: []string{"en-EN", "tr-TR"},

		HasRelatedArticles: true,

		WithPreview: true,
	}

	categories := []goblogs.Category{
		{ID: "5c9c85f98f25f76a40f63522", Slug: "expert-article"},
		{ID: "5c9c86058f25f76a40f63523", Slug: "checklist"},
	}

	firstPost := &goblogs.Post{
		ID:   "first-post-id",
		Slug: "first-post",
	}
	firstPostComments := []goblogs.Comment{
		{ID: "first-post-comment"},
	}
	firstPostRelatedArticles := []goblogs.RelatedArticle{
		{Slug: "first-post-related-article"},
	}

	secondPost := &goblogs.Post{
		ID:   "second-post-id",
		Slug: "second-post",
	}
	secondPostComments := []goblogs.Comment{
		{ID: "second-post-comment"},
	}
	secondPostRelatedArticles := []goblogs.RelatedArticle{
		{Slug: "second-post-related-article"},
	}

	posts := []goblogs.Post{*firstPost, *secondPost}

	tags := []goblogs.Tag{
		{Slug: "obuchenie", DisplayName: "Обучение"},
		{Slug: "webmaster", DisplayName: "Вебмастер"},
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/adv-news", handlerWith(blog))
	mux.HandleFunc("/categories/all/adv-news", handlerWith(categories))
	mux.HandleFunc("/posts/adv-news", handlerWith(posts))
	mux.HandleFunc("/post/adv-news/first-post-id", handlerWith(firstPost))
	mux.HandleFunc("/comments/all/adv-news/first-post-id", handlerWith(firstPostComments))
	mux.HandleFunc("/post/related/adv-news/first-post-id", handlerWith(firstPostRelatedArticles))
	mux.HandleFunc("/post/adv-news/second-post-id", handlerWith(secondPost))
	mux.HandleFunc("/comments/all/adv-news/second-post-id", handlerWith(secondPostComments))
	mux.HandleFunc("/post/related/adv-news/second-post-id", handlerWith(secondPostRelatedArticles))
	mux.HandleFunc("/tags/all/adv-news", handlerWith(tags))

	ts := httptest.NewServer(mux)

	defer ts.Close()

	cache := NewLocalCache()
	blogsClient := goblogs.New(goblogs.Options{BaseURL: ts.URL})

	err := WarmUp(
		cache,
		blogsClient.BlogIdentity("adv-news", "ru-RU"),
		blogsClient.BlogIdentity("adv-news", "en-EN"),
	)

	assert.NoError(t, err)

	ruBlogFromCache, _ := cache.GetBlog("adv-news", "ru-RU")
	enBlogFromCache, _ := cache.GetBlog("adv-news", "en-EN")

	assert.Equal(t, blog, ruBlogFromCache)
	assert.Equal(t, blog, enBlogFromCache)

	ruCategoriesFromCache, _ := cache.GetBlogCategories("adv-news", "ru-RU")
	enCategoriesFromCache, _ := cache.GetBlogCategories("adv-news", "en-EN")

	assert.Equal(t, categories, ruCategoriesFromCache)
	assert.Equal(t, categories, enCategoriesFromCache)

	ruPostsFromCache, _ := cache.GetBlogPosts("adv-news", "ru-RU")
	enPostsFromCache, _ := cache.GetBlogPosts("adv-news", "en-EN")

	assert.Equal(t, posts, ruPostsFromCache)
	assert.Equal(t, posts, enPostsFromCache)

	ruFirstPostFromCache, _ := cache.GetPost("adv-news", "ru-RU", "first-post")
	enFirstPostFromCache, _ := cache.GetPost("adv-news", "en-EN", "first-post")

	assert.Equal(t, firstPost, ruFirstPostFromCache)
	assert.Equal(t, firstPost, enFirstPostFromCache)

	ruFirstPostCommentsFromCache, _ := cache.GetPostComments("adv-news", "ru-RU", "first-post")
	enFirstPostCommentsFromCache, _ := cache.GetPostComments("adv-news", "en-EN", "first-post")

	assert.Equal(t, firstPostComments, ruFirstPostCommentsFromCache)
	assert.Equal(t, firstPostComments, enFirstPostCommentsFromCache)

	ruFirstPostRelatedArticlesFromCache, _ := cache.GetPostRelatedArticles("adv-news", "ru-RU", "first-post")
	enFirstPostRelatedArticlesFromCache, _ := cache.GetPostRelatedArticles("adv-news", "en-EN", "first-post")

	assert.Equal(t, firstPostRelatedArticles, ruFirstPostRelatedArticlesFromCache)
	assert.Equal(t, firstPostRelatedArticles, enFirstPostRelatedArticlesFromCache)

	ruTagsFromCache, _ := cache.GetBlogTags("adv-news", "ru-RU")
	enTagsFromCache, _ := cache.GetBlogTags("adv-news", "en-EN")

	assert.Equal(t, tags, ruTagsFromCache)
	assert.Equal(t, tags, enTagsFromCache)
}

func TestWarmUp_ShouldHandleError(t *testing.T) {
	cache := NewLocalCache()
	blogsClient := goblogs.New(goblogs.Options{})

	err := WarmUp(
		cache,
		blogsClient.BlogIdentity("adv-news", "ru-RU"),
	)

	assert.Error(t, err)

	_, ok := cache.GetBlog("adv-news", "ru-RU")

	assert.False(t, ok)

	_, ok = cache.GetBlogCategories("adv-news", "ru-RU")

	assert.False(t, ok)

	_, ok = cache.GetBlogPosts("adv-news", "ru-RU")

	assert.False(t, ok)

	_, ok = cache.GetBlogTags("adv-news", "ru-RU")

	assert.False(t, ok)
}
