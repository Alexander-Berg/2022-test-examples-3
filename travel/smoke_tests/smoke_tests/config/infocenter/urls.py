urls = [
    'ping'
]

urls_admin = [
    'ping',
    ['admin/info/330', {'code': 302, 'allow_redirects': False}],  # unauthorized - redirect
]
