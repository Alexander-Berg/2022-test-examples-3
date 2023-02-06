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

func TestPost_ShouldRespondWithPostFromCache(t *testing.T) {
	post := &goblogs.Post{Slug: "edu"}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/post/:blog/:post", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/post/:blog/:post")
	c.SetParamNames("blog", "post")
	c.SetParamValues("adv-news", "edu")

	_ = c.Cache().SetPost("adv-news", "ru-RU", "edu", post)

	assert.NoError(t, Post(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Contains(t, recorder.Body.String(), `"slug":"edu"`)
}

func TestPost_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/post/:blog/:post", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/post/:blog/:post")
	c.SetParamNames("blog")
	c.SetParamValues("adv-news")

	assert.NoError(t, Post(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, postNotFoundMessage, recorder.Body.String())
}

func TestPostComments_ShouldRespondWithCommentsFromCache(t *testing.T) {
	comments := []goblogs.Comment{
		{ID: "5c9c85f98f25f76a40f63522"},
		{ID: "5c9c86058f25f76a40f63523"},
	}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/comments/all/:blog/:post", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/comments/all/:blog/:post")
	c.SetParamNames("blog", "post")
	c.SetParamValues("adv-news", "edu")

	_ = c.Cache().SetPostComments("adv-news", "ru-RU", "edu", comments)

	assert.NoError(t, PostComments(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Contains(t, recorder.Body.String(), `"_id":"5c9c85f98f25f76a40f63522"`)
	assert.Contains(t, recorder.Body.String(), `"_id":"5c9c86058f25f76a40f63523"`)
}

func TestPostComments_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/comments/all/:blog/:post", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/comments/all/:blog/:post")
	c.SetParamNames("blog", "post")
	c.SetParamValues("adv-news", "edu")

	assert.NoError(t, PostComments(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, postNotFoundMessage, recorder.Body.String())
}

func TestPostRelatedArticles_ShouldRespondWithRelatedArticlesFromCache(t *testing.T) {
	relatedArticles := []goblogs.RelatedArticle{
		{Slug: "obuchenie"},
		{Slug: "webmaster"},
	}

	e := echo.New()
	r := e.Router()
	r.Add("GET", "/post/related/:blog/:post", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/post/related/:blog/:post")
	c.SetParamNames("blog", "post")
	c.SetParamValues("adv-news", "edu")

	_ = c.Cache().SetPostRelatedArticles("adv-news", "ru-RU", "edu", relatedArticles)

	assert.NoError(t, PostRelatedArticles(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Contains(t, recorder.Body.String(), `"slug":"obuchenie"`)
	assert.Contains(t, recorder.Body.String(), `"slug":"webmaster"`)
}

func TestPostRelatedArticles_ShouldRespondWithNotFoundIfNotInCache(t *testing.T) {
	e := echo.New()
	r := e.Router()
	r.Add("GET", "/post/related/:blog/:post", func(echo.Context) error { return nil })

	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())
	c.SetPath("/post/related/:blog/:post")
	c.SetParamNames("blog", "post")
	c.SetParamValues("adv-news", "edu")

	assert.NoError(t, PostRelatedArticles(c))
	assert.Equal(t, http.StatusNotFound, recorder.Code)
	assert.Equal(t, postNotFoundMessage, recorder.Body.String())
}
