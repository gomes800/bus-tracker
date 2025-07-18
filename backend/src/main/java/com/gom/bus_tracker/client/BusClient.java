package com.gom.bus_tracker.client;

import com.gom.bus_tracker.config.FeignConfig;
import com.gom.bus_tracker.dto.BusPositionDTO;
import com.gom.bus_tracker.dto.BusRawDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "busClient",
        url = "https://dados.mobilidade.rio",
        configuration = FeignConfig.class
)
public interface BusClient {

    @GetMapping("/gps/sppo")
    List<BusRawDTO> getBusPositions(
            @RequestParam("dataInicial") String dataIncial,
            @RequestParam("dataFinal") String dataFinal
    );
}
