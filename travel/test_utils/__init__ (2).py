class MordaBackendClientStub:
    def __init__(self, keys=(), values=()):
        self.db = dict(zip(keys, values))

    async def get_transfers(self, point_from, point_to, when, transport_types, language):
        key = point_from, point_to, when.strftime('%Y-%m-%d'), tuple(transport_types), language
        return self.db.get(key)
