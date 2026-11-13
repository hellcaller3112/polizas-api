package com.prueba.polizas.config;

import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.TipoPoliza;
import com.prueba.polizas.repository.PolizaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga datos de ejemplo al iniciar. Se construyen con los mismos métodos de dominio
 * que usa la API, por lo que respetan todas las reglas de negocio.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final PolizaRepository polizaRepository;

    public DataLoader(PolizaRepository polizaRepository) {
        this.polizaRepository = polizaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);

        // id 1 - Individual activa: canon 1.000.000 x 12 meses = prima 12.000.000
        Poliza individual = Poliza.crear("IND-0001", TipoPoliza.INDIVIDUAL, "Carlos Pérez", inicio, 12);
        individual.agregarRiesgo("Calle 100 # 15-20 Apto 301, Bogotá", "Carlos Pérez", "María Gómez",
                new BigDecimal("1000000"));

        // id 2 - Colectiva activa con 3 riesgos: canon total 4.600.000 x 12 = prima 55.200.000
        Poliza colectiva = Poliza.crear("COL-0001", TipoPoliza.COLECTIVA, "Inmobiliaria Los Andes S.A.S.", inicio, 12);
        colectiva.agregarRiesgo("Carrera 7 # 72-10 Apto 502, Bogotá", "Laura Rojas", "Jorge Torres",
                new BigDecimal("1500000"));
        colectiva.agregarRiesgo("Calle 26 # 68-35 Apto 1104, Bogotá", "Andrés Díaz", "Patricia León",
                new BigDecimal("2200000"));
        colectiva.agregarRiesgo("Avenida 19 # 120-45 Apto 201, Bogotá", "Sofía Méndez", "Ricardo Silva",
                new BigDecimal("900000"));

        // id 3 - Colectiva cancelada (sirve para probar que no se puede renovar)
        Poliza cancelada = Poliza.crear("COL-0002", TipoPoliza.COLECTIVA,
                "Administración Edificio Torres del Parque", inicio.minusMonths(6), 12);
        cancelada.agregarRiesgo("Carrera 5 # 26-40 Apto 1503, Bogotá", "Diego Castro", "Elena Vargas",
                new BigDecimal("1800000"));
        cancelada.agregarRiesgo("Carrera 5 # 26-40 Apto 904, Bogotá", "Valentina Ruiz", "Hernán Mora",
                new BigDecimal("1650000"));
        cancelada.cancelar();

        // id 4 - Individual activa a 6 meses: canon 2.500.000 x 6 = prima 15.000.000
        Poliza individualSemestral = Poliza.crear("IND-0002", TipoPoliza.INDIVIDUAL, "Natalia Herrera", inicio, 6);
        individualSemestral.agregarRiesgo("Calle 93 # 11-26 Apto 702, Bogotá", "Natalia Herrera", "Felipe Ortiz",
                new BigDecimal("2500000"));

        polizaRepository.save(individual);
        polizaRepository.save(colectiva);
        polizaRepository.save(cancelada);
        polizaRepository.save(individualSemestral);

        log.info("Datos de ejemplo cargados: {} pólizas", polizaRepository.count());
    }
}
