package com.gom.bus_tracker.service;

import com.gom.bus_tracker.client.BusClient;
import com.gom.bus_tracker.dto.BusPositionDTO;
import com.gom.bus_tracker.dto.BusRawDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class BusService {

    @Autowired
    private BusClient busClient;

    @Autowired
    private RedisTemplate<String, BusPositionDTO> redisBusTemplate;

    @Autowired
    private RedisTemplate<String, String> redisStringTemplate;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MAX_AGE_MS = 2 * 60 * 1000;
    private static final String BUS_KEY_PREFIX = "bus::";
    private static final String LINE_KEY_PREFIX = "line-buses::";

    private String convertCoordinate(String coord) {
        return coord.replace(",", ".");
    }

    @Scheduled(fixedRate = 25000)
    public void updatePeriodically() {
        updateCache();
    }

    public void updateCache() {
        List<BusRawDTO> rawData = fetchRawBusData();
        processAndStoreBusData(rawData);
        System.out.println("Cache atualizado.");
    }

    private List<BusRawDTO> fetchRawBusData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime secondsAgo = now.minusSeconds(120);

        String dataInicial = secondsAgo.format(formatter);
        String dataFinal = now.format(formatter);

        return busClient.getBusPositions(dataInicial, dataFinal);
    }

    private void processAndStoreBusData(List<BusRawDTO> rawData) {
        Map<String, BusPositionDTO> latestBusMap = new HashMap<>();

        for (BusRawDTO bus : rawData) {
            String ordem = bus.getOrdem();
            BusPositionDTO newPosition = new BusPositionDTO(
                    ordem,
                    bus.getLinha(),
                    convertCoordinate(bus.getLongitude()),
                    convertCoordinate(bus.getLatitude()),
                    bus.getDatahoraservidor()
            );

            BusPositionDTO existing = latestBusMap.get(ordem);
            if (existing == null || isNewer(newPosition, existing)) {
                latestBusMap.put(ordem, newPosition);
            }
        }

        for (BusPositionDTO bus : latestBusMap.values()) {
            updateBusPosition(bus);
        }
    }

    private void updateBusPosition(BusPositionDTO newPosition) {
        String busKey = BUS_KEY_PREFIX + newPosition.ordem();
        String lineKey = LINE_KEY_PREFIX + newPosition.linha();

        BusPositionDTO existing = redisBusTemplate.opsForValue().get(busKey);

        if (existing == null || isNewer(newPosition, existing)) {
            redisBusTemplate.opsForValue().set(busKey, newPosition, MAX_AGE_MS, TimeUnit.MILLISECONDS);
            redisStringTemplate.opsForSet().add(lineKey, newPosition.ordem());
            redisStringTemplate.expire(lineKey, MAX_AGE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private boolean isNewer(BusPositionDTO newBus, BusPositionDTO existingBus) {
        long newTs = parseTimestamp(newBus.datahoraservidor());
        long existingTs = parseTimestamp(existingBus.datahoraservidor());
        return newTs > existingTs;
    }

    private boolean isRecent(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            return System.currentTimeMillis() - ts <= MAX_AGE_MS;
        } catch (Exception e) {
            return false;
        }
    }

    private long parseTimestamp(String ts) {
        try {
            return Long.parseLong(ts);
        } catch (Exception e) {
            return 0L;
        }
    }

    public List<BusPositionDTO> getPositionByLine(String line) {
        String lineKey = LINE_KEY_PREFIX + line;
        Set<String> busOrders = redisStringTemplate.opsForSet().members(lineKey);

        if (busOrders == null || busOrders.isEmpty()) {
            return Collections.emptyList();
        }

        List<BusPositionDTO> positions = new ArrayList<>();
        for (String ordem : busOrders) {
            BusPositionDTO bus = redisBusTemplate.opsForValue().get(BUS_KEY_PREFIX + ordem);
            if (bus != null && isRecent(bus.datahoraservidor())) {
                positions.add(bus);
            }
        }

        return positions;
    }
}
