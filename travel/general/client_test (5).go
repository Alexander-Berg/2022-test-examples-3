package exp3matcher

import (
	"context"
	"net/http"
	"testing"

	"github.com/go-resty/resty/v2"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap"
	"go.uber.org/zap/zaptest"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"

	yzap "a.yandex-team.ru/library/go/core/log/zap"
	nopmetrics "a.yandex-team.ru/library/go/core/metrics/nop"
	"a.yandex-team.ru/library/go/core/xerrors"
	v1 "a.yandex-team.ru/travel/app/backend/api/serverconfig/v1"
	"a.yandex-team.ru/travel/app/backend/internal/l10n"
	"a.yandex-team.ru/travel/app/backend/internal/lib/clientscommon"
	"a.yandex-team.ru/travel/library/go/geobase"
)

const (
	forceUpdateResponseBody = "{\"version\":1225009,\"items\":[{\"name\":\"travel_app_update_info\"," +
		"\"value\":{\"alert\":{\"title\":\"Доступно обновление\"," +
		"\"subtitle\":\"ура\",\"url\":\"https://beta.ru/42\"}," +
		"\"update_type\":\"UPDATE_TYPE_FORCE\",\"version\":\"0.2.1\",\"version_code\":\"4\"}}]}"
	softUpdateResponseBody = "{\"version\":1225009,\"items\":[{\"name\":\"travel_app_update_info\"," +
		"\"value\":{\"update_type\":\"UPDATE_TYPE_SOFT\",\"version\":\"0.2.1\",\"version_code\":\"4\"}}]}"
	forceUpdateTankerResponseBody = "{\"version\":1225009,\"items\":[{\"name\":\"travel_app_update_info\"," +
		"\"value\":{\"alert\":{\"title\":\"tanker:backend_config:update_available\"," +
		"\"subtitle\":\"tanker:backend_config:congratulations\",\"url\":\"https://beta.ru/42\"}," +
		"\"update_type\":\"UPDATE_TYPE_FORCE\",\"version\":\"0.2.1\",\"version_code\":\"4\"}}]}"
	retryPolicyResponseBody = "{\"version\":1276017,\"items\":[{\"name\":\"travel_app_client_retry_policy\"," +
		"\"value\":{\"default_config\":{\"error_codes\":[],\"path\":\"/\",\"policy_id\":\"4000_0_20000\"}," +
		"\"requests_retry_policy_cfg\":[{\"error_codes\":[429,408],\"path\":\"/api/hotels/v1/suggest\",\"policy_id\":\"5000_1_1000\"}]," +
		"\"retry_policies\":[{\"delay_ms\":5000,\"id\":\"5000_1_1000\",\"retry_attempts\":1,\"timeout_ms\":1000}," +
		"{\"delay_ms\":4000,\"id\":\"4000_0_20000\",\"retry_attempts\":0,\"timeout_ms\":20000}]}}]}"
	userProfileConfig = "{\"version\":1286888,\"items\":[{\"name\":\"travel_app_user_profile\"," +
		"\"value\":{\"elements\":[\"USER_PROFILE_ELEMENT_PLUS\",\"USER_PROFILE_ELEMENT_NOTIFICATIONS\"," +
		"\"USER_PROFILE_ELEMENT_PASSENGERS\",\"USER_PROFILE_ELEMENT_SUPPORT_CHAT\",\"USER_PROFILE_ELEMENT_PROMOCODES\"," +
		"\"USER_PROFILE_ELEMENT_PAYMENT_METHODS\",\"USER_PROFILE_ELEMENT_DARK_THEME\",\"USER_PROFILE_ELEMENT_EXIT\"]}}]}"
)

func TestGetUpdateInfoConfigOK(t *testing.T) {
	type testCase struct {
		testName string
		jsonRsp  string
		expected *v1.UpdateInfo
	}
	testCases := []testCase{
		{
			"forceUpdateInfo",
			forceUpdateResponseBody,
			&v1.UpdateInfo{
				Version:     "0.2.1",
				VersionCode: 4,
				UpdateType:  v1.UpdateType_UPDATE_TYPE_FORCE,
				Alert: &v1.Alert{
					Title:    "Доступно обновление",
					Url:      "https://beta.ru/42",
					Subtitle: "ура",
				},
			},
		},
		{
			"softUpdateInfo",
			softUpdateResponseBody,
			&v1.UpdateInfo{
				Version:     "0.2.1",
				VersionCode: 4,
				UpdateType:  v1.UpdateType_UPDATE_TYPE_SOFT,
			},
		},
		{
			"forceUpdateInfoTanker",
			forceUpdateTankerResponseBody,
			&v1.UpdateInfo{
				Version:     "0.2.1",
				VersionCode: 4,
				UpdateType:  v1.UpdateType_UPDATE_TYPE_FORCE,
				Alert: &v1.Alert{
					Title:    "Доступно обновление!",
					Url:      "https://beta.ru/42",
					Subtitle: "Поздравляем!",
				},
			},
		},
	}
	transport := httpmock.NewMockTransport()
	client := buildClient(transport, t)
	for _, c := range testCases {
		t.Run(c.testName, func(t *testing.T) {
			transport.RegisterResponder("POST", "http://localhost/v1/configs",
				httpmock.ResponderFromResponse(mockResponseFromJSONString(200, c.jsonRsp)))
			ctx := context.Background()
			ctx = prepareLanguageContext(ctx, "ru-RU")
			res := client.GetUpdateInfoConfig(ctx)
			require.NotNil(t, res)
			assertProtoEquals(t, c.expected, res)
		})
	}
}

func TestGetRetryPolicyConfig(t *testing.T) {
	expected := &v1.RetryPolicyCfg{
		DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
			Path:       "/",
			PolicyId:   "4000_0_20000",
			ErrorCodes: []uint32{},
		},
		RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{
			&v1.RetryPolicyCfg_RequestInfo{
				Path:       "/api/hotels/v1/suggest",
				PolicyId:   "5000_1_1000",
				ErrorCodes: []uint32{429, 408},
			},
		},
		RetryPolicies: []*v1.RetryPolicyCfg_Policy{
			&v1.RetryPolicyCfg_Policy{
				Id:            "5000_1_1000",
				DelayMs:       5000,
				RetryAttempts: 1,
				TimeoutMs:     1000,
			},
			&v1.RetryPolicyCfg_Policy{
				Id:            "4000_0_20000",
				DelayMs:       4000,
				RetryAttempts: 0,
				TimeoutMs:     20000,
			},
		},
	}
	transport := httpmock.NewMockTransport()
	client := buildClient(transport, t)
	transport.RegisterResponder("POST", "http://localhost/v1/configs",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, retryPolicyResponseBody)))
	ctx := context.Background()
	ctx = prepareLanguageContext(ctx, "ru-RU")
	res := client.GetRetryPolicyConfig(ctx)
	require.NotNil(t, res)
	assertProtoEquals(t, expected, res)
}

func TestGetUserProfileConfig(t *testing.T) {
	expected := &v1.UserProfileConfig{
		Elements: []v1.UserProfileElement{
			v1.UserProfileElement_USER_PROFILE_ELEMENT_PLUS,
			v1.UserProfileElement_USER_PROFILE_ELEMENT_NOTIFICATIONS,
			v1.UserProfileElement_USER_PROFILE_ELEMENT_PASSENGERS,
			v1.UserProfileElement_USER_PROFILE_ELEMENT_SUPPORT_CHAT,
			v1.UserProfileElement_USER_PROFILE_ELEMENT_PROMOCODES,
			v1.UserProfileElement_USER_PROFILE_ELEMENT_PAYMENT_METHODS,
			v1.UserProfileElement_USER_PROFILE_ELEMENT_DARK_THEME,
			v1.UserProfileElement_USER_PROFILE_ELEMENT_EXIT,
		},
	}
	transport := httpmock.NewMockTransport()
	client := buildClient(transport, t)
	transport.RegisterResponder("POST", "http://localhost/v1/configs",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, userProfileConfig)))
	ctx := context.Background()
	ctx = prepareLanguageContext(ctx, "ru-RU")
	res := client.GetUserProfileConfig(ctx)
	require.NotNil(t, res)
	assertProtoEquals(t, expected, res)
}

func TestValidateRetryPolicy(t *testing.T) {
	transport := httpmock.NewMockTransport()
	client := buildClient(transport, t)
	ctx := context.Background()
	ctx = prepareLanguageContext(ctx, "ru-RU")
	t.Run("valid", func(t *testing.T) {
		cfg := &v1.RetryPolicyCfg{
			DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
				Path:       "/",
				PolicyId:   "4000_0_20000",
				ErrorCodes: []uint32{},
			},
			RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{
				&v1.RetryPolicyCfg_RequestInfo{
					Path:       "/api/hotels/v1/suggest",
					PolicyId:   "5000_1_1000",
					ErrorCodes: []uint32{429, 408},
				},
			},
			RetryPolicies: []*v1.RetryPolicyCfg_Policy{
				&v1.RetryPolicyCfg_Policy{
					Id:            "5000_1_1000",
					DelayMs:       5000,
					RetryAttempts: 1,
					TimeoutMs:     1000,
				},
				&v1.RetryPolicyCfg_Policy{
					Id:            "4000_0_20000",
					DelayMs:       4000,
					RetryAttempts: 0,
					TimeoutMs:     20000,
				},
			},
		}
		validated := client.validateRetryPolicy(ctx, cfg)
		assertProtoEquals(t, cfg, validated)
	})
	t.Run("valid", func(t *testing.T) {
		cfg := &v1.RetryPolicyCfg{
			DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
				Path:       "/",
				PolicyId:   "4000_0_20000",
				ErrorCodes: []uint32{},
			},
			RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{
				&v1.RetryPolicyCfg_RequestInfo{
					Path:       "/api/hotels/v1/suggest",
					PolicyId:   "5000_1_1000",
					ErrorCodes: []uint32{429, 408},
				},
			},
			RetryPolicies: []*v1.RetryPolicyCfg_Policy{
				&v1.RetryPolicyCfg_Policy{
					Id:            "5000_1_1000",
					DelayMs:       5000,
					RetryAttempts: 1,
					TimeoutMs:     1000,
				},
				&v1.RetryPolicyCfg_Policy{
					Id:            "4000_0_20000",
					DelayMs:       4000,
					RetryAttempts: 0,
					TimeoutMs:     20000,
				},
			},
		}
		validated := client.validateRetryPolicy(ctx, cfg)
		assertProtoEquals(t, cfg, validated)
	})
	t.Run("invalid default config", func(t *testing.T) {
		cfg := &v1.RetryPolicyCfg{
			DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
				Path:       "/",
				PolicyId:   "404",
				ErrorCodes: []uint32{},
			},
			RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{},
			RetryPolicies:          []*v1.RetryPolicyCfg_Policy{},
		}
		validated := client.validateRetryPolicy(ctx, cfg)
		assert.Nil(t, validated)
	})
	t.Run("invalid request config", func(t *testing.T) {
		source := &v1.RetryPolicyCfg{
			DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
				Path:       "/",
				PolicyId:   "4000_0_20000",
				ErrorCodes: []uint32{},
			},
			RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{
				&v1.RetryPolicyCfg_RequestInfo{
					Path:       "/api/hotels/v1/suggest",
					PolicyId:   "404",
					ErrorCodes: []uint32{429, 408},
				},
			},
			RetryPolicies: []*v1.RetryPolicyCfg_Policy{
				&v1.RetryPolicyCfg_Policy{
					Id:            "4000_0_20000",
					DelayMs:       4000,
					RetryAttempts: 0,
					TimeoutMs:     20000,
				},
			},
		}
		expected := &v1.RetryPolicyCfg{
			DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
				Path:       "/",
				PolicyId:   "4000_0_20000",
				ErrorCodes: []uint32{},
			},
			RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{},
			RetryPolicies: []*v1.RetryPolicyCfg_Policy{
				&v1.RetryPolicyCfg_Policy{
					Id:            "4000_0_20000",
					DelayMs:       4000,
					RetryAttempts: 0,
					TimeoutMs:     20000,
				},
			},
		}
		validated := client.validateRetryPolicy(ctx, source)
		assertProtoEquals(t, expected, validated)
	})
	t.Run("duplicate policy", func(t *testing.T) {
		source := &v1.RetryPolicyCfg{
			DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
				Path:       "/",
				PolicyId:   "4000_0_20000",
				ErrorCodes: []uint32{},
			},
			RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{
				&v1.RetryPolicyCfg_RequestInfo{
					Path:       "/api/hotels/v1/suggest",
					PolicyId:   "4000_0_20000",
					ErrorCodes: []uint32{429, 408},
				},
			},
			RetryPolicies: []*v1.RetryPolicyCfg_Policy{
				&v1.RetryPolicyCfg_Policy{
					Id:            "4000_0_20000",
					DelayMs:       4000,
					RetryAttempts: 0,
					TimeoutMs:     20000,
				},
				&v1.RetryPolicyCfg_Policy{
					Id:            "4000_0_20000",
					DelayMs:       4000,
					RetryAttempts: 0,
					TimeoutMs:     20000,
				},
			},
		}
		expected := &v1.RetryPolicyCfg{
			DefaultConfig: &v1.RetryPolicyCfg_RequestInfo{
				Path:       "/",
				PolicyId:   "4000_0_20000",
				ErrorCodes: []uint32{},
			},
			RequestsRetryPolicyCfg: []*v1.RetryPolicyCfg_RequestInfo{
				&v1.RetryPolicyCfg_RequestInfo{
					Path:       "/api/hotels/v1/suggest",
					PolicyId:   "4000_0_20000",
					ErrorCodes: []uint32{429, 408},
				},
			},
			RetryPolicies: []*v1.RetryPolicyCfg_Policy{
				&v1.RetryPolicyCfg_Policy{
					Id:            "4000_0_20000",
					DelayMs:       4000,
					RetryAttempts: 0,
					TimeoutMs:     20000,
				},
			},
		}
		validated := client.validateRetryPolicy(ctx, source)
		assertProtoEquals(t, expected, validated)
	})
}

func assertProtoEquals(t *testing.T, expected, actual proto.Message) {
	exp := protojson.Format(expected)
	act := protojson.Format(actual)
	assert.Equal(t, exp, act)
}

func TestTryLocalizeString(t *testing.T) {
	type tryLocalizeStringCase struct {
		name         string
		lang         string
		sourceString string
	}
	transport := httpmock.NewMockTransport()
	client := buildClient(transport, t)
	t.Run("ok", func(t *testing.T) {
		ok, res, err := client.tryLocalizeString("ru", "tanker:backend_config:update_available")
		assert.NoError(t, err)
		assert.True(t, ok)
		assert.Equal(t, "Доступно обновление!", res)
	})
	skipCases := []tryLocalizeStringCase{
		{
			"skipJustText",
			"ru",
			"это просто текст",
		},
		{
			"skipJustTextWithColon",
			"ru",
			"это:просто:текст",
		},
		{
			"skipJustTextStartsWithTanker",
			"ru",
			"tanker is the best",
		},
	}
	for _, c := range skipCases {
		t.Run(c.name, func(t *testing.T) {
			ok, _, err := client.tryLocalizeString(c.lang, c.sourceString)
			assert.NoError(t, err)
			assert.False(t, ok)
		})
	}
	errorCases := []tryLocalizeStringCase{
		{
			"tankerWrongFormat",
			"ru",
			"tanker:backend_config",
		},
		{
			"tankerWrongFormat2",
			"ru",
			"tanker:backend_config:update_available:что-то",
		},
		{
			"tankerKeySetNotFound",
			"ru",
			"tanker:ks404:key",
		},
		{
			"tankerKeySetNotFound",
			"ru",
			"tanker:ks404:update_available",
		},
		{
			"tankerKeyNotFound",
			"ru",
			"tanker:backend_config:key404",
		},
	}
	for _, c := range errorCases {
		t.Run(c.name, func(t *testing.T) {
			ok, _, err := client.tryLocalizeString(c.lang, c.sourceString)
			assert.Error(t, err)
			assert.False(t, ok)
		})
	}
}

func buildClient(transport http.RoundTripper, t *testing.T) *HTTPClient {
	client := resty.New()
	client.SetTransport(transport).SetScheme("http").SetBaseURL("localhost")
	return &HTTPClient{
		config:      DefaultConfig,
		httpClient:  client,
		logger:      NewLogger(t),
		metrics:     clientscommon.NewHTTPClientMetrics(nopmetrics.Registry{}, "exp3-matcher"),
		l10nService: &l10nServiceForTests{},
		geoBase:     &geobase.StubGeobase{},
	}
}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}

func NewLogger(t *testing.T) *yzap.Logger {
	return &yzap.Logger{L: zaptest.NewLogger(t, zaptest.Level(zap.DebugLevel))}
}

type l10nServiceForTests struct{}

func (*l10nServiceForTests) Get(keyset string, language string) (*l10n.Keyset, error) {
	if keyset == "backend_config" && language == "ru" {
		return &l10n.Keyset{
			Keys: map[string]string{
				"update_available": "Доступно обновление!",
				"congratulations":  "Поздравляем!",
			},
		}, nil
	}
	return nil, xerrors.Errorf("l10n for test: case not implemented")
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}
