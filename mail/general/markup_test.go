package provider

import (
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/logic/models"
	"context"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"
	"testing"
)

type RequestMarkupTestSuite struct {
	suite.Suite
	ctx     context.Context
	sf      serviceFixture
	st      string
	pt      string
	spasibo string
	uid     uint64
	env     models.OrderEnvironment
}

func (s *RequestMarkupTestSuite) SetupTest() {
	s.ctx, s.sf = newPaymentServiceFixture(s.T())
	s.st = "service_token"
	s.pt = "purchase_token"
	s.spasibo = "100.00"
	s.uid = uint64(123456789)
	s.env = models.ProductionOrder
}

func (s *RequestMarkupTestSuite) callRequestMarkup() {
	err := s.sf.Service.RequestMarkup(s.ctx, s.uid, s.pt, s.spasibo, s.st, s.env)
	if err != nil {
		s.Fail("Unexpected RequestMarkup error", err)
	}
}

func (s *RequestMarkupTestSuite) TestWithServiceToken() {
	s.sf.TrustMock.EXPECT().RequestMarkup(gomock.Any(), s.pt, s.st, s.uid, s.spasibo).Times(1)
	s.callRequestMarkup()
}

func (s *RequestMarkupTestSuite) TestWithServiceTokenSandbox() {
	s.env = models.SandboxOrder
	s.sf.SandboxTrustMock.EXPECT().RequestMarkup(gomock.Any(), s.pt, s.st, s.uid, s.spasibo).Times(1)
	s.callRequestMarkup()
}

func (s *RequestMarkupTestSuite) TestWithoutServiceToken() {
	const defaultServiceToken = "default_service_token"
	s.sf.Config.DefaultAcquirer = "default"
	s.sf.Config.YaPaymentsServices = map[string]string{"default": defaultServiceToken}
	s.sf.TrustMock.EXPECT().RequestMarkup(gomock.Any(), s.pt, defaultServiceToken, s.uid, s.spasibo).Times(1)
	s.st = ""
	s.callRequestMarkup()
}

func (s *RequestMarkupTestSuite) TestWithoutServiceTokenSandbox() {
	s.env = models.SandboxOrder
	const sandboxServiceToken = "sandbox_service_token"
	s.sf.Config.SandboxServiceToken = sandboxServiceToken
	s.sf.SandboxTrustMock.EXPECT().RequestMarkup(gomock.Any(), s.pt, sandboxServiceToken, s.uid, s.spasibo).Times(1)
	s.st = ""
	s.callRequestMarkup()
}

func (s *RequestMarkupTestSuite) TestEmptyUID() {
	err := s.sf.Service.RequestMarkup(s.ctx, 0, s.pt, s.spasibo, s.st, s.env)
	s.Require().Error(err)
}

func TestRequestMarkupTestSuite(t *testing.T) {
	suite.Run(t, new(RequestMarkupTestSuite))
}
