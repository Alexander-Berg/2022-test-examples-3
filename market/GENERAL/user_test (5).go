package actions

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/market/logistics/wms/robokotov/internal/core/entities"
)

type UserTestSuite struct {
	ActionTestSuite
}

func (s *UserTestSuite) TestGetUsers() {
	expected := []entities.User{
		{
			UID:  uint64(123),
			Name: "hardcoded user name",
		},
		{
			UID:  uint64(456),
			Name: "hardcoded user name",
		},
	}
	res := make([]entities.User, 2)
	for i, user := range expected {
		u, err := GetUserAction(s.ctx, user.UID)
		if err != nil {
			s.T().Fatal(err)
		}
		res[i] = u
	}
	assert.Equal(s.T(), expected, res)
}

func TestUserTestSuite(t *testing.T) {
	suite.Run(t, new(UserTestSuite))
}
