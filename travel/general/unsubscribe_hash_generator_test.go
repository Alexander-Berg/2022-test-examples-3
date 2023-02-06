package subscriptions

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestRandomSaltProvider(t *testing.T) {
	provider := &RandSaltProvider{}
	saltLength := 10

	salt := provider.GetSalt(saltLength)

	require.Equal(t, saltLength, len(salt))
}

type testSaltProvider struct{ salt string }

func (p *testSaltProvider) GetSalt(int) string {
	return p.salt
}

func TestMD5UnsubscribeHashGenerator(t *testing.T) {
	salt := "salt"
	secret := "secret"
	hashGenerator := NewMD5UnsubscribeHashGenerator(secret, &testSaltProvider{salt: salt}, 10)

	hash := hashGenerator.Generate("email@test.com")

	require.Equal(t, "c8763c789d482e32a652052fe5a073d2", hash)
}
