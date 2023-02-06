class NslsResponse:
    def __init__(self, code, error):
        self.code = code
        self.error = error


class NslsResponses(object):
    OK = NslsResponse(250, b"Ok")
    HTTP_OK = NslsResponse(200, b"Ok")
    ERR_451 = NslsResponse(451, b"4.5.0 Sorry, service unavailable")
