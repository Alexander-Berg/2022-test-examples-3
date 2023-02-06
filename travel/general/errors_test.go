package pkg

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/xerrors"
)

func TestNewErrorWithHttpCode(t *testing.T) {
	err := NewErrorWithHTTPCode("error description", xerrors.New("some internal error"), 404)
	err = xerrors.Errorf("wrap into high-level error: %w", err)

	var httpError ErrorWithHTTPCode
	expect := assert.New(t)
	expect.True(xerrors.As(err, &httpError), "http error should be detected")
	expect.Equal("error description: some internal error", httpError.Error())
	expect.Equal(404, httpError.HTTPCode())

	assert.False(t, xerrors.As(xerrors.New("not an http error"), &httpError), "no validation error here")
}

func TestNewValidationError(t *testing.T) {
	err := NewValidationError(xerrors.New("some internal error"))
	err = xerrors.Errorf("wrap into high-level error: %w", err)

	var valErr ErrorWithHTTPCode
	assert.True(t, xerrors.As(err, &valErr), "validation error should be detected")
	assert.Equal(t, "validation error: some internal error", valErr.Error())

	assert.False(t, xerrors.As(xerrors.New("not a validation error"), &valErr), "no validation error here")
}

func TestNewDecodingError(t *testing.T) {
	err := NewDecodingError(xerrors.New("some internal error"))
	err = xerrors.Errorf("wrap into high-level error: %w", err)

	var decErr ErrorWithHTTPCode
	assert.True(t, xerrors.As(err, &decErr), "decoding error should be detected")
	assert.Equal(t, "decoding error: some internal error", decErr.Error())

	assert.False(t, xerrors.As(xerrors.New("not a decoding error"), &decErr), "no decoding error here")
}
