package debugmessages

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestDebugMessages(t *testing.T) {

	mh := NewHolder()
	testMessage := "error while searching path"
	mh.AddMessage(DomainRoute, testMessage)

	expect := Messages{Message(testMessage)}
	messages := mh.GetRouteMessages()
	require.Equal(t, expect, messages)

}
