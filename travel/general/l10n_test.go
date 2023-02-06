package common

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"

	"a.yandex-team.ru/library/go/core/log/nop"
)

func Test_modifyAcceptLanguageIfNeeded_noMetadata_setDefault(t *testing.T) {
	ctx := setDefaultGrpcAcceptLanguageHeader(context.Background())
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		t.Errorf("missing metadata in context")
	}

	header, ok := md[acceptLanguageGrpcHeader]
	if !ok {
		t.Errorf("missing %s header in metadata", acceptLanguageGrpcHeader)
	}

	if len(header) != 1 {
		t.Errorf("no header value specified")
	}

	if header[0] != defaultAcceptLanguageHeaderValue {
		t.Errorf("header value should be %s, actual %s", defaultAcceptLanguageHeaderValue, header[0])
	}
}

func Test_modifyAcceptLanguageIfNeeded_incorrectHeaderValue_setDefault(t *testing.T) {
	ctx := context.Background()
	ctx = metadata.NewIncomingContext(ctx, metadata.New(map[string]string{acceptLanguageGrpcHeader: "en-En"}))
	ctx = setDefaultGrpcAcceptLanguageHeader(ctx)

	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		t.Errorf("missing metadata in context")
	}

	header, ok := md[acceptLanguageGrpcHeader]
	if !ok {
		t.Errorf("missing %s header in metadata", acceptLanguageGrpcHeader)
	}

	if len(header) < 1 {
		t.Errorf("no header value specified")
	}

	if header[0] != defaultAcceptLanguageHeaderValue {
		t.Errorf("header value should be %s, actual %s", defaultAcceptLanguageHeaderValue, header[0])
	}
}

func Test_validateAcceptLanguageHeader_emptyMetadata_returnError(t *testing.T) {
	ctx := context.Background()
	supportedLanguages := getSupportedLanguages()
	supportedCountries := getSupportedCountries()
	err := validateGrpcAcceptLanguageHeader(ctx, supportedLanguages, supportedCountries)

	if err == nil {
		t.Errorf("should check metadata")
	}

	if err.Error() != "empty metadata" {
		t.Errorf("incorrect error: %s", err.Error())
	}
}

func Test_validateAcceptLanguageHeader_missingHeader_returnError(t *testing.T) {
	ctx := context.Background()
	ctx = metadata.NewIncomingContext(ctx, metadata.MD{})
	supportedLanguages := getSupportedLanguages()
	supportedCountries := getSupportedCountries()
	err := validateGrpcAcceptLanguageHeader(ctx, supportedLanguages, supportedCountries)

	if err == nil {
		t.Errorf("should missing header")
	}

	if err.Error() != "missing Accept-Language header" {
		t.Errorf("incorrect error: %s", err.Error())
	}
}

func Test_validateAcceptLanguageHeader_unsupportedHeaderValue_returnError(t *testing.T) {
	for _, headerValue := range []string{"en-EN", "en-RU", "ru-EN"} {
		ctx := context.Background()
		ctx = metadata.NewIncomingContext(ctx, metadata.New(map[string]string{acceptLanguageGrpcHeader: headerValue}))
		supportedLanguages := getSupportedLanguages("ru")
		supportedCountries := getSupportedCountries("RU")
		err := validateGrpcAcceptLanguageHeader(ctx, supportedLanguages, supportedCountries)

		if err == nil {
			t.Errorf("should check header value")
		}

		if err.Error() != fmt.Sprintf("unsupported Accept-Language header value %s", headerValue) {
			t.Errorf("incorrect error: %s", err.Error())
		}
	}
}

func Test_validateAcceptLanguageHeader_emptyAcceptedLanguagesMap_returnError(t *testing.T) {
	ctx := context.Background()
	headerValue := "en-EN"
	ctx = metadata.NewIncomingContext(ctx, metadata.New(map[string]string{acceptLanguageGrpcHeader: headerValue}))
	supportedLanguages := getSupportedLanguages()
	supportedCountries := getSupportedCountries("RU")
	err := validateGrpcAcceptLanguageHeader(ctx, supportedLanguages, supportedCountries)

	if err == nil {
		t.Errorf("should check header value")
	}

	if err.Error() != fmt.Sprintf("unsupported Accept-Language header value %s", headerValue) {
		t.Errorf("incorrect error: %s", err.Error())
	}
}

func Test_validateAcceptLanguageHeader_nilAcceptedLanguagesMap_returnError(t *testing.T) {
	ctx := context.Background()
	headerValue := "en-EN"
	ctx = metadata.NewIncomingContext(ctx, metadata.New(map[string]string{acceptLanguageGrpcHeader: headerValue}))
	supportedCountries := getSupportedCountries("RU")
	err := validateGrpcAcceptLanguageHeader(ctx, nil, supportedCountries)

	if err == nil {
		t.Errorf("should check header value")
	}

	if err.Error() != fmt.Sprintf("unsupported Accept-Language header value %s", headerValue) {
		t.Errorf("incorrect error: %s", err.Error())
	}
}

func Test_validateAcceptLanguageHeader_parsingHeaderError_returnError(t *testing.T) {
	ctx := context.Background()
	ctx = metadata.NewIncomingContext(ctx, metadata.New(map[string]string{acceptLanguageGrpcHeader: "ru-ru"}))
	supportedLanguages := getSupportedLanguages()
	supportedCountries := getSupportedCountries("RU")
	err := validateGrpcAcceptLanguageHeader(ctx, supportedLanguages, supportedCountries)

	if err == nil {
		t.Errorf("should check header value")
	}

	expectedMsg := "invalid Accept-Language header ru-ru"
	if err.Error() != expectedMsg {
		t.Errorf("incorrect error: %s", err.Error())
	}
}

func Test_validateAcceptLanguageHeader_validHeader_returnNil(t *testing.T) {
	ctx := context.Background()
	ctx = metadata.NewIncomingContext(ctx, metadata.New(map[string]string{acceptLanguageGrpcHeader: "ru-RU"}))
	supportedLanguages := getSupportedLanguages("ru")
	supportedCountries := getSupportedCountries("RU")
	err := validateGrpcAcceptLanguageHeader(ctx, supportedLanguages, supportedCountries)

	if err != nil {
		t.Errorf("function should not return error")
	}
}

func TestNewGrpcL10NInterceptor(t *testing.T) {
	// Если проверки заголовка Accept-Language по языку или по стране провалятся,
	// то произойдет замена значения заголовка на ru-RU
	type TestCase struct {
		Title              string
		Header             string
		SupportedLanguages []string
		SupportedCountries []string
		ExpectedLanguage   string
		ExpectedCountry    string
	}
	testCases := []TestCase{
		{
			Title:              "Supported language, supported country",
			Header:             "en-GB",
			SupportedLanguages: []string{"en"},
			SupportedCountries: []string{"GB"},
			ExpectedLanguage:   "en",
			ExpectedCountry:    "GB",
		},
		{
			Title:              "Not supported language and not supported country",
			Header:             "en-GB",
			SupportedLanguages: []string{"fr"},
			SupportedCountries: []string{"FR"},
			ExpectedLanguage:   "ru",
			ExpectedCountry:    "RU",
		},
		{
			Title:              "Not supported language and supported country",
			Header:             "en-GB",
			SupportedLanguages: []string{"fr"},
			SupportedCountries: []string{"GB"},
			ExpectedLanguage:   "ru",
			ExpectedCountry:    "RU",
		},
		{
			Title:              "Supported language and not supported country",
			Header:             "en-GB",
			SupportedLanguages: []string{"en"},
			SupportedCountries: []string{"FR"},
			ExpectedLanguage:   "ru",
			ExpectedCountry:    "RU",
		},
	}
	for _, tc := range testCases {
		t.Run(tc.Title, func(t *testing.T) {
			ctx := context.Background()
			ctx = metadata.NewIncomingContext(ctx, metadata.New(map[string]string{acceptLanguageGrpcHeader: tc.Header}))
			// чтобы проверить интерцептор, определим ручку таким образом, чтобы она возвращала измененный контекст
			handler := func(ctx2 context.Context, req interface{}) (interface{}, error) {
				return ctx2, nil
			}
			interceptor := NewGrpcL10NInterceptor(&nop.Logger{}, tc.SupportedLanguages, tc.SupportedCountries)

			correctedCtx, err := interceptor(ctx, nil, nil, handler)

			require.NoError(t, err)
			require.EqualValues(t, Locale{
				Language:          tc.ExpectedLanguage,
				CountryCodeAlpha2: tc.ExpectedCountry,
			}, GetLocale(correctedCtx.(context.Context)))
		})
	}
}

func getSupportedLanguages(languages ...string) map[string]struct{} {
	m := make(map[string]struct{})
	for _, language := range languages {
		m[language] = struct{}{}
	}
	return m
}

func getSupportedCountries(countries ...string) map[string]struct{} {
	m := make(map[string]struct{})
	for _, language := range countries {
		m[language] = struct{}{}
	}
	return m
}
