package com.gom.bus_tracker.service;

import com.gom.bus_tracker.client.BusClient;
import com.gom.bus_tracker.dto.BusPositionDTO;
import com.gom.bus_tracker.dto.BusRawDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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

        List<BusRawDTO> rawData = busClient.getBusPositions(dataInicial, dataFinal);

        Map<String, BusPositionDTO> latestPerBus = rawData.stream()
                .map(bus -> new BusPositionDTO(
                        bus.getOrdem(),
                        bus.getLinha(),
                        convertCoordinate(bus.getLongitude()),
                        convertCoordinate(bus.getLatitude()),
                        bus.getDatahoraservidor()
                ))
                .filter(dto -> dto.datahoraservidor() != null && !dto.datahoraservidor().isBlank())
                .collect(Collectors.toMap(
                        BusPositionDTO::ordem,
                        dto -> dto,
                        (dto1, dto2) -> {
                            long t1 = parseTimestamp(dto1.datahoraservidor());
                            long t2 = parseTimestamp(dto2.datahoraservidor());
                            return t1 >= t2 ? dto1 : dto2;
                        }
                ));

        Map<String, List<BusPositionDTO>> byLine = latestPerBus.values().stream()
                .collect(Collectors.groupingBy(BusPositionDTO::linha));

        byLine.forEach((line, list) -> {
            String key = "bus-data::" + line;
            cacheManager.getCache("bus-data").put(key, list);
        });
    }

    private long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    public List<BusPositionDTO> getCurrentBusPositionsFromCache() {
        return cacheManager.getCache("bus-data")
                .get("bus-data", List.class);
    }

    public List<BusPositionDTO> getPositionByLine(String line) {
        String key = "bus-data::" + line;

        List<BusPositionDTO> cached = cacheManager.getCache("bus-data")
                .get(key, List.class);

        return cached != null ? cached :List.of();
    }

    @Scheduled(fixedRate = 30000)
    public void updatePeriodically() {
        updateCache();
    }
}
