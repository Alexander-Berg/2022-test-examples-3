package vault

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"a.yandex-team.ru/library/go/yandex/yav/httpyav"
	"github.com/stretchr/testify/assert"
)

func setUp() (*httptest.Server, *httpyav.Client, error) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.RequestURI == "/1/versions/sec-000001400089/" {
			_, _ = w.Write([]byte(`
				{
					"status": "ok",
					"version": {
						"created_at": 1564828284,
						"created_by": 42,
						"creator_login": "pg",
						"secret_name": "ololo",
						"secret_uuid": "sec-000001400089",
						"value": [
							{
								"key": "looken",
								"value": "tooken"
							}
						],
						"version": "ver-000001400089"
					}
				}
			`))
		} else {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte(`
				{
					"status": "error",
					"code": "user_auth_required_error",
					"message": "User auth required"
				}
			`))
		}
	}))
	c, err := httpyav.NewClient(
		httpyav.WithOAuthToken("looken-tooken"),
		httpyav.WithHTTPHost(srv.URL),
	)
	return srv, c, err
}

func TestGetSecretValue(t *testing.T) {
	srv, c, err := setUp()
	assert.NoError(t, err)
	defer srv.Close()
	r := &YavSecretsResolver{
		client: c,
		cache:  make(map[string]map[string]string),
	}
	s, err := r.GetSecretValue("sec-000001400089", "looken")
	assert.NoError(t, err)
	assert.Equal(t, "tooken", s)
}

func TestGetSecretValueBadResponse(t *testing.T) {
	srv, c, err := setUp()
	assert.NoError(t, err)
	defer srv.Close()
	r := &YavSecretsResolver{
		client: c,
		cache:  make(map[string]map[string]string),
	}
	s, err := r.GetSecretValue("sec", "looken")
	assert.Equal(t, "", s)
	assert.EqualError(t, err, "unexpected yav response {error user_auth_required_error}")
}

func TestGetSecretValueNotFound(t *testing.T) {
	srv, c, err := setUp()
	assert.NoError(t, err)
	defer srv.Close()
	r := &YavSecretsResolver{
		client: c,
		cache:  make(map[string]map[string]string),
	}
	s, err := r.GetSecretValue("sec-000001400089", "bad-looken")
	assert.Equal(t, "", s)
	assert.EqualError(t, err, "secret with key bad-looken not found")
}

func TestGetSecretValueFromCache(t *testing.T) {
	secUID := "sec-000001400089"
	key := "looken"
	srv, c, err := setUp()
	assert.NoError(t, err)
	defer srv.Close()
	r := &YavSecretsResolver{
		client: c,
		cache:  make(map[string]map[string]string),
	}
	r.cache[secUID] = make(map[string]string)
	r.cache[secUID][key] = "cached"

	s2, err := r.GetSecretValue(secUID, key)
	assert.NoError(t, err)
	assert.Equal(t, "cached", s2)
}
