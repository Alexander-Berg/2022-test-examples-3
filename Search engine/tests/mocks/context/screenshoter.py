class Screenshoter:
    async def fetch_image(self, url: str):
        return bytes(url, 'utf8'), 'png'
