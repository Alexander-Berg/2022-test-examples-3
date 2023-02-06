
_CATEGORIES = {
    500: [
        'queue is full',
        'slave has been discarded',
        ('error: Service', 'has been disconnected'),
        'unistorage - INTERNAL_UNISTORAGE_ERROR: "server replied with 503 - "',
        'unistorage - Read timeout occurred in receive channel, timeout = 2000 ms',
        'unistorage - BACKEND_IO_ERROR: "file IO failure: no successful lookup result"',
        ('Stream ends prematurely at 0', 'Input/output error'),
        'Stream ends prematurely at 0',
        'Stream ends prematurely at ',
        ('error: unable to establish connection: ', 'Stream is closed')
    ],
    502: ['Bad Gateway', 'Backend unavailable'],
    504: ['Gateway Time-out', 'Backend timeout']
}


def categorize_response(code, message):
    if code == 200:
        return 'OK'

    for substrings in _CATEGORIES.get(code, []):
        substrings = (substrings,) if isinstance(substrings, basestring) else substrings
        if all(substring in message for substring in substrings):
            return ' '.join(substrings)

    return 'other'
