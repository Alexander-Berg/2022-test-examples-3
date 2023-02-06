package cache

import (
	"reflect"
	"testing"

	"github.com/alicebob/miniredis/v2"
	"github.com/go-redis/redis/v8"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/commerce/libs/goblogs"
)

func startMiniRedis() func() {
	miniRedis, err := miniredis.Run()

	if err != nil {
		panic(err)
	}

	defaultRedisClient := redisClient

	redisClient = redis.NewUniversalClient(&redis.UniversalOptions{
		Addrs:    []string{miniRedis.Addr()},
		DB:       0,
		ReadOnly: false,
	})

	return func() {
		miniRedis.Close()

		redisClient = defaultRedisClient
	}
}

func TestCache_ShouldHandleBlogInfo(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			blog := &goblogs.Blog{
				Slug: "adv-news",

				Language:              "ru-RU",
				LanguagesForTranslate: []string{"en-EN", "tr-TR"},

				HasRelatedArticles: true,

				WithPreview: true,
			}

			_, ok := cache.GetBlog("adv-news", "ru-RU")

			assert.False(t, ok)

			err := cache.SetBlog("adv-news", "ru-RU", blog)

			assert.NoError(t, err)

			blogFromCache, ok := cache.GetBlog("adv-news", "ru-RU")

			assert.True(t, ok)
			assert.Equal(t, blog, blogFromCache)
		})
	}
}

func TestCache_ShouldHandleBlogCategories(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			categories := []goblogs.Category{
				{ID: "5c9c85f98f25f76a40f63522", Slug: "expert-article"},
				{ID: "5c9c86058f25f76a40f63523", Slug: "checklist"},
			}

			_, ok := cache.GetBlogCategories("adv-news", "ru-RU")

			assert.False(t, ok)

			err := cache.SetBlogCategories("adv-news", "ru-RU", categories)

			assert.NoError(t, err)

			categoriesFromCache, ok := cache.GetBlogCategories("adv-news", "ru-RU")

			assert.True(t, ok)
			assert.Equal(t, categories, categoriesFromCache)
		})
	}
}

func TestCache_ShouldHandleBlogPosts(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			posts := []goblogs.Post{
				{ID: "54dae43ab6637a226fba4123", Slug: "novosti-adfox-v-sentyabre"},
				{ID: "55dae43ab6637a226fba4123", Slug: "catboost-preemnik-matriksneta"},
			}

			_, ok := cache.GetBlogPosts("adv-news", "ru-RU")

			assert.False(t, ok)

			err := cache.SetBlogPosts("adv-news", "ru-RU", posts)

			assert.NoError(t, err)

			postsFromCache, ok := cache.GetBlogPosts("adv-news", "ru-RU")

			assert.True(t, ok)
			assert.Equal(t, posts, postsFromCache)
		})
	}
}

func TestCache_ShouldHandleBlogTags(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			tags := []goblogs.Tag{
				{Slug: "obuchenie", DisplayName: "Обучение"},
				{Slug: "webmaster", DisplayName: "Вебмастер"},
			}

			_, ok := cache.GetBlogTags("adv-news", "ru-RU")

			assert.False(t, ok)

			err := cache.SetBlogTags("adv-news", "ru-RU", tags)

			assert.NoError(t, err)

			tagsFromCache, ok := cache.GetBlogTags("adv-news", "ru-RU")

			assert.True(t, ok)
			assert.Equal(t, tags, tagsFromCache)
		})
	}
}

func TestCache_ShouldHandlePostInfo(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			post := &goblogs.Post{
				ID: "597f1c87917f85672b296df4",

				Slug: "catboost-preemnik-matriksneta",

				CanComment: true,
			}

			_, ok := cache.GetPost("adv-news", "ru-RU", "edu")

			assert.False(t, ok)

			err := cache.SetPost("adv-news", "ru-RU", "edu", post)

			assert.NoError(t, err)

			postFromCache, ok := cache.GetPost("adv-news", "ru-RU", "edu")

			assert.True(t, ok)
			assert.Equal(t, post, postFromCache)
		})
	}
}

func TestCache_ShouldHandlePostComments(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			comments := []goblogs.Comment{
				{ID: "597f1c87917f85672b296df6", AuthorID: "12670985"},
				{ID: "597f1c87917f85672b296df7", AuthorID: "504182847"},
			}

			_, ok := cache.GetPostComments("adv-news", "ru-RU", "edu")

			assert.False(t, ok)

			err := cache.SetPostComments("adv-news", "ru-RU", "edu", comments)

			assert.NoError(t, err)

			commentsFromCache, ok := cache.GetPostComments("adv-news", "ru-RU", "edu")

			assert.True(t, ok)
			assert.Equal(t, comments, commentsFromCache)
		})
	}
}

func TestCache_ShouldHandlePostRelatedArticles(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			relatedArticles := []goblogs.RelatedArticle{
				{Slug: "first", Title: "first article title"},
				{Slug: "second", Title: "second article title"},
			}

			_, ok := cache.GetPostRelatedArticles("adv-news", "ru-RU", "edu")

			assert.False(t, ok)

			err := cache.SetPostRelatedArticles("adv-news", "ru-RU", "edu", relatedArticles)

			assert.NoError(t, err)

			relatedArticlesFromCache, ok := cache.GetPostRelatedArticles("adv-news", "ru-RU", "edu")

			assert.True(t, ok)
			assert.Equal(t, relatedArticles, relatedArticlesFromCache)
		})
	}
}

func TestCache_ShouldHandleBlogsStatus(t *testing.T) {
	defer startMiniRedis()()

	for _, cache := range []Cache{NewLocalCache(), NewRedisCache()} {
		t.Run(reflect.TypeOf(cache).Elem().Name(), func(t *testing.T) {
			assert.Equal(t, BlogsOK, cache.GetBlogsStatus())

			assert.NoError(t, cache.SetBlogsStatus(BlogsNotOK))

			assert.Equal(t, BlogsNotOK, cache.GetBlogsStatus())

			assert.NoError(t, cache.SetBlogsStatus(BlogsOK))

			assert.Equal(t, BlogsOK, cache.GetBlogsStatus())
		})
	}
}
