package controllers

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/commerce/blogs_pumpkin/cache"
	"a.yandex-team.ru/commerce/libs/goblogs"
)

func TestBlog_ShouldRespondWithBlogFromCache(t *testing.T) {
	blog := &goblogs.Blog{Slug: "adv-news"}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	err := c.Cache().SetBlog("adv-news", "ru-RU", blog)
	assert.NoError(t, err)

	assert.NoError(t, Blog(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Contains(t, recorder.Body.String(), `"slug":"adv-news"`)
}

func TestBlog_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	assert.NoError(t, Blog(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, blogNotFoundMessage, recorder.Body.String())
}

func TestBlogCategories_ShouldRespondWithCategoriesFromCache(t *testing.T) {
	categories := []goblogs.Category{
		{ID: "5c9c85f98f25f76a40f63522", Slug: "expert-article"},
		{ID: "5c9c86058f25f76a40f63523", Slug: "checklist"},
	}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/categories/all/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/categories/all/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	_ = c.Cache().SetBlogCategories("adv-news", "ru-RU", categories)

	assert.NoError(t, BlogCategories(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Contains(t, recorder.Body.String(), `"_id":"5c9c85f98f25f76a40f63522"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"5c9c86058f25f76a40f63523"`)
}

func TestBlogCategories_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/categories/all/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/categories/all/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	assert.NoError(t, BlogCategories(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, blogNotFoundMessage, recorder.Body.String())
}

func TestBlogTags_ShouldRespondWithTagsFromCache(t *testing.T) {
	tags := []goblogs.Tag{
		{Slug: "obuchenie", DisplayName: "Обучение"},
		{Slug: "webmaster", DisplayName: "Вебмастер"},
	}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/tags/all/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/tags/all/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	_ = c.Cache().SetBlogTags("adv-news", "ru-RU", tags)

	assert.NoError(t, BlogTags(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Contains(t, recorder.Body.String(), `"slug":"obuchenie"`)
	assert.Contains(t, recorder.Body.String(), `"slug":"webmaster"`)
}

func TestBlogTags_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	assert.NoError(t, BlogTags(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, blogNotFoundMessage, recorder.Body.String())
}

func TestBlogTag_ShouldRespondWithTagFromCache(t *testing.T) {
	tags := []goblogs.Tag{
		{Slug: "obuchenie", DisplayName: "Обучение"},
		{Slug: "webmaster", DisplayName: "Вебмастер"},
	}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/tag/:blog/:tag", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/tag/:blog/:tag")
	c.SetParamNames("blog", "tag")
	c.SetParamValues("adv-news", "webmaster")

	_ = c.Cache().SetBlogTags("adv-news", "ru-RU", tags)

	assert.NoError(t, BlogTag(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.NotContains(t, recorder.Body.String(), `"slug":"obuchenie"`)
	assert.Contains(t, recorder.Body.String(), `"slug":"webmaster"`)
}

func TestBlogTag_ShouldRespondWithNotFoundIfNotInArray(t *testing.T) {
	tags := []goblogs.Tag{
		{Slug: "obuchenie", DisplayName: "Обучение"},
		{Slug: "webmaster", DisplayName: "Вебмастер"},
	}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/tag/:blog/:tag", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/tag/:blog/:tag")
	c.SetParamNames("blog", "tag")
	c.SetParamValues("adv-news", "webmaster?")

	_ = c.Cache().SetBlogTags("adv-news", "ru-RU", tags)

	assert.NoError(t, BlogTag(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, blogNotFoundMessage, recorder.Body.String())
}

func TestBlogTag_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/tag/:blog/:tag", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/tag/:blog/:tag")
	c.SetParamNames("blog", "tag")
	c.SetParamValues("adv-news", "webmaster")

	assert.NoError(t, BlogTag(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, blogNotFoundMessage, recorder.Body.String())
}

func TestBlogPostsCustom_ShouldRespondWithEmptyList(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/custom/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/posts/custom/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	assert.NoError(t, BlogPostsCustom(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Equal(t, recorder.Body.String(), `[]`)
}

func fillPostsForTests(c cache.Cache) cache.Cache {
	_ = c.SetBlogPosts("adv-news", "ru-RU", []goblogs.Post{
		{
			ID:          "1",
			CategoryIDs: []string{"category-1"},

			Tags: []goblogs.Tag{
				{Slug: "tag-1"},
			},
		},
		{
			ID:          "2",
			CategoryIDs: []string{"category-1"},

			Tags: []goblogs.Tag{
				{Slug: "tag-1"},
				{Slug: "tag-2"},
			},
		},
		{
			ID:          "3",
			CategoryIDs: []string{"category-1", "category-2"},

			Tags: []goblogs.Tag{
				{Slug: "tag-1"},
				{Slug: "tag-2"},
				{Slug: "tag-3"},
			},
		},
	})

	return c
}

func TestBlogPosts_ShouldHandleFromPostIdQueryParam(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("from", "2")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.NotContains(t, recorder.Body.String(), `"_id":"1"`)
	assert.NotContains(t, recorder.Body.String(), `"_id":"2"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldHandleCategoryIdQueryParam(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("categoryId", "category-2")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.NotContains(t, recorder.Body.String(), `"_id":"1"`)
	assert.NotContains(t, recorder.Body.String(), `"_id":"2"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldHandleSizeQueryParam(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("size", "1")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.Contains(t, recorder.Body.String(), `"_id":"1"`)
	assert.NotContains(t, recorder.Body.String(), `"_id":"2"`)
	assert.NotContains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldHandleTagQueryParam(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("tag", "tag-3")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.NotContains(t, recorder.Body.String(), `"_id":"1"`)
	assert.NotContains(t, recorder.Body.String(), `"_id":"2"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldHandleTagsQueryParam(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("tags", "tag-1,tag-2")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.NotContains(t, recorder.Body.String(), `"_id":"1"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"2"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldHandleTagAndTagsQueryParams(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("tag", "tag-1")
	c.QueryParams().Set("tags", "tag-2,tag-3")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.NotContains(t, recorder.Body.String(), `"_id":"1"`)
	assert.NotContains(t, recorder.Body.String(), `"_id":"2"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldHandleFromAndSizeQueryParams(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("from", "2")
	c.QueryParams().Set("size", "1")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.NotContains(t, recorder.Body.String(), `"_id":"1"`)
	assert.NotContains(t, recorder.Body.String(), `"_id":"2"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldHandleBigSizeQueryParam(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), fillPostsForTests(cache.NewLocalCache()))
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")
	c.QueryParams().Set("size", "4")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusOK, recorder.Code)

	assert.Contains(t, recorder.Body.String(), `"_id":"1"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"2"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"3"`)
}

func TestBlogPosts_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/posts/:blog", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/posts/:blog")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	assert.NoError(t, BlogPosts(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, blogNotFoundMessage, recorder.Body.String())
}
