class MyBean(object):

    def __getitem__(self, key):
        return self.square(key)

    def square(self, value):
        return value**2


Bean = MyBean
