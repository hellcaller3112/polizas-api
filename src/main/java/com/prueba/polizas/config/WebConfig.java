package com.prueba.polizas.config;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.TipoPoliza;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite filtrar sin importar mayúsculas: ?tipo=Colectiva, ?tipo=colectiva o ?tipo=COLECTIVA.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, TipoPoliza.class, valor -> TipoPoliza.valueOf(valor.trim().toUpperCase()));
        registry.addConverter(String.class, EstadoPoliza.class, valor -> EstadoPoliza.valueOf(valor.trim().toUpperCase()));
    }
}
