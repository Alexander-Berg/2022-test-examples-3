package secret_test

import (
	"testing"

	"github.yandex-team.ru/telepath/telepath/core/secret"
	. "gopkg.in/check.v1"
)

func Test(t *testing.T) { TestingT(t) }

type BoxSuite struct {
	box secret.Box
}

type StripSuite struct{}

var (
	_ = Suite(&BoxSuite{})
	_ = Suite(&StripSuite{})
)

func (s *BoxSuite) SetUpSuite(c *C) {
	s.box = secret.NewBox("secret")
}

func (s *BoxSuite) Test(c *C) {
	v := secret.Data("Must be the same after decryption")

	v2 := s.box.Encrypt(v)
	c.Assert(v2, Not(DeepEquals), v)

	v3, ok := s.box.Decrypt(v2)
	c.Assert(ok, Equals, true)
	c.Assert(v3, DeepEquals, v)
}

var StripTests = []struct {
	A, B []byte
}{
	{[]byte{0, 0, 0, 0}, []byte{}},
	{[]byte{1, 0, 0, 0}, []byte{1}},
	{[]byte{1, 0, 2, 0}, []byte{1, 0, 2}},
	{[]byte{1, 0, 2, 3}, []byte{1, 0, 2, 3}},
}

func (*StripSuite) Test(c *C) {
	for _, tt := range StripTests {
		c.Assert(secret.Strip(tt.A), DeepEquals, secret.Data(tt.B))
	}
}
