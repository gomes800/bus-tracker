package com.gom.bus_tracker.dto;

import java.io.Serializable;

public record BusPositionDTO(
        String ordem,
        String linha,
        String longitude,
        String latitude,
        String datahoraservidor
) implements Serializable {}



