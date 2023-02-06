import asyncio
import aiohttp
import async_timeout
import socket
import time


HOSTNAME = socket.gethostname()
version = "1603315443"
BACKPACK_LOCK_URL = f"http://localhost:4480/v1/slot/request?hostname={HOSTNAME}&timestamp={int(time.time())}&version={version}"
WAIT_SLOT_TIMEOUT = 10

async def gethttp_req(url):
    async with aiohttp.ClientSession() as session:
        with async_timeout.timeout(10):
            async with session.get(url) as response:
                return await response.json()

loop = asyncio.get_event_loop()

async def getlock():
    while True:
        try:
            print(f"Requesting: {BACKPACK_LOCK_URL}")
            data = await gethttp_req(BACKPACK_LOCK_URL)
            if data["status"] == "TRY_LATER":
                print(f"Cannot get lock. Retrying. Answer: {data}")
                await asyncio.sleep(WAIT_SLOT_TIMEOUT)
                continue
            break
        except Exception as e:
            print(f"Exception reached: {e}, wait {WAIT_SLOT_TIMEOUT} secs and continue")
            await asyncio.sleep(WAIT_SLOT_TIMEOUT)
    return data

data = loop.run_until_complete(getlock())
loop.close()

config = data["info"]["config"]
if data["status"] != "LOCK_ACQUIRED":
    version_new = data["info"]["version"]
    print(f"Version changed from meta server. Worker state: {data['status']} was: {version} now: {version_new}")
    version = version_new
else:
    print(f"Looks like it new run. Version: {version}")

print(f"Config is {config}")
