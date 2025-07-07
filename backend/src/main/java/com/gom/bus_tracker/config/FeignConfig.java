package com.gom.bus_tracker.config;

import feign.codec.Decoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

public class FeignConfig {
    @Bean
    public Decoder feignDecoder() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_HTML // <- isso é o que resolve o problema!
        ));

        HttpMessageConverters converters = new HttpMessageConverters(converter);
        return new ResponseEntityDecoder(new SpringDecoder(() -> converters));
    }
}
