class Label(object):
    class Item():
        def __init__(self, alias, name):
            self.Name = name
            self.Alias = alias
            self.FullAlias = f"SystMetkaSO:{alias}"

    GREETING = Item("greeting", "12")
    PROMO = Item("promo", "66")
    PERSONAL = Item("personal", "65")
    PF_HAM = Item("pf_ham", "77")
    PF_SPAM = Item("pf_spam", "78")
    MT_NEWS = Item("news", "13")
