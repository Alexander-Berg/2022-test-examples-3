from ymod_python_sys import deadline_timer

def cb2a(ident):
    if ident >= 10:
        exit()
    print({"t" : ident, "s": 42, "garbage": cb2a})
    deadline_timer(ident+1, 100, cb2a)

deadline_timer(0, 100, cb2a)
