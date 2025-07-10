package com.gom.bus_tracker.service;

import com.gom.bus_tracker.client.BusClient;
import com.gom.bus_tracker.dto.BusPositionDTO;
import com.gom.bus_tracker.dto.BusRawDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final long MAX_AGE_MS = 2 * 60 * 1000;

    private String convertCoordinate(String coord) {
        return coord.replace(",", ".");
    }

    @Scheduled(fixedRate = 25000)
    public void updatePeriodically() {
        updateCache();
    }

    public void updateCache() {
        List<BusRawDTO> rawData = fetchRawBusData();
        Map<String, List<BusPositionDTO>> groupedNewData = groupNewBusDataByLine(rawData);
        groupedNewData.forEach(this::mergeAndStoreLineData);
        System.out.println("Cache atualizado.");
    }

    private List<BusRawDTO> fetchRawBusData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime secondsAgo = now.minusSeconds(30);

        String dataInicial = secondsAgo.format(formatter);
        String dataFinal = now.format(formatter);

        return busClient.getBusPositions(dataInicial, dataFinal);
    }

    private Map<String, List<BusPositionDTO>> groupNewBusDataByLine(List<BusRawDTO> rawData) {
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
                        this::mostRecent
                ));

        return latestPerBus.values().stream()
                .collect(Collectors.groupingBy(BusPositionDTO::linha));
    }

    private void mergeAndStoreLineData(String line, List<BusPositionDTO> newList) {
        String key = "bus-data::" + line;
        List<BusPositionDTO> oldList = getCachedLineData(key);

        Map<String, BusPositionDTO> merged = new HashMap<>();

        for (BusPositionDTO dto : oldList) {
            if (isRecent(dto.datahoraservidor())) {
                merged.put(dto.ordem(), dto);
            }
        }

        for (BusPositionDTO dto : newList) {
            merged.put(dto.ordem(), dto);
        }

        cacheManager.getCache("bus-data").put(key, new ArrayList<>(merged.values()));
    }

    private List<BusPositionDTO> getCachedLineData(String key) {
        List<BusPositionDTO> list = cacheManager.getCache("bus-data").get(key, List.class);
        if (list == null) return List.of();
        return list.stream()
                .filter(item -> item instanceof BusPositionDTO)
                .map(item -> (BusPositionDTO) item)
                .toList();
    }

    private boolean isRecent(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            return System.currentTimeMillis() - ts <= MAX_AGE_MS;
        } catch (Exception e) {
            return false;
        }
    }

    private BusPositionDTO mostRecent(BusPositionDTO dto1, BusPositionDTO dto2) {
        long t1 = parseTimestamp(dto1.datahoraservidor());
        long t2 = parseTimestamp(dto2.datahoraservidor());
        return t1 >= t2 ? dto1 : dto2;
    }

    private long parseTimestamp(String ts) {
        try {
            return Long.parseLong(ts);
        } catch (Exception e) {
            return 0L;
        }
    }

    public List<BusPositionDTO> getPositionByLine(String line) {
        String key = "bus-data::" + line;
        List<BusPositionDTO> cached = cacheManager.getCache("bus-data")
                .get(key, List.class);

        if (cached == null) {
            return List.of();
        }

        return cached.stream()
                .filter(item -> item instanceof BusPositionDTO)
                .map(item -> (BusPositionDTO) item)
                .toList();
    }
}
