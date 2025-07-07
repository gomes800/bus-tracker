package com.gom.bus_tracker.service;

import com.gom.bus_tracker.client.BusClient;
import com.gom.bus_tracker.dto.BusPositionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BusService {

    @Autowired
    private BusClient busClient;

    @Autowired
    private CacheManager cacheManager;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String convertCoordinate(String coord) {
        return coord.replace(",", ".");
    }


    public void updateCache() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime secondsAgo = now.minusSeconds(15);

        String dataInicial = secondsAgo.format(formatter);
        String dataFinal = now.format(formatter);

        List<BusPositionDTO> newData = busClient.getBusPositions(dataInicial, dataFinal).stream()
                .map(bus -> new BusPositionDTO(
                        bus.ordem(),
                        bus.linha(),
                        convertCoordinate(bus.longitude()),
                        convertCoordinate(bus.latitude())
                ))
                .collect(Collectors.toList());

        cacheManager.getCache("bus-data").put("bus-data", newData);

    }

    public List<BusPositionDTO> getCurrentBusPositionsFromCache() {
        return cacheManager.getCache("bus-data")
                .get("bus-data", List.class);
    }

    @Scheduled(fixedRate = 30000)
    public void updatePeriodically() {
        updateCache();
    }
}
