
spi_transitions = {
    'noe': [dict(id='diagnostics'), dict(id='neo')],
    'diagnostics': [dict(id='resolved'), dict(id='serviceRestored'), dict(id='needInfo'), dict(id='inDevelopment')],
    'resolved': [dict(id='neo'), dict(id='serviceRestored'), dict(id='inDevelopment')],
    'serviceRestored': [dict(id='diagnostics'), dict(id='closed'), dict(id='inDevelopment')],
}


class Startrek():
    _spies = {}

    async def get_queue(self, key) -> dict | None:
        if key not in ('TEST', 'TESTSPI', 'NOCREQUESTS'):
            return None
        return ...

    async def set_priority(self, id, priority):
        ...

    async def get_transitions(self, key):
        return self._spies.get(key, {}).get('transitions', spi_transitions['noe'])

    async def set_status(self, key, status, **fields):
        data = dict(status=status, transitions=spi_transitions[status])
        self._spies[key] = data
