package abstractions

type YQL interface {
	ExecuteQuery(query string, requestProcessor RequestProcessor, ctx ShootContext, token string) (*[]ResultRow, error)
	GetOperations(requestProcessor RequestProcessor, ctx ShootContext, token string) (*[]OperationDto, error)
	PostOperation(body []byte, requestProcessor RequestProcessor, ctx ShootContext, token string) (*OperationDto, error)
	GetOperation(id string, requestProcessor RequestProcessor, ctx ShootContext, token string) (*OperationDto, error)
	GetOperationData(id string, requestProcessor RequestProcessor, ctx ShootContext, token string) (*[]ResultRow, error)
}

type ResultRow struct {
	Values map[string]interface{}
}

type QueryRequest struct {
	Type    string `json:"type"`
	Action  string `json:"action"`
	Content string `json:"content"`
}

type OperationDto struct {
	ID     string `json:"id"`
	Status string `json:"status"`
}
