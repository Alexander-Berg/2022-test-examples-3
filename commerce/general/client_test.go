package goblogs

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

const (
	testServiceTicket = "TESTS"
)

func newTestClient(testServerURL string) *Client {
	return New(Options{
		BaseURL:       testServerURL,
		ServiceTicket: testServiceTicket,
	})
}

func newTestServer(t *testing.T, urlPath string, statusCode int, body string) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		actualURL := req.URL.Path

		if req.URL.RawQuery != "" {
			actualURL += "?" + req.URL.RawQuery
		}

		assert.Equal(t, urlPath, actualURL)
		assert.Equal(t, req.Header.Get(serviceTicketHeader), testServiceTicket)

		res.Header().Set("Content-Type", "application/json")
		res.WriteHeader(statusCode)

		_, _ = res.Write([]byte(body))
	}))
}

func TestClient_GetBlogInfo_ShouldReturnBlogInfo(t *testing.T) {
	ts := newTestServer(t, "/adv-news?lang=ru-RU", http.StatusOK, `{
		"_id": "57220b37ee46a4216ed5e5f7",
		"canComment": true,
		"canWrite": false,
		"description": "",
		"facebookPageId": "",
		"feedbackLink": "",
		"gaId": "",
		"hasArchiveDisclaimer": false,
		"hasDescription": false,
		"hasImageTitle": true,
		"hasRelatedArticles": true,
		"hasSubscriptionBlock": false,
		"hasTypography": false,
		"hideShareCount": false,
		"isBanned": false,
		"isClosed": false,
		"isModerated": false,
		"isModerator": false,
		"isPrivate": false,
		"langsForTranslate": ["tr-TR", "en-EN"],
		"language": "ru-RU",
		"localeTimezone": "Europe/Moscow",
		"maxCommentsLevel": 3,
		"maxPostSize": "30000",
		"metaDescription": "",
		"metaKeywords": "",
		"metrikaId": "",
		"postsCount": {"2004": {"Dec": 2, "Nov": 2}},
		"prefix": 0,
		"serviceLogotype": null,
		"serviceName": "",
		"serviceUrl": "",
		"showAuthor": false,
		"showcase": [],
		"slug": "adv-news",
		"socialHeader": "",
		"socialLinks": [],
		"title": "Новости Рекламных технологий Яндекса",
		"type": "club",
		"viewType": "blogInfo",
		"withPreview": true,
		"withSignature": false
	}`)

	defer ts.Close()

	blogInfo, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetInfo()

	if !assert.NoError(t, err) || !assert.NotNil(t, blogInfo) {
		return
	}

	assert.Equal(t, Blog{
		ID: "57220b37ee46a4216ed5e5f7",

		Slug:     "adv-news",
		Title:    "Новости Рекламных технологий Яндекса",
		Type:     "club",
		ViewType: "blogInfo",

		Language:       "ru-RU",
		LocaleTimezone: "Europe/Moscow",

		LanguagesForTranslate: []string{"tr-TR", "en-EN"},

		MaxCommentsLevel: 3,

		HasImageTitle:      true,
		HasRelatedArticles: true,
		WithPreview:        true,
		CanComment:         true,

		PostsCount: map[string]map[string]uint{"2004": {"Dec": 2, "Nov": 2}},

		SocialLinks: []SocialLink{},
	}, *blogInfo)
}

func TestClient_GetBlogInfo_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/adv-news?lang=ru-RU", http.StatusNotFound, `{}`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetInfo()

	assert.Error(t, err)
}

func TestClient_GetAllCategories_ShouldReturnAllCategories(t *testing.T) {
	ts := newTestServer(t, "/categories/all/adv-news?lang=ru-RU", http.StatusOK, `[
		{
			"_id": "5d3aff6c2794ff00300ce029",
			"displayName": "1234",
			"parentCategoryId": "5d3aff6c2794ff00300ce027",
			"slug": "1234"
		},
 		{
			"_id": "5d3858522794ff00300cb033",
			"displayName": "2",
			"parentCategoryId": null,
			"slug": "2"
		}
	]`)

	defer ts.Close()

	categories, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetAllCategories()

	if !assert.NoError(t, err) {
		return
	}

	assert.Len(t, categories, 2)
	assert.Equal(t, "5d3aff6c2794ff00300ce029", categories[0].ID)
	assert.Equal(t, "1234", categories[0].DisplayName)
	assert.Equal(t, "5d3aff6c2794ff00300ce027", *categories[0].ParentID)
	assert.Equal(t, "1234", categories[0].Slug)
	assert.Nil(t, categories[1].ParentID)
}

func TestClient_GetAllCategories_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/categories/all/adv-news?lang=ru-RU", http.StatusNotFound, `[]`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetAllCategories()

	assert.Error(t, err)
}

func TestClient_GetAllTags_ShouldReturnAllTags(t *testing.T) {
	ts := newTestServer(t, "/tags/all/adv-news?lang=ru-RU", http.StatusOK, `[
		{
			"displayName": "ADFOX",
			"slug": "adfox"
		},
		{
			"displayName": "PVL",
			"slug": "pvl"
		}
	]`)

	defer ts.Close()

	tags, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetAllTags()

	if !assert.NoError(t, err) {
		return
	}

	assert.Len(t, tags, 2)
	assert.Equal(t, Tag{Slug: "pvl", DisplayName: "PVL"}, tags[1])
}

func TestClient_GetAllTags_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/tags/all/adv-news?lang=ru-RU", http.StatusNotFound, `[]`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetAllTags()

	assert.Error(t, err)
}

func TestClient_GetAllPosts_ShouldReturnAllPosts(t *testing.T) {
	ts := newTestServer(t, "/posts/adv-news?lang=ru-RU&size=100", http.StatusOK, `[
		{
			"_id": "54dae43ab6637a226fba4123",
			"created_at": "2015-02-10T11:33:27.074Z",
			"updated_at": "2015-02-10T11:33:27.074Z",
			"publishDate": "2015-02-10T11:33:27.074Z",
			"approvedTitle": "Заголовок первого поста",
			"approvedBody": {
				"source": "s1",
				"html": "h1",
				"contentType": "c1"
			},
			"approvedPreview": {
				"source": "s2",
				"html": "h2",
				"contentType": "c2"
			},
			"categoryIds": ["5c98bf970825df766988df10"],
			"titleImage": {
				"orig" : {
				   "height" : 640,
				   "path" : "/get-yablogs/603/imagename/orig",
				   "width" : 1024,
				   "fullPath": "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/orig"
				}
			},
			"tags": [{"displayName": "Справочник", "slug": "справочник"}],
			"authorId": "8600685941399260644",
			"slug": "first_test_post",
			"viewType": "major",
			"commentsCount": 32,
			"hasNext": false
		}
	]`)

	defer ts.Close()

	posts, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetAllPosts()

	if !assert.NoError(t, err) {
		return
	}

	assert.Len(t, posts, 1)
	assert.Equal(t, Post{
		ID: "54dae43ab6637a226fba4123",

		AuthorID: "8600685941399260644",

		Slug:     "first_test_post",
		ViewType: "major",

		ApprovedBody:    map[string]string{"contentType": "c1", "source": "s1", "html": "h1"},
		ApprovedPreview: map[string]string{"contentType": "c2", "source": "s2", "html": "h2"},
		ApprovedTitle:   "Заголовок первого поста",

		CommentsCount: 32,

		HasNext: false,

		CategoryIDs: []string{"5c98bf970825df766988df10"},
		Tags:        []Tag{{"справочник", "Справочник"}},

		TitleImages: map[string]Image{
			"orig": {
				Path:     "/get-yablogs/603/imagename/orig",
				FullPath: "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/orig",
				Height:   640,
				Width:    1024,
			},
		},

		CreatedAt:   &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
		PublishDate: &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
		UpdatedAt:   &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
	}, posts[0])
}

func TestClient_GetAllPosts_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/posts/adv-news?lang=ru-RU&size=100", http.StatusNotFound, `[]`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetAllPosts()

	assert.Error(t, err)
}

func TestClient_GetArchivePostsByYear_ShouldReturnPosts(t *testing.T) {
	ts := newTestServer(t, "/posts/archive/adv-news?lang=ru-RU&year=2015", http.StatusOK, `[
		{
			"_id": "54dae43ab6637a226fba4123",
			"created_at": "2015-02-10T11:33:27.074Z",
			"updated_at": "2015-02-10T11:33:27.074Z",
			"publishDate": "2015-02-10T11:33:27.074Z",
			"approvedTitle": "Заголовок первого поста",
			"approvedBody": {
				"source": "s1",
				"html": "h1",
				"contentType": "c1"
			},
			"approvedPreview": {
				"source": "s2",
				"html": "h2",
				"contentType": "c2"
			},
			"categoryIds": ["5c98bf970825df766988df10"],
			"titleImage": {
				"orig" : {
				   "height" : 640,
				   "path" : "/get-yablogs/603/imagename/orig",
				   "width" : 1024,
				   "fullPath": "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/orig"
				}
			},
			"tags": [{"displayName": "Справочник", "slug": "справочник"}],
			"authorId": "8600685941399260644",
			"slug": "first_test_post",
			"viewType": "major",
			"commentsCount": 32,
			"hasNext": true
		}
	]`)

	defer ts.Close()

	posts, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetArchivePostsByYear(2015)

	if !assert.NoError(t, err) {
		return
	}

	assert.Len(t, posts, 1)
	assert.Equal(t, Post{
		ID: "54dae43ab6637a226fba4123",

		AuthorID: "8600685941399260644",

		Slug:     "first_test_post",
		ViewType: "major",

		ApprovedBody:    map[string]string{"contentType": "c1", "source": "s1", "html": "h1"},
		ApprovedPreview: map[string]string{"contentType": "c2", "source": "s2", "html": "h2"},
		ApprovedTitle:   "Заголовок первого поста",

		CommentsCount: 32,

		HasNext: true,

		CategoryIDs: []string{"5c98bf970825df766988df10"},
		Tags:        []Tag{{"справочник", "Справочник"}},

		TitleImages: map[string]Image{
			"orig": {
				Path:     "/get-yablogs/603/imagename/orig",
				FullPath: "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/orig",
				Height:   640,
				Width:    1024,
			},
		},

		CreatedAt:   &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
		PublishDate: &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
		UpdatedAt:   &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
	}, posts[0])
}

func TestClient_GetArchivePostsByYear_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/posts/archive/adv-news?lang=ru-RU&year=2015", http.StatusNotFound, `[]`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		GetArchivePostsByYear(2015)

	assert.Error(t, err)
}

func TestClient_GetPostInfo_ShouldReturnPostInfo(t *testing.T) {
	ts := newTestServer(t, "/post/adv-news/test?lang=ru-RU", http.StatusOK, `{
		"_id": "54dae43ab6637a226fba4123",
		"created_at": "2015-02-10T11:33:27.074Z",
		"updated_at": "2015-02-10T11:33:27.074Z",
		"publishDate": "2015-02-10T11:33:27.074Z",
		"approvedTitle": "Заголовок первого поста",
		"approvedBody": {
			"source": "s1",
			"html": "h1",
			"contentType": "c1"
		},
		"approvedPreview": {
			"source": "s2",
			"html": "h2",
			"contentType": "c2"
		},
		"categoryIds": ["5c98bf970825df766988df10"],
		"titleImage": {
			"orig" : {
			   "height" : 640,
			   "path" : "/get-yablogs/603/imagename/orig",
			   "width" : 1024,
			   "fullPath": "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/orig"
			}
		},
		"tags": [{"displayName": "Справочник", "slug": "справочник"}],
		"authorId": "8600685941399260644",
		"slug": "first_test_post",
		"viewType": "major",
		"commentsCount": 32,
		"canComment": true,
		"hasNext": true
	}`)

	defer ts.Close()

	postInfo, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		PostIdentity("test").
		GetInfo()

	if !assert.NoError(t, err) || !assert.NotNil(t, postInfo) {
		return
	}

	assert.Equal(t, Post{
		ID: "54dae43ab6637a226fba4123",

		AuthorID: "8600685941399260644",

		Slug:     "first_test_post",
		ViewType: "major",

		ApprovedBody:    map[string]string{"contentType": "c1", "source": "s1", "html": "h1"},
		ApprovedPreview: map[string]string{"contentType": "c2", "source": "s2", "html": "h2"},
		ApprovedTitle:   "Заголовок первого поста",

		CommentsCount: 32,
		CanComment:    true,

		HasNext: true,

		CategoryIDs: []string{"5c98bf970825df766988df10"},
		Tags:        []Tag{{"справочник", "Справочник"}},

		TitleImages: map[string]Image{
			"orig": {
				Path:     "/get-yablogs/603/imagename/orig",
				FullPath: "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/orig",
				Height:   640,
				Width:    1024,
			},
		},

		CreatedAt:   &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
		PublishDate: &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
		UpdatedAt:   &Time{time.Date(2015, 2, 10, 11, 33, 27, 74000000, time.UTC)},
	}, *postInfo)
}

func TestClient_GetPostInfo_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/post/adv-news/test?lang=ru-RU", http.StatusNotFound, `{}`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		PostIdentity("test").
		GetInfo()

	assert.Error(t, err)
}

func TestClient_GetAllRelatedArticles_ShouldReturnAllRelatedArticles(t *testing.T) {
	ts := newTestServer(t, "/post/related/adv-news/test?lang=ru-RU", http.StatusOK, `[
		{
			"title": "Поиск с учётом региона",
			"image": "https://avatars.yandex.net/get-bunker/080546fe68bf4d5088f032e6e5ad289b0d450484/normal/080546.png",
			"url": "https://yandex.ru/company/technologies/regions"
		},
	  	{
			"title": "Как показать рекламу целевой аудитории",
			"image": "https://avatars.yandex.net/get-bunker/46bec1aff17419c1b6a050ba41903aee06e2ef53/normal/46bec1.png",
			"slug": "adv"
		}
	]`)

	defer ts.Close()

	relatedArticles, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		PostIdentity("test").
		GetAllRelatedArticles()

	if !assert.NoError(t, err) {
		return
	}

	assert.Len(t, relatedArticles, 2)
	assert.Equal(t, []RelatedArticle{
		{
			URL:   "https://yandex.ru/company/technologies/regions",
			Title: "Поиск с учётом региона",
			Image: "https://avatars.yandex.net/get-bunker/080546fe68bf4d5088f032e6e5ad289b0d450484/normal/080546.png",
		},
		{
			Title: "Как показать рекламу целевой аудитории",
			Image: "https://avatars.yandex.net/get-bunker/46bec1aff17419c1b6a050ba41903aee06e2ef53/normal/46bec1.png",
			Slug:  "adv",
		},
	}, relatedArticles)
}

func TestClient_GetAllRelatedArticles_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/post/related/adv-news/test?lang=ru-RU", http.StatusNotFound, `[]`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		PostIdentity("test").
		GetAllRelatedArticles()

	assert.Error(t, err)
}

func TestClient_GetAllComments_ShouldReturnAllComments(t *testing.T) {
	ts := newTestServer(t, "/comments/all/adv-news/test?lang=ru-RU", http.StatusOK, `[
		{
			"_id": "5c5c04d41a5d24002ae8c7ed",
			"authorId": "192214205",
			"body": {"html": "1", "source": "1"},
			"canEdit": false,
			"canRemove": false,
			"canSubscribe": false,
			"childrenCount": 0,
			"created_at": "2019-02-07T10:13:41.393Z",
			"isRemoved": false,
			"isReviewed": false,
			"isTrustedMember": false,
			"isYandexStaff": false,
			"lastBodyUpdate": "2019-02-07T10:13:40.265Z",
			"level": 1,
			"path": "5a7d64dc172baa0024fc93eb/5c5c04d41a5d24002ae8c7ed",
			"postId": "5a7d64dc172baa0024fc93eb",
			"replyCount": 0
		},
		{
			"_id": "5c5c04fd1a5d24002ae8c7f2",
			"authorId": "192214205",
			"body": {"html": "2", "source": "2"},
			"canEdit": false,
			"canRemove": false,
			"canSubscribe": false,
			"childrenCount": 0,
			"created_at": "2019-02-07T10:14:22.938Z",
			"isRemoved": false,
			"isReviewed": false,
			"isTrustedMember": false,
			"isYandexStaff": false,
			"lastBodyUpdate": "2019-02-07T10:14:21.971Z",
			"level": 1,
			"path": "5a7d64dc172baa0024fc93eb/5c5c04fd1a5d24002ae8c7f2",
			"postId": "5a7d64dc172baa0024fc93eb",
			"replyCount": 0
		}
	]`)

	defer ts.Close()

	comments, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		PostIdentity("test").
		GetAllComments()

	if !assert.NoError(t, err) {
		return
	}

	assert.Len(t, comments, 2)
	assert.Equal(t, Comment{
		ID: "5c5c04fd1a5d24002ae8c7f2",

		Path: "5a7d64dc172baa0024fc93eb/5c5c04fd1a5d24002ae8c7f2",

		AuthorID: "192214205",

		Body: map[string]string{"html": "2", "source": "2"},

		Level: 1,

		CreatedAt:      &Time{time.Date(2019, 2, 7, 10, 14, 22, 938000000, time.UTC)},
		LastBodyUpdate: &Time{time.Date(2019, 2, 7, 10, 14, 21, 971000000, time.UTC)},
	}, comments[1])
}

func TestClient_GetAllComments_ShouldReturnError(t *testing.T) {
	ts := newTestServer(t, "/comments/all/adv-news/test?lang=ru-RU", http.StatusNotFound, `[]`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("adv-news", "ru-RU").
		PostIdentity("test").
		GetAllComments()

	assert.Error(t, err)
}

func TestClient_ShouldHandleError(t *testing.T) {
	ts := newTestServer(t, "/posts/unknown?lang=ru-RU&size=100", http.StatusNotFound, `{
		"internalCode": "404_BNF",
		"message": "blog not found"
	}`)

	defer ts.Close()

	_, err := newTestClient(ts.URL).
		BlogIdentity("unknown", "ru-RU").
		GetAllPosts()

	assert.EqualError(t, err, "404_BNF: blog not found")
}
