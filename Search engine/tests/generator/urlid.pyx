from util.generic.string cimport TString, TStringBuf
from util.system.types cimport i64, ui64, ui32

cdef extern from "<kernel/urlid/doc_route.h>" nogil:
    cdef cppclass TDocRoute:
        TDocRoute()
        TDocRoute(i64 raw)

cdef extern from "<kernel/urlid/doc_handle.h>" namespace "TDocHandle" nogil:
    cdef enum EFormat:
        AppendIndGeneration = 1 << 0
        PrintRoute = 1 << 1
        PrintZDocId = 1 << 2
        OmitZ = 1 << 3
        PrintClientDocId = 1 << 4 | PrintRoute
        PrintAll = PrintRoute | AppendIndGeneration
        PrintHashOnly = 0
        PrintZDocIdOmitZ = PrintZDocId | OmitZ
        PrintClientDocIdWithIndex = PrintClientDocId | AppendIndGeneration

cdef extern from "<kernel/urlid/doc_handle.h>" nogil:
    cdef cppclass TDocHandle:
        TDocHandle()
        TDocHandle(ui64 docHash, TDocRoute docRoute, ui32 indGen)
        TString ToString(EFormat format) const

def BinaryDocIdToString(docHash, docRoute):
    cdef TDocRoute route = TDocRoute(docRoute)
    cdef TDocHandle r = TDocHandle(docHash, route, 0)
    return r.ToString(EFormat.PrintRoute).decode('ascii')
