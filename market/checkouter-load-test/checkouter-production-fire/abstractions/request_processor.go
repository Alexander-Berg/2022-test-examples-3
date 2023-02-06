package abstractions

import (
	"net/http"
)

type BodyProcessor func([]byte, interface{}) error

type RequestProcessor interface {
	SendRequest(label string, shootContext ShootContext, req *http.Request, request interface{}, result interface{}, logFullResponse bool, processor BodyProcessor) error
}
