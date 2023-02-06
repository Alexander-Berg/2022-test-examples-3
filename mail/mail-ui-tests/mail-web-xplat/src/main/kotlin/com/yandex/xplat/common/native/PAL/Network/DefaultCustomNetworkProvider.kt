package native.PAL.Network

import com.yandex.xplat.common.*
import okhttp3.HttpUrl

class DefaultCustomNetworkProvider(private val networkConfig: NetworkConfig) : CustomNetworkProvider {
    override fun provideNetwork(baseUrl: String): Network {
        return DefaultNetwork(HttpUrl.parse(baseUrl)!!.url(), networkConfig, DefaultJSONSerializer())
    }
}
