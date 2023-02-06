package auth

import (
	"context"
	"net/http"
	"testing"

	"a.yandex-team.ru/library/go/yandex/tvm"
)

func TestOAuth(t *testing.T) {
	oauth := OAuth("test")
	req, err := http.NewRequest(http.MethodGet, "/", nil)
	if err != nil {
		t.Fatal("Error:", err)
	}
	if err := oauth.UpdateRequest(req); err != nil {
		t.Fatal("Error:", err)
	}
	if h := req.Header.Get("Authorization"); h != "OAuth test" {
		t.Fatalf("Expected %q, got %q", "OAuth test", h)
	}
}

type stubTvm struct{}

func (t stubTvm) GetServiceTicketForAlias(ctx context.Context, alias string) (string, error) {
	return alias, nil
}

func (t stubTvm) GetServiceTicketForID(ctx context.Context, dstID tvm.ClientID) (string, error) {
	panic("not implemented")
}

func (t stubTvm) CheckServiceTicket(ctx context.Context, ticket string) (*tvm.CheckedServiceTicket, error) {
	panic("not implemented")
}

func (t stubTvm) CheckUserTicket(ctx context.Context, ticket string, opts ...tvm.CheckUserTicketOption) (*tvm.CheckedUserTicket, error) {
	panic("not implemented")
}

func (t stubTvm) GetRoles(ctx context.Context) (*tvm.Roles, error) {
	panic("not implemented")
}

func (t stubTvm) GetStatus(ctx context.Context) (tvm.ClientStatusInfo, error) {
	panic("not implemented")
}

func TestServiceTicket(t *testing.T) {
	ticket := ServiceTicket{
		TVM:    stubTvm{},
		Target: "test_target",
	}
	req, err := http.NewRequest(http.MethodGet, "/", nil)
	if err != nil {
		t.Fatal("Error:", err)
	}
	if err := ticket.UpdateRequest(req); err != nil {
		t.Fatal("Error:", err)
	}
	if h := req.Header.Get("X-Ya-Service-Ticket"); h != "test_target" {
		t.Fatalf("Expected %q, got %q", "test_target", h)
	}
}
