from libc.stdint cimport uint64_t
from libcpp.string cimport string

cdef extern from "mail/furita/src/processor/verification_token.h" namespace "furita::processor" nogil:
    string make_token(uint64_t, uint64_t, string&);

def make_verification_token(rule_id, uid, mail_from):
    return make_token(rule_id, uid, mail_from)
