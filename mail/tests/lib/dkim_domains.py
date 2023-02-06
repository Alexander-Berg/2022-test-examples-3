from typing import Optional


class DKIMDomain:
    def __init__(self, domain: str, selector: str, is_enabled=True, is_incorrect=False):
        self.domain = domain
        self.selector = selector
        self.is_enabled = is_enabled
        self.is_incorrect = is_incorrect


class DKIMDomains:
    def __init__(self):
        self._domains = {}

    def add(self, dkim_domain: DKIMDomain) -> None:
        self._domains[dkim_domain.domain] = dkim_domain

    def get(self, domain) -> Optional[DKIMDomain]:
        return self._domains.get(domain.lower())
