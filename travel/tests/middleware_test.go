package test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"a.yandex-team.ru/travel/library/go/tracing"
	"github.com/opentracing/opentracing-go/mocktracer"
	"github.com/stretchr/testify/assert"
)

func TestFilterPath(t *testing.T) {
	tr := mocktracer.New()
	m := tracing.NewTracingMiddlewareBuilder().
		WithFilter(tracing.NewPathFilter("/foo")).
		WithSpanCreater(tr).
		Build()
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {})
	r := httptest.NewRequest("GET", "http://test.com/foo", nil)
	w := httptest.NewRecorder()
	m.Handle(handler).ServeHTTP(w, r)
	assert.Equal(t, 0, len(tr.FinishedSpans()))
}

func TestExtractGetParamTag(t *testing.T) {
	tr := mocktracer.New()
	m := tracing.NewTracingMiddlewareBuilder().
		WithSpanCreater(tr).
		WithExtractor(tracing.NewGetParamTagExtractor("foo", "bar")).
		Build()
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {})
	r := httptest.NewRequest("GET", "http://test.com/foo?bar=0", nil)
	w := httptest.NewRecorder()
	m.Handle(handler).ServeHTTP(w, r)
	assert.Equal(t, 1, len(tr.FinishedSpans()))
	assert.Equal(t, 1, len(tr.FinishedSpans()[0].Tags()))
	assert.Equal(t, "0", tr.FinishedSpans()[0].Tag("foo"))
}

func TestExtractHeaderTag(t *testing.T) {
	tr := mocktracer.New()
	m := tracing.NewTracingMiddlewareBuilder().
		WithSpanCreater(tr).
		WithExtractor(tracing.NewHeaderTagExtractor("foo", "bar")).
		Build()
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {})
	r := httptest.NewRequest("GET", "http://test.com", nil)
	r.Header.Set("bar", "0")
	w := httptest.NewRecorder()
	m.Handle(handler).ServeHTTP(w, r)
	assert.Equal(t, 1, len(tr.FinishedSpans()))
	assert.Equal(t, 1, len(tr.FinishedSpans()[0].Tags()))
	assert.Equal(t, "0", tr.FinishedSpans()[0].Tag("foo"))
}

func TestFilterUnnecessaryPath(t *testing.T) {
	tr := mocktracer.New()
	m := tracing.NewTracingMiddlewareBuilder().
		WithFilter(tracing.NewPathFilter("/foo")).
		WithExtractor(tracing.NewHeaderTagExtractor("baz", "bar")).
		WithSpanCreater(tr).
		Build()
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {})
	r1 := httptest.NewRequest("GET", "http://test.com/foo", nil)
	r2 := httptest.NewRequest("GET", "http://test.com", nil)
	r2.Header.Set("bar", "0")
	w := httptest.NewRecorder()
	m.Handle(handler).ServeHTTP(w, r1)
	m.Handle(handler).ServeHTTP(w, r2)
	assert.Equal(t, 1, len(tr.FinishedSpans()))
	assert.Equal(t, 1, len(tr.FinishedSpans()[0].Tags()))
	assert.Equal(t, "0", tr.FinishedSpans()[0].Tag("baz"))
}
