package ru.yandex.canvas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.canvas.model.stillage.StillageInfoConverter;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.service.video.files.StockMoviesService;

@Configuration
public class VideoFilesServiceConfig {
    @Bean
    public MovieAndVideoSourceFactory movieAndVideoSourceFactory() {
        return new MovieAndVideoSourceFactory(new StillageInfoConverter(new ObjectMapper()));
    }
    @Bean
    public StockMoviesService mergedFilesStockService(MovieAndVideoSourceFactory movieAndVideoSourceFactory) {
        return new StockMoviesService(null, movieAndVideoSourceFactory);
    }
}
