package com.gom.bus_tracker.service;

import com.gom.bus_tracker.client.BusClient;
import com.gom.bus_tracker.dto.BusPositionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BusService {

    @Autowired
    private BusClient busClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String convertCoordinate(String coord) {
        return coord.replace(",", ".");
    }

    public List<BusPositionDTO> getCurrentBusPositions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime secondsAgo = now.minusSeconds(15);

        String dataInicial = secondsAgo.format(formatter);
        String dataFinal = now.format(formatter);

        return busClient.getBusPositions(dataInicial, dataFinal).stream()
                .map(bus -> new BusPositionDTO(
                        bus.ordem(),
                        bus.linha(),
                        convertCoordinate(bus.longitude()),
                        convertCoordinate(bus.latitude())
                ))
                .collect(Collectors.toList());
    }
}
