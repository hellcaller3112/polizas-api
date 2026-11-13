package com.prueba.polizas.repository;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.TipoPoliza;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolizaRepository extends JpaRepository<Poliza, Long> {

    /**
     * Lista pólizas filtrando por tipo y/o estado (ambos opcionales).
     * El EntityGraph carga los riesgos en la misma consulta para evitar el problema N+1.
     */
    @EntityGraph(attributePaths = "riesgos")
    @Query("""
            select p from Poliza p
            where (:tipo is null or p.tipo = :tipo)
              and (:estado is null or p.estado = :estado)
            order by p.id
            """)
    List<Poliza> buscar(@Param("tipo") TipoPoliza tipo, @Param("estado") EstadoPoliza estado);
}
