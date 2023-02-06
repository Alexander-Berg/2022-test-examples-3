package toolkit;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import retrofit2.Retrofit;
import toolkit.converter_factory.ConverterFactory;

public enum Retrofits {

    RETROFIT {
        private final boolean cacheON = false;

        @Override
        public synchronized Retrofit getRetrofit(String baseUrl) {
            return builder
                    .baseUrl(baseUrl)
                    .client(new OkClient(cacheON).getOkHttpClient())
                    .addConverterFactory(ConverterFactory.create(Mapper.getDefaultMapper()))
                    .build();
        }

        @Override
        public boolean getCache() {
            return cacheON;
        }
    },
    RETROFIT_LMS {
        private final boolean cacheON = false;

        @Override
        public synchronized Retrofit getRetrofit(String baseUrl) {
            return builder
                .baseUrl(baseUrl)
                .client(new OkClient(cacheON).getOkHttpClient())
                .addConverterFactory(ConverterFactory.create(Mapper.getLmsMapper()))
                .build();
        }

        @Override
        public boolean getCache() {
            return cacheON;
        }
    },
    RETROFIT_XML {
        private final boolean cacheON = false;

        @Override
        public synchronized Retrofit getRetrofit(String baseUrl) {
            return builder
                    .baseUrl(baseUrl)
                    .client(new OkClient(cacheON).getOkHttpClient())
                    .addConverterFactory(ConverterFactory.create(new XmlMapper()))
                    .build();
        }

        @Override
        public boolean getCache() {
            return cacheON;
        }
    },
    RETROFIT_WITH_CACHE {
        private final boolean cacheON = true;

        @Override
        public synchronized Retrofit getRetrofit(String baseUrl) {
            return builder
                    .baseUrl(baseUrl)
                    .client(new OkClient(cacheON).getOkHttpClient())
                    .addConverterFactory(ConverterFactory.create(Mapper.getDefaultMapper()))
                    .build();
        }

        @Override
        public boolean getCache() {
            return cacheON;
        }
    };

    final Retrofit.Builder builder = new Retrofit.Builder();

    public abstract Retrofit getRetrofit(String baseUrl);

    public abstract boolean getCache();
}
